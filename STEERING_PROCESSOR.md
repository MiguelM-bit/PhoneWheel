# Steering Processing - Implementação Completa

## 📋 Componentes Implementados

### CalibrationManager.cs
- ✅ Armazena offset de calibração
- ✅ Permite recalibração manual
- ✅ Aplica offset ao ângulo
- ✅ Thread-safe com locks

### SteeringProcessor.cs
- ✅ Aplica deadzone configurável (padrão: 5°)
- ✅ Limita ao intervalo [-450°, +450°]
- ✅ Normaliza para [-1.0, +1.0]
- ✅ Aplica suavização (EMA, padrão: 0.2)

## 🔄 Pipeline de Processamento

```
Ângulo Android (ex: 50°)
    ↓
Calibração (aplica offset)
    ↓ exemplo: offset = 0°
Ângulo Calibrado (50°)
    ↓
Deadzone (rejeita ±5°)
    ↓ 50° > 5° (fora da deadzone)
Deadzone OK (50°)
    ↓
Clamp [-450°, +450°]
    ↓ 50° ∈ intervalo
Clamped OK (50°)
    ↓
Normalizar (÷ 450)
    ↓ 50 / 450 = 0.1111
Normalized (0.1111)
    ↓
Smooth (α = 0.2)
    ↓ 0.2 * prev + 0.8 * current
Smoothed Final (≈ 0.0889)
```

## 📊 Mapeamento de Valores

| Ângulo (°) | Normalizado | Descrição |
|---|---|---|
| -450 | -1.0 | Máxima esquerda |
| -225 | -0.5 | Meia esquerda |
| -45 | -0.1 | Leve esquerda |
| 0 | 0.0 | Centro |
| +45 | +0.1 | Leve direita |
| +225 | +0.5 | Meia direita |
| +450 | +1.0 | Máxima direita |

## 🎯 Exemplos de Saída

```
[14:35:22.123] Android: 192.168.1.100
  ├─ Ângulo recebido:   45.50°
  ├─ Ângulo calibrado:  45.50°         (offset = 0°)
  ├─ Valor normalizado: 0.1011
  ├─ Gyro:              0.2340 rad/s
  └─ Timestamp:         1694567890000ms

[14:35:22.234] Android: 192.168.1.100
  ├─ Ângulo recebido:   -30.25°
  ├─ Ângulo calibrado:  -30.25°        (dentro da deadzone? não)
  ├─ Valor normalizado: -0.0671
  ├─ Gyro:              -0.1520 rad/s
  └─ Timestamp:         1694567891000ms

[14:35:22.345] Android: 192.168.1.100
  ├─ Ângulo recebido:   2.00°          (dentro da deadzone!)
  ├─ Ângulo calibrado:  2.00°
  ├─ Valor normalizado: 0.0000         (mapeado para 0 pelo deadzone)
  ├─ Gyro:              0.0050 rad/s
  └─ Timestamp:         1694567892000ms
```

## ⚙️ Configuração Padrão

```
Deadzone:       5.0°
Smoothing:      0.2 (20%)
Min Angle:      -450°
Max Angle:      +450°
Normalize Range: 450°
```

## 🧪 Casos de Teste Implementados

Script: `test-steering-processing.ps1`

1. ✅ Volante centralizado (0°)
2. ✅ Virada suave direita (45°)
3. ✅ Virada suave esquerda (-45°)
4. ✅ Virada extrema direita (200°)
5. ✅ Virada extrema esquerda (-200°)
6. ✅ Limite máximo direita (450°)
7. ✅ Limite máximo esquerda (-450°)
8. ✅ Acima do limite (500° será clamped)
9. ✅ Dentro da deadzone (2° será 0°)
10. ✅ Sequência de movimento suave (0° → 200°)

## 🏗️ Separação de Responsabilidades

| Componente | Responsabilidades | Restrições |
|---|---|---|
| **CalibrationManager** | • Armazenar offset<br>• Aplicar calibração<br>• Thread-safe | ✗ Sem processamento<br>✗ Sem lógica de rede |
| **SteeringProcessor** | • Deadzone<br>• Clamping<br>• Normalização<br>• Suavização | ✗ Sem calibração<br>✗ Sem vJoy<br>✗ Sem UI |
| **Program.cs** | • Orquestração<br>• Ligação de eventos<br>• Exibição | ✗ Sem lógica de negócio |

## 📈 Continuidade do Fluxo

```
Android App
    ↓ UDP Packet JSON
Windows UdpServer
    ↓ Event: SteeringDataReceived
Program.cs
    ├─→ CalibrationManager.ApplyCalibration()
    ├─→ SteeringProcessor.Process()
    └─→ Console.WriteLine()
        
        Próximo: VirtualController (vJoy)
        └─→ vejoy.SetAxis(normalizedValue)
```

## ✅ Status

- [x] CalibrationManager implementado
- [x] SteeringProcessor implementado
- [x] Integração em Program.cs
- [x] Build com sucesso (0 erros)
- [x] Documentação completa
- [x] Testes criados
- [x] GitHub sincronizado

## 🎯 Próximas Etapas

1. **VirtualController** - Integração com vJoy
   - Instanciar driver vJoy
   - Mapear valor normalizado para eixo
   - Enviar feedback

2. **Testes Unitários**
   - Validar cálculos de deadzone
   - Validar normalização
   - Validar suavização

3. **Interface de Controle**
   - CLI para ajustar calibração
   - CLI para alterar deadzone/smoothing
   - Salvar/carregar perfis

4. **Logging**
   - Registrar pacotes processados
   - Rastrear taxa de erro
   - Monitorar latência

---

**Versão**: 1.0 | **Build**: Sucesso ✅ | **Escopo**: Respeitado ✅
