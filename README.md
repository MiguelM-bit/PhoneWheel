# PhoneWheel

Monorepo que integra um **aplicativo Android** (Kotlin) a um **servidor Windows** (C#/.NET), permitindo usar o giroscópio do smartphone como volante para controlar dispositivos virtuais (vJoy).

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│  Android App (Kotlin)                                       │
│  ┌──────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │ Gyroscope    │→ │ SteeringProcessor│→ │ UdpClient     │  │
│  │ Manager      │  │ (integra ângulo) │  │ (JSON/UDP)    │  │
│  └──────────────┘  └──────────────────┘  └───────┬───────┘  │
└───────────────────────────────────────────────────┼─────────┘
                                                    │ UDP :5005
                                                    ↓
┌─────────────────────────────────────────────────────────────┐
│  Windows Server (C#/.NET)                                   │
│  ┌──────────┐  ┌────────────────┐  ┌────────────────────┐   │
│  │ UdpServer│→ │ SteeringPipeline│→ │ IVirtualController│   │
│  │ (rede)   │  │ (calibra+norm.) │  │ (vJoy, Fase 1:    │   │
│  └──────────┘  └────────────────┘  │  simulação)        │   │
│                                    └────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Fluxo de dados

1. **Android** lê o giroscópio, integra a velocidade angular em um ângulo acumulado (`-450°` a `+450°`) e envia pacotes JSON via UDP a cada ~50ms.
2. **Windows** recebe na porta `5005`, valida o JSON, aplica calibração → deadzone → normalização → suavização e envia o valor normalizado `[-1.0, +1.0]` ao controlador virtual.
3. **Handshake**: o Android envia `connect` e só envia `steering` após receber `connect_ack`.
4. **Descoberta de servidor**: o Android pode localizar o servidor na rede via broadcast UDP (`discover`/`discover_ack`) sem digitar o IP manualmente.
5. **Watchdog**: o servidor detecta perda de conexão (500ms sem pacotes) e centraliza o volante automaticamente.

## 📁 Estrutura do Monorepo

```
PhoneWheel/
├── android/                          # Aplicativo Android (Kotlin)
│   └── app/src/main/java/com/phonewheel/
│       ├── ui/          # MainActivity (tela, botões, dialog de descoberta)
│       ├── sensor/      # GyroscopeManager, GyroAxis
│       ├── steering/    # SteeringProcessor, Calibration/Sensitivity/Deadzone/Smoothing
│       ├── network/     # UdpClient, PacketSerializer, ServerDiscovery, ConnectionState
│       ├── connection/  # ConnectionManager (handshake, heartbeat, reconexão)
│       ├── model/       # SteeringPacket, ConnectionPackets (connect/discover)
│       └── settings/    # SettingsManager
├── windows/                          # Servidor Windows (C#/.NET)
│   ├── PhoneWheel.Server/            # Núcleo do servidor (biblioteca + host console)
│   │   ├── Program.cs                # Host console (thin) usando ServerEngine
│   │   ├── Services/     # ServerEngine, ServerLogger, SteeringPipeline
│   │   ├── Network/      # UdpServer, PacketParser, ServerDiscovery
│   │   ├── Connection/   # ConnectionWatchdog, HandshakeManager, HeartbeatManager
│   │   ├── Input/        # SteeringProcessor, CalibrationManager
│   │   ├── VirtualController/  # IVirtualController, VJoyController (simulação)
│   │   ├── Profiles/     # SteeringProfile, ProfileManager
│   │   ├── UI/           # UIStatusManager
│   │   └── Models/       # SteeringPacket, ConnectPacket, DiscoverPacket, ...
│   └── PhoneWheel.Server.UI/         # Interface visual WPF (volante + status)
│       ├── MainWindow.xaml           # Volante giratório, painel de valores, log
│       ├── ViewModels/   # MainViewModel, RelayCommand
│       └── App.xaml                  # Bootstrap da aplicação
├── protocol/
│   └── protocol.md                   # Especificação do protocolo UDP (fonte da verdade)
├── test-discovery.ps1                # Teste de descoberta de servidor
├── test-udp-client.ps1               # Teste de envio de pacotes UDP
├── test-steering-processing.ps1      # Teste de cenários de direção
└── TESTING.md                        # Como usar e testar o projeto
```

## 🛠️ Compilar

### Windows (servidor)

```bash
cd windows
dotnet build PhoneWheel.sln
```

Requisitos: .NET SDK 10.0+

### Android (app)

```bash
cd android
gradlew.bat assembleDebug        # Windows
./gradlew assembleDebug           # Linux/Mac
```

O APK fica em `android/app/build/outputs/apk/debug/app-debug.apk`.

Requisitos: JDK 11+, Android SDK (API 34), Gradle wrapper incluso.

## 🚀 Executar

### Interface visual (recomendado)

```bash
cd windows
dotnet run --project PhoneWheel.Server.UI/PhoneWheel.Server.UI.csproj
```

Abre a janela WPF com:
- **Status de conexão** (Parado / Aguardando conexão / Conectado / Desconectado)
- Botão **Iniciar/Parar** o servidor
- **Volante giratório** que acompanha o ângulo de inclinação do celular
- Painel com ângulo recebido, calibrado, normalizado, giroscópio e contagem de pacotes
- Log em tempo real dos eventos do servidor

### Console (alternativa)

```bash
cd windows
dotnet run --project PhoneWheel.Server/PhoneWheel.Server.csproj
```

O servidor escuta em `0.0.0.0:5005` e aguarda pacotes.

### Conectar o Android (mesma rede Wi-Fi)

- Instalar o APK e abrir o app.
- Pressionar **"Buscar servidor"** para descobrir o servidor automaticamente (broadcast UDP) e selecionar o IP na lista.
- Ou digitar o IP/porta manualmente (fallback).
- Pressionar **"Conectar"** — o app faz o handshake e começa a enviar dados do giroscópio.

Na interface visual, o volante gira conforme o celular é inclinado; no console, cada pacote `steering` é exibido com ângulo, valor normalizado e status do controlador virtual.

## 📡 Protocolo

A especificação completa dos pacotes (`steering`, `connect`, `connect_ack`, `discover`, `discover_ack`) está em [`protocol/protocol.md`](protocol/protocol.md).

## 🧪 Testes

Instruções de uso e teste — incluindo a **descoberta de servidor** — estão em [`TESTING.md`](TESTING.md).

## ✅ Status Atual

| Área | Status |
|------|--------|
| Handshake (connect/connect_ack) | ✅ Implementado e testado |
| Envio de steering (Android → Windows) | ✅ Implementado e testado |
| Pipeline de processamento (calibração, deadzone, normalização, suavização) | ✅ Implementado e testado |
| Watchdog de conexão (500ms) | ✅ Implementado e testado |
| Descoberta de servidor (discover/discover_ack) | ✅ Implementado e testado |
| Interface visual WPF (volante, status, iniciar/parar) | ✅ Implementado e testado |
| Controle virtual vJoy | ⚠️ Simulação (Fase 1) — integração real pendente |
| Testes unitários | ❌ Não existem |

## 🗓️ Roadmap

- [x] Estrutura do monorepo + protocolo UDP
- [x] Aplicativo Android: giroscópio, steering, UDP, handshake
- [x] Servidor Windows: listener UDP, pipeline, watchdog
- [x] Descoberta de servidor via broadcast
- [x] Interface visual WPF (volante, status, iniciar/parar)
- [ ] Integração real com driver vJoy (Fase 2)
- [ ] Testes unitários (Android e Windows)
- [ ] Configuração via arquivo (calibração, deadzone, porta)
- [ ] Múltiplos dispositivos Android simultâneos
- [ ] Logging persistente e métricas (latência, taxa de pacotes)

## 📝 Notas

- Ambos os dispositivos devem estar na **mesma rede local** (broadcast não atravessa subnets).
- O protocolo é JSON em texto puro sobre UDP — adequado para LAN confiável; criptografia fica para versões futuras.
- Componentes órfãos existentes (SettingsManager, steering managers no Android; HandshakeManager, HeartbeatManager, ProfileManager, UIStatusManager no Windows) ainda não estão integrados ao fluxo principal.

---

**Status**: Em desenvolvimento | **Versão**: 2.0
