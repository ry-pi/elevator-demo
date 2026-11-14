// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.revrobotics.PersistMode;
import com.revrobotics.REVLibError;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Elevator demo. One SPARK MAX drives a NEO. Hold Y to run up, hold A to run down, and release
 * both (or hold both) to stop.
 */
public class Robot extends TimedRobot {
  private final SparkMax m_elevator =
      new SparkMax(Constants.kElevatorCanId, MotorType.kBrushless);
  private final XboxController m_controller = new XboxController(Constants.kControllerPort);

  /**
   * Runs when the robot starts. Configures the SPARK MAX and persists that configuration across
   * power cycles.
   */
  public Robot() {
    SparkMaxConfig config = new SparkMaxConfig();
    config
        .idleMode(IdleMode.kBrake)
        .smartCurrentLimit(Constants.kSmartCurrentLimitAmps)
        .inverted(false);

    REVLibError error =
        m_elevator.configure(
            config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    if (error != REVLibError.kOk) {
      DriverStation.reportError("Elevator SPARK MAX configuration failed: " + error, false);
    }
  }

  @Override
  public void robotPeriodic() {
    SmartDashboard.putNumber("Elevator/Applied Output", m_elevator.getAppliedOutput());
    SmartDashboard.putNumber("Elevator/Output Current", m_elevator.getOutputCurrent());
  }

  @Override
  public void teleopPeriodic() {
    boolean up = m_controller.getYButton();
    boolean down = m_controller.getAButton();

    double speed = 0.0;
    if (up && !down) {
      speed = Constants.kElevatorSpeed;
    } else if (down && !up) {
      speed = -Constants.kElevatorSpeed;
    }
    m_elevator.set(speed);
  }

  @Override
  public void teleopExit() {
    m_elevator.stopMotor();
  }

  @Override
  public void disabledInit() {
    m_elevator.stopMotor();
  }
}
