namespace PhoneWheel.Server.VirtualController;

/// <summary>
/// Abstração de um controlador virtual de entrada.
///
/// Define a interface que qualquer implementação de dispositivo virtual
/// (vJoy, XInput, etc) deve seguir. Assim, o resto da aplicação fica
/// independente da implementação específica.
///
/// Responsabilidades:
/// - Conectar/desconectar do dispositivo virtual
/// - Enviar valores de direção (eixo X)
/// - Controlar botões (futuro)
/// - Manter o ciclo de vida do dispositivo
///
/// Cada implementação deve lidar com:
/// - Inicialização do hardware específico
/// - Tratamento de erros e timeouts
/// - Limpeza e liberação de recursos
/// </summary>
public interface IVirtualController : IDisposable
{
    /// <summary>
    /// Status atual da conexão com o dispositivo virtual.
    /// </summary>
    VirtualControllerStatus Status { get; }

    /// <summary>
    /// Conecta ao dispositivo virtual.
    ///
    /// Deve ser chamado antes de usar SetSteering ou SetButton.
    /// Lança VirtualControllerException se falhar.
    /// </summary>
    /// <exception cref="VirtualControllerException">
    /// Se o dispositivo não estiver disponível ou não conseguir conectar.
    /// </exception>
    void Connect();

    /// <summary>
    /// Define a posição do eixo de direção.
    ///
    /// O valor deve estar normalizado entre -1.0 e +1.0:
    /// -1.0 = máxima esquerda
    ///  0.0 = centro
    /// +1.0 = máxima direita
    /// </summary>
    /// <param name="value">Valor normalizado [-1.0, +1.0].</param>
    /// <exception cref="VirtualControllerException">
    /// Se não estiver conectado ou a comunicação falhar.
    /// </exception>
    void SetSteering(double value);

    /// <summary>
    /// Define o estado de um botão.
    ///
    /// Implementação futura.
    /// </summary>
    /// <param name="button">Índice do botão.</param>
    /// <param name="pressed">true = pressionado, false = solto.</param>
    /// <exception cref="VirtualControllerException">
    /// Se não estiver conectado ou o botão não existir.
    /// </exception>
    void SetButton(int button, bool pressed);

    /// <summary>
    /// Desconecta do dispositivo virtual e libera recursos.
    ///
    /// Deve ser chamado antes de descartar a instância.
    /// Seguro chamar múltiplas vezes.
    /// </summary>
    void Disconnect();
}

/// <summary>
/// Status de conexão do controlador virtual.
/// </summary>
public enum VirtualControllerStatus
{
    /// <summary>Não conectado ao dispositivo.</summary>
    Disconnected = 0,

    /// <summary>Conectado e operacional.</summary>
    Connected = 1,

    /// <summary>Erro na comunicação com o dispositivo.</summary>
    Error = 2
}

/// <summary>
/// Exceção específica de erro no controlador virtual.
/// </summary>
public class VirtualControllerException : Exception
{
    public VirtualControllerException(string message)
        : base(message)
    {
    }

    public VirtualControllerException(string message, Exception innerException)
        : base(message, innerException)
    {
    }
}
