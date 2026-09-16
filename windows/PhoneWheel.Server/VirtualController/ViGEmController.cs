using System;
using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;
using PhoneWheel.Server.Models;

namespace PhoneWheel.Server.VirtualController;

/// <summary>
/// Implementação de IVirtualController usando ViGEmBus (emula um controle Xbox 360).
///
/// Responsabilidades:
/// - Conectar ao driver ViGEmBus
/// - Criar um controle Xbox 360 virtual
/// - Enviar valores de direção (LeftThumbX)
/// - Desconectar e liberar o controle
///
/// Esta classe é específica da implementação ViGEmBus e não deve ser
/// usada diretamente pelo resto da aplicação. Use IVirtualController.
///
/// Notas:
/// - ViGEmBus deve estar instalado no sistema
/// - Sem o driver instalado, Connect() lança VirtualControllerException
///   com uma mensagem clara; o servidor continua funcionando sem o controle.
/// </summary>
public class ViGEmController : IVirtualController
{
    private readonly object _lock = new();
    private bool _connected;
    private bool _disposed;
    private VirtualControllerStatus _status;
    private double _lastSteeringValue = double.NaN;

    private ViGEmClient? _client;
    private IXbox360Controller? _controller;

    // Limites do eixo LeftThumbX do Xbox 360 (XInput).
    private const short AxisMin = short.MinValue; // -32768
    private const short AxisMax = short.MaxValue; //  32767

    public ViGEmController()
    {
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

            public IReadOnlyList<string> ConfigurationWarnings => Array.Empty<string>();

            public void Connect()
        {
            lock (_lock)
            {
                if (_disposed)
                {
                    throw new ObjectDisposedException(nameof(ViGEmController));
                }

                if (_connected)
                {
                    return; // Já conectado
                }

                try
                {
                    _client = new ViGEmClient();
                    _controller = _client.CreateXbox360Controller();
                    _controller.Connect();

                    _connected = true;
                    _status = VirtualControllerStatus.Connected;
                }
                catch (Exception ex)
                {
                    _status = VirtualControllerStatus.Error;
                    throw new VirtualControllerException(
                        "Erro ao conectar ao ViGEmBus. Verifique se o driver ViGEmBus " +
                        $"está instalado (https://github.com/nefarius/ViGEmBus/releases). Detalhe: {ex.Message}",
                        ex);
                }
            }
        }

    public void SetSteering(double value)
    {
            lock (_lock)
        {
                if (_disposed)
                {
                    throw new ObjectDisposedException(nameof(ViGEmController));
                }

                if (!_connected)
                {
                    throw new VirtualControllerException(
                        "Não conectado ao ViGEmBus. Chame Connect() primeiro.");
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
                    // Mapear [-1.0, 1.0] para [AxisMin, AxisMax]
                    // -1.0 → AxisMin
                    //  0.0 → 0
                    // +1.0 → AxisMax
                    var mappedValue = (short)Math.Clamp(
                        clampedValue * AxisMax,
                        AxisMin,
                        AxisMax);

                    if (_controller == null)
                    {
                        throw new VirtualControllerException(
                            "Controle Xbox 360 virtual não inicializado.");
                    }

                    _controller.SetAxisValue(Xbox360Axis.LeftThumbX, mappedValue);

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
                    throw new ObjectDisposedException(nameof(ViGEmController));
                }

                if (!_connected)
                {
                    throw new VirtualControllerException(
                        "Não conectado ao ViGEmBus. Chame Connect() primeiro.");
                }

                try
            {
                    if (_controller == null)
                    {
                        return;
                    }

                    // Mapear índice de botão genérico para o botão Xbox 360 correspondente.
                    var xboxButton = button switch
                    {
                        0 => Xbox360Button.A,
                        1 => Xbox360Button.B,
                        2 => Xbox360Button.X,
                        3 => Xbox360Button.Y,
                        4 => Xbox360Button.LeftShoulder,
                        5 => Xbox360Button.RightShoulder,
                        6 => Xbox360Button.LeftThumb,
                        7 => Xbox360Button.RightThumb,
                        8 => Xbox360Button.Back,
                        9 => Xbox360Button.Start,
                                            10 => Xbox360Button.Up,
                                            11 => Xbox360Button.Down,
                                            12 => Xbox360Button.Left,
                                            13 => Xbox360Button.Right,
                                            _ => throw new VirtualControllerException(
                                                $"Botão {button} não existe no controle Xbox 360.")
                                        };

                    _controller.SetButtonState(xboxButton, pressed);
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
                throw new ObjectDisposedException(nameof(ViGEmController));
            }

            if (!_connected)
            {
                throw new VirtualControllerException(
                    "Não conectado ao ViGEmBus. Chame Connect() primeiro.");
            }

            // Triggers ainda não são suportados pelo backend Xbox 360.
            if (axis is AxisId.LeftTrigger or AxisId.RightTrigger)
            {
                throw new VirtualControllerException(
                    $"Eixo {axis} (trigger) não é suportado pelo Xbox 360 nesta versão.");
            }

            // Validar valor normalizado
            if (double.IsNaN(value) || double.IsInfinity(value))
            {
                throw new VirtualControllerException(
                    $"Valor de eixo inválido: {value}");
            }

            // Limitar a [-1.0, 1.0]
            var clampedValue = Math.Clamp(value, -1.0, 1.0);

            // Inversão do eixo Y para ViGEm (Xbox 360 / XInput).
            // O app Android usa convenção DirectInput: up = -1.0, down = +1.0.
            // O XInput espera o oposto: up = +32767, down = -32768.
            // Portanto, negamos o valor dos eixos Y antes do mapeamento.
            if (axis is AxisId.LeftStickY or AxisId.RightStickY)
            {
                clampedValue = -clampedValue;
            }

            try
            {
                if (_controller == null)
                {
                    throw new VirtualControllerException(
                        "Controle Xbox 360 virtual não inicializado.");
                }

                // Mapear [-1.0, 1.0] para [AxisMin, AxisMax]
                // -1.0 → AxisMin
                //  0.0 → 0
                // +1.0 → AxisMax
                var mappedValue = (short)Math.Clamp(
                    clampedValue * AxisMax,
                    AxisMin,
                    AxisMax);

                var xboxAxis = axis switch
                {
                    AxisId.LeftStickX => Xbox360Axis.LeftThumbX,
                    AxisId.LeftStickY => Xbox360Axis.LeftThumbY,
                    AxisId.RightStickX => Xbox360Axis.RightThumbX,
                    AxisId.RightStickY => Xbox360Axis.RightThumbY,
                    _ => throw new VirtualControllerException(
                        $"Eixo {axis} não existe no controle Xbox 360.")
                };

                _controller.SetAxisValue(xboxAxis, mappedValue);
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
                        _controller.Disconnect();
                    }

                    _controller = null;
                    _client?.Dispose();
                    _client = null;
                    _connected = false;
                    _status = VirtualControllerStatus.Disconnected;
                    _lastSteeringValue = double.NaN;
                }
                catch (Exception ex)
                {
                    Console.WriteLine(
                        $"[ERR] Erro ao desconectar do ViGEmBus: {ex.Message}");
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