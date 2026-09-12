namespace PhoneWheel.Server.Connection;

/// <summary>
/// Gerencia handshake inicial com o cliente Android.
///
/// Responsabilidades:
/// - Protocolo de handshake inicial
/// - Validação de versão do protocolo
/// - Negociação de parâmetros
/// </summary>
public class HandshakeManager
{
    private readonly string _protocolVersion = "1.0";
    private bool _handshakeDone = false;

    public bool IsHandshakeDone => _handshakeDone;

    public void InitiateHandshake(string clientVersion)
    {
        if (clientVersion == _protocolVersion)
        {
            _handshakeDone = true;
        }
    }

    public void Reset()
    {
        _handshakeDone = false;
    }

    public string GetProtocolVersion() => _protocolVersion;
}
