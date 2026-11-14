// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

/** Hardware and control constants for the elevator demo. */
public final class Constants {
  /** SPARK MAX CAN ID. Confirm this in the REV Hardware Client before driving the elevator. */
  public static final int kElevatorCanId = 3;

  /** Open-loop output while Y (up) or A (down) is held. */
  public static final double kElevatorSpeed = 0.3;

  /** Driver Xbox controller port in the Driver Station. */
  public static final int kControllerPort = 0;

  /** NEO smart current limit, in amps. */
  public static final int kSmartCurrentLimitAmps = 40;

  private Constants() {}
}
