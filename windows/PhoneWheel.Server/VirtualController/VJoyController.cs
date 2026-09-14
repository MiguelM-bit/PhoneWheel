using System;
using CoreDX.vJoy.Wrapper;
using PhoneWheel.Server.Models;

namespace PhoneWheel.Server.VirtualController;

/// <summary>
/// Implementação de IVirtualController usando vJoy.
///
/// Responsabilidades:
/// - Conectar ao dispositivo virtual vJoy
/// - Assumir o dispositivo
/// - Enviar valores de direção (eixo Z)
/// - Liberar o dispositivo ao desconectar
///
/// Esta classe é específica da implementação vJoy e não deve ser
/// usada diretamente pelo resto da aplicação. Use IVirtualController.
///
/// Notas:
/// - vJoy deve estar instalado no sistema
/// - Pelo menos um dispositivo virtual deve estar criado
/// - Este código assume o dispositivo 1 por padrão
/// - Sem o driver instalado, Connect() lança VirtualControllerException
///   com uma mensagem clara; o servidor continua funcionando sem vJoy.
/// </summary>
public class VJoyController : IVirtualController
{
    private readonly uint _deviceId;
        private readonly object _lock = new();
        private bool _connected;
    private bool _disposed;
    private VirtualControllerStatus _status;
    private double _lastSteeringValue = double.NaN;

    private VJoyControllerManager? _manager;
    private IVJoyController? _controller;

    // Valores do enum VjdStat do SDK vJoy (retornado como object pelo wrapper).
    private const int VjdStatOwn = 0;
    private const int VjdStatFree = 1;
    private const int VjdStatBusy = 2;
    private const int VjdStatMiss = 3;

    /// <summary>
    /// Inicializa a implementação vJoy.
    /// </summary>
    /// <param name="deviceId">
    /// ID do dispositivo vJoy (padrão: 1).
    /// Deve ser um dispositivo virtual existente.
    /// </param>
    public VJoyController(uint deviceId = 1)
    {
        _deviceId = deviceId;
        _connected = false;
        _status = VirtualControllerStatus.Disconnected;
    }

    public VirtualControllerStatus Status
        {
            get
            {
                lock (_lock)
                {
                    return _status;
                }
            }
        }

        public void Connect()
        {
            lock (_lock)
            {
                if (_disposed)
                {
                    throw new ObjectDisposedException(nameof(VJoyController));
                }

                if (_connected)
                {
                    return; // Já conectado
            }

                try
            {
                    _manager = VJoyControllerManager.GetManager();

                    if (!VJoyControllerManager.IsDriverLoaded)
                    {
                        throw new VirtualControllerException(
                            "Driver vJoy não está instalado. Instale o vJoy " +
                            "(https://sourceforge.net/projects/vjoystick/) e crie um dispositivo virtual.");
                    }

                    if (!_manager.IsVJoyEnabled)
                    {
                        throw new VirtualControllerException(
                            "vJoy está instalado, mas nenhum dispositivo está habilitado. " +
                            "Abra o 'Configure vJoy' e habilite pelo menos um dispositivo.");
                    }

                    var status = _manager.GetVJDStatus(_deviceId);
                    var statusValue = status != null ? Convert.ToInt32(status) : VjdStatMiss;

                    if (statusValue == VjdStatMiss)
                    {
                        throw new VirtualControllerException(
                            $"Dispositivo vJoy {_deviceId} não existe. " +
                            "Crie-o no 'Configure vJoy'.");
                    }

                    if (statusValue == VjdStatBusy)
                    {
                        throw new VirtualControllerException(
                            $"Dispositivo vJoy {_deviceId} está em uso por outro aplicativo.");
                    }

                    _controller = _manager.AcquireController(_deviceId);
                    if (_controller == null)
                    {
                        throw new VirtualControllerException(
                            $"Falha ao adquirir o dispositivo vJoy {_deviceId}.");
                    }

                    if (!_controller.HasAxisZ)
                    {
                        _manager.RelinquishController(_controller);
                        _controller = null;
                        throw new VirtualControllerException(
                                        $"Dispositivo vJoy {_deviceId} não possui eixo Z. " +
                                        "Habilite o eixo Z no 'Configure vJoy'.");
                    }

                    _connected = true;
                    _status = VirtualControllerStatus.Connected;
                }
                catch (VirtualControllerException)
                {
                    _status = VirtualControllerStatus.Error;
                    throw;
                }
                catch (Exception ex)
                {
                    _status = VirtualControllerStatus.Error;
                    throw new VirtualControllerException(
                        $"Erro ao conectar ao vJoy: {ex.Message}", ex);
                }
            }
        }

    public void SetSteering(double value)
    {
            lock (_lock)
        {
                if (_disposed)
                {
                    throw new ObjectDisposedException(nameof(VJoyController));
                }

                if (!_connected)
                {
                    throw new VirtualControllerException(
                        "Não conectado ao dispositivo vJoy. Chame Connect() primeiro.");
                }

                // Validar valor normalizado
                if (double.IsNaN(value) || double.IsInfinity(value))
                {
                    throw new VirtualControllerException(
                        $"Valor de direção inválido: {value}");
                }

                // Limitar a [-1.0, 1.0]
                var clampedValue = Math.Clamp(value, -1.0, 1.0);

                // Evitar atualizações desnecessárias
                if (Math.Abs(_lastSteeringValue - clampedValue) < 0.0001)
                {
                    return;
                }

                try
                {
                    // Mapear [-1.0, 1.0] para [0, max]
                    // -1.0 → 0
                    //  0.0 → max / 2
                    // +1.0 → max
                    var max = _controller?.AxisMaxValue ?? 32767;
                    var center = max / 2.0;
                    var range = max / 2.0;
                    var mappedValue = (int)Math.Clamp(center + (clampedValue * range), 0, max);

                    if (_controller == null || !_controller.SetAxisZ(mappedValue))
                    {
                        _status = VirtualControllerStatus.Error;
                        throw new VirtualControllerException(
                                        $"Falha ao enviar valor ao eixo Z do vJoy {_deviceId}.");
                    }

                    _lastSteeringValue = clampedValue;
                }
                catch (VirtualControllerException)
                {
                    throw;
                }
                catch (Exception ex)
                {
                    _status = VirtualControllerStatus.Error;
                    throw new VirtualControllerException(
                        $"Erro ao definir direção: {ex.Message}", ex);
                }
            }
        }

