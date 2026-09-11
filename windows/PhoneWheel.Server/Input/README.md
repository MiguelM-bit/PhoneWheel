# Input Component - Steering Processing

## Visão Geral

O componente `Input` é responsável por transformar valores brutos de ângulo recebidos do Android em valores normalizados e suavizados, prontos para serem utilizados pelo componente `VirtualController`.

## Arquitetura

### Componentes

#### 1. CalibrationManager.cs

Gerencia a calibração do volante independentemente do processamento.

**Responsabilidades:**
- Armazenar o centro configurado (offset)
- Permitir recalibração manual
- Aplicar offset aos valores
- Manter calibração separada (thread-safe)

**Interface:**

```csharp
var calibration = new CalibrationManager();
calibration.Calibrate(45.0);              // Registra 45° como novo centro
var calibrated = calibration.ApplyCalibration(90.0); // 90° - 45° = 45°
calibration.ResetCalibration();            // Volta ao padrão (offset = 0)
var info = calibration.GetInfo();          // Obtém informações
```

**Thread-safety:**
- Usa lock para todas as operações
- Seguro para ambientes multi-threaded
- Suportado: leitura concorrente, escritas exclusivas

#### 2. SteeringProcessor.cs

Processa ângulo calibrado e retorna valor normalizado.

**Responsabilidades:**
1. Aplicar deadzone
2. Limitar ao intervalo [-450°, +450°]
3. Normalizar para [-1.0, +1.0]
4. Aplicar suavização (filtro)

**Interface:**

```csharp
var processor = new SteeringProcessor(
    deadzone: 5.0,        // Graus (padrão: 5°)
    smoothingFactor: 0.2  // [0, 1] (padrão: 0.2)
);

var normalized = processor.Process(45.0); // Retorna número em [-1.0, 1.0]
var info = processor.GetInfo();           // Obtém configuração
```

**Mapeamento:**
```
-450° → -1.0
  0° →  0.0
+450° → +1.0
```

## Pipeline de Processamento

```
┌─────────────────────────────────┐
│  Pacote UDP do Android          │
│  angle: 50.0°                   │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  CalibrationManager             │
│  ApplyCalibration(50.0)         │
│  (offset = 5.0°)                │
│  50.0 - 5.0 = 45.0°             │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  SteeringProcessor              │
│                                 │
│  1. ApplyDeadzone (5°)          │
│     45.0° > 5.0° → 45.0°        │
│                                 │
│  2. Clamp [-450°, +450°]        │
│     45.0° ∈ range → 45.0°       │
│                                 │
│  3. Normalize (÷ 450)           │
│     45.0 / 450 = 0.1            │
│                                 │
│  4. Smooth (α=0.2)              │
│     0.2 * 0.0 + 0.8 * 0.1       │
│     = 0.08                      │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  Valor Final: 0.0800            │
│  (entre -1.0 e +1.0)            │
└─────────────────────────────────┘
```

## Detalhes Técnicos

### Deadzone

Valores dentro de ±deadzone° em torno de 0° são mapeados para 0.

```
-5°  ┌────────┐  +5°
     │ deadzone│
     └────────┘
      = 0.0
```

**Benefício:** Elimina tremulações naturais do sensor quando o volante está centralizado.

### Normalização

Converte graus para valor decimal entre -1.0 e +1.0.

**Fórmula:**
```
normalized = angle / 450.0
```

**Exemplos:**
```
-450° → -1.0
-225° → -0.5
 -45° → -0.1
   0° →  0.0
  +45° → +0.1
 +225° → +0.5
 +450° → +1.0
```

### Suavização (Exponential Moving Average)

Reduz oscilações rápidas aplicando filtro de média móvel exponencial.

**Fórmula:**
```
smoothed = α * previous + (1 - α) * current
```

Onde α = smoothingFactor (padrão: 0.2)

**Comportamento:**
- α = 0.0 → sem suavização (resposta imediata)
- α = 0.5 → suavização média
- α = 1.0 → máxima suavização (resposta muito lenta)

**Exemplo (α = 0.2):**
```
t=0: current = 0.0  → smoothed = 0.0
t=1: current = 0.2  → smoothed = 0.2 * 0.0 + 0.8 * 0.2 = 0.16
t=2: current = 0.3  → smoothed = 0.2 * 0.16 + 0.8 * 0.3 = 0.272
t=3: current = 0.25 → smoothed = 0.2 * 0.272 + 0.8 * 0.25 = 0.254
```

### Limitação (Clamping)

Garante que o valor final nunca ultrapasse [-450°, +450°].

Protege contra: valores corrompidos, sensor defeituoso, overflow.

## Configurações

### Defaults

| Parâmetro | Valor | Descrição |
|-----------|-------|-----------|
| Deadzone | 5.0° | Intervalo de silêncio |
| Smoothing | 0.2 (20%) | Filtro suavizador |
| Min Angle | -450° | Limite inferior |
| Max Angle | +450° | Limite superior |

### Ajuste Recomendado

```csharp
// Resposta rápida (para corridas)
var processor = new SteeringProcessor(deadzone: 2.0, smoothingFactor: 0.1);

// Resposta equilibrada (padrão)
var processor = new SteeringProcessor(deadzone: 5.0, smoothingFactor: 0.2);

// Resposta suave (para simuladores)
var processor = new SteeringProcessor(deadzone: 8.0, smoothingFactor: 0.4);
```

## Separação de Responsabilidades

| Componente | Faz | Não Faz |
|---|---|---|
| CalibrationManager | Calibração, offset | Processamento, cálculos |
| SteeringProcessor | Normalização, filtros | Calibração, rede |
| Program | Orquestração | Lógica de negócio |
| UdpServer | Rede, eventos | Processamento |

## Uso Integrado

```csharp
var calibration = new CalibrationManager();
var processor = new SteeringProcessor();

// Receber pacote
var angle = 50.0;

// Processar
var calibrated = calibration.ApplyCalibration(angle);
var normalized = processor.Process(calibrated);

// Usar em vJoy (próximo)
vjoy.SetSteeringValue(normalized);
```

## Próximos Passos

1. Integração com VirtualController (aplicar valor no vJoy)
2. Interface CLI para ajustar calibração/deadzone
3. Perfis de sensibilidade salvos
4. Logging de dados para análise
5. Testes unitários com valores conhecidos

---

**Status**: Implementado e testado
