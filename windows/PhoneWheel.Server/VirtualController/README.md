# VirtualController

Componente responsável pela abstração do controle virtual do joystick.

## Arquitetura

```
IVirtualController (interface)
    ├── VJoyController (implementação vJoy)
    └── (futuras implementações: XInput, DirectInput, etc)
```

## Componentes

### IVirtualController

Interface que define o contrato para qualquer implementação de controle virtual.

**Métodos:**
- `Connect()`: Conecta ao dispositivo virtual
- `Disconnect()`: Desconecta do dispositivo
- `SetSteering(double value)`: Define o valor de direção [-1.0, +1.0]
- `SetButton(int button, bool pressed)`: Define o estado de um botão (implementação futura)

**Propriedades:**
- `Status`: Retorna o status atual (Disconnected, Connected, Error)

**Exceções:**
- `VirtualControllerException`: Exceção customizada para erros específicos do controlador

### VJoyController

Implementação da interface `IVirtualController` usando a biblioteca vJoy.

**Responsabilidades:**
- Conectar ao dispositivo virtual vJoy (padrão: dispositivo 1)
- Mapear valores normalizados [-1.0, +1.0] para o intervalo do eixo vJoy
- Enviar valores de direção através do eixo X
- Gerenciar o ciclo de vida do dispositivo (Connect/Disconnect)
- Tratar erros de comunicação com vJoy

**Implementação atual:**
- Simulação de vJoy (fase 1)
- Mapeamento direto de [-1.0, +1.0] para [0, 32767]
- Logs de operações no console

**Integração com vJoy real:**
- Remover dependência de simulação
- Adicionar referência real à biblioteca vJoy
- Implementar todas as verificações de driver e dispositivo
- Teste em hardware real

## Uso

```csharp
using PhoneWheel.Server.VirtualController;

// Criar instância (dispositivo 1 é padrão)
var controller = new VJoyController(deviceId: 1);

try
{
    // Conectar
    controller.Connect();
    
    // Enviar comando de direção
    controller.SetSteering(0.5); // Meia volta para a direita
    
    // Desconectar
    controller.Disconnect();
}
catch (VirtualControllerException ex)
{
    Console.WriteLine($"Erro: {ex.Message}");
}
finally
{
    controller.Dispose();
}
```

## Estados

```
┌─────────────────┐
│  Disconnected   │
└────────┬────────┘
         │ Connect()
         v
┌─────────────────┐
│   Connected     │
└────────┬────────┘
         │ Erro ou Disconnect()
         v
┌─────────────────┐
│     Error       │
└─────────────────┘
```

## Tratamento de Erros

- Se vJoy não estiver instalado → `VirtualControllerException`
- Se o dispositivo não estiver disponível → `VirtualControllerException`
- Se um valor inválido for enviado → `VirtualControllerException`
- Se estiver desconectado ao enviar dados → `VirtualControllerException`

## Roadmap

1. **Fase 1** (atual): Simulação com logs
2. **Fase 2**: Integração real com vJoy
3. **Fase 3**: Implementação de botões
4. **Fase 4**: Suporte a múltiplos dispositivos
5. **Fase 5**: Suporte a XInput/DirectInput como fallback

## Notas Técnicas

- O mapeamento de valores é feito internamente ao `VJoyController`
- A interface recebe sempre valores normalizados [-1.0, +1.0]
- Valores fora desse intervalo são clampeados
- Atualizações muito pequenas (<0.0001) são ignoradas para reduzir carga
