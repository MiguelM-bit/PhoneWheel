# Protocolo de Comunicação - PhoneWheel

## 📡 Visão Geral

A comunicação entre o aplicativo Android e o servidor Windows é realizada via **UDP (User Datagram Protocol)**.

## 🔌 Especificações Básicas

- **Protocolo**: UDP
- **Camada**: Transporte (Camada 4 - OSI)
- **Características**: 
  - Sem conexão
  - Baixa latência
  - Ideal para aplicações em tempo real
  - Sem garantia de entrega, mas com velocidade

## 📊 Razão da Escolha

UDP foi escolhido por:
1. **Baixa latência** - Essencial para controle em tempo real
2. **Simplicidade** - Implementação direta em ambas plataformas
3. **Performance** - Menos overhead que TCP
4. **Adequação** - Perda ocasional de pacotes é aceitável para controle de entrada

## 📋 Estrutura de Pacotes

*A ser definida em versões posteriores do projeto*

Serão documentados:
- [ ] Formato de pacotes UDP
- [ ] Estrutura de headers customizados
- [ ] Tipos de mensagens (gyroscope data, commands, acknowledgments)
- [ ] Codificação de dados (JSON, binary, protobuf, etc.)
- [ ] Tamanho máximo de pacotes
- [ ] Checksums ou validação

## 🔄 Fluxo de Comunicação

*A ser definido em versões posteriores do projeto*

```
[Android App]
     ↓
[Sensor de Giroscópio]
     ↓
[Formato de Pacote UDP]
     ↓
[Envio via UDP Socket]
     ↓
[Windows Server - UDP Listener]
     ↓
[Processamento de Dados]
     ↓
[Ação (Mouse/Teclado/Outro)]
```

## 🔐 Segurança

*A ser definida em versões posteriores do projeto*

Considerações futuras:
- Autenticação entre dispositivos
- Validação de origem
- Criptografia de dados (se necessário)

## 📝 Notas

- Ambos os componentes devem estar na mesma rede local
- A porta UDP será definida durante a implementação
- Configurações de timeout e retry serão estabelecidas conforme necessário

---

**Status**: Documentação inicial | **Versão**: 0.1.0 | **Data**: 2025
