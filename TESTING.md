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
→ Normalização (÷450) → Suavização (EMA α=0.2) → Valor [-1.0, +1.0] → vJoy
```

---

## 🛡️ Estabilidade (watchdog)

O servidor centraliza o volante (vJoy X = 0) após **500ms sem pacotes** e restaura quando os pacotes voltam.

### Como testar

1. Enviar 3 pacotes `steering` (ex: `test-udp-client.ps1`).
2. Pausar >500ms.
3. Enviar mais pacotes.

**Esperado no servidor:**

```
Conexão perdida! Nenhum pacote recebido por 558 ms
vJoy X = 16383 (0.000 normalizado)   ← volante centralizado
Conexão restaurada
```

---

## 🧪 Resumo dos Scripts de Teste

| Script | O que testa | Como rodar |
|--------|-------------|------------|
| `test-discovery.ps1` | Descoberta de servidor (broadcast) | `.\test-discovery.ps1` |
| `test-udp-client.ps1` | Envio de pacotes UDP (válidos/inválidos) | `.\test-udp-client.ps1` |
| `test-steering-processing.ps1` | Cenários de direção (10 casos) | `.\test-steering-processing.ps1` |

> Todos os scripts exigem o servidor Windows rodando (exceto o teste de timeout do handshake, que espera o servidor **parado**).

## ✅ Resultados Registrados

| Suite | Data | Resultado |
|-------|------|-----------|
| Handshake (7 cenários) | 2026-09-12 | ✅ ALL PASS |
| Descoberta de servidor (3 cenários) | 2026-09-13 | ✅ ALL PASS |
| Steering (10 cenários) | — | ✅ ALL PASS |
| Estabilidade/watchdog | — | ✅ PASS |

---

**Status**: Guia de testes atualizado | **Versão**: 2.0