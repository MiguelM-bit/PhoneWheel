using System;
using System.Collections.Generic;
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

    // IDs dos botões do D-pad no protocolo (0-based): 10=UP, 11=DOWN, 12=LEFT, 13=RIGHT.
    private const int DPadUpButton = 10;
    private const int DPadDownButton = 11;
    private const int DPadLeftButton = 12;
    private const int DPadRightButton = 13;

    // Índice do POV hat (0-based, primeiro hat).
    private const uint PovIndex = 0;

    // Valor neutro do POV (nenhuma direção pressionada).
    private const int PovNeutral = -1;

    // Ângulos do POV contínuo em centésimos de grau (SetContPov).
    private const int PovUp = 0;
    private const int PovUpRight = 4500;
    private const int PovRight = 9000;
    private const int PovDownRight = 13500;
    private const int PovDown = 18000;
    private const int PovDownLeft = 22500;
    private const int PovLeft = 27000;
    private const int PovUpLeft = 31500;

    // Valores do POV discreto (SetDiscPov): 0=Norte, 1=Leste, 2=Sul, 3=Oeste.
    private const int DiscPovUp = 0;
    private const int DiscPovRight = 1;
    private const int DiscPovDown = 2;
    private const int DiscPovLeft = 3;

    // Direções do D-pad atualmente pressionadas (IDs de botão 10-13).
    private readonly HashSet<int> _dpadDirections = new();

    // Último valor POV enviado (para evitar atualizações redundantes).
    private int _lastPovValue = PovNeutral;

    // true = usa SetContPov (suporta diagonais); false = usa SetDiscPov (cardinais apenas).
    private bool _useContinuousPov = true;

    private VJoyControllerManager? _manager;
    private IVJoyController? _controller;
        private readonly List<string> _configurationWarnings = new();

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

            public IReadOnlyList<string> ConfigurationWarnings
            {
                get
                {
                    lock (_lock)
                    {
                        return _configurationWarnings.ToArray();
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

                    // O gamepad virtual usa 10 botões (A/B/X/Y/LB/RB/LS/RS/Back/Start),
                                        // o D-pad via POV hat e os eixos X/Y/Rx/Ry. A configuração é
                                        // validada de forma NÃO-bloqueante: o controle conecta mesmo
                                        // incompleto, e botões/eixos existentes funcionam. Avisos são
                                        // expostos via ConfigurationWarnings para o servidor logar.
                                        _configurationWarnings.Clear();

                                        // --- Botões ---
                                        // O gamepad usa 10 botões (IDs 0-9):
                                        //   0-7:  A, B, X, Y, LB, RB, LS, RS
                                        //   8-9:  Back, Start
                                        // O D-pad (UP/DOWN/LEFT/RIGHT) usa POV hat, não botões.
                                        var buttonCount = _controller.ButtonCount;
                                        if (buttonCount < 10)
                                        {
                                            _configurationWarnings.Add(
                                                $"Dispositivo vJoy {_deviceId} possui apenas {buttonCount} botões. " +
                                                "A/B/X/Y/LB/RB/LS/RS (protocolo 0-7 → botões vJoy 1-8) funcionam, mas " +
                                                "Back (protocolo 8 → botão vJoy 9) e Start (protocolo 9 → botão vJoy 10) " +
                                                "não existirão. No 'Configure vJoy', defina o número de botões para pelo menos 10.");
                                        }

                                        // --- Eixos ---
                                        // Eixos necessários para o gamepad completo:
                                        //   X, Y    → Analógico esquerdo
                                        //   Z       → Volante (steering)
                                        //   Rx, Ry  → Analógico direito

                                        if (!_controller.HasAxisZ)
                                        {
                                            _configurationWarnings.Add(
                                                $"Dispositivo vJoy {_deviceId} não possui eixo Z. " +
                                                "O volante (steering) não funcionará. " +
                                                "Habilite o eixo Z no 'Configure vJoy'.");
                                        }

                                        if (!_controller.HasAxisX || !_controller.HasAxisY)
                                        {
                                            _configurationWarnings.Add(
                                                $"Dispositivo vJoy {_deviceId} não possui os eixos X e/ou Y. " +
                                                "O analógico esquerdo não funcionará. " +
                                                "Habilite os eixos X e Y no 'Configure vJoy'.");
                                        }

                                        if (!_controller.HasAxisRx || !_controller.HasAxisRy)
                                        {
                                            _configurationWarnings.Add(
                                                $"Dispositivo vJoy {_deviceId} não possui os eixos Rx e/ou Ry. " +
                                                "O analógico direito não funcionará. " +
                                                "Habilite os eixos Rx e Ry no 'Configure vJoy'.");
                                        }

                                        // Quando o eixo Z (steering) está habilitado, o vJoy reporta
                                        // os eixos na ordem: X, Y, Z, Rx, Ry. No browser (ControllerTest.io),
                                        // os eixos ficam: axes[0]=X, axes[1]=Y, axes[2]=Z, axes[3]=Rx, axes[4]=Ry.
                                        // O ControllerTest.io mapeia axes[2]/axes[3] como "analógico direito",
                                        // mas axes[2] é o Z (volante), não Rx. Isso faz o analógico direito
                                        // parecer não funcionar no ControllerTest.io. Em jogos com mapeamento
                                        // padrão XInput (ViGEm), não há este problema.
                                        if (_controller.HasAxisZ &&
                                            (_controller.HasAxisRx || _controller.HasAxisRy))
                                        {
                                            _configurationWarnings.Add(
                                                $"Dispositivo vJoy {_deviceId} tem o eixo Z (steering) habilitado. " +
                                                "No browser (ControllerTest.io), o vJoy reporta os eixos na ordem X, Y, Z, Rx, Ry, " +
                                                "então o analógico direito aparece nos eixos 3-4 (Rx/Ry), não nos eixos 2-3. " +
                                                "Se o analógico direito não responder no ControllerTest.io, mova o stick direito e " +
                                                "verifique se os eixos 3-4 respondem. " +
                                                "Para o mapeamento padrão (direito nos eixos 2-3), desabilite o eixo Z no 'Configure vJoy' " +
                                                "se não for usar o volante (steering). Em jogos com mapeamento XInput (ViGEm), funciona normalmente.");
                                        }

                                        // O D-pad usa o POV hat do vJoy (não botões), pois o
                                        // ControllerTest.io (Web Gamepad API) lê o D-pad do POV hat.
                                        // POV contínuo é preferido (suporta diagonais); POV discreto
                                        // funciona apenas nas direções cardinais.
                                        var contPovCount = _controller.ContPovCount;
                                        var discPovCount = _controller.DiscPovCount;

                                        if (contPovCount < 1 && discPovCount < 1)
                                        {
                                            throw new VirtualControllerException(
                                                $"Dispositivo vJoy {_deviceId} não possui POV hat configurado. " +
                                                "O D-pad não funcionará. No 'Configure vJoy', habilite pelo menos " +
                                                "1 POV contínuo (Continuous POV) no dispositivo.");
                                        }

                                        _useContinuousPov = contPovCount >= 1;

                                        if (!_useContinuousPov)
                                        {
                                            _configurationWarnings.Add(
                                                $"Dispositivo vJoy {_deviceId} possui apenas POV discreto. " +
                                                "O D-pad funcionará apenas nas direções cardinais (sem diagonais). " +
                                                "Para diagonais, habilite um POV contínuo no 'Configure vJoy'.");
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

                                    // D-pad (10=UP, 11=DOWN, 12=LEFT, 13=RIGHT) usa o POV hat,
                                    // não botões. O ControllerTest.io (Web Gamepad API) lê o
                                    // D-pad do POV hat, não de botões.
                                    if (button is >= DPadUpButton and <= DPadRightButton)
                                    {
                                        UpdateDpadState(button, pressed);
                                        return;
                                    }

                                    // O protocolo usa IDs 0-based (A=0, B=1, ...), mas o SDK
                                    // vJoy é 1-based (PressButton(1) = primeiro botão).
                                    var vjoyButton = (uint)(button + 1);

                                    var ok = pressed
                                        ? _controller.PressButton(vjoyButton)
                                        : _controller.ReleaseButton(vjoyButton);

                                    if (!ok)
                                    {
                                        throw new VirtualControllerException(
                                            $"Falha ao definir botão {button} (vJoy {vjoyButton}) do vJoy {_deviceId}. " +
                                            "Verifique se o dispositivo possui pelo menos 10 botões no 'Configure vJoy'.");
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

    /// <summary>
    /// Atualiza o estado do D-pad e envia o novo valor ao POV hat do vJoy.
    /// Chamado por SetButton para os botões 10-13 (UP/DOWN/LEFT/RIGHT).
    /// </summary>
    private void UpdateDpadState(int button, bool pressed)
    {
        if (pressed)
        {
            _dpadDirections.Add(button);
        }
        else
        {
            _dpadDirections.Remove(button);
        }

        var povValue = _useContinuousPov
            ? ComputeContinuousPovValue()
            : ComputeDiscretePovValue();

        // Evitar atualizações redundantes: só envia se o valor mudou.
        if (povValue == _lastPovValue)
        {
            return;
        }

        var ok = _useContinuousPov
            ? _controller!.SetContPov(povValue, PovIndex)
            : _controller!.SetDiscPov(povValue, PovIndex);

        if (!ok)
        {
            _status = VirtualControllerStatus.Error;
            throw new VirtualControllerException(
                $"Falha ao enviar valor POV {povValue} ao vJoy {_deviceId}.");
        }

        _lastPovValue = povValue;
    }

    /// <summary>
    /// Calcula o ângulo do POV contínuo (centésimos de grau) a partir das
    /// direções do D-pad atualmente pressionadas. Suporta diagonais.
    /// </summary>
    private int ComputeContinuousPovValue()
    {
        var up = _dpadDirections.Contains(DPadUpButton);
        var down = _dpadDirections.Contains(DPadDownButton);
        var left = _dpadDirections.Contains(DPadLeftButton);
        var right = _dpadDirections.Contains(DPadRightButton);

        if (!up && !down && !left && !right)
        {
            return PovNeutral;
        }

        if (up && right) return PovUpRight;
        if (down && right) return PovDownRight;
        if (down && left) return PovDownLeft;
        if (up && left) return PovUpLeft;

        if (up) return PovUp;
        if (right) return PovRight;
        if (down) return PovDown;
        if (left) return PovLeft;

        return PovNeutral;
    }

    /// <summary>
    /// Calcula o valor do POV discreto (0=Norte, 1=Leste, 2=Sul, 3=Oeste)
    /// a partir das direções do D-pad atualmente pressionadas.
    /// POV discreto não suporta diagonais; em caso de múltiplas direções,
    /// prioriza uma direção cardinal.
    /// </summary>
    private int ComputeDiscretePovValue()
    {
        var up = _dpadDirections.Contains(DPadUpButton);
        var down = _dpadDirections.Contains(DPadDownButton);
        var left = _dpadDirections.Contains(DPadLeftButton);
        var right = _dpadDirections.Contains(DPadRightButton);

        if (!up && !down && !left && !right)
        {
            return PovNeutral;
        }

        if (up) return DiscPovUp;
        if (right) return DiscPovRight;
        if (down) return DiscPovDown;
        if (left) return DiscPovLeft;

        return PovNeutral;
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
                        "Habilite-o no 'Configure vJoy' (X/Y = analógico esquerdo, Rx/Ry = analógico direito).");
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
                    _dpadDirections.Clear();
                    _lastPovValue = PovNeutral;
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


