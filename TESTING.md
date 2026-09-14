# Testes e Uso — PhoneWheel

Guia central de como **usar** e **testar** o PhoneWheel. Cobre a descoberta de servidor (última implementação), handshake, steering e estabilidade.

## 📋 Pré-requisitos

- **Windows**: .NET SDK 10.0+ (`dotnet --version`)
- **Android**: JDK 11+, Android SDK (API 34), Gradle wrapper incluso
- **Rede**: ambos os dispositivos na **mesma rede local** (mesmo subnet)

---

## 🔍 Descoberta de Servidor (última implementação)

Permite que o app Android encontre o servidor Windows **sem digitar o IP manualmente**, via broadcast UDP.

### Como funciona

1. O usuário pressiona **"Buscar servidor"** no app Android.
2. O Android envia `discover` via broadcast para `255.255.255.255:5005` (repetido durante uma janela de ~2s).
3. O servidor Windows (que já escuta em `0.0.0.0:5005`) responde com `discover_ack` contendo seu IP e porta.
4. O Android coleta as respostas, deduplica por IP e exibe uma lista.
5. O usuário seleciona o servidor → os campos IP/porta são preenchidos automaticamente.

### Como usar (no app Android)

1. Iniciar o servidor Windows (ver [README.md](README.md#-executar)).
2. Abrir o app no celular (mesma rede Wi-Fi).
3. Pressionar **"Buscar servidor"**.
4. Na lista exibida, tocar no servidor encontrado (ex: `192.168.0.76:5005`).
5. Pressionar **"Conectar"** para iniciar o handshake.

> **Fallback**: se nenhum servidor for encontrado (roteador bloqueia broadcast ou subnets diferentes), digite o IP/porta manualmente — o fluxo antigo continua funcionando.

### Como testar (script PowerShell)

O script `test-discovery.ps1` simula o Android: envia `discover` via broadcast e valida a resposta `discover_ack`.

```powershell
# 1. Iniciar o servidor em um terminal
cd windows
dotnet run --project PhoneWheel.Server/PhoneWheel.Server.csproj

# 2. Em outro terminal, rodar o teste
.\test-discovery.ps1
```

**Saída esperada (PASS):**

```
[SEND] Enviando DISCOVER para 255.255.255.255:5005 ...
[OK] DISCOVER_ACK recebido de 192.168.0.76:5005
[OK] Servidor encontrado: 192.168.0.76:5005
[OK] Todas as validacoes passaram!
```

**No console do servidor:**

```
[19:53:05.619] [OK] DISCOVER recebido de 192.168.0.76:63396
[19:53:05.710] [INFO] DISCOVER_ACK enviado para 192.168.0.76:63396
```

**Validações do script:** `type=discover_ack`, `device=PhoneWheel`, `version=2.0`, `server_ip` preenchido, `server_port=5005`, `timestamp>0`.

### Limitações conhecidas

| Limitação | Impacto | Mitigação |
|-----------|---------|-----------|
| Broadcast não atravessa subnets | Servidor não encontrado em redes diferentes | Digitar IP manualmente |
| Alguns roteadores bloqueiam broadcast | Servidor não encontrado | Digitar IP manualmente |
| Descoberta é manual (botão) | Sem descoberta contínua automática | Pressionar "Buscar servidor" novamente |

---

## 🖥️ Interface Visual (WPF)

Aplicativo Windows com volante giratório, status de conexão e controle iniciar/parar. Substitui o console como forma principal de operar o servidor.

### Como usar

```powershell
cd windows
dotnet run --project PhoneWheel.Server.UI/PhoneWheel.Server.UI.csproj
```

1. Pressionar **Iniciar** — o servidor passa a escutar em `0.0.0.0:5005` (status: *Aguardando conexão*).
2. No Android, pressionar **"Buscar servidor"** e selecionar o servidor, ou digitar o IP manualmente, e **"Conectar"**.
3. Ao conectar, o status muda para *Conectado* e o **volante gira** conforme o celular é inclinado.
4. O painel mostra ângulo recebido, calibrado, normalizado, giroscópio e contagem de pacotes.
5. Pressionar **Parar** encerra o servidor (status: *Parado*).

### Como testar (sem Android)

Com a UI aberta e **Iniciar** pressionado, envie pacotes de outro terminal:

```powershell
# Handshake + steering (simula o celular)
$socket = New-Object System.Net.Sockets.UdpClient
$connect = @{ type="connect"; device="PhoneWheel"; version="2.0"; timestamp=[DateTimeOffset]::Now.ToUnixTimeMilliseconds() } | ConvertTo-Json
$bytes = [Text.Encoding]::UTF8.GetBytes($connect)
$socket.Send($bytes, $bytes.Length, "127.0.0.1", 5005)
Start-Sleep -Milliseconds 200
$steer = @{ type="steering"; device="PhoneWheel"; version="2.0"; angle=67.5; timestamp=[DateTimeOffset]::Now.ToUnixTimeMilliseconds() } | ConvertTo-Json
$bytes = [Text.Encoding]::UTF8.GetBytes($steer)
$socket.Send($bytes, $bytes.Length, "127.0.0.1", 5005)
```

**Esperado na UI:** status *Conectado*, ângulo recebido `67,5°`, volante girado, log com os pacotes processados.

> **Nota:** o console (`PhoneWheel.Server`) continua disponível como alternativa e compartilha o mesmo motor (`ServerEngine`).

---

## 🎮 Testador de Controle (controllertest.io)

A UI WPF inclui uma aba **"Teste de controle"** com o [controllertest.io](https://controllertest.io) embutido (via WebView2). Ela lê o controle virtual (vJoy ou Xbox 360/ViGEmBus) diretamente do navegador e mostra em tempo real quais botões/eixos estão ativos — ideal para validar a integração sem abrir um jogo.

### Como usar

1. Iniciar o servidor (UI) e pressionar **Iniciar**.
2. Conectar o Android (ou usar `test-button.ps1` para simular).
3. Abrir a aba **"Teste de controle"**.
4. Pressionar os botões touch no Android e observar o mapeamento no testador.

> **Requisito**: o **WebView2 Runtime** deve estar instalado (vem com o Windows 11 ou via [Microsoft Edge WebView2](https://developer.microsoft.com/microsoft-edge/webview2/)). Se faltar, a UI mostra um aviso ao abrir a aba.

### Cenários de teste de botões

| Cenário | Como testar | Esperado |
|---------|-------------|----------|
| Pressionar | Tocar e segurar um botão no Android | Botão acende no testador |
| Soltar | Soltar o botão | Botão apaga no testador |
| Vários botões | Segurar A + B + X + Y simultaneamente | Todos acendem ao mesmo tempo |
| Conectar/desconectar | Parar e reiniciar o servidor com o Android conectado | Botões liberados; reconectar funciona |
| Perder conexão | Desligar o Wi-Fi do celular (ou pausar >500ms) | Watchdog libera todos os botões pressionados |
| Reconectar | Religar o Wi-Fi / reconectar | Botões voltam a funcionar |
| Steering + botões | Inclinar o celular enquanto segura um botão | Eixo se move **e** o botão permanece aceso |

> **Nota**: o testador também valida o **eixo do volante** (vJoy: eixo Z; Xbox 360: eixo LX) — incline o celular e confira a barra do eixo no controllertest.io.

---

## 🤝 Handshake (connect/connect_ack)

O Android envia `connect` e só envia `steering` após receber `connect_ack`. O servidor rejeita `steering` de clientes não autenticados.

### Como testar

Com o servidor rodando, envie um `connect` e verifique a resposta:

```powershell
$socket = New-Object System.Net.Sockets.UdpClient
$connect = @{ type="connect"; device="PhoneWheel"; version="2.0"; timestamp=[DateTimeOffset]::Now.ToUnixTimeMilliseconds() } | ConvertTo-Json
$bytes = [Text.Encoding]::UTF8.GetBytes($connect)
$socket.Send($bytes, $bytes.Length, "127.0.0.1", 5005)
$socket.Client.ReceiveTimeout = 2000
$ack = $socket.Receive([ref](New-Object System.Net.IPEndPoint([Net.IPAddress]::Any, 0)))
[Text.Encoding]::UTF8.GetString($ack)
```

**Esperado:** resposta `connect_ack` com `type`, `device`, `version` e `timestamp`.

**Cenários validados (todos PASS):**

| Cenário | Resultado |
|---------|-----------|
| Handshake com servidor ativo | ✅ `connect_ack` recebido |
| Timeout sem servidor | ✅ Exceção tratada, sem crash |
| Múltiplos handshakes sequenciais | ✅ 3/3 conexões |
| Steering sem handshake | ✅ Ignorado pelo servidor |
| Dispositivo inválido | ✅ Rejeitado (timeout) |
| Campos obrigatórios ausentes | ✅ Rejeitado (timeout) |

---

## 🎮 Steering (envio de dados de direção)

### Como testar

**Cenários de direção** — `test-steering-processing.ps1` envia 10 cenários (centro, viradas, limites ±450°, acima do limite, deadzone, sequência suave):

```powershell
# Servidor rodando em outro terminal
.\test-steering-processing.ps1
```

**Pacotes UDP básicos** — `test-udp-client.ps1` envia 2 pacotes válidos e 2 inválidos:

```powershell
.\test-udp-client.ps1
```

**Esperado no servidor:** pacotes válidos processados (ângulo, normalizado, vJoy); inválidos logados como `⚠ Pacote inválido` sem encerrar o servidor.

### Pipeline de processamento

```
Ângulo recebido → Calibração (offset) → Deadzone (±5°) → Clamp [-450°, +450°]
→ Normalização (÷450) → Suavização (EMA α=0.2) → Valor [-1.0, +1.0]
→ Controlador virtual (vJoy eixo Z ou Xbox 360 eixo LX)
```

---

## 🎮 Teste em Jogo Real (controle virtual)

Com os drivers instalados (ver [README.md](README.md#-drivers-de-controle-virtual)), o valor normalizado é enviado a um **controle virtual** que os jogos reconhecem. O servidor detecta automaticamente o backend disponível: **vJoy** (DirectInput) ou **Xbox 360** (ViGEmBus/XInput).

### 1. Validar o controle no Windows (antes do jogo)

1. Instale o driver (vJoy ou ViGEmBus) e reinicie o PC se solicitado.
2. Abra o **Game Controllers**: `Win+R` → `joy.cpl`.
3. Inicie o servidor (UI ou console) e conecte o Android.
4. Incline o celular: o eixo do controle virtual deve se mover (vJoy: eixo **Z**; Xbox 360: eixo **LX**).

> Se o eixo não aparecer no vJoy, abra o **vJoy Config**, habilite o Device 1 e marque o eixo **Z**.

### 2. Escolher um jogo compatível

| Tipo de jogo | Exemplos | Observação |
|--------------|----------|------------|
| **DirectInput** (vJoy) | Euro Truck Simulator 2, American Truck Simulator, BeamNG.drive, Forza Horizon (modo DirectInput) | Jogos mais antigos ou com suporte a volante |
| **XInput** (Xbox 360) | Forza Horizon 5, Need for Speed, GTA V, Rocket League, qualquer jogo com suporte a gamepad | Maioria dos jogos modernos |

### 3. Configurar o jogo

1. Abra o jogo com o servidor **já rodando** e o controle virtual ativo.
2. Nas configurações de **controle/volante**, selecione o dispositivo virtual (ex: "vJoy Device" ou "Xbox 360 Controller").
3. Mapeie o **eixo do volante** para a direção do veículo (vJoy: eixo Z; Xbox 360: eixo esquerdo).
4. Ajuste a **sensibilidade** no app Android e a **deadzone** conforme a resposta do jogo.

### 4. Testar

1. No jogo, incline o celular para a esquerda/direita — o veículo deve virar proporcionalmente.
2. Verifique no servidor (UI/console) se o valor normalizado acompanha o movimento.
3. Se o volante "tremer" ou ficar instável, aumente a suavização no app ou reduza a sensibilidade.

### Problemas comuns

| Sintoma | Causa provável | Solução |
|---------|----------------|---------|
| Jogo não reconhece o controle | Driver não instalado / backend errado | Instalar vJoy ou ViGEmBus; verificar `joy.cpl` |
| Eixo não se move no jogo | Eixo errado mapeado | Mapear eixo Z (vJoy) ou LX (Xbox 360) |
| Volante instável/treme | Suavização baixa ou sensibilidade alta | Aumentar suavização, reduzir sensibilidade |
| Jogo usa XInput mas só vJoy instalado | Backend incompatível | Instalar ViGEmBus (Xbox 360) |
| Controle some ao fechar o servidor | Controle virtual é criado em runtime | Manter o servidor aberto durante o jogo |

---

## 🔘 Botões (pacote `button`)

O servidor Windows processa eventos de botão (`type: "button"`) e os encaminha ao controle virtual. O app Android já envia esses eventos pela conexão UDP existente.

### Como testar (no app Android)

1. Iniciar o servidor Windows (UI ou console).
2. No Android, conectar ao servidor (buscar ou digitar IP).
3. Ao conectar, o card **"Gamepad (teste)"** aparece com 10 botões touch.
4. Tocar e segurar um botão → o servidor loga `Botão N pressionado`; soltar → `Botão N liberado`.

**Mapeamento dos botões touch:**

| Botão | ID | vJoy | Xbox 360 (ViGEmBus) |
|-------|----|------|---------------------|
| A | 0 | Botão 0 | A |
| B | 1 | Botão 1 | B |
| X | 2 | Botão 2 | X |
| Y | 3 | Botão 3 | Y |
| LB | 4 | Botão 4 | LB |
| RB | 5 | Botão 5 | RB |
| LT | 6 | Botão 6 | LS |
| RT | 7 | Botão 7 | RS |
| Back | 8 | Botão 8 | Back |
| Start | 9 | Botão 9 | Start |

> **Nota**: pressionar um botão já pressionado (ou liberar um já liberado) não gera pacote duplicado. Ao desconectar, todos os botões são liberados automaticamente — no Android via `releaseAll` e no servidor via watchdog (libera todos os botões pressionados no controle virtual).

### Como testar (script PowerShell)

O script `test-button.ps1` simula o Android: faz o handshake (`connect` → `connect_ack`) e envia uma sequência de press/release para os botões 0..3.

```powershell
# 1. Iniciar o servidor em um terminal
cd windows
dotnet run --project PhoneWheel.Server/PhoneWheel.Server.csproj

# 2. Em outro terminal, rodar o teste
.\test-button.ps1
```

**Saída esperada (PASS):**

```
[OK] CONNECT_ACK recebido de 127.0.0.1:5005
[SEND] Botão 0 pressionado
[SEND] Botão 0 liberado
[SEND] Botão 1 pressionado
...
```

**No console do servidor:**

```
[20:30:00.123] [OK] CONNECT recebido de 127.0.0.1:54321
[20:30:00.200] [INFO] CONNECT_ACK enviado para 127.0.0.1:54321
[20:30:00.350] [INFO] Botão 0 pressionado de 127.0.0.1
[20:30:00.500] [INFO] Botão 0 liberado de 127.0.0.1
```

### Mapeamento de botões

O mapeamento completo (ID → vJoy → Xbox 360) está na tabela da seção **"Como testar (no app Android)"** acima.

> **Nota**: botões fora do intervalo suportado (ex: >9 no Xbox 360) são rejeitados com log de erro, sem derrubar o servidor. O pacote `button` também mantém a conexão ativa no watchdog.

---

## 🛡️ Estabilidade (watchdog)

O servidor centraliza o volante (vJoy Z = 0) e **libera todos os botões pressionados** após **500ms sem pacotes**, restaurando quando os pacotes voltam.

### Como testar

1. Enviar 3 pacotes `steering` (ex: `test-udp-client.ps1`).
2. Pausar >500ms.
3. Enviar mais pacotes.

**Esperado no servidor:**

```
Conexão perdida! Nenhum pacote recebido por 558 ms
vJoy Z = 16383 (0.000 normalizado)   ← volante centralizado
Botões pressionados liberados        ← botões soltos no controle virtual
Conexão restaurada
```

---

## 🧪 Resumo dos Scripts de Teste

| Script | O que testa | Como rodar |
|--------|-------------|------------|
| `test-discovery.ps1` | Descoberta de servidor (broadcast) | `.\test-discovery.ps1` |
| `test-udp-client.ps1` | Envio de pacotes UDP (válidos/inválidos) | `.\test-udp-client.ps1` |
| `test-steering-processing.ps1` | Cenários de direção (10 casos) | `.\test-steering-processing.ps1` |
| `test-button.ps1` | Handshake + eventos de botão (press/release) | `.\test-button.ps1` |

> Todos os scripts exigem o servidor Windows rodando (exceto o teste de timeout do handshake, que espera o servidor **parado**).

## ✅ Resultados Registrados

| Suite | Data | Resultado |
|-------|------|-----------|
| Handshake (7 cenários) | 2026-09-12 | ✅ ALL PASS |
| Descoberta de servidor (3 cenários) | 2026-09-13 | ✅ ALL PASS |
| Steering (10 cenários) | — | ✅ ALL PASS |
| Estabilidade/watchdog | — | ✅ PASS |
| Controle virtual (vJoy/ViGEmBus) | — | ✅ Implementado (teste em jogo pendente) |
| Botões (pacote `button`) | — | ✅ Implementado (Android + Windows) |
| Testador de controle (controllertest.io) | — | ✅ Implementado (aba WebView2 na UI) |

---

**Status**: Guia de testes atualizado | **Versão**: 2.0