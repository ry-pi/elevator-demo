// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

/**
 * Every number the elevator uses lives here, on purpose.
 *
 * <p>Nothing in this file has been measured on a real elevator yet. The numbers are slow, safe
 * placeholders so the first time you enable, the motor creeps instead of slamming into a stop.
 * Each TODO says what to measure and what to type in after you measure it. Change a number here,
 * deploy again, and the SPARK MAX picks up the new value on startup.
 *
 * <p>Units, until you change the conversion factors below: position is motor rotations (how many
 * times the NEO's shaft has turned), and speed limits are in RPM (rotations per minute).
 */
public final class Constants {
  private Constants() {}

  // ---------------------------------------------------------------------------
  // Hardware
  // ---------------------------------------------------------------------------

  /**
   * CAN ID of the SPARK MAX that drives the elevator.
   *
   * <p>TODO: this must match the ID shown in the REV Hardware Client. If the controller was
   * assigned a different ID, change this number. Do not guess. A wrong ID talks to a different
   * device, or to nothing.
   */
  public static final int kElevatorCanId = 3;

  /**
   * Which Driver Station slot the Xbox controller is plugged into. 0 is the first controller.
   */
  public static final int kControllerPort = 0;

  /**
   * Smart current limit, in amps.
   *
   * <p>The SPARK MAX watches current and cuts voltage before the NEO can pull a damaging spike. 40
   * A is a normal starting limit for one NEO. It is not a measured value for this elevator.
   *
   * <p>TODO: if the carriage is heavy and the motor stalls against a hard stop, you can lower this.
   * If MAXMotion never reaches the cruise speed because the limit is clipping acceleration, you can
   * raise it a little. Stay inside what the motor, breaker, and mechanism can survive.
   */
  public static final int kSmartCurrentLimitAmps = 40;

  // ---------------------------------------------------------------------------
  // What a "position" means
  //
  // The NEO has a built-in relative encoder: three hall sensors inside the motor that count
  // motor-shaft turns. "Relative" means it does not know where the carriage is when power comes
  // on. It only knows how far it has moved since we told it "you are at zero." That is why the
  // robot zeros the encoder in the constructor, and why Start can zero it again.
  //
  // Native position units are motor rotations. Native velocity units are RPM.
  // The two factors below are multiplied by those native readings.
  //
  //   measured position = motor rotations * kPositionConversionFactor
  //   measured velocity = RPM            * kVelocityConversionFactor
  //
  // Both factors are 1.0, so the dashboard, the setpoints, and the soft limits are all in motor
  // rotations, and the MAXMotion speed limits are in RPM. We do not know the gear ratio or the
  // spool size yet, so rotations are the honest unit.
  //
  // TODO: switch to inches or meters only after you measure travel per motor turn.
  //
  //   1. Leave both factors at 1.0.
  //   2. Zero the encoder with the carriage at the bottom (Start, or just reboot if it already is).
  //   3. Let the motor turn until the dashboard position reads about 1.0.
  //   4. Measure how far the carriage actually moved with a tape. Call that distance D
  //      (inches, or meters — pick one and stick to it).
  //   5. Set kPositionConversionFactor = D.
  //      Example: a 1.5 in diameter sprocket moves pi * 1.5 inches per spool turn. If the gearbox
  //      is 5:1, the motor turns 5 times per spool turn, so one motor rotation moves
  //      (pi * 1.5) / 5 inches. That quotient is the factor.
  //   6. Redeploy. Every setpoint, both soft limits, kAllowedProfileError, and
  //      kAtSetpointTolerance must then be rewritten in the new unit (inches or meters).
  //      The easiest way is to drive to each stage and copy the new dashboard position.
  //
  // Leave kVelocityConversionFactor at 1.0 unless you want the speed limits in the new unit too.
  // At 1.0, kCruiseVelocity stays in RPM, which is easier to reason about. If you set the velocity
  // factor equal to the position factor, cruise velocity becomes "position-units per minute"
  // (inches per minute, for example). Divide the position factor by 60 if you want per-second.
  // If you change it, retune kCruiseVelocity and kMaxAcceleration in that new unit.
  // ---------------------------------------------------------------------------

  /** Multiplies motor rotations. 1.0 means "position numbers are motor rotations." */
  public static final double kPositionConversionFactor = 1.0;

  /** Multiplies motor RPM. 1.0 means "MAXMotion speeds below are in RPM." */
  public static final double kVelocityConversionFactor = 1.0;

