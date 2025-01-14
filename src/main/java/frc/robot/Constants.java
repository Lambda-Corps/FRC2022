package frc.robot;

import edu.wpi.first.math.util.Units;

public final class Constants {

    // Robot Characteristics
    public static final double kWheelDiameterMeters = Units.inchesToMeters(6);
    public static final double kTrackWidthMeters = Units.inchesToMeters(11);
    public static final int kGearRatio = 1;
    public static final int kMaxRPM = 5330;

    public static final int kCountsPerRevolution = 4096; //a.k.a Sensor Units Per Rotation
    public static final double kExpectedVelocity = (kMaxRPM/600) * kCountsPerRevolution;
    /**
	 * Using the configSelectedFeedbackCoefficient() function, scale units to 3600 per rotation.
	 * This is nice as it keeps 0.1 degrees of resolution, and is fairly intuitive.
	 */
	public final static double kTurnTravelUnitsPerRotation = 3600;
	
	/**
	 * Empirically measure what the difference between encoders per 360'
	 * Drive the robot in clockwise rotations and measure the units per rotation.
	 * Drive the robot in counter clockwise rotations and measure the units per rotation.
	 * Take the average of the two.
	 */
	public final static int kEncoderUnitsPerRotation = 17598;//51711; // Measure This!!!
    public static final double kEncoderTicksPerInch = (kCountsPerRevolution * kGearRatio) / (Math.PI * Units.metersToInches(kWheelDiameterMeters));
    public static final double kEncoderTicksPerDegree = kEncoderUnitsPerRotation / 360;

    // TalonSRX configurations
    public static final int k100msPerSecond = 10;
    public static final int kTimeoutMs = 30;
    public static final int kPeriodMs = 10;
    public static final double kSecondsFromNeutral = 0.5; //Slew Rate
    public static final double kNeutralDeadband = 0.001;
    public static final int kLoopTimeMs = 1;
    public static final int kAllowableCloseLoopError = 25;
    public static final double kOpenLoopRampRate = 0.3;
    public static final int kContinuousCurrent = 20;
    public static final int kMotionCruiseVelocity = 1500;
    public static final int kMotionAcceleration = 750;

    // Drive Train gains and max speed
    public static final double kDriveAbsMax = 0.7;
    public static final Gains kDriveGains = new Gains(0.0016, 0, 0, 0.35, 100, 1.0);
    public static final Gains kTurnGains = new Gains(0.004,   0, 0, 0.35, 200, 1.0);
    public static final Gains kVelGains = new Gains(0,        0, 0, 0.00, 300, 1.0);
    public static final Gains kMotProfGains = new Gains(0,    0, 0, 0.35, 400, 1.0);

    // Controllers and deadbands
    public static final int kDRIVER_CONTROLLER = 0;
    public static final double kDriverDeadband = 0.2;

    // Do not change anything after this line unless you rewire the robot and
    // update the spreadsheet!
    // Port assignments should match up with the spreadsheet here:
    // https://docs.google.com/spreadsheets/d/12oIY3QIFFsC3DQdTJzkeNdQm-VWWuzg8NxCcLUb8LFU/edit#gid=0

    // TalonSRXs
    public static final int kLEFT_LEADER = 2;
    public static final int kLEFT_FOLLOWER = 1;
    public static final int kRIGHT_LEADER = 4;
    public static final int kRIGHT_FOLLOWER = 3;

    /** ---- Flat constants, you should not need to change these ---- */
	/* We allow either a 0 or 1 when selecting an ordinal for remote devices [You can have up to 2 devices assigned remotely to a talon/victor] */
	public final static int kREMOTE_0 = 0;
	public final static int kREMOTE_1 = 1;
	/* We allow either a 0 or 1 when selecting a PID Index, where 0 is primary and 1 is auxiliary */
	public final static int kPID_PRIMARY = 0;
	public final static int kPID_TURN = 1;
	/* Firmware currently supports slots [0, 3] and can be used for either PID Set */
	public final static int kSLOT_0 = 0;
	public final static int kSLOT_1 = 1;
	public final static int kSLOT_2 = 2;
	public final static int kSLOT_3 = 3;
	/* ---- Named slots, used to clarify code ---- */
	public final static int kSlotDrive = kSLOT_0;
	public final static int kSlotTurning = kSLOT_1;
	public final static int kSlotVelocit = kSLOT_2;
	public final static int kSlotMotProf = kSLOT_3;
}
