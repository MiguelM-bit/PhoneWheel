# Protocolo de Comunicação - PhoneWheel

## 📡 Visão Geral

A comunicação entre o aplicativo Android e o servidor Windows é realizada via **UDP (User Datagram Protocol)**, utilizando mensagens no formato **JSON UTF-8**.

## 🔌 Especificações Básicas

- **Protocolo**: UDP
- **Camada**: Transporte (Camada 4 - OSI)
- **Porta padrão**: `5005`
- **Formato inicial**: JSON UTF-8
- **Características**:
  - Sem conexão
  - Baixa latência
  - Ideal para aplicações em tempo real
  - Sem garantia de entrega, mas com velocidade

## 📊 Razão da Escolha do UDP

UDP foi escolhido por:
1. **Baixa latência** - Essencial para controle em tempo real; ao contrário do TCP, não há handshake, controle de congestionamento ou espera por confirmação de entrega
2. **Simplicidade** - Implementação direta em ambas plataformas
3. **Performance** - Menos overhead que TCP
4. **Adequação** - Perda ocasional de pacotes é aceitável para controle de entrada, já que um novo pacote com dados mais recentes chega em seguida

> **Nota sobre evolução futura**: O formato JSON foi escolhido para esta fase inicial por ser simples de implementar, ler e depurar. Futuramente, o protocolo poderá ser otimizado para um **formato binário** (por exemplo, campos de tamanho fixo serializados diretamente em bytes), reduzindo o tamanho dos pacotes e o overhead de parsing, uma vez que a comunicação e os dados estejam estabilizados.

## 📦 Pacote: `steering`

Este é o primeiro tipo de pacote definido no protocolo. Representa o estado atual da direção calculado pelo aplicativo Android, enviado periodicamente ao servidor Windows.

### Formato

```json
{
  "type": "steering",
  "angle": 137.4,
  "gyro": -0.52,
  "timestamp": 123456789
}
```

### Descrição dos Campos

| Campo       | Tipo   | Unidade                  | Descrição                                                                 |
|-------------|--------|--------------------------|----------------------------------------------------------------------------|
| `type`      | string | -                        | Identifica o tipo de mensagem. Para este pacote, o valor é sempre `"steering"`. Permite que o receptor distinga diferentes tipos de mensagens no futuro. |
| `angle`     | float  | graus (°)                | Ângulo de direção acumulado, calculado pelo `SteeringProcessor` a partir da integração da velocidade angular do giroscópio ao longo do tempo. Varia dentro do intervalo configurado (inicialmente `-450°` a `+450°`). |
| `gyro`      | float  | radianos por segundo (rad/s) | Velocidade angular instantânea lida diretamente do sensor de giroscópio no eixo configurado (X, Y ou Z), antes da integração. Representa o dado "bruto" no momento do envio. |
| `timestamp` | long   | milissegundos (ms) desde a época Unix (Unix epoch) | Momento em que o pacote foi gerado no dispositivo Android. Usado para identificar a ordem/recência dos pacotes e permitir que o receptor descarte pacotes atrasados ou fora de ordem. |

### Observações sobre os Campos

- **`angle`**: representa o estado já processado (integrado), pronto para ser interpretado como a posição do "volante".
- **`gyro`**: representa o dado bruto do sensor, útil para diagnóstico, calibração e depuração no lado do servidor.
- **`timestamp`**: não é sincronizado entre dispositivos (relógio local do Android). Deve ser usado apenas para comparação relativa entre pacotes recebidos da mesma origem, não como tempo absoluto confiável.

---

## 📦 Pacote: `connect` (Handshake)

Enviado pelo Android quando o usuário pressiona "Conectar". Inicia o handshake com o servidor Windows.

### Formato

```json
{
  "type": "connect",
  "device": "PhoneWheel",
  "version": "2.0",
  "timestamp": 123456789
}
```

### Descrição dos Campos

| Campo       | Tipo   | Descrição                                                    |
|-------------|--------|--------------------------------------------------------------|
| `type`      | string | Sempre `"connect"` para pacotes de handshake inicial         |
| `device`    | string | Identificador do dispositivo (`"PhoneWheel"`)                |
| `version`   | string | Versão do protocolo (ex: `"2.0"`)                            |
| `timestamp` | long   | Timestamp local do Android em milissegundos                  |

### Comportamento Esperado

1. **Android** envia `connect` repetidamente até receber resposta
2. **Windows** responde com `connect_ack` confirmando a conexão
3. **Android** aguarda `connect_ack` por timeout configurável (ex: 5 segundos)
4. Se resposta não chegar, o Android permanece em estado `DISCONNECTED`

