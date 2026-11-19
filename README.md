# Elevator demo

A WPILib 2026 Java `TimedRobot` project. One SPARK MAX (CAN ID 3) drives a NEO and moves an elevator to a preset position, then holds it there.

The position loop runs on the SPARK MAX, not on the roboRIO. A button sends a setpoint with `SparkClosedLoopController.setSetpoint` and `ControlType.kMAXMotionPositionControl`. MAXMotion accelerates, cruises, and decelerates instead of jumping the motor to full effort. Feedback is the NEO's built-in relative encoder. Gravity compensation uses the 2026 `kG` feedforward term (volts, constant with height). It ships at `0` until you measure it.

This is intentionally not a command-based program. `Robot.teleopPeriodic` reads the controller, and the comments in `Constants.java` and `Robot.java` explain what each block does.

The project uses GradleRIO 2026.2.1 and the official REVLib 2026 vendordep (REVLib 2026.0.5). Method names were checked against that jar. The public javadoc site can move ahead of 2026.0.5; if a page shows a different `SparkMax` constructor, trust this project.

## Controls

Xbox controller on Driver Station port 0.

| Input | What it does |
| --- | --- |
| A | Go to home / bottom and hold |
| X | Go to the low preset and hold |
| B | Go to the mid preset and hold |
| Y | Go to the high preset and hold |
| Hold left bumper | Slow manual down (open loop). Cancels the hold |
| Hold right bumper | Slow manual up (open loop). Cancels the hold |
| Start | Call the current position `0` and hold it. Only when the carriage is physically at the bottom |

Releasing A/X/B/Y does not stop the move. The setpoint stays latched. Disable does stop it, and the next enable will not move until you press a button again.

SmartDashboard:

| Key | Meaning |
| --- | --- |
| `Elevator/Position` | Encoder position (motor rotations until you change the conversion factor) |
| `Elevator/Target Setpoint` | Height the last button asked for |
| `Elevator/At Setpoint` | True only while closed loop is active and the SPARK says it is inside the tolerance |
| `Elevator/Applied Output` | Duty cycle the controller is applying, about -1 to 1 |
| `Elevator/Output Current` | Motor current in amps |

## What the placeholder numbers are

Nothing here was measured on a real elevator. With the conversion factors at `1.0`, position is **motor rotations** and MAXMotion speeds are **RPM**. Change them in `src/main/java/frc/robot/Constants.java`.

| Constant | Placeholder | You need to |
| --- | --- | --- |
| `kPositionConversionFactor` | `1.0` (motor rotations) | Measure carriage travel per motor turn, then set this if you want inches or meters. Rewrite every position number in the new unit |
| `kVelocityConversionFactor` | `1.0` (RPM) | Leave it unless you also want the speed limits in inches or meters |
| `kHomePosition` | `0` | Bottom. This is the encoder zero |
| `kLowPosition` | `1` rotation | Drive there, copy `Elevator/Position` |
| `kMidPosition` | `2.5` rotations | Same |
| `kHighPosition` | `4` rotations | Same. Keep it below the forward soft limit |
| `kReverseSoftLimit` | `0` | Bottom of allowed travel |
| `kForwardSoftLimit` | `5` rotations | A little past the highest real stage |
| `kP` / `kI` / `kD` | `0.01` / `0` / `0` | Tune P after a slow test. Leave I and D at 0 |
| `kG` | `0` volts | Measure the hold voltage (steps below). A wrong value drives the elevator as soon as you enable |
| `kCruiseVelocityRpm` | `30` RPM | Raise only after the motion is stable |
| `kMaxAccelerationRpmPerSec` | `10` RPM/s | Raise only while the stop at the setpoint stays smooth |
| `kAllowedProfileError` | `1` rotation | Shrink once the profile tracks instead of resetting |
| `kAtSetpointTolerance` | `0.05` rotation | How close counts as "arrived" |
| `kClosedLoopMinOutput` / `Max` | `-0.5` / `0.5` | Hard cap on closed-loop effort. Raise toward ±1 only after tunes look calm |
| `kManualOverrideSpeed` | `0.1` (10% power) | Bumper jog. Lower it if 10% is already fast |
| `kSmartCurrentLimitAmps` | `40` | Reasonable NEO default, not a measured stall current |
| `kElevatorCanId` | `3` | Must match REV Hardware Client |

