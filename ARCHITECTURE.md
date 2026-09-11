# Integração e Arquitetura do PhoneWheel

## Fluxo de Dados Completo

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ANDROID (Cliente)                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. GyroscopeManager                                                       │
│     └─ Lê sensor TYPE_GYROSCOPE (X, Y, Z rad/s)                          │
│     └─ Callback onSensorDataChangedListener()                             │
│                                                                             │
│  2. SteeringProcessor (Android)                                           │
│     ├─ Input: velocidade angular (X, Y, Z)                               │
│     ├─ Integração: angle += angularVelocity * deltaTime                  │
│     ├─ Limita: [-450°, +450°]                                            │
│     ├─ Permite: recenter(), setSensitivity()                             │
│     └─ Output: ângulo acumulado em graus                                 │
│                                                                             │
│  3. UdpClient                                                             │
│     ├─ Input: ângulo + velocidade angular                               │
│     ├─ Serializa: SteeringPacket → JSON UTF-8                           │
│     └─ Envia: DatagramPacket via UDP/IP (porta 5005)                    │
│                                                                             │
│  4. MainActivity (Orquestração)                                           │
│     ├─ setupGyroscope() → inicia GyroscopeManager                       │
│     ├─ setupButtons() → controles de UI (recenter, eixo, sensibilidade)│
│     ├─ setupNetwork() → inicializa UdpClient                             │
│     ├─ Event loop: a cada 50ms, obtém ângulo e envia via UDP           │
│     └─ Ciclo de vida: onResume/onPause/onDestroy                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                        ↓ UDP Port 5005
                                     Wi-Fi Network
                                        ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ WINDOWS (Servidor)                                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. UdpServer (Network Layer)                                             │
│     ├─ Escuta: 0.0.0.0:5005 UDP                                         │
│     ├─ Recebe: DatagramPacket (bytes)                                    │
│     ├─ Desserializa: JSON → SteeringPacket via PacketParser             │
│     ├─ Valida: type=="steering", campos obrigatórios, ranges válidos   │
│     ├─ Evento: SteeringDataReceived (pacote validado)                   │
│     └─ Evento: InvalidPacketReceived (pacote inválido)                  │
│                                                                             │
│  2. SteeringPipeline (Business Logic - Orquestração)                    │
│     ├─ Entrada: SteeringPacket do Android                               │
│     │                                                                     │
│     ├─ Etapa 1: CalibrationManager                                      │
│     │  └─ calibratedAngle = angle + calibrationOffset                   │
│     │     (padrão offset = 0)                                            │
│     │                                                                     │
│     ├─ Etapa 2: SteeringProcessor (Windows Input)                       │
│     │  ├─ Aplica deadzone (5° padrão)                                   │
│     │  ├─ Limita: [-450°, +450°]                                        │
│     │  ├─ Normaliza: [-1.0, +1.0] baseado em [-450°, +450°]            │
│     │  ├─ Suavização: EMA com factor 0.2                                │
│     │  └─ Output: valor normalizado [-1.0, +1.0]                       │
│     │                                                                     │
│     ├─ Etapa 3: IVirtualController (abstração)                         │
│     │  └─ SetSteering(normalizedValue)                                  │
│     │                                                                     │
│     ├─ Etapa 4: VJoyController (implementação)                         │
│     │  ├─ Mapeia: [-1.0, +1.0] → [0, 32767] (padrão vJoy)             │
│     │  └─ Envia: eixo X via simulação (Fase 1)                         │
│     │                                                                     │
│     └─ Saída: SteeringResult com trace completo                        │
│        ├─ receivedAngle, calibratedAngle, normalizedValue              │
│        ├─ virtualControllerStatus                                      │
│        ├─ success, error                                               │
│        └─ timestamp                                                     │
│                                                                             │
│  3. ServerLogger (I/O Layer)                                             │
│     ├─ Log estruturado com thread-safety                               │
│     ├─ Cores: Info (Cyan), Success (Green), Warning (Yellow), Error (Red)│
│     └─ Formata: timestamp + nível + mensagem estruturada               │
│                                                                             │
│  4. Program.cs (Composição e Orquestração)                              │
│     ├─ Cria componentes (CalibrationManager, SteeringProcessor, etc)   │
│     ├─ Inicializa SteeringPipeline                                      │
│     ├─ Inicia UdpServer                                                │
│     ├─ Conecta VJoyController (com degradação graciosa)               │
│     ├─ Event handler: UdpServer.SteeringDataReceived                   │
│     │  └─ steeringPipeline.Process(packet)                             │
│     │  └─ ServerLogger.LogSteeringData(result)                         │
│     └─ Ciclo de vida: await Task.Delay(Infinity) até Ctrl+C           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Componentes por Camada