    public void SetButton(int button, bool pressed)
    {
            lock (_lock)
        {
                if (_disposed)
                {
                    throw new ObjectDisposedException(nameof(VJoyController));
                }

                if (!_connected)
                {
                    throw new VirtualControllerException(
                        "Não conectado ao dispositivo vJoy. Chame Connect() primeiro.");
                }

                try
            {
                    if (_controller == null)
                    {
                        return;
                    }

                    var ok = pressed
                        ? _controller.PressButton((uint)button)
                        : _controller.ReleaseButton((uint)button);

                    if (!ok)
                    {
                        throw new VirtualControllerException(
                            $"Falha ao definir botão {button} do vJoy {_deviceId}.");
                    }
                }
                catch (VirtualControllerException)
                {
                    throw;
                }
                catch (Exception ex)
                {
                    _status = VirtualControllerStatus.Error;
                    throw new VirtualControllerException(
                        $"Erro ao definir botão: {ex.Message}", ex);
                }
            }
        }

    public void SetAxis(AxisId axis, double value)
    {
        lock (_lock)
        {
            if (_disposed)
            {
                throw new ObjectDisposedException(nameof(VJoyController));
            }

            if (!_connected)
            {
                throw new VirtualControllerException(
                    "Não conectado ao dispositivo vJoy. Chame Connect() primeiro.");
            }

            // Triggers ainda não são suportados pelo backend vJoy.
            if (axis is AxisId.LeftTrigger or AxisId.RightTrigger)
            {
                throw new VirtualControllerException(
                    $"Eixo {axis} (trigger) não é suportado pelo vJoy nesta versão.");
            }

            // Validar valor normalizado
            if (double.IsNaN(value) || double.IsInfinity(value))
            {
                throw new VirtualControllerException(
                    $"Valor de eixo inválido: {value}");
            }

            // Limitar a [-1.0, 1.0]
            var clampedValue = Math.Clamp(value, -1.0, 1.0);

            try
            {
                if (_controller == null)
                {
                    throw new VirtualControllerException(
                        "Controlador vJoy não inicializado.");
                }

                // Verificar se o eixo está habilitado no dispositivo vJoy
                var hasAxis = axis switch
                {
                    AxisId.LeftStickX => _controller.HasAxisX,
                    AxisId.LeftStickY => _controller.HasAxisY,
                    AxisId.RightStickX => _controller.HasAxisRx,
                    AxisId.RightStickY => _controller.HasAxisRy,
                    _ => false
                };

                if (!hasAxis)
                {
                    throw new VirtualControllerException(
                        $"Dispositivo vJoy {_deviceId} não possui o eixo {GetVJoyAxisName(axis)}. " +
                        "Habilite-o no 'Configure vJoy'.");
                }

                // Mapear [-1.0, 1.0] para [0, max]
                // -1.0 → 0
                //  0.0 → max / 2
                // +1.0 → max
                                var max = _controller.AxisMaxValue ?? 32767;
                var center = max / 2.0;
                var range = max / 2.0;
                var mappedValue = (int)Math.Clamp(center + (clampedValue * range), 0, max);

                var ok = axis switch
                {
                    AxisId.LeftStickX => _controller.SetAxisX(mappedValue),
                    AxisId.LeftStickY => _controller.SetAxisY(mappedValue),
                    AxisId.RightStickX => _controller.SetAxisRx(mappedValue),
                    AxisId.RightStickY => _controller.SetAxisRy(mappedValue),
                    _ => false
                };

                if (!ok)
                {
                    _status = VirtualControllerStatus.Error;
                    throw new VirtualControllerException(
                        $"Falha ao enviar valor ao eixo {GetVJoyAxisName(axis)} do vJoy {_deviceId}.");
                }
            }
            catch (VirtualControllerException)
            {
                throw;
            }
            catch (Exception ex)
            {
                _status = VirtualControllerStatus.Error;
                throw new VirtualControllerException(
                    $"Erro ao definir eixo: {ex.Message}", ex);
            }
        }
    }

    private static string GetVJoyAxisName(AxisId axis)
    {
        return axis switch
        {
            AxisId.LeftStickX => "X",
            AxisId.LeftStickY => "Y",
            AxisId.RightStickX => "Rx",
            AxisId.RightStickY => "Ry",
            _ => axis.ToString()
        };
    }

    public void Disconnect()
    {
            lock (_lock)
        {
                if (!_connected)
                {
                    return; // Já desconectado
                }

                try
            {
                    if (_controller != null)
                    {
                        _controller.Reset();
                        _manager?.RelinquishController(_controller);
                    }

                    _controller = null;
                    _manager = null;
                    _connected = false;
                    _status = VirtualControllerStatus.Disconnected;
                    _lastSteeringValue = double.NaN;
                }
                catch (Exception ex)
                {
                    Console.WriteLine(
                        $"[ERR] Erro ao desconectar do vJoy: {ex.Message}");
                    _status = VirtualControllerStatus.Error;
                }
            }
        }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        Disconnect();
        _disposed = true;
    }
}


