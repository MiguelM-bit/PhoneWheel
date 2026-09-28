# Agents ativos

| Identificador | Função | Tarefa atual | Status | Ownership/arquivos | Dependências |
|---|---|---|---|---|---|
| principal | Orquestrador principal | 9 melhorias Android (LT/RT, settings, slider, preview, gyro) | ACTIVE | `.orchestration/` | — |
| E1 (task-executor) | Correção LT/RT via UDP | Item 1 — concluído | DONE | `input/**`, `network/ButtonSender.kt`, `connection/ConnectionManager.kt`, testes input/network | — |
| E2 (task-executor) | Rework do overlay de settings | Itens 2-4 (UI) — concluído | DONE | `ui/controls/SettingsOverlayView.kt` | — |
| E3 (task-executor) | Wiring + curva + preview + gyro | Cancelado (gradle travou) — retomado pelo principal | CANCELLED | — | E2 |
| principal | Integração Fase 2 | Itens 4-6 (persistência, curva, preview, lifecycle) — concluído | DONE | `settings/`, `ui/ControllerActivity.kt`, `ui/PreviewControllerActivity.kt`, `steering/`, `sensor/` | E1, E2 |
| principal | Validação final | Revisão manual de compilação + relatório | ACTIVE | — | integração |

## Notas
- Build/tests Android **não executados** nesta sessão (instrução do usuário: deixar para o usuário rodar).
- Comandos pendentes para o usuário: `android\gradlew.bat test` e `android\gradlew.bat assembleDebug`.