### Camada de Rede (Network/)
- **UdpServer.cs**: Escuta UDP, dispara eventos (contém PacketParser)
- **PacketParser.cs**: Desserialização + validação JSON

### Camada de Negócio (Input/) + Núcleo (Core/)
- **SteeringProcessor.cs**: Processamento de direção (deadzone, normalização)
- **CalibrationManager.cs**: Offset de calibração
- **SteeringPipeline.cs**: Orquestração de calibração → processamento → vJoy

### Camada de Abstração (VirtualController/)
- **IVirtualController.cs**: Contrato de controle virtual
- **VJoyController.cs**: Implementação vJoy (Fase 1: simulação)

### Camada de Apresentação (Core/)
- **ServerLogger.cs**: Log centralizado com thread-safety

### Camada de Modelos (Models/)
- **SteeringPacket.cs**: Protocolo (type, angle, gyro, timestamp)

### Ponto de Composição
- **Program.cs**: Factory e orquestração de inicialização

## Garantias Arquiteturais

✅ **Separação de Responsabilidades**
- UdpServer: Apenas rede + desserialização
- SteeringProcessor (Windows): Apenas processamento matemático
- VJoyController: Apenas interfaceamento com hardware virtual
- SteeringPipeline: Apenas orquestração (sem I/O)
- ServerLogger: Apenas formatação + console

✅ **Sem Duplicação**
- SteeringProcessor Android ≠ SteeringProcessor Windows (domínios diferentes)
  - Android: integra velocidade → ângulo acumulado
  - Windows: processa ângulo → valor normalizado
- Nenhum componente implementa responsabilidades de outro

✅ **Composição Clara**
- Dependências explícitas no constructor
- Program.cs centraliza criação
- Sem globals ou singletons

✅ **Thread-safety**
- CalibrationManager: locks em operações críticas
- ServerLogger: locks em Console.WriteLine
- UdpServer: Task baseado, nenhuma race condition conhecida

✅ **Tratamento de Erros**
- UdpServer: erros de rede não encerram o servidor
- SteeringPipeline: captura exceções em cada etapa, retorna SteeringResult.Error
- VJoyController: degradação graciosa se not disponível
- Program.cs: try/catch com finally para cleanup

✅ **Extensibilidade**
- IVirtualController permite múltiplas implementações (XInput, DirectInput, etc)
- SteeringPipeline não conhece detalhes de vJoy
- Fácil adicionar novo componente sem modificar existentes

## Fluxo de Teste

1. Iniciar PhoneWheel.Server.exe
2. Conectar Android ao Wi-Fi da mesma rede
3. Android MainActivity:
   - Ativar giroscópio (observable em tempo real)
   - Configurar IP/porta do servidor
   - Clicar Connect
4. Giroscópio lê valores continuamente
5. A cada 50ms: GyroManager → SteeringProcessor → UdpClient.send()
6. Windows recebe UDP:
   - UdpServer desserializa (PacketParser)
   - SteeringPipeline processa completo
   - ServerLogger exibe estruturado
   - VJoyController recebe comando
7. Console mostra trace completo de cada pacote

## Exemplo de Log Esperado

```
[17:23:19.015] [INFO] Iniciando servidor UDP na porta 5005...
[17:23:19.040] [OK] Controlador virtual conectado (Status: Connected)
[17:23:19.069] [OK] Servidor aguardando pacotes de Android...
[17:23:25.123] [OK] Android: 192.168.1.100
  ├─ Ângulo recebido:   45.34°
  ├─ Ângulo calibrado:  45.34°
  ├─ Normalizado:       0.1007
  ├─ Controle Virtual:  Connected
  ├─ Gyro:              0.2500 rad/s
  └─ Timestamp:         1234567890ms
```

## Futuras Melhorias

1. **Fase 2 (vJoy real)**: Substituir VJoyController simulação pela API real
2. **Configuração**: Arquivo config.json para calibração, deadzone, porta
3. **Estatísticas**: Pacotes recebidos, latência média, taxa de erro
4. **Múltiplos dispositivos**: Suportar mais Android conectados simultaneamente
5. **Gravação de dados**: Log em arquivo para análise posterior
6. **CLI interativa**: Comandos para ajuste em tempo real (calibração, deadzone)
7. **UI gráfica**: WinForms/WPF para visualização de dados

## Dependências Verificadas

- ✅ Android MainActivity: GyroscopeManager, SteeringProcessor, UdpClient
- ✅ Windows Program.cs: CalibrationManager, SteeringProcessor, VJoyController, SteeringPipeline
- ✅ UdpServer: PacketParser (integrado no ProcessReceivedData)
- ✅ SteeringPipeline: CalibrationManager, SteeringProcessor, IVirtualController
- ✅ ServerLogger: sem dependências (I/O apenas)
- ✅ Sem circular dependencies
- ✅ Sem dependências não-intencionais
