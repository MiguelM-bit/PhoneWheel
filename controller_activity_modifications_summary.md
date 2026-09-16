# Modifications to ControllerActivity.kt and activity_controller.xml

## Changes Made

### 1. ControllerActivity.kt
- Added `wheelInverted` flag to track inversion state
- Added import for `com.phonewheel.model.StickAxis`
- Added click listener for invert button (`buttonInvertWheel`) that:
  - Toggles `wheelInverted` flag
  - Updates button text to "Invertido" when active, "Inverter" when inactive
  - Calls `updateWheelUi()` with current steering value to immediately apply inversion
- Modified `updateWheelUi()` function to:
  - Apply inversion by negating the clamped value when `wheelInverted` is true
  - Send the X axis value via `axisSender.send(StickId.LEFT.xAxis, output)` when `wheelModeEnabled` is true
  - Keep Y axis at 0 (implicitly handled by `setWheelValue()` in VirtualAnalogStick)
  - Update steering wheel UI elements with the (possibly inverted) output value
- Reorganized indentation of wheelMode-related functions for consistency

### 2. activity_controller.xml
- Added invert button (`buttonInvertWheel`) next to recenter button in the wheel mode controls
- Increased size of A, B, X, Y buttons from 64dp to 80dp width
- Fixed typo: changed `com.google.android.material.materialswitch.MaterialSwitch` to `com.google.android.material.switchmaterial.SwitchMaterial`

### 3. VirtualAnalogStick.kt (unchanged, but relevant)
- The `setWheelValue()` method already:
  - Updates only the X axis (Y remains 0)
  - Does not send axis events when in wheelMode (touch events are ignored)
  - This means when wheelMode is enabled, only the gyroscope/steering pipeline updates the stick visually
  - Our ControllerActivity now additionally sends the X axis value via AxisSender when wheelMode is enabled

## Behavior Summary
- When wheelMode is enabled:
  - Left stick visually shows the steering value (X axis only, Y=0)
  - Left stick X axis value is sent via UDP as "left_x" axis packet
  - Left stick Y axis is not sent (remains centered at 0)
  - Steering wheel indicator shows the same value
  - Invert button can negate the steering value sent and displayed
- Button sizes increased for A, B, X, Y to 80dp width for easier pressing
- All existing functionality (handshake, heartbeat, etc.) remains unchanged

## Files Modified
- android/app/src/main/java/com/phonewheel/ui/ControllerActivity.kt
- android/app/src/main/res/layout/activity_controller.xml