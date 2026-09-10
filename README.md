# PhoneWheel

Projeto de monorepo para integração entre aplicativo Android e servidor Windows, permitindo controlar dispositivos via giroscópio do smartphone.

## 🎯 Objetivo

PhoneWheel é uma solução que permite usar um smartphone Android como controle remoto, capturando dados do giroscópio e enviando-os para um servidor Windows que, em seguida, controla dispositivos virtuais ou reais.

## 🏗️ Arquitetura

O projeto é organizado em um monorepo com dois componentes principais:

```
┌─────────────────────────────────────────────────┐
│          Android App (Kotlin)                   │
│  - Leitura do Giroscópio                        │
│  - Envio de dados via UDP                       │
└────────────────┬────────────────────────────────┘
                 │
                 │ UDP Packets
                 ↓
┌─────────────────────────────────────────────────┐
│       Windows Server (C#/.NET)                  │
│  - Recebimento de dados UDP                     │
│  - Controle de dispositivos virtuais            │
└─────────────────────────────────────────────────┘
```

### Componentes

- **`android/`** - Aplicativo Android nativo em Kotlin
  - Captura dados do giroscópio do dispositivo
  - Envia pacotes UDP para o servidor Windows
  
- **`windows/`** - Servidor Windows em C#/.NET
  - Recebe e processa pacotes UDP
  - Controla dispositivos virtuais (mouse, teclado, etc.)
  
- **`protocol/`** - Documentação do protocolo de comunicação
  - Especificação do formato dos pacotes UDP
  - Definição de mensagens e estruturas

## 🛠️ Tecnologias Planejadas

### Android
- **Linguagem**: Kotlin
- **Framework**: Android SDK
- **Sensor**: Giroscópio (GyroscopeSensor)
- **Comunicação**: UDP Sockets

### Windows
- **Linguagem**: C#
- **Framework**: .NET (versão a definir)
- **Comunicação**: UDP Sockets
- **Controle de Dispositivo**: Bibliotecas para mouse/teclado virtual

## 📡 Comunicação

A comunicação entre Android e Windows é feita através de **UDP (User Datagram Protocol)**:

- **Protocolo**: UDP
- **Porta**: A definir
- **Formato**: A definir em `protocol/protocol.md`
- **Frequência**: A definir (tempo real)

UDP foi escolhido por sua baixa latência, essencial para controle em tempo real.

## 🗓️ Roadmap

### Fase 1: Estrutura Base
- [x] Criar estrutura do monorepo
- [x] Documentação do protocolo UDP
- [ ] Configurar projetos Android e Windows

### Fase 2: Aplicativo Android
- [ ] Configurar projeto Kotlin/Android
- [ ] Implementar leitura do giroscópio
- [ ] Implementar envio de dados via UDP
- [ ] Teste de comunicação básica

### Fase 3: Servidor Windows
- [ ] Configurar projeto C#/.NET
- [ ] Implementar listener UDP
- [ ] Processar dados recebidos
- [ ] Teste de recebimento

### Fase 4: Integração
- [ ] Sincronização entre Android e Windows
- [ ] Controle de dispositivo virtual
- [ ] Testes de ponta a ponta
- [ ] Otimizações de performance

### Fase 5: Melhorias
- [ ] Interface gráfica (Android)
- [ ] Painel de controle (Windows)
- [ ] Suporte a múltiplos dispositivos
- [ ] Documentação completa

## 📝 Notas

- Este é um projeto inicial. Funcionalidades e tecnologias podem ser ajustadas conforme o desenvolvimento.
- A comunicação UDP será documentada mais detalhadamente na fase de implementação.
- Ambos os componentes (Android e Windows) devem estar na mesma rede para comunicação.

## 📄 Licença

A definir

---

**Status**: Em desenvolvimento | **Versão**: 0.1.0