### Switching off motor rotations

The NEO encoder counts motor-shaft turns, so the gear ratio is inside the conversion factor, not a separate constant.

1. Leave both factors at `1.0` and zero at the bottom.
2. Turn the motor until the dashboard position reads about `1.0`.
3. Measure how far the carriage moved. That distance is `kPositionConversionFactor` (inches per motor rotation, or meters — pick one).
4. A formula, if you know the hardware: `(pi * sprocket diameter) / gear ratio` in your distance unit. Gear ratio means motor turns per one spool turn.
5. Redeploy, then re-copy home / low / mid / high and both soft limits from the dashboard in the new unit. Also redo `kAllowedProfileError` and `kAtSetpointTolerance`.
6. Leave `kVelocityConversionFactor` at `1.0` so cruise velocity stays in RPM. If you set it equal to the position factor, cruise velocity becomes "distance per minute" and you must retune `kCruiseVelocityRpm` and `kMaxAccelerationRpmPerSec` in that unit. Divide the position factor by 60 for per-second.

## Safe first test

Do this before you raise any speed, gain, or output cap.

1. Set the team number (below) and confirm CAN ID 3 in the REV Hardware Client. Phase wires match colors, hall sensor is plugged in.
2. Put the carriage at the physical bottom before you power the robot. The constructor zeros the encoder and assumes that is true.
3. Keep the placeholder speeds (`30` RPM, `10` RPM/s, output cap `±0.5`, bumpers at `10%`).
4. Clear people out from under the carriage. One person keeps a hand on Disable the entire time.
5. Enable teleop. The elevator should not move yet.
6. Press A. It should sit at zero. Watch `Elevator/Position` and `Elevator/Applied Output`.
7. Press X. Position should creep toward `1.0`. If it runs the wrong way, disable, set `inverted(true)` in `Robot`, deploy, and zero at the bottom again.
8. If it oscillates, runs away, or the current limit chatters, disable. Do not "give it more P" while it is moving.
9. Use a bumper to inch it, and Start only when it is actually at the bottom.

Soft limits stop the controller from planning a move outside 0 to 5 rotations. They do not catch a wrong encoder zero. A person on Disable does.

## Tuning

Read the longer version in the comments at the top of the PID and MAXMotion sections in `Constants.java`. Background:

- PID, in plain language: [Introduction to PID](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/introduction-to-pid.html)
- What the SPARK loop is doing: [Closed Loop Control](https://docs.revrobotics.com/revlib/spark/closed-loop)
- The trapezoid we use: [MAXMotion Position Control](https://docs.revrobotics.com/revlib/spark/closed-loop/maxmotion-position-control)
- `kG` for an elevator: [Feed Forward Control](https://docs.revrobotics.com/revlib/spark/closed-loop/feed-forward-control)
- Class names: [REVLib Java API](https://codedocs.revrobotics.com/java/) (this repo tracks REVLib 2026.0.5)

Order that will not bite you:

1. Mechanism free to move, hard stops known, hand on Disable, gains still at the placeholders (`kP = 0.01`, `kI = 0`, `kD = 0`, `kG = 0`, cruise `30` RPM).
2. Make a short move (A, then X). You want a slow creep, not a snap.
3. If it shakes, lower `kP`. If it sits there and barely moves, you may raise `kP` a little. REV notes that a tiny `kP` plus a bad feedforward can also run away; with `kG` still 0, prefer smaller moves over bigger gains.
4. Measure `kG` before chasing position error. In the middle of travel, find the voltage that just lifts (`V1`) and the voltage that just lets it fall (`V2`). `kG = V2 + (V1 - V2) / 2`. Put that in `Constants` and deploy. The feedforward page linked above has the same test.
5. Leave `kI` at 0. If it still sags after `kG` is right, add a very small `kP` change, not integral.
6. When the slow move is stable, raise cruise velocity a step at a time. Then acceleration. Shrink `kAllowedProfileError` last.
7. If up and down are not symmetric, `kG` is wrong. If the motor is loud and current is pegged, the current limit or the acceleration is too aggressive for the gearing. Slow down before you raise current.

`kP` here is "duty cycle per rotation of error" at the default units. After you change the position conversion factor, the same `0.01` means something else, because one unit of error is no longer one rotation. Retune `kP` after you change units.

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
