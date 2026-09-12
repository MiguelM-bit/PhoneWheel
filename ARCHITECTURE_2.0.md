# PhoneWheel 2.0 - Arquitetura Reorganizada

## Visão Geral

O projeto foi reorganizado para melhorar a separação de responsabilidades, preparando a base para a versão 2.0. Nenhuma funcionalidade foi adicionada ou removida - apenas a estrutura foi melhorada.

## Estrutura Android

```
android/app/src/main/java/com/phonewheel/
├── ui/
│   └── MainActivity.kt                    // Activities, telas e componentes visuais
├── sensor/
│   ├── GyroscopeManager.kt               // Leitura do giroscópio
│   └── GyroAxis.kt                       // Enum dos eixos
├── steering/
│   ├── SteeringProcessor.kt              // Processador de direção (já existente)
│   ├── CalibrationManager.kt             // Calibração
│   ├── SensitivityManager.kt             // Sensibilidade
│   ├── DeadzoneManager.kt                // Zona morta (deadzone)
│   └── SmoothingManager.kt               // Suavização
├── network/
│   ├── UdpClient.kt                      // Envio UDP
│   ├── SteeringPacketSerializer.kt       // Serialização JSON
│   └── ConnectionState.kt                // Estados de conexão
├── connection/
│   └── ConnectionManager.kt              // Gerenciador de conexão
├── model/
│   └── SteeringPacket.kt                 // Modelo de dados
└── settings/
    └── SettingsManager.kt                // Configurações persistentes

```

### Responsabilidades Android

**sensor/** - Leitura de hardware
- Acesso ao SensorManager
- Eventos do giroscópio
- Verificação de disponibilidade

**steering/** - Processamento de dados
- Cálculo do ângulo acumulado
- Calibração e offset
- Sensibilidade
- Deadzone
- Suavização (filtro)

**network/** - Comunicação
- UDP com servidor Windows
- Serialização de pacotes
- Estados de conexão

**connection/** - Gerenciamento de conexão
- Handshake
- Heartbeat
- Reconexão
- Ciclo de vida

**ui/** - Interface gráfica
- Activities
- Visualização de dados
- Controles

**settings/** - Dados persistentes
- Configurações salvas
- Preferências do usuário

**model/** - Estruturas compartilhadas
- SteeringPacket (protocolo)

---

## Estrutura Windows

```
windows/PhoneWheel.Server/
├── UI/
│   └── UIStatusManager.cs                // Gerenciamento de status na UI
├── Network/
│   ├── UdpServer.cs                      // Servidor UDP
│   └── PacketParser.cs                   // Desserialização
├── Connection/
│   ├── ConnectionWatchdog.cs             // Detecção de desconexão
│   ├── HandshakeManager.cs               // Handshake inicial
│   └── HeartbeatManager.cs               // Heartbeat periódico
├── Input/
│   ├── SteeringProcessor.cs              // Processamento de direção
│   └── CalibrationManager.cs             // Calibração
├── VirtualController/
│   ├── IVirtualController.cs             // Interface abstrata
│   └── VJoyController.cs                 // Implementação vJoy
├── Profiles/
│   ├── SteeringProfile.cs                // Modelo de perfil
│   └── ProfileManager.cs                 // Gerenciamento de arquivos
├── Services/
│   ├── ServerLogger.cs                   // Logger centralizado
│   └── SteeringPipeline.cs               // Pipeline de processamento
├── Models/
│   └── SteeringPacket.cs                 // Modelo de dados
└── Program.cs                             // Ponto de entrada

```

### Responsabilidades Windows

**Network/** - Comunicação UDP
- Recebimento de pacotes
- Desserialização JSON
- Validação de dados

**Connection/** - Gerenciamento de conexão
- Watchdog (timeout detection)
- Handshake com cliente
- Heartbeat periódico
- Estado da conexão

**Input/** - Processamento de dados
- Aplicar deadzone
- Limitar ao intervalo
- Normalização
- Suavização

**VirtualController/** - Abstração de hardware
- Interface IVirtualController
- Implementação vJoy
- Status do dispositivo

**UI/** - Apresentação
- Status da conexão
- Valores de steering
- Mensagens de erro

**Profiles/** - Configurações salvas
- Carregamento de perfis JSON
- Salvamento de preferências
- Listar perfis disponíveis

**Services/** - Funcionalidades auxiliares
- Logger centralizado
- SteeringPipeline (composição)

**Models/** - Estruturas compartilhadas
- SteeringPacket (protocolo)

---

## Benefícios da Reorganização

✅ **Separação clara de responsabilidades**
- Cada pasta tem um propósito bem definido
- Facilita manutenção e testes

✅ **Escalabilidade**
- Fácil adicionar novos componentes
- Menos conflitos ao trabalhar em paralelo

✅ **Reutilização de código**
- Models e protocolos compartilhados
- Interfaces bem definidas

✅ **Testes unitários**
- Componentes isolados e testáveis
- Dependências claras

✅ **Documentação implícita**
- A estrutura mostra a intenção
- Novos desenvolvedores entendem rapidamente

---

## Integração com Protocolo

Ambos os lados usam o mesmo protocolo de comunicação:

```json
{
  "type": "steering",
  "angle": 45.5,
  "gyro": 1.2345,
  "timestamp": 1234567890000
}
```

---

## Status de Compilação

- ✅ **Android**: BUILD SUCCESSFUL
- ✅ **Windows**: BUILD SUCCESSFUL
- ✅ Todos os namespaces atualizados
- ✅ Imports corrigidos
- ✅ Nenhuma funcionalidade removida

---

## Próximas Etapas (v2.0)

Com a arquitetura reorganizada, agora é possível:

1. **Melhorar features de conexão**
   - Implementar handshake robusto
   - Heartbeat com timeout configurável
   - Reconexão automática

2. **Adicionar suporte a perfis**
   - Carregar/salvar presets
   - Sincronizar com servidor
   - Histórico de configurações

3. **Melhorar UI**
   - Dashboard com métricas
   - Gráficos em tempo real
   - Configuração visual de parâmetros

4. **Expandir testes**
   - Unit tests para cada módulo
   - Testes de integração
   - Simulação de pacotes

---

## Checklist de Reorganização

- ✅ Criar estrutura de diretórios Android
- ✅ Mover classes para novos pacotes
- ✅ Atualizar imports/namespaces Android
- ✅ Atualizar AndroidManifest.xml
- ✅ Criar lint-baseline.xml
- ✅ Compilar Android com sucesso
- ✅ Criar estrutura de diretórios Windows
- ✅ Mover classes para novos namespaces
- ✅ Atualizar namespaces em arquivos movidos
- ✅ Atualizar imports no Program.cs
- ✅ Compilar Windows com sucesso
- ✅ Criar classes de suporte faltantes
- ✅ Documentar arquitetura

**Reorganização Completa! 🎉**
