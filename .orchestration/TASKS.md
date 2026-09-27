# Backlog operacional

### TASK-001 — Helper de janela, tema, Manifest e splitMotionEvents
- **Prioridade:** HIGH
- **Responsável:** task-executor
- **Status:** IN_PROGRESS
- **Dependências:** Nenhuma
- **Ownership:** `ControllerWindowHelper.kt`, `styles.xml`, `AndroidManifest.xml`, layouts, teste de rotation
- **Observações:** Travar landscape atual; imersivo moderno; sem kiosk.

### TASK-002 — Lifecycle da ControllerActivity
- **Prioridade:** HIGH
- **Responsável:** task-executor
- **Status:** TODO
- **Dependências:** TASK-001
- **Ownership:** `ControllerActivity.kt`
- **Observações:** `pauseControllerInputs()` não desliga o modo volante.

### TASK-003 — Lifecycle da PreviewControllerActivity
- **Prioridade:** HIGH
- **Responsável:** task-executor
- **Status:** TODO
- **Dependências:** TASK-001
- **Ownership:** `PreviewControllerActivity.kt`
- **Observações:** Mesma política, sem UDP.

### TASK-004 — Integração
- **Prioridade:** HIGH
- **Responsável:** integrator
- **Status:** TODO
- **Dependências:** TASK-002, TASK-003
- **Ownership:** Activities + helper
- **Observações:** Sem alterar protocolo/Windows.

### TASK-005 — Validação
- **Prioridade:** HIGH
- **Responsável:** validator
- **Status:** TODO
- **Dependências:** TASK-004
- **Ownership:** testes unitários + compile
- **Observações:** Confirmar minSdk inalterado.