  // ---------------------------------------------------------------------------
  // Setpoints
  //
  // A setpoint is the position we ask the SPARK MAX to hold. Pressing a button does not "hold a
  // speed." It hands the controller a target and the controller keeps working until the encoder
  // says the carriage is there.
  //
  // TODO: these four heights are made up. With the conversion factor still 1.0 they are motor
  // rotations above the zero you set at the bottom. Measure each real stage: zero at the bottom,
  // move the carriage to that stage (face button, or the slow bumper jog), read
  // "Elevator/Position" on SmartDashboard, and paste that number here. Keep every setpoint between
  // the two soft limits below.
  // ---------------------------------------------------------------------------

  /** A button. Bottom of travel. This is also the encoder zero. */
  public static final double kHomePosition = 0.0;

  /** X button. Low scoring / intake height. Placeholder: 1 motor rotation up. */
  public static final double kLowPosition = 1.0;

  /** B button. Middle height. Placeholder: 2.5 motor rotations up. */
  public static final double kMidPosition = 2.5;

  /**
   * Y button. High height. Placeholder: 4 motor rotations up. Must stay below
   * kForwardSoftLimit.
   */
  public static final double kHighPosition = 4.0;

  // ---------------------------------------------------------------------------
  // Soft limits
  //
  // A soft limit is a position the SPARK MAX refuses to command past. It is not a metal hard
  // stop. If the encoder is wrong, the soft limit is wrong too. Still turn them on so a bad
  // setpoint cannot wind the motor forever.
  //
  // Forward is the positive direction (up, once positive is wired as up). Reverse is negative
  // (down). Both are in the same units as the setpoints.
  //
  // TODO: set the forward limit a little past the highest real stage, and the reverse limit at
  // the bottom (0, or a hair below 0 if the zero sits right on the hard stop). The placeholders
  // allow 0 to 5 motor rotations, which covers the placeholder setpoints with a little room.
  // ---------------------------------------------------------------------------

  /** Lowest position the closed loop is allowed to command. */
  public static final double kReverseSoftLimit = 0.0;

  /** Highest position the closed loop is allowed to command. */
  public static final double kForwardSoftLimit = 5.0;

  // ---------------------------------------------------------------------------
  // PID and gravity feedforward
  //
  // The SPARK MAX runs this loop on the controller itself, about every millisecond. We do not run
  // PID on the roboRIO.
  //
  //   output = kP * position error + kI * accumulated error + kD * error rate + kG
  //
  // What each term does, physically:
  //
  // kP (proportional) — a software spring. The farther the carriage is from the setpoint, the
  // harder it pulls. Too small and it barely moves, or a feedforward error makes it run away.
  // Too big and it overshoots and oscillates. REV's starting point for default units (rotations)
  // is 0.01: one rotation of error commands about 1% of full output.
  //
  // kI (integral) — adds up leftover error and pushes harder the longer you sit short of the
  // target. It fights steady sag, but it winds up and then overshoots. FRC teams usually leave
  // it at 0 and use kG instead. Leave it at 0.
  //
  // kD (derivative) — a software damper. It pushes back against how fast the error is changing,
  // which slows the approach. Useful later. 0 is the right first value. kV (not used yet) is
  // usually a better way to track speed than a large kD.
  //
  // kG (gravity) — volts that are always added while holding position, to cancel the weight of
  // the carriage. An elevator needs the same upward bias at every height, so kG is a constant
  // voltage, not a cosine. (An arm would use kCos instead. This is an elevator, so kG only.)
  // 0.0 will not hold a heavy carriage. That is intentional for the first enable: a wrong kG
  // drives the elevator by itself the moment you enable.
  //
  // TODO: tune in the order in the README. Short version:
  //   1. Keep kI, kD, and kG at 0. Keep kP at 0.01.
  //   2. Confirm a small move (A, then X) is slow and stable.
  //   3. Measure kG with the carriage in the middle and a hand on Disable:
  //        raise voltage until it just starts to rise (V1),
  //        lower voltage until it just starts to fall (V2),
  //        kG = (V1 + V2) / 2, which is the same as V2 + (V1 - V2) / 2.
  //      The REV write-up of that test:
  //      https://docs.revrobotics.com/revlib/spark/closed-loop/feed-forward-control
  //   4. Only then nudge kP. If it is sluggish, raise kP a little. If it shakes, lower kP.
  //
  // Plain-language PID background:
  // https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/introduction-to-pid.html
  // How the SPARK runs that loop:
  // https://docs.revrobotics.com/revlib/spark/closed-loop
  // ---------------------------------------------------------------------------

