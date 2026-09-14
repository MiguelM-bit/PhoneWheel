# PhoneWheel

Monorepo que integra um **aplicativo Android** (Kotlin) a um **servidor Windows** (C#/.NET), permitindo usar o giroscópio do smartphone como volante para controlar dispositivos virtuais (vJoy / Xbox 360 via ViGEmBus) em jogos.

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│  Android App (Kotlin)                                       │
│  ┌──────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │ Gyroscope    │→ │ SteeringProcessor│→ │ UdpClient     │  │
│  │ Manager      │  │ (integra ângulo) │  │ (JSON/UDP)    │  │
│  └──────────────┘  └──────────────────┘  └───────┬───────┘  │
│  ┌──────────────┐  ┌──────────────────┐          │          │
│  │ UI (10 botões)│→ │ ButtonPacket     │──────────┘          │
│  │ touch        │  │ (serializer)     │                     │
│  └──────────────┘  └──────────────────┘                     │
└─────────────────────────────────────────────────────────────┘
                                                    │ UDP :5005
                                                    ↓
┌─────────────────────────────────────────────────────────────┐
│  Windows Server (C#/.NET)                                   │
│  ┌──────────┐  ┌────────────────┐  ┌────────────────────┐   │
│  │ UdpServer│→ │ SteeringPipeline│→ │ IVirtualController│   │
│  │ (rede)   │  │ (calibra+norm.) │  │ (vJoy ou ViGEmBus)│   │
│  │          │  └────────────────┘  │ (SetSteering/      │   │
│  │          │  ┌────────────────┐  │  SetButton)        │   │
│  │          │→ │ Validação      │→ │                    │   │
│  │          │  │ (cliente+watch)│  │                    │   │
│  └──────────┘  └────────────────┘  └────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Fluxo de dados

1. **Android** lê o giroscópio, integra a velocidade angular em um ângulo acumulado (`-450°` a `+450°`) e envia pacotes JSON via UDP a cada ~50ms.
2. **Windows** recebe na porta `5005`, valida o JSON, aplica calibração → deadzone → normalização → suavização e envia o valor normalizado `[-1.0, +1.0]` ao controlador virtual.
3. **Botões**: a UI do Android envia eventos de botão (`type: "button"`) pela mesma conexão UDP; o servidor valida o cliente e encaminha `button`/`pressed` para `IVirtualController.SetButton()`.
4. **Eixos analógicos**: o Android pode enviar valores de analógico (`type: "axis"`, ex: `left_x`, `right_y`) normalizados em `[-1.0, 1.0]`; o servidor encaminha para `IVirtualController.SetAxis()`.
5. **Handshake**: o Android envia `connect` e só envia `steering`/`button`/`axis` após receber `connect_ack`.
6. **Descoberta de servidor**: o Android pode localizar o servidor na rede via broadcast UDP (`discover`/`discover_ack`) sem digitar o IP manualmente.
7. **Watchdog**: o servidor detecta perda de conexão (500ms sem pacotes), centraliza o volante e **libera todos os botões pressionados** automaticamente.
8. **Controle virtual**: o valor normalizado é enviado ao backend selecionado — **vJoy** (joystick DirectInput) ou **Xbox 360** (ViGEmBus/XInput) — que os jogos reconhecem como um controle real.

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
- **Seletor de controlador virtual** (vJoy ou Xbox 360) e log em tempo real
- Aba **Teste de controle** com o [controllertest.io](https://controllertest.io) embutido (WebView2) para validar os botões do controle virtual

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

## 🎮 Drivers de controle virtual

Para o Windows reconhecer o volante como um controle real em jogos, instale **um** dos drivers abaixo (o app detecta automaticamente qual está disponível):

### Opção A — vJoy (joystick DirectInput)

1. Baixe o instalador em <https://sourceforge.net/projects/vjoystick/> (versão 2.2.1 ou superior).
2. Instale e abra o **vJoy Config**.
3. Em "Device 1", marque **Enable** e defina **Axis Z** como o eixo do volante (o servidor envia o valor normalizado no eixo Z).
4. Para o **gamepad completo** (botões + D-pad + analógicos), defina **Buttons ≥ 14** e habilite os eixos **X, Y, Rx, Ry** (além do Z).
5. Aplique as configurações. O Windows passa a listar um "vJoy Device" em Dispositivos de Jogo.

### Opção B — ViGEmBus (Xbox 360 / XInput)

1. Baixe o driver em <https://github.com/ViGEm/ViGEmBus/releases> (arquivo `ViGEmBus_Setup_x64.exe`).
2. Execute o instalador e reinicie o PC se solicitado.
3. O servidor cria um controle **Xbox 360 virtual** com o eixo esquerdo (LX) mapeado para o volante — compatível com a maioria dos jogos modernos (XInput).

> **Dica**: para validar antes de abrir o jogo, use o **Game Controllers** do Windows (`joy.cpl`) — o eixo deve se mover ao inclinar o celular. Veja o guia completo em [TESTING.md](TESTING.md).

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
| Controle virtual **vJoy** (joystick DirectInput) | ✅ Integração real implementada |
| Controle virtual **Xbox 360** (ViGEmBus/XInput) | ✅ Integração real implementada |
| Botões (pacote `button`, Android → Windows) | ✅ Implementado e testado |
| Eixos analógicos (pacote `axis`, Android → Windows) | ✅ Implementado |
| Controller Screen (gamepad virtual completo em paisagem) | ✅ Implementado |
| Liberação de botões na perda de conexão | ✅ Implementado |
| Teste de controle (controllertest.io na UI) | ✅ Implementado |
| Teste em jogo real | ⚠️ Pendente (ver [TESTING.md](TESTING.md)) |
| Testes unitários | ❌ Não existem |

## 🗓️ Roadmap

- [x] Estrutura do monorepo + protocolo UDP
- [x] Aplicativo Android: giroscópio, steering, UDP, handshake
- [x] Servidor Windows: listener UDP, pipeline, watchdog
- [x] Descoberta de servidor via broadcast
- [x] Interface visual WPF (volante, status, iniciar/parar)
- [x] Integração real com driver vJoy (Fase 2)
- [x] Integração real com ViGEmBus (Xbox 360)
- [x] Botões touch (Android) → controle virtual (Windows)
- [x] Liberação de botões na perda de conexão
- [x] Testador de controle embutido (controllertest.io)
- [x] Controller Screen (gamepad virtual completo em paisagem)
- [x] Testes unitários (Android: controles virtuais, filtro de eixos)
- [ ] Teste em jogo real (validação final)
- [ ] Testes unitários (Windows)
- [ ] Configuração via arquivo (calibração, deadzone, porta)
- [ ] Múltiplos dispositivos Android simultâneos
- [ ] Logging persistente e métricas (latência, taxa de pacotes)

## 📝 Notas

- Ambos os dispositivos devem estar na **mesma rede local** (broadcast não atravessa subnets).
- O protocolo é JSON em texto puro sobre UDP — adequado para LAN confiável; criptografia fica para versões futuras.
- Componentes órfãos existentes (SettingsManager, steering managers no Android; HandshakeManager, HeartbeatManager, ProfileManager, UIStatusManager no Windows) ainda não estão integrados ao fluxo principal.

---

**Status**: Em desenvolvimento | **Versão**: 2.0
