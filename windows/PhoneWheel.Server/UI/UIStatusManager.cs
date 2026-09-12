namespace PhoneWheel.Server.UI;

/// <summary>
/// Gerencia a exibição de informações na UI.
///
/// Responsabilidades:
/// - Exibir status da conexão
/// - Exibir valores de steering
/// - Exibir logs
/// </summary>
public class UIStatusManager
{
    private string _connectionStatus = "Desconectado";
    private string _steeringValue = "0.0";
    private string _lastError = "";

    public string ConnectionStatus
    {
        get => _connectionStatus;
        set => _connectionStatus = value;
    }

    public string SteeringValue
    {
        get => _steeringValue;
        set => _steeringValue = value;
    }

    public string LastError
    {
        get => _lastError;
        set => _lastError = value;
    }

    public void UpdateStatus(string status)
    {
        ConnectionStatus = status;
    }

    public void UpdateSteeringDisplay(double steeringValue)
    {
        SteeringValue = $"{steeringValue:F2}";
    }

    public void SetError(string errorMessage)
    {
        LastError = errorMessage;
    }

    public void ClearError()
    {
        LastError = "";
    }
}
