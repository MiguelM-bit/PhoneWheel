# Servidor UDP - Documentação Técnica

## Visão Geral

O `UdpServer` é o componente Windows responsável por receber pacotes de direção enviados pelo aplicativo Android via UDP na porta **5005**.

## Arquitetura

### Componentes

#### 1. `UdpServer.cs`
Gerencia a comunicação UDP de forma assíncrona:
- **Porta**: 5005 (configurável)
- **Protocolo**: UDP
- **Formato**: JSON UTF-8

**Responsabilidades:**
- Abrir socket UDP na porta configurada
- Escutar pacotes continuamente em thread assíncrona
- Decodificar UTF-8 de pacotes brutos
- Validar pacotes via `PacketParser`
- Disparar eventos para pacotes válidos e inválidos
- Rastrear endereço IP do remetente
- Lidar com erros sem encerrar o servidor
- Iniciar/parar de forma assíncrona via `StartAsync()` / `StopAsync()`

**Eventos:**
- `SteeringDataReceived`: dispara quando um pacote válido é recebido
- `InvalidPacketReceived`: dispara quando um pacote inválido é recebido

#### 2. `PacketParser.cs`
Responsável apenas pela desserialização e validação JSON:
- Desserializa strings JSON em objetos `SteeringPacket`
- Valida campos obrigatórios (type, angle, gyro, timestamp)
- Valida tipos de dados e ranges
- Descarta pacotes inválidos silenciosamente
- Nunca lança exceções (retorna bool)

**Validações:**
- `type` deve ser "steering"
- `angle` e `gyro` devem ser números válidos (não NaN/Infinity)
- `timestamp` deve ser >= 0

#### 3. `SteeringPacket.cs`
Modelo de dados que espelha o protocolo:
```csharp
public sealed record SteeringPacket
{
    public string Type { get; init; }      // "steering"
    public double Angle { get; init; }     // graus
    public double Gyro { get; init; }      // rad/s
    public long Timestamp { get; init; }   // ms desde Unix epoch
}
```

## Fluxo de Dados

```
Android (UdpClient)
    │
    └─→ UDP Pacote JSON
        {
            "type": "steering",
            "angle": 45.5,
            "gyro": -0.234,
            "timestamp": 1694567890000
        }
    │
    ↓
Windows (UdpServer)
    │
    ├─→ ReceiveAsync() aguarda pacotes
    │   
    └─→ ProcessReceivedData()
        │
        ├─→ Decodificar UTF-8
        │
        ├─→ PacketParser.TryParse()
        │   │
        │   ├─ Deserializar JSON
        │   ├─ Validar campos
        │   └─ Retornar bool + SteeringPacket
        │
        └─→ Disparar evento
            ├─ SteeringDataReceived (válido)
            └─ InvalidPacketReceived (inválido)
```

## Tratamento de Erros

### Pacotes Inválidos
Quando um pacote não pode ser processado:
1. O servidor **NÃO** encerra
2. Executa callback `InvalidPacketReceived` com motivo
3. Log opcional (console ou future logging)
4. Continua aguardando próximos pacotes

### Erros de Rede
- `OperationCanceledException`: esperado ao parar o servidor
- Outras exceções: logadas e o loop continua

### Exceções durante Desserialização
- `JsonException`: JSON malformado
- Qualquer outra: falha genérica
- Todas retornam `false` sem lançar

## Uso

### Iniciar Servidor
```csharp
var server = new UdpServer(5005);

server.SteeringDataReceived += (sender, args) =>
{
    Console.WriteLine($"Ângulo: {args.Packet.Angle}");
    Console.WriteLine($"IP: {args.RemoteEndPoint.Address}");
};

server.InvalidPacketReceived += (sender, args) =>
{
    Console.WriteLine($"Erro: {args.Reason}");
};

await server.StartAsync();
// Servidor rodando...
await server.StopAsync();
```

## Build e Execução

```bash
# Build
dotnet build windows/PhoneWheel.sln

# Executar servidor
dotnet run --project windows/PhoneWheel.Server

# Testar com script PowerShell
.\test-udp-client.ps1
```

## Testing

O arquivo `test-udp-client.ps1` simula:
1. ✅ Pacote válido
2. ✅ Pacote com ângulo negativo
3. ❌ Pacote com tipo inválido
4. ❌ JSON malformado

Todos os pacotes são enviados para `127.0.0.1:5005`.

## Arquitetura Limpa

**O que está AQUI:**
- ✅ Comunicação UDP
- ✅ Desserialização JSON
- ✅ Validação básica
- ✅ Tratamento de erros de rede

**O que NÃO está AQUI:**
- ❌ Cálculos de direção (tarefa do `Input`)
- ❌ Controle virtual / vJoy (tarefa do `VirtualController`)
- ❌ Interface gráfica
- ❌ Persistência de dados

## Próximos Passos

1. Implementar `Input/` para processar pacotes recebidos
2. Implementar `VirtualController/` para controlar dispositivo virtual (vJoy)
3. Adicionar testes unitários
4. Considerar logging estruturado
5. Adicionar métricas (pacotes recebidos, erros, latência)

## Protocolo

Ver `protocol/protocol.md` para especificação completa do formato de pacotes.
