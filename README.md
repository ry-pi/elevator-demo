# Elevator demo

A WPILib 2026 Java `TimedRobot` project that runs one elevator motor for a bench demo.

One SPARK MAX (CAN ID 3) drives a NEO brushless motor. The controller is configured for brake idle mode, a 40 A smart current limit, and `inverted` false. That configuration is applied at startup with `ResetMode.kResetSafeParameters` and `PersistMode.kPersistParameters`, so it is saved on the SPARK MAX and kept across power cycles.

The project uses GradleRIO 2026.2.1 and the official REVLib 2026 vendordep (REVLib 2026.0.5).

## Controls

Plug an Xbox controller into the Driver Station as controller port 0.

| Input | Elevator |
| --- | --- |
| Hold Y | Up at output `0.3` |
| Hold A | Down at output `-0.3` |
| Neither button, or both buttons | Stop (`0`) |

Disabling the robot calls `stopMotor()`. Leaving teleop also stops the motor. Brake mode holds position when the output is zero.

SmartDashboard publishes:

- `Elevator/Applied Output` — duty cycle commanded to the motor
- `Elevator/Output Current` — motor current in amps

CAN ID, output speed, controller port, and current limit live in `src/main/java/frc/robot/Constants.java`.

## Set the team number

`teamNumber` in `.wpilib/wpilib_preferences.json` is `-1`, an unset placeholder. **TODO: replace it with your FRC team number before deploying.** Deploy looks up the roboRIO at `10.TE.AM.2` from this value. Until you replace `-1`, Gradle uses team `0` so `./gradlew build` still succeeds, and a deploy will not reach your robot.

You can edit the file directly, or open the project in WPILib VS Code and run **WPILib: Set Team Number** from the Command Palette.

`projectYear` is `2026` and `currentLanguage` is `java`. Leave those as they are.

## Deploy from WPILib VS Code

1. Install the [WPILib 2026](https://github.com/wpilibsuite/allwpilib/releases/tag/v2026.2.1) VS Code tools (the full installer, not only this repository).
2. Open this folder in that VS Code window (**File → Open Folder**). WPILib recognizes the project from `.wpilib/wpilib_preferences.json`.
3. Set the team number, as above.
4. Connect to the robot over USB or the radio, and turn the roboRIO on.
5. Deploy with the WPILib deploy button, or Command Palette → **WPILib: Deploy Robot Code**.

From a terminal in this folder, `./gradlew deploy` does the same thing once the team number is set.

## Confirm the CAN ID

Before the first run, open **REV Hardware Client** and confirm the elevator SPARK MAX is on **CAN ID 3**. If the controller was assigned a different ID, change `Constants.kElevatorCanId` to match. The motor must be a NEO (brushless): match the phase-wire colors and plug in the hall sensor.