---

## 📦 Pacote: `connect_ack` (Resposta de Handshake)

Enviado pelo Windows em resposta a um `connect`. Confirma que o servidor está ativo e pronto para receber dados.

### Formato

```json
{
  "type": "connect_ack",
  "device": "PhoneWheel",
  "version": "2.0",
  "timestamp": 123456789
}
```

### Descrição dos Campos

| Campo       | Tipo   | Descrição                                          |
|-------------|--------|--------------------------------------------------|
| `type`      | string | Sempre `"connect_ack"`                           |
| `device`    | string | Eco do identificador do dispositivo               |
| `version`   | string | Versão do protocolo suportada pelo Windows        |
| `timestamp` | long   | Timestamp do servidor Windows em milissegundos    |

### Observações

- **Windows** usa `connect_ack` para informar ao Android que está ativo e pronto
- O IP do cliente é extraído do endereço remoto do socket UDP
- Apenas uma resposta por `connect` é suficiente para Android considerar conectado
- Se Android receber múltiplos `connect_ack`, apenas o primeiro é processado

---

## 📦 Pacote: `discover` (Descoberta de Servidor)

Enviado pelo Android via **broadcast UDP** (`255.255.255.255:5005`) quando o usuário pressiona "Buscar servidor". Permite encontrar o servidor Windows na rede local sem digitar o IP manualmente.

### Formato

```json
{
  "type": "discover",
  "device": "PhoneWheel",
  "version": "2.0",
  "timestamp": 123456789
}
```

### Descrição dos Campos

| Campo       | Tipo   | Descrição                                                    |
|-------------|--------|--------------------------------------------------------------|
| `type`      | string | Sempre `"discover"` para pacotes de descoberta               |
| `device`    | string | Identificador do dispositivo (`"PhoneWheel"`)                |
| `version`   | string | Versão do protocolo (ex: `"2.0"`)                            |
| `timestamp` | long   | Timestamp local do Android em milissegundos                  |

### Comportamento Esperado

1. **Android** envia `discover` via broadcast para `255.255.255.255:5005` (repetido algumas vezes durante a janela de descoberta)
2. **Windows** recebe o broadcast (já escuta em `0.0.0.0:5005`) e responde com `discover_ack` para o IP/porta de origem do cliente
3. **Android** coleta respostas por uma janela configurável (ex: 2 segundos), deduplicando por IP
4. Se nenhum servidor responder, o Android informa "Nenhum servidor encontrado"

> **Nota**: O broadcast não atravessa subnets. Ambos os dispositivos devem estar na mesma rede local (mesmo subnet).

---

## 📦 Pacote: `discover_ack` (Resposta de Descoberta)

Enviado pelo Windows em resposta a um `discover`. Informa ao Android o IP e a porta do servidor.

### Formato

```json
{
  "type": "discover_ack",
  "device": "PhoneWheel",
  "version": "2.0",
  "server_ip": "192.168.1.100",
  "server_port": 5005,
  "timestamp": 123456789
}
```

### Descrição dos Campos

| Campo         | Tipo   | Descrição                                          |
|---------------|--------|--------------------------------------------------|
| `type`        | string | Sempre `"discover_ack"`                          |
| `device`      | string | Eco do identificador do dispositivo               |
| `version`     | string | Versão do protocolo suportada pelo Windows        |
| `server_ip`   | string | Endereço IPv4 do servidor na rede local           |
| `server_port` | int    | Porta UDP em que o servidor escuta (padrão: 5005) |
| `timestamp`   | long   | Timestamp do servidor Windows em milissegundos    |

### Observações

- **Windows** determina seu IP local enumerando as interfaces de rede (preferindo IPv4 não-loopback no mesmo subnet do cliente)
- O Android também conhece o IP do servidor pela origem do pacote; `server_ip` é informativo para exibição/verificação
- Apenas uma resposta por `discover` é necessária; respostas duplicadas são ignoradas

## 📦 Pacote: `heartbeat` (Manutenção de Conexão)

Enviado pelo servidor Windows periodicamente (ex: a cada 1 segundo) para os clientes conectados para informar que o servidor continua ativo.

### Formato

```json
{
  "type": "heartbeat",
  "device": "PhoneWheel",
  "version": "2.0",
  "timestamp": 123456789
}
```

### Descrição dos Campos

| Campo       | Tipo   | Descrição                                          |
|-------------|--------|--------------------------------------------------|
| `type`      | string | Sempre `"heartbeat"`                             |
| `device`    | string | Identificador do dispositivo (`"PhoneWheel"`)    |
| `version`   | string | Versão do protocolo suportada pelo Windows        |
| `timestamp` | long   | Timestamp do servidor Windows em milissegundos    |

### Observações

- O Windows envia heartbeats periodicamente em uma thread/tarefa paralela após a conexão inicial.
- O Android escuta ativamente por esses pacotes para monitorar a saúde do servidor.
- Se o Android passar do limite de tempo configurado (ex: 3 segundos) sem receber um heartbeat, ele deve declarar a conexão como perdida (estado `CONNECTION_LOST`).

---

## 🔄 Fluxo de Comunicação Completo

```
┌─────────────────┐                          ┌──────────────────┐
│   Android App   │                          │  Windows Server  │
└────────┬────────┘                          └────────┬─────────┘
         │                                            │
         │ Usuário pressiona "Buscar servidor"        │
         │                                            │
         │────── DISCOVER (broadcast 255.255.255.255) →│
         │ ← ─────────────── DISCOVER_ACK ────────────│
         │                                            │
         │ Usuário pressiona "Conectar"               │
         │                                            │
         │─────────────── CONNECT ──────────────────→ │
         │                                            │
         │─────────────── CONNECT ──────────────────→ │ (retry)
         │                                            │ (valida)
         │                                            │ (registra IP)
         │ ← ─────────────── CONNECT_ACK ─────────────│
         │                                            │
         │ (marca como CONNECTED)                     │
         │ (aguarda steering/heartbeat)               │ (aguarda steering, envia heartbeat)
         │                                            │
         │─────────────── STEERING ──────────────────→ │
         │ ← ────────────── HEARTBEAT ─────────────── │
         │─────────────── STEERING ──────────────────→ │
         │─────────────── STEERING ──────────────────→ │
         │ ← ────────────── HEARTBEAT ─────────────── │
         │                                            │
         │ (sem heartbeat por 3s)                     │ (sem steering por 0.5s)
         │ (marca como CONNECTION_LOST)               │ (marca como DESCONECTADO)
```

---

## 🚦 Estados de Conexão

### Android
- **DISCONNECTED**: Não conectado, aguardando ação do usuário
- **CONNECTING**: Enviando CONNECT, aguardando resposta
- **CONNECTED**: Handshake completo, enviando STEERING
- **CONNECTION_LOST**: Perda de conexão detectada

### Windows
- **LISTENING**: Aguardando CONNECT de cliente
- **CONNECTED**: Dispositivo conectado, recebendo STEERING
- **TIMEOUT**: Nenhum pacote recebido por período configurado

---

## 🔄 Comportamento Esperado ao Processar Pacotes

- Cada pacote UDP recebido deve ser tratado como uma **mensagem independente e completa** (stateless).
- Se um pacote não puder ser processado (JSON malformado, campo ausente, tipo desconhecido, valor fora do esperado, etc.), o receptor deve:
  1. **Descartar o pacote silenciosamente** (sem interromper o serviço ou travar o listener);
  2. Opcionalmente registrar um log de diagnóstico (nível debug/warning) para auxiliar na investigação de problemas;
  3. Continuar aguardando o próximo pacote normalmente.
- Nenhuma resposta de erro é enviada de volta ao Android nesta fase — a comunicação é unidirecional (Android → Windows) e sem confirmação (sem ACK).
- Pacotes fora de ordem ou atrasados podem ser ignorados pelo receptor com base no campo `timestamp`, a critério da implementação futura do servidor.


## 🔄 Fluxo de Comunicação

```
[Android App]
     ↓
[Sensor de Giroscópio] → [SteeringProcessor]
     ↓
[Montagem do pacote JSON "steering"]
     ↓
[Envio via UDP Socket → porta 5005]
     ↓
[Windows Server - UDP Listener]
     ↓
[Parsing e validação do pacote]
     ↓
[Processamento de Dados] (definido em fase posterior)
```

## 🔐 Segurança

*A ser definida em versões posteriores do projeto*

Considerações futuras:
- Autenticação entre dispositivos
- Validação de origem
- Criptografia de dados (se necessário)

## 📝 Notas

- Ambos os componentes devem estar na mesma rede local
- Porta UDP padrão: `5005`
- Configurações de timeout e retry serão estabelecidas conforme necessário
- Este documento cobre apenas a definição do protocolo; a implementação de rede (sockets, envio/recebimento) será feita em etapas posteriores

---

**Status**: Protocolo inicial definido | **Versão**: 0.2.0 | **Data**: 2025
