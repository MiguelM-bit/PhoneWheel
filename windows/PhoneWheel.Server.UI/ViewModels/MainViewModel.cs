using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Diagnostics;
using System.Runtime.CompilerServices;
using System.Windows.Media;
using System.Windows.Threading;
using PhoneWheel.Server.Connection;
using PhoneWheel.Server.Models;
using PhoneWheel.Server.Network;
using PhoneWheel.Server.Services;
using PhoneWheel.Server.UI.Configuration;
using PhoneWheel.Server.VirtualController;

namespace PhoneWheel.Server.UI.ViewModels;

/// <summary>
/// ViewModel principal da interface: expõe o estado do servidor,
/// os valores de direção e o log, e orquestra o <see cref="ServerEngine"/>.
/// </summary>
public class MainViewModel : INotifyPropertyChanged, IDisposable
{
    private const int MaxLogLines = 300;

    private readonly Dispatcher _dispatcher;
    private readonly RelayCommand _toggleCommand;
    private readonly RelayCommand _testControllerCommand;

    private ServerEngine? _engine;
    private bool _isRunning;
    private string _statusText = "Parado";
    private SolidColorBrush _statusBrush = Brushes.Gray;
    private double _wheelAngle;
    private string _receivedAngleText = "—";
    private string _calibratedAngleText = "—";
    private string _normalizedText = "—";
    private string _gyroText = "—";
    private string _packetCountText = "0";
    private string _clientIpText = "—";
    private string _localIpText = "—";
    private string _virtualControllerStatusText = "—";
    private VirtualControllerType _selectedControllerType;
    private int _packetCount;

    public MainViewModel()
    {
        _dispatcher = Dispatcher.CurrentDispatcher;

        var config = AppConfig.Load();
        _selectedControllerType = config.ControllerType;

        _toggleCommand = new RelayCommand(Toggle);
        _testControllerCommand = new RelayCommand(TestController);

        LogLines = new ObservableCollection<string>();
        ControllerTypes = new ObservableCollection<VirtualControllerType>
        {
            VirtualControllerType.VJoy,
            VirtualControllerType.Xbox360
        };

        LocalIpText = GetLocalIp();
    }

    /// <summary>Linhas do painel de log.</summary>
    public ObservableCollection<string> LogLines { get; }

    /// <summary>Tipos de controle virtual disponíveis para seleção.</summary>
    public ObservableCollection<VirtualControllerType> ControllerTypes { get; }

    /// <summary>Comando do botão Iniciar/Parar.</summary>
    public RelayCommand ToggleCommand => _toggleCommand;

    /// <summary>Comando do botão Testar controle (abre joy.cpl).</summary>
    public RelayCommand TestControllerCommand => _testControllerCommand;

    /// <summary>Backend de controle virtual selecionado.</summary>
    public VirtualControllerType SelectedControllerType
    {
        get => _selectedControllerType;
        set
        {
            if (_selectedControllerType == value)
            {
                return;
            }

            _selectedControllerType = value;
            OnPropertyChanged();
            OnPropertyChanged(nameof(ControllerTypeText));

            // Persistir a seleção
            new AppConfig { ControllerType = value }.Save();
        }
    }

    /// <summary>Texto descritivo do backend selecionado.</summary>
    public string ControllerTypeText => SelectedControllerType switch
    {
        VirtualControllerType.VJoy => "vJoy (DirectInput)",
        VirtualControllerType.Xbox360 => "Xbox 360 (ViGEmBus)",
        _ => SelectedControllerType.ToString()
    };

    /// <summary>Indica se o servidor está em execução.</summary>
    public bool IsRunning
    {
        get => _isRunning;
        private set
        {
            _isRunning = value;
            OnPropertyChanged();
            OnPropertyChanged(nameof(ToggleButtonText));
            OnPropertyChanged(nameof(IsControllerSelectionEnabled));
        }
    }

    /// <summary>Habilita a seleção de backend apenas quando parado.</summary>
    public bool IsControllerSelectionEnabled => !IsRunning;

    /// <summary>Texto do botão Iniciar/Parar.</summary>
    public string ToggleButtonText => IsRunning ? "Parar" : "Iniciar";

    /// <summary>Texto do status de conexão.</summary>
    public string StatusText
    {
        get => _statusText;
        private set
        {
            _statusText = value;
            OnPropertyChanged();
        }
    }

    /// <summary>Cor do indicador de status.</summary>
    public SolidColorBrush StatusBrush
    {
        get => _statusBrush;
        private set
        {
            _statusBrush = value;
            OnPropertyChanged();
        }
    }

    /// <summary>Status do controle virtual (conectado/erro/desconectado).</summary>
    public string VirtualControllerStatusText
    {
        get => _virtualControllerStatusText;
        private set
        {
            _virtualControllerStatusText = value;
            OnPropertyChanged();
        }
    }

    /// <summary>Ângulo de rotação do volante (graus).</summary>
    public double WheelAngle
    {
        get => _wheelAngle;
        private set
        {
            _wheelAngle = value;
            OnPropertyChanged();
        }
    }

    /// <summary>Ângulo recebido do Android.</summary>
    public string ReceivedAngleText
    {
        get => _receivedAngleText;
        private set
        {
            _receivedAngleText = value;
            OnPropertyChanged();
        }
    }

    /// <summary>Ângulo após calibração.</summary>
    public string CalibratedAngleText
    {
        get => _calibratedAngleText;
        private set
        {
            _calibratedAngleText = value;
            OnPropertyChanged();
        }
    }

    /// <summary>Valor normalizado [-1, +1].</summary>
    public string NormalizedText
    {
        get => _normalizedText;
        private set
        {
            _normalizedText = value;
            OnPropertyChanged();
        }
    }

    /// <summary>Velocidade angular do giroscópio.</summary>
    public string GyroText
    {
        get => _gyroText;
        private set
        {
            _gyroText = value;
            OnPropertyChanged();
        }
    }

    /// <summary>Contagem de pacotes de direção recebidos.</summary>
    public string PacketCountText
    {
        get => _packetCountText;
        private set
        {
            _packetCountText = value;
            OnPropertyChanged();
        }
    }

    /// <summary>IP do cliente conectado.</summary>
    public string ClientIpText
    {
        get => _clientIpText;
        private set
        {
            _clientIpText = value;
            OnPropertyChanged();
        }
    }

    /// <summary>IP local do servidor.</summary>
    public string LocalIpText
    {
        get => _localIpText;
        private set
        {
            _localIpText = value;
            OnPropertyChanged();
        }
    }

    private void Toggle()
    {
        if (IsRunning)
        {
            _ = StopAsync();
        }
        else
        {
            _ = StartAsync();
        }
    }

    private void TestController()
    {
        try
        {
            // joy.cpl lista todos os controles de jogo do Windows
            // (inclui vJoy e o Xbox 360 virtual do ViGEmBus).
            Process.Start(new ProcessStartInfo("joy.cpl") { UseShellExecute = true });
        }
        catch (Exception ex)
        {
            AddLog($"[ERR] Falha ao abrir o painel de controles: {ex.Message}");
        }
    }

    private async Task StartAsync()
    {
        try
        {
            _engine = new ServerEngine(controllerType: SelectedControllerType);
            WireEngineEvents(_engine);

            await _engine.StartAsync();
        }
        catch (Exception ex)
        {
            AddLog($"[ERR] Falha ao iniciar: {ex.Message}");
        }
    }

    private async Task StopAsync()
    {
        try
        {
            if (_engine != null)
            {
                await _engine.StopAsync();
                UnwireEngineEvents(_engine);
                _engine.Dispose();
                _engine = null;
            }
        }
        catch (Exception ex)
        {
            AddLog($"[ERR] Falha ao parar: {ex.Message}");
        }
    }

    private void WireEngineEvents(ServerEngine engine)
    {
        engine.LogMessage += OnLogMessage;
        engine.SteeringProcessed += OnSteeringProcessed;
        engine.AxisProcessed += OnAxisProcessed;
        engine.ConnectionStateChanged += OnConnectionStateChanged;
        engine.ClientConnected += OnClientConnected;
        engine.InvalidPacketReceived += OnInvalidPacketReceived;
        engine.ServerStarted += OnServerStarted;
        engine.ServerStopped += OnServerStopped;
    }

    private void UnwireEngineEvents(ServerEngine engine)
    {
        engine.LogMessage -= OnLogMessage;
        engine.SteeringProcessed -= OnSteeringProcessed;
        engine.AxisProcessed -= OnAxisProcessed;
        engine.ConnectionStateChanged -= OnConnectionStateChanged;
        engine.ClientConnected -= OnClientConnected;
        engine.InvalidPacketReceived -= OnInvalidPacketReceived;
        engine.ServerStarted -= OnServerStarted;
        engine.ServerStopped -= OnServerStopped;
    }

    private void OnServerStarted(object? sender, EventArgs e)
    {
        RunOnUi(() =>
        {
            IsRunning = true;
            StatusText = "Aguardando conexão...";
            StatusBrush = Brushes.Orange;
            VirtualControllerStatusText = _engine?.VirtualControllerStatus.ToString() ?? "—";
        });
    }

    private void OnServerStopped(object? sender, EventArgs e)
    {
        RunOnUi(() =>
        {
            IsRunning = false;
            StatusText = "Parado";
            StatusBrush = Brushes.Gray;
            WheelAngle = 0;
            ClientIpText = "—";
            PacketCountText = "0";
            _packetCount = 0;
            VirtualControllerStatusText = "—";
        });
    }

    private void OnConnectionStateChanged(object? sender, ConnectionStatus status)
    {
        RunOnUi(() =>
        {
            if (status == ConnectionStatus.Connected)
            {
                StatusText = "Conectado";
                StatusBrush = Brushes.Green;
            }
            else
            {
                StatusText = "Desconectado";
                StatusBrush = Brushes.Red;
                WheelAngle = 0;
            }
        });
    }

    private void OnClientConnected(object? sender, ClientConnectedEventArgs e)
    {
        RunOnUi(() =>
        {
            ClientIpText = e.ClientIp;
            PacketCountText = "0";
            _packetCount = 0;
        });
    }

    private void OnSteeringProcessed(object? sender, SteeringProcessedEventArgs e)
    {
        var result = e.Result;
        RunOnUi(() =>
        {
            _packetCount++;
            PacketCountText = _packetCount.ToString();
            ReceivedAngleText = $"{result.ReceivedAngle:F2}°";
            CalibratedAngleText = result.CalibratedAngle.HasValue ? $"{result.CalibratedAngle:F2}°" : "—";
            NormalizedText = result.NormalizedValue.HasValue ? $"{result.NormalizedValue:F4}" : "—";
            GyroText = $"{result.ReceivedGyro:F4} rad/s";
            WheelAngle = result.CalibratedAngle ?? result.ReceivedAngle;
        });
    }

    private void OnAxisProcessed(object? sender, AxisProcessedEventArgs e)
    {
        // O Modo Volante do Android envia o eixo esquerdo X (left_x) para controlar
        // a direção. Atualiza a animação do volante com a mesma escala usada no
        // Android (binding.steeringWheel.setAngle(output * 450f)).
        if (e.AxisId != AxisId.LeftStickX)
        {
            return;
        }

        RunOnUi(() =>
        {
            WheelAngle = e.Value * 450;
            NormalizedText = $"{e.Value:F4}";
        });
    }

    private void OnLogMessage(object? sender, EngineLogEntry entry)
    {
        var prefix = entry.Level switch
        {
            EngineLogLevel.Success => "[OK]",
            EngineLogLevel.Warning => "[WARN]",
            EngineLogLevel.Error => "[ERR]",
            EngineLogLevel.Debug => "[DBG]",
            _ => "[INFO]"
        };

        AddLog($"{prefix} {entry.Message}");
    }

    private void OnInvalidPacketReceived(object? sender, InvalidPacketEventArgs e)
    {
        AddLog($"[WARN] Pacote inválido de {e.RemoteEndPoint.Address}: {e.Reason}");
    }

    private void AddLog(string line)
    {
        RunOnUi(() =>
        {
            LogLines.Add($"[{DateTime.Now:HH:mm:ss.fff}] {line}");
            while (LogLines.Count > MaxLogLines)
            {
                LogLines.RemoveAt(0);
            }
        });
    }

    private void RunOnUi(Action action)
    {
        if (_dispatcher.CheckAccess())
        {
            action();
        }
        else
        {
            _dispatcher.BeginInvoke(action);
        }
    }

    private static string GetLocalIp()
    {
        try
        {
            return ServerDiscovery.GetLocalIPv4();
        }
        catch
        {
            return "—";
        }
    }

    public void Dispose()
    {
        if (_engine != null)
        {
            UnwireEngineEvents(_engine);
            _engine.Dispose();
            _engine = null;
        }
    }

    public event PropertyChangedEventHandler? PropertyChanged;

    private void OnPropertyChanged([CallerMemberName] string? propertyName = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
}