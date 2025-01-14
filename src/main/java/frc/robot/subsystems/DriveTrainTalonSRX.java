package frc.robot.subsystems;

import static frc.robot.Constants.kAllowableCloseLoopError;
import static frc.robot.Constants.kContinuousCurrent;
import static frc.robot.Constants.kDriveGains;
import static frc.robot.Constants.kDriverDeadband;
import static frc.robot.Constants.kLEFT_FOLLOWER;
import static frc.robot.Constants.kLEFT_LEADER;
import static frc.robot.Constants.kMotionAcceleration;
import static frc.robot.Constants.kMotionCruiseVelocity;
import static frc.robot.Constants.kNeutralDeadband;
import static frc.robot.Constants.kOpenLoopRampRate;
import static frc.robot.Constants.kPID_PRIMARY;
import static frc.robot.Constants.kPID_TURN;
import static frc.robot.Constants.kRIGHT_FOLLOWER;
import static frc.robot.Constants.kRIGHT_LEADER;
import static frc.robot.Constants.kSlotDrive;
import static frc.robot.Constants.kSlotTurning;
import static frc.robot.Constants.kTimeoutMs;
import static frc.robot.Constants.kTurnGains;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.InvertType;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatusFrame;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.SupplyCurrentLimitConfiguration;
import com.ctre.phoenix.motorcontrol.can.TalonSRXConfiguration;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.SerialPort;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class DriveTrainTalonSRX extends SubsystemBase{

    private WPI_TalonSRX m_leftLeader, m_leftFollower, m_rightLeader, m_rightFollower;

    private AHRS m_gyro;

    private DifferentialDrive m_diffDrive;

    // Creates a variable for Open Loop Ramp value a.k.a. Slew Rate.
    // This is for tuning purposes. After tuned, update the kSecondsFromNeutral variable on Constants.java
    private double m_secondsFromNeutral, m_driveAbsMax;
    NetworkTableEntry m_secondsFromNeutralEntry, m_driveAbsMaxEntry;

    //Motion Magic set points
    private double m_leftSetpoint, m_rightSetpoint;

    public DriveTrainTalonSRX() {

        m_rightLeader = new WPI_TalonSRX(kRIGHT_LEADER);
        m_rightFollower = new WPI_TalonSRX(kRIGHT_FOLLOWER);
        m_leftLeader  = new WPI_TalonSRX(kLEFT_LEADER);
        m_leftFollower = new WPI_TalonSRX(kLEFT_FOLLOWER);
        TalonSRXConfiguration config = new TalonSRXConfiguration();

        // Factory default the talons
        m_rightLeader.configAllSettings(config);
        m_rightFollower.configAllSettings(config);

         // Set all the configuration values that are common across
        // both sides of the drivetrain
        config.openloopRamp = kOpenLoopRampRate;
        config.continuousCurrentLimit = kContinuousCurrent;
        config.nominalOutputForward = 0;
        config.nominalOutputReverse = 0;
        config.peakOutputForward = 1;
        config.peakOutputReverse = -1;
        config.neutralDeadband = kNeutralDeadband;
        config.slot0.kF = kDriveGains.kF;
        config.slot0.kP = kDriveGains.kP;
        config.slot0.kI = kDriveGains.kI;
        config.slot0.kD = kDriveGains.kD;
        config.slot0.integralZone = kDriveGains.kIzone;
        config.slot0.closedLoopPeakOutput = kDriveGains.kPeakOutput;
        config.slot0.allowableClosedloopError = 10;
        config.slot0.closedLoopPeriod = 1; // 1 ms loop
        config.slot1.kF = kTurnGains.kF;
        config.slot1.kP = kTurnGains.kP;
        config.slot1.kI = kTurnGains.kI;
        config.slot1.kD = kTurnGains.kD;
        config.slot1.allowableClosedloopError = 10;
        config.slot1.integralZone = kTurnGains.kIzone;
        config.slot1.closedLoopPeakOutput = kTurnGains.kPeakOutput;
        config.slot1.closedLoopPeriod = 1; // 1 ms
        config.motionCruiseVelocity = kMotionCruiseVelocity;
        config.motionAcceleration = kMotionAcceleration;
        m_rightLeader.configAllSettings(config);
        m_leftLeader.configAllSettings(config);

        // Set the followers and inverts
        m_leftLeader.setInverted(true);
        m_leftLeader.setSensorPhase(true);
        m_rightLeader.setInverted(false);
        m_rightLeader.setSensorPhase(false);
        m_rightFollower.follow(m_rightLeader);
        m_rightFollower.setInverted(InvertType.FollowMaster);
        m_leftFollower.follow(m_leftLeader);
        m_leftFollower.setInverted(InvertType.FollowMaster);
  
        // Neutral Mode to Help slow things down
        m_rightLeader.setNeutralMode(NeutralMode.Brake);
        m_leftLeader.setNeutralMode(NeutralMode.Brake);

        // Setup quadrature as primary encoder for PID driving
        m_rightLeader.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, kPID_PRIMARY, 0);
        m_leftLeader.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, kPID_PRIMARY, 0);
        m_rightLeader.setSelectedSensorPosition(0, kSlotDrive, 0);
        m_leftLeader.setSelectedSensorPosition(0, kSlotDrive, 0);
    
        // select profile slot
        m_leftLeader.selectProfileSlot(kSlotDrive, kPID_PRIMARY);
        m_rightLeader.selectProfileSlot(kSlotDrive, kPID_PRIMARY);
    
        // We have observed a couple times where the robot loses control and continues without operator
        // input, changed the TalonSRX objects to be WPI_Talons so we can use the differential drive.
        // We aren't going to actually drive with it.  We are just going to use it for the Watchdog timer.
        m_diffDrive = new DifferentialDrive(m_leftLeader, m_rightLeader);

        //Creates a gyro
        try{
            m_gyro = new AHRS(SerialPort.Port.kUSB1);
            m_gyro.enableLogging(true);
        } catch (RuntimeException ex) {
            DriverStation.reportError("Error instantiating navX" + ex.getMessage(), true);
        }

        /* Set status frame periods */
		m_rightLeader.setStatusFramePeriod(StatusFrame.Status_12_Feedback1, 20, kTimeoutMs);
		m_rightLeader.setStatusFramePeriod(StatusFrame.Status_14_Turn_PIDF1, 20, kTimeoutMs);
		m_leftLeader.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, 5, kTimeoutMs);		//Used remotely by right Talon, speed up

        m_leftLeader.setStatusFramePeriod(StatusFrameEnhanced.Status_13_Base_PIDF0, 10, kTimeoutMs);
        m_leftLeader.setStatusFramePeriod(StatusFrameEnhanced.Status_10_MotionMagic, 10, kTimeoutMs);
        m_rightLeader.setStatusFramePeriod(StatusFrameEnhanced.Status_13_Base_PIDF0, 10, kTimeoutMs);
        m_rightLeader.setStatusFramePeriod(StatusFrameEnhanced.Status_10_MotionMagic, 10, kTimeoutMs);

    
        m_leftLeader.configSupplyCurrentLimit(new SupplyCurrentLimitConfiguration(true, 35, 40, 1.0), 0);
        m_rightLeader.configSupplyCurrentLimit(new SupplyCurrentLimitConfiguration(true, 35, 40, 1.0), 0);

      
        // Tuning Params //

        NetworkTable m_driveTestTable = NetworkTableInstance.getDefault().getTable("Shuffleboard").getSubTable("Drive Testing");

        m_driveAbsMaxEntry = m_driveTestTable.getEntry("Drive Max");
        m_secondsFromNeutralEntry = m_driveTestTable.getEntry("Forward Limiter");
        m_gyro.reset();

        // End of Tuning Params //

    }

    // Encoder methods
    public void resetEncoders() {
        m_leftLeader.setSelectedSensorPosition(0,kPID_PRIMARY,0);
        m_rightLeader.setSelectedSensorPosition(0,kPID_PRIMARY,0);
    }

    public double getLeftEncoderPosition() {
        return m_leftLeader.getSelectedSensorPosition();
    }

    public double getRightEncoderPosition() {
        return m_leftLeader.getSelectedSensorPosition();
    }

    // Gyro methods
    public double getHeading() {
        return Math.IEEEremainder(m_gyro.getAngle(), 360);
    }

    public double getRawAngle() {
        return m_gyro.getAngle();
    }

    public AHRS getGyro() {
        return m_gyro;
    }

    // 

    // Drive Train Methods
    private double deadband(double value) {
        if (Math.abs(value) >= kDriverDeadband) {
            return value;
        } else {
            return 0;
        }
    }


	@SuppressWarnings("unused")
    private double clamp(double value) {
        if (value >= m_driveAbsMax){
            return m_driveAbsMax;
        } 
        
        if (value <= -m_driveAbsMax) {
            return -m_driveAbsMax;
        }

        return value;
    }

    public void teleopDrive(double speedValue, double rotationValue, boolean isSquared) {
        m_diffDrive.arcadeDrive(deadband(speedValue), deadband(-rotationValue), isSquared);
    }

    public void teleopDrive(double speedValue, double rotationValue) {
        m_diffDrive.arcadeDrive(deadband(speedValue), deadband(-rotationValue));
    }

    public void motionMagicStartConfigDrive(double lengthInTicks) {
        resetEncoders();

        m_leftLeader.configMotionCruiseVelocity(kMotionCruiseVelocity, kTimeoutMs);
        m_leftLeader.configMotionAcceleration(kMotionAcceleration, kTimeoutMs);
        m_rightLeader.configMotionCruiseVelocity(kMotionCruiseVelocity, kTimeoutMs);
        m_rightLeader.configMotionAcceleration(kMotionAcceleration, kTimeoutMs);

        m_leftLeader.selectProfileSlot(kSlotDrive, kPID_PRIMARY);
        m_rightLeader.selectProfileSlot(kSlotDrive, kPID_PRIMARY);
    }

    public boolean motionMagicDrive(double targetPosition) {

        m_leftLeader.set(ControlMode.MotionMagic, targetPosition);
        m_rightLeader.set(ControlMode.MotionMagic, targetPosition);

        double m_currentLetfPos = getLeftEncoderPosition();
        double m_currentRightPos = getRightEncoderPosition();

        m_diffDrive.feedWatchdog();

        return Math.abs(m_currentLetfPos - targetPosition) < kAllowableCloseLoopError && Math.abs(m_currentRightPos - targetPosition) < kAllowableCloseLoopError;
    }

    public void motionMagicStartConfigsTurn(boolean isCCWturn, double lengthInTicks){   

        resetEncoders();
		m_leftLeader.selectProfileSlot(kSlotTurning, kPID_TURN);
		m_rightLeader.selectProfileSlot(kSlotTurning, kPID_TURN);
		m_leftLeader.configMotionCruiseVelocity(1000, kTimeoutMs);
		m_leftLeader.configMotionAcceleration(500, kTimeoutMs);
		m_rightLeader.configMotionCruiseVelocity(1000, kTimeoutMs);
		m_rightLeader.configMotionAcceleration(500, kTimeoutMs);

	}

    public boolean motionMagicTurn(double arc_in_ticks) {
    
        m_leftLeader.set(ControlMode.MotionMagic, arc_in_ticks);
		m_rightLeader.set(ControlMode.MotionMagic, -arc_in_ticks);

        double currentL = getLeftEncoderPosition();
        double currentR = getRightEncoderPosition();

        int targetTicks = Math.abs((int)arc_in_ticks);

        m_diffDrive.feedWatchdog();

        return (targetTicks - currentL) < kAllowableCloseLoopError && (targetTicks - currentR) < kAllowableCloseLoopError;
    }

    public void motionMagicEndConfigTurn(){
		//m_leftLeader.configMotionCruiseVelocity(16636, kTimeoutMs);
		//m_leftLeader.configMotionAcceleration(8318, kTimeoutMs);
		//m_rightLeader.configMotionCruiseVelocity(16636, kTimeoutMs);
		//m_rightLeader.configMotionAcceleration(8318, kTimeoutMs);
	}

    public double getLeftSetPoint() {
        return m_leftSetpoint;
    }

    public double getRightSetPoint() {
        return m_rightSetpoint;
    }

    public void resetDrivePIDValues(double kP, double kI, double kD, double kF) {

        m_leftLeader.config_kP(kSlotDrive, kP);
        m_leftLeader.config_kI(kSlotDrive, kI);
        m_leftLeader.config_kD(kSlotDrive, kD);
        m_leftLeader.config_kF(kSlotDrive, kF);

    }

    public void resetTurnPIDValues(double kP, double kI, double kD, double kF) {

        m_leftLeader.config_kP(kSlotTurning, kP);
        m_leftLeader.config_kI(kSlotTurning, kI);
        m_leftLeader.config_kD(kSlotTurning, kD);
        m_leftLeader.config_kF(kSlotTurning, kF);

    }

    public void updateDriveLimiters() {
        m_driveAbsMax = m_driveAbsMaxEntry.getDouble(0);
        m_secondsFromNeutral = m_secondsFromNeutralEntry.getDouble(0);

        m_leftLeader.configOpenloopRamp(m_secondsFromNeutral);
        m_leftFollower.configOpenloopRamp(m_secondsFromNeutral);
        m_rightLeader.configOpenloopRamp(m_secondsFromNeutral);
        m_rightFollower.configOpenloopRamp(m_secondsFromNeutral);
    }

    public void feedWatchdog() {
        m_diffDrive.feed();
    }
    
}
