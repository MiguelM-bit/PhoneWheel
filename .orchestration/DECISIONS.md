# Decisões

## DEC-001 — Travar o landscape atual
- **Data:** 2026-09-20
- **Decisão:** Ao abrir controle/prévia, lock `LANDSCAPE` ou `REVERSE_LANDSCAPE` conforme `Display.rotation`. Se ainda estiver em retrato, manter `sensorLandscape` até a janela estar em landscape.
- **Motivo:** Impedir virar 180° no meio do uso sem forçar um lado específico.
- **Impacto:** Layout estável durante o controle; ConnectionActivity restaura o comportamento ao voltar.
- **Alternativas:** `landscape` fixo (ignora o lado atual); `sensorLandscape` permanente (ainda vira 180°).

## DEC-002 — Pause não desliga o modo volante
- **Data:** 2026-09-20
- **Decisão:** `pauseControllerInputs()` solta controles e chama `stopListening()`, mas não zera `wheelModeEnabled` nem faz `recenter()`.
- **Motivo:** O resume precisa retomar o giroscópio no mesmo `GyroscopeManager` sem listener duplicado.
- **Impacto:** Background/lock screen não desliga o volante.
- **Alternativas:** Desligar o modo no pause (bug atual).

## DEC-003 — Sem kiosk / lock task
- **Data:** 2026-09-20
- **Decisão:** Imersivo com `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`. Usuário sai pelo sistema normalmente.
- **Motivo:** Pedido explícito de não impedir a saída.
- **Impacto:** Barras reaparecem com swipe; Home/Recents/Back continuam funcionando.
- **Alternativas:** Lock task / kiosk (rejeitado).
