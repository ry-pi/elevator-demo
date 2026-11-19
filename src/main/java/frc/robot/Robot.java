// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.revrobotics.PersistMode;
import com.revrobotics.REVLibError;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.ClosedLoopSlot;
import com.revrobotics.spark.FeedbackSensor;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Elevator demo: one NEO on a SPARK MAX, moved to preset positions and held there.
 *
 * <p>This is a TimedRobot on purpose. There is no command framework. The roboRIO calls {@link
 * #teleopPeriodic()} about every 20 milliseconds while the Driver Station is in teleop, and that
 * method reads the controller and tells the motor what to do.
 *
 * <p>The position loop does not run on the roboRIO. We configure the SPARK MAX's own closed-loop
 * controller and then, when a button is pressed, send it a setpoint. The motor controller chases
 * that position by itself until we send a different one, switch to the bumper jog, or disable.
 *
 * <p>API notes (checked against REVLib 2026.0.5, the jar {@code vendordeps/REVLib.json} downloads):
 *
 * <ul>
 *   <li>{@code setSetpoint} is the 2026 call. {@code setReference} still exists and is deprecated.
 *   <li>{@code ControlType.kMAXMotionPositionControl} is the smooth profiled move. {@code
 *       kPosition} would be a plain PID jump with no accel limit, so we do not use it.
 *   <li>Cruise velocity and allowed profile error are {@code cruiseVelocity} and {@code
 *       allowedProfileError}. The older {@code maxVelocity} and MAXMotion {@code
 *       allowedClosedLoopError} methods are deprecated.
 *   <li>{@code https://codedocs.revrobotics.com/java/} can show a newer SparkMax constructor than
 *       2026.0.5. The calls in this file match the 2026.0.5 jar, not that newer page.
 * </ul>
 */
public class Robot extends TimedRobot {
  /**
   * The motor controller. CAN ID and "brushless" (NEO) are fixed at construction. Brushless vs
   * brushed is not a config-object setting: driving a NEO in brushed mode can destroy the motor.
   */
  private final SparkMax m_elevator =
      new SparkMax(Constants.kElevatorCanId, MotorType.kBrushless);

  /**
   * Handle used to send setpoints. The NEO's built-in relative encoder is the feedback device,
   * selected explicitly in the constructor even though it is also the SPARK default.
   */
  private final SparkClosedLoopController m_closedLoop = m_elevator.getClosedLoopController();

  /** The same built-in encoder, used to read position and to zero it. */
  private final RelativeEncoder m_encoder = m_elevator.getEncoder();

  /** Xbox controller on port 0. Face buttons choose a height. Bumpers are the slow manual jog. */
  private final XboxController m_controller = new XboxController(Constants.kControllerPort);

  /**
   * Last height a face button asked for, in the same units as {@code Constants} positions. Starts
   * at home so the dashboard has a number before anyone presses a button.
   */
  private double m_targetSetpoint = Constants.kHomePosition;

  /**
   * False until a face button or Start asks for a hold. While this is false we command 0% output
   * instead of closed loop, so enabling the robot never starts a move by itself.
   */
  private boolean m_closedLoopActive = false;

  /**
   * Runs once when the robot program starts (the robot is still disabled here).
   *
   * <p>Order matters. Build the config, push it to the controller, then zero the encoder. Zeroing
   * first would be overwritten by nothing in our case, but configuring first means the conversion
   * factor is already in place when we define "position 0."
   */
  public Robot() {
    SparkMaxConfig config = new SparkMaxConfig();

    // Brake: when output is zero, the motor leads are shorted and the carriage resists falling.
    // Coast would let it drop. Inverted false means positive output is the controller's default
    // positive direction. If the first test drives the carriage down when we ask for a higher
    // number, flip inverted to true, deploy again, and zero at the bottom. Do not "fix" that by
    // swapping the setpoint numbers.
    config.idleMode(IdleMode.kBrake).smartCurrentLimit(Constants.kSmartCurrentLimitAmps).inverted(false);

    // Tell the encoder what one native rotation should be reported as. 1.0 leaves it as motor
    // rotations. Velocity factor 1.0 leaves MAXMotion speeds in RPM.
    config
        .encoder
        .positionConversionFactor(Constants.kPositionConversionFactor)
        .velocityConversionFactor(Constants.kVelocityConversionFactor);

    // Closed-loop slot 0 (the only slot we use):
    //   which sensor, P/I/D, an output clamp, and how close "at setpoint" means.
    // feedbackSensor(kPrimaryEncoder) is the NEO hall sensor inside the motor, not an external
    // absolute encoder on the carriage.
    config
        .closedLoop
        .feedbackSensor(FeedbackSensor.kPrimaryEncoder)
        .pid(Constants.kP, Constants.kI, Constants.kD)
        .outputRange(Constants.kClosedLoopMinOutput, Constants.kClosedLoopMaxOutput)
        .allowedClosedLoopError(Constants.kAtSetpointTolerance, ClosedLoopSlot.kSlot0);

    // kG is volts added constantly to hold the carriage up. 0 is safe for the first enable.
    // An elevator uses kG. Do not also set kCos; the firmware uses only one of them.
    config.closedLoop.feedForward.kG(Constants.kG);

    // Trapezoid limits. cruiseVelocity / maxAcceleration / allowedProfileError are the 2026
    // names. Units are RPM, RPM/s, and position units, because the conversion factors are 1.
    config
        .closedLoop
        .maxMotion
        .cruiseVelocity(Constants.kCruiseVelocityRpm)
        .maxAcceleration(Constants.kMaxAccelerationRpmPerSec)
        .allowedProfileError(Constants.kAllowedProfileError);

    // Soft limits are on. The controller will not plan a move outside this window. They are not
    // a substitute for a physical stop or for a person on the Disable switch.
    config
        .softLimit
        .forwardSoftLimit(Constants.kForwardSoftLimit)
        .forwardSoftLimitEnabled(true)
        .reverseSoftLimit(Constants.kReverseSoftLimit)
        .reverseSoftLimitEnabled(true);

    // kResetSafeParameters puts writable settings back to defaults, then applies this config, so
    // an old tune cannot hide underneath. kPersistParameters saves it on the SPARK MAX so a
    // brownout does not forget the gains. While you are still changing numbers every deploy, that
    // save is fine: the next deploy sends the new numbers. (If you ever want a trial that is NOT
    // saved, switch the second argument to PersistMode.kNoPersistParameters.)
    REVLibError configError =
        m_elevator.configure(
            config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    if (configError != REVLibError.kOk) {
      DriverStation.reportError("Elevator SPARK MAX configuration failed: " + configError, false);
    }

    // The relative encoder wakes up at an arbitrary count. We define the boot position as 0.
    // That is only true if the carriage is physically at the bottom when the robot is turned on.
    // If it is not, lower it later and press Start. Do not trust the dashboard until you have
    // done one or the other.
    zeroEncoder();
  }

  /**
   * Called every mode, about every 20 ms. Safe to read sensors here. Do not send a new setpoint
   * from this method: disabled and autonomous would start moving the elevator too.
   */
  @Override
  public void robotPeriodic() {
    // "At setpoint" is only meaningful while we are actually asking the closed loop to hold.
    // Otherwise the SPARK's flag can be leftover from the previous command.
    boolean atSetpoint = m_closedLoopActive && m_closedLoop.isAtSetpoint();

    SmartDashboard.putNumber("Elevator/Position", m_encoder.getPosition());
    SmartDashboard.putNumber("Elevator/Target Setpoint", m_targetSetpoint);
    SmartDashboard.putBoolean("Elevator/At Setpoint", atSetpoint);
    SmartDashboard.putNumber("Elevator/Applied Output", m_elevator.getAppliedOutput());
    SmartDashboard.putNumber("Elevator/Output Current", m_elevator.getOutputCurrent());
  }

  /**
   * Teleop loop. Buttons are read here, not in robotPeriodic, so the elevator only moves when a
   * driver is in control.
   *
   * <p>Face buttons are edge-triggered ({@code getXButtonPressed}): one press latches a setpoint.
   * Releasing the button does not cancel the hold. That is the whole point of a setpoint.
   *
   * <p>Bumpers are level-triggered: motion continues only while the bumper is held, and it is
   * open-loop ({@code set}, a percent output). That bypasses PID on purpose, so you can creep to
   * the bottom even if the encoder zero is wrong. Letting go commands 0 until a face button or
   * Start asks for a hold again.
   */
  @Override
  public void teleopPeriodic() {
    // Start means "wherever the carriage is right now, call it position 0, and hold that."
    // Only do this when the carriage is physically at the bottom. If you zero in the middle of
    // the travel, every setpoint and both soft limits are suddenly measured from the wrong place
    // and the next button press can drive into a hard stop.
    if (m_controller.getStartButtonPressed()) {
      zeroEncoder();
      m_targetSetpoint = Constants.kHomePosition;
      m_closedLoopActive = true;
    }

    boolean manualDown = m_controller.getLeftBumperButton();
    boolean manualUp = m_controller.getRightBumperButton();

    if (manualDown && !manualUp) {
      // Manual override wins over a latched setpoint. Negative output is down.
      m_closedLoopActive = false;
      m_elevator.set(-Constants.kManualOverrideSpeed);
      return;
    }
    if (manualUp && !manualDown) {
      m_closedLoopActive = false;
      m_elevator.set(Constants.kManualOverrideSpeed);
      return;
    }

    // One new request per loop. If two face buttons were pressed in the same 20 ms window, the
    // higher stage wins (Y, then B, then X, then A).
    if (m_controller.getYButtonPressed()) {
      latchSetpoint(Constants.kHighPosition);
    } else if (m_controller.getBButtonPressed()) {
      latchSetpoint(Constants.kMidPosition);
    } else if (m_controller.getXButtonPressed()) {
      latchSetpoint(Constants.kLowPosition);
    } else if (m_controller.getAButtonPressed()) {
      latchSetpoint(Constants.kHomePosition);
    }

    if (m_closedLoopActive) {
      // Sent every teleop loop, not only on the press. The SPARK would remember a setpoint, but
      // repeating it keeps the goal obvious and recovers if a control frame is dropped.
      // kMAXMotionPositionControl is what makes cruise velocity and max acceleration apply.
      m_closedLoop.setSetpoint(m_targetSetpoint, ControlType.kMAXMotionPositionControl);
    } else {
      // No goal yet this enable cycle (or the driver just let go of a bumper). Stay stopped.
      m_elevator.set(0.0);
    }
  }

  @Override
  public void teleopExit() {
    // Leaving teleop should not leave a closed-loop goal armed for the next mode.
    stopAndDisarm();
  }

  @Override
  public void disabledInit() {
    // Required: when the robot is disabled, stop the motor. Also forget the latched goal so the
    // next time teleop enables, nothing moves until a button is pressed again.
    stopAndDisarm();
  }

  /** Remember a height and start holding it on the next lines of teleopPeriodic. */
  private void latchSetpoint(double setpoint) {
    m_targetSetpoint = setpoint;
    m_closedLoopActive = true;
  }

  /**
   * Define the current carriage position as 0. Does not, by itself, choose whether closed loop is
   * on. Callers decide that.
   */
  private void zeroEncoder() {
    REVLibError error = m_encoder.setPosition(0.0);
    if (error != REVLibError.kOk) {
      DriverStation.reportError("Elevator encoder zero failed: " + error, false);
    }
  }

  /** Cut output and require a new button press before the next closed-loop move. */
  private void stopAndDisarm() {
    m_closedLoopActive = false;
    m_elevator.stopMotor();
  }
}