  /**
   * Duty-cycle fraction per rotation of position error. {@code 0.01} is about 1% output when the
   * carriage is 1 motor rotation away from the setpoint. Start this small.
   */
  public static final double kP = 0.01;

  /** Leave at 0. Integral windup is a bad trade for an elevator. */
  public static final double kI = 0.0;

  /** Leave at 0 until kP and kG are honest. */
  public static final double kD = 0.0;

  /**
   * Gravity hold, in volts. 0 until you measure it.
   *
   * <p>TODO: replace with the hold voltage from the V1/V2 test in the comment above. Applied only
   * in position and MAXMotion position control, which is what this demo uses.
   */
  public static final double kG = 0.0;

  /**
   * How close counts as "we are there," in the same units as position.
   *
   * <p>The SPARK uses this as the allowed closed-loop error. {@code isAtSetpoint()} turns true
   * inside this band. 0.05 rotations is a tight placeholder so the dashboard is not lying. Widen
   * it if the flag chatters, after the mechanism is actually tuned.
   *
   * <p>TODO: set this to the error you can live with at a scoring height (for example 0.25 inch,
   * after you switch units).
   */
  public static final double kAtSetpointTolerance = 0.05;

  /**
   * Closed-loop output clamp, as a duty cycle from -1 (full reverse) to +1 (full forward).
   *
   * <p>This is a safety cap, separate from the MAXMotion speed limit. Even if kP is typed wrong,
   * the controller cannot command more than half power. TODO: raise these toward -1 and 1 only
   * after a slow move looks calm. Do not start at ±1.
   */
  public static final double kClosedLoopMinOutput = -0.5;

  /** See {@link #kClosedLoopMinOutput}. */
  public static final double kClosedLoopMaxOutput = 0.5;

  // ---------------------------------------------------------------------------
  // MAXMotion
  //
  // Plain position PID (ControlType.kPosition) only knows "how far am I from the target?" It will
  // try to cover that distance as hard as kP allows, which is a jerk at the start and a jerk at
  // the end.
  //
  // MAXMotion position control builds a trapezoid: speed up at a limited acceleration, cruise at
  // a limited speed, then slow down so it arrives at the setpoint instead of flying through it.
  // The profile is rebuilt if the mechanism falls too far behind. That is the 2026 replacement
  // for the old Smart Motion mode.
  //
  // Java names on MAXMotionConfig (REVLib 2026.0.5):
  //   cruiseVelocity(...)       — was maxVelocity, which is deprecated
  //   maxAcceleration(...)
  //   allowedProfileError(...)  — was allowedClosedLoopError on the MAXMotion config, deprecated
  // The underlying parameter ids are kMAXMotionCruiseVelocity and kMAXMotionAllowedProfileError.
  //
  // With the velocity conversion factor at 1.0, cruise velocity is RPM and max acceleration is
  // RPM per second. REV's own "go slow and watch" starting points are 30 RPM and 10 RPM/s. These
  // placeholders copy that. They will look very slow. That is the point of the first test.
  //
  // kAllowedProfileError is how far the real position may drift from the planned profile before
  // MAXMotion throws the plan away and makes a new one. A large value (1 rotation) makes the
  // first graphs easier to read. Tighten it once the motion tracks.
  //
  // Guide: https://docs.revrobotics.com/revlib/spark/closed-loop/maxmotion-position-control
  //
  // TODO: after kP and kG are stable, raise cruise velocity a little at a time (30, then 60, then
  // whatever the mechanism can do without tripping the current limit). Raise acceleration only
  // while the slowdown into the setpoint stays smooth. Shrink kAllowedProfileError toward
  // something like a tenth of a rotation once the profile stops resetting.
  // ---------------------------------------------------------------------------

  /** Top speed of the trapezoid, in RPM while the velocity factor is 1. */
  public static final double kCruiseVelocityRpm = 30.0;

  /** How fast that top speed may change, in RPM per second while the velocity factor is 1. */
  public static final double kMaxAccelerationRpmPerSec = 10.0;

  /** How far off the planned profile we tolerate before MAXMotion replans, in position units. */
  public static final double kAllowedProfileError = 1.0;

  /**
   * Duty cycle used while a bumper is held, bypassing the closed loop.
   *
   * <p>Left bumper drives down, right bumper drives up, at this fraction of full output. This is
   * how you inch the carriage to the physical bottom before pressing Start to zero. 0.1 is 10%
   * power.
   *
   * <p>TODO: lower it if 10% is already fast on your gearing. Do not raise it for convenience.
   */
  public static final double kManualOverrideSpeed = 0.1;
}
