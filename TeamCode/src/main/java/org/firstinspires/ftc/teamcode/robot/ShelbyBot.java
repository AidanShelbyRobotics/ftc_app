
package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cColorSensor;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DeviceInterfaceModule;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.teamcode.util.Units;

import java.util.HashMap;
import java.util.Map;

/**
 * This is NOT an opmode.
 *
 * This class can be used to define all the specific hardware for a single robot.
 * In this case that robot is a Shelbybot
 *
 * This hardware class assumes the following device names have been configured on the robot:
 * Note:  All names are lower case and some have single spaces between words.
 *
 * Motor channel:  Left  drive motor:        "left_drive"
 * Motor channel:  Right drive motor:        "right_drive"
 */
@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal", "unused"})
public class ShelbyBot
{
    private LinearOpMode op = null;
    /* Public OpMode members. */
    public DcMotor  leftMotor   = null;
    public DcMotor  rightMotor  = null;
    public DcMotor  elevMotor   = null;
    public DcMotor  sweepMotor  = null;
    public DcMotor  shotmotor1  = null;
    public DcMotor  shotmotor2  = null;
    public Servo    lpusher     = null;
    public Servo    rpusher     = null;

    public ModernRoboticsI2cGyro        gyro        = null;
    public ModernRoboticsI2cColorSensor colorSensor = null;
    public DeviceInterfaceModule        dim         = null;

    public BNO055IMU imu = null;

    public boolean gyroInverted = true;

    //Distance from ctr of rear wheel to tail
    public float REAR_OFFSET;
    public float FRNT_OFFSET;
    protected static float CAMERA_X_IN_BOT;
    protected static float CAMERA_Y_IN_BOT;
    protected static float CAMERA_Z_IN_BOT;

    private int colorPort = 0;
    private DriveDir ddir = DriveDir.UNKNOWN;
    public DriveDir calibrationDriveDir = DriveDir.UNKNOWN;
    protected HardwareMap hwMap = null;

    boolean colorEnabled = false;

    private int initHdg = 0;

    //The values below are for the 6 wheel 2016-2017 drop center bot
    //with center wheels powered by Neverest 40 motors.
    //NOTE:  Notes reference center of bot on ground as bot coord frame origin.
    //However, it seems logical to use the center of the rear axis (pivot point)
    public float BOT_WIDTH       = 16.8f; //Vehicle width at rear wheels
    public float BOT_LENGTH      = 18.0f;

    protected double COUNTS_PER_MOTOR_REV = 28;
    protected double DRIVE_GEARS[];

    protected double WHEEL_DIAMETER_INCHES;
    protected double TUNE = 1.00;
    public double CPI;

    public static DcMotor.Direction  LEFT_DIR = DcMotor.Direction.FORWARD;
    public static DcMotor.Direction RIGHT_DIR = DcMotor.Direction.REVERSE;

    public boolean gyroReady = false;
    Map<String, Boolean> capMap = new HashMap<>();

    /* local OpMode members. */
    private ElapsedTime period  = new ElapsedTime();

    private static final String TAG = "SJH_BOT";

    /* Constructor */
    public ShelbyBot()
    {
        //Neverest classic 20,40,60, and orbital 20 have 7 rising edges of Channel A per revolution
        //with a quadrature encoder (4 total edges - A rise, B rise, A fall, B fall) for a total
        //of 28 counts per pre-gear box motor shaft revolution.
        COUNTS_PER_MOTOR_REV = 28;
        DRIVE_GEARS = new double[]{40.0, 1.0/2.0};

        WHEEL_DIAMETER_INCHES = 4.1875;
        TUNE = 1.00;

        BOT_WIDTH  = 16.8f;
        BOT_LENGTH = 18.0f;

        REAR_OFFSET = 9.0f;
        FRNT_OFFSET = BOT_LENGTH - REAR_OFFSET;

        CAMERA_X_IN_BOT = 0.0f * (float)Units.MM_PER_INCH;
        CAMERA_Y_IN_BOT = 0.0f * (float)Units.MM_PER_INCH;
        CAMERA_Z_IN_BOT = 0.0f * (float)Units.MM_PER_INCH;

        capMap.put("drivetrain", false);
        capMap.put("shooter",    false);
        capMap.put("collector",  false);
        capMap.put("pusher",     false);
        capMap.put("sensor",     false);
    }

    /* Initialize standard Hardware interfaces */
    public void init(LinearOpMode op)
    {
        System.out.println("In ShelbyBot.init");
        RobotLog.dd(TAG, "in robot init");
        computeCPI();

        initOp(op);
        initDriveMotors();
        initCollectorLifter();
        initShooters();
        initPushers();
        initSensors();
        initCapabilities();
    }

    protected void initOp(LinearOpMode op)
    {
        this.op = op;
        this.hwMap = op.hardwareMap;
    }

    protected void initDriveMotors()
    {
        // FORWARD for CCW drive shaft rotation if using AndyMark motors
        // REVERSE for  CW drive shaft rotation if using AndyMark motors
        try  //Drivetrain
        {
            leftMotor  = hwMap.dcMotor.get("leftdrive");
            rightMotor = hwMap.dcMotor.get("rightdrive");

             leftMotor.setDirection(LEFT_DIR);
            rightMotor.setDirection(RIGHT_DIR);
             leftMotor.setPower(0);
            rightMotor.setPower(0);
             leftMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            rightMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
             leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
             leftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            capMap.put("drivetrain", true);
        }
        catch (Exception e)
        {
            RobotLog.ee("SJH", "ERROR get hardware map\n" + e.toString());
        }
    }

    protected void initCollectorLifter()
    {
        try  //Collector
        {
            elevMotor = hwMap.dcMotor.get("elevmotor");
            sweepMotor = hwMap.dcMotor.get("sweepmotor");
            capMap.put("collector", true);
        }
        catch (Exception e)
        {
            RobotLog.ee("SJH", "ERROR get hardware map\n" + e.toString());
        }

        if(elevMotor  != null)  elevMotor.setDirection(DcMotor.Direction.REVERSE);
        if(sweepMotor != null) sweepMotor.setDirection(DcMotor.Direction.FORWARD);
        if(elevMotor  != null)  elevMotor.setPower(0);
        if(sweepMotor != null) sweepMotor.setPower(0);
        if(elevMotor  != null)  elevMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        if(sweepMotor != null) sweepMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    protected void initShooters()
    {
        try  //Shooters
        {
            shotmotor1 = hwMap.dcMotor.get("leftshooter");
            shotmotor2 = hwMap.dcMotor.get("rightshooter");
            capMap.put("shooter", true);
        }
        catch (Exception e)
        {
            RobotLog.ee("SJH", "ERROR get hardware map\n" + e.toString());
        }

        if(shotmotor1 != null) shotmotor1.setDirection(DcMotor.Direction.FORWARD);
        if(shotmotor2 != null) shotmotor2.setDirection(DcMotor.Direction.REVERSE);
        if(shotmotor1 != null) shotmotor1.setPower(0);
        if(shotmotor2 != null) shotmotor2.setPower(0);
        if(shotmotor1 != null) shotmotor1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        if(shotmotor2 != null) shotmotor2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        if(shotmotor1 != null) shotmotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        if(shotmotor2 != null) shotmotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

    }

    protected void initPushers()
    {
        try  //Pushers
        {
            lpusher = hwMap.servo.get("lpusher");
            rpusher = hwMap.servo.get("rpusher");
            capMap.put("pusher", true);
        }
        catch (Exception e)
        {
            RobotLog.ee("SJH", "ERROR get hardware map\n" + e.toString());
        }
    }

    protected void initSensors()
    {
        System.out.println("In ShelbyBot.initSensors");
        try  //I2C and DAIO
        {
            dim = hwMap.deviceInterfaceModule.get("dim");

            gyro = (ModernRoboticsI2cGyro) hwMap.gyroSensor.get("gyro");
            colorSensor = (ModernRoboticsI2cColorSensor) hwMap.colorSensor.get("color");
            capMap.put("sensor", true);
        }
        catch (Exception e)
        {
            RobotLog.ee("SJH", "ERROR get hardware map\n" + e.toString());
        }

        if(colorSensor != null)
        {
            //colorPort = colorSensor.getPort();
            //RobotLog.ii("SJH", "I2C Controller version %d",
            //        colorSensor.getI2cController().getVersion());

            RobotLog.ii("SJH", "COLOR_SENSOR");
            RobotLog.ii("SJH", "ConnectionInfo %s", colorSensor.getConnectionInfo());
            RobotLog.ii("SJH", "I2cAddr %s", Integer.toHexString(colorSensor.getI2cAddress().get8Bit()));
            RobotLog.ii("SJH", "I2cAddr %s", Integer.toHexString(colorSensor.getI2cAddress().get7Bit()));

            colorSensor.enableLed(false);
            colorSensor.enableLed(true);

            turnColorOff();
        }

        if(gyro != null)
        {
            RobotLog.ii("SJH", "GYRO_SENSOR");
            RobotLog.ii("SJH", "ConnectionInfo %s", gyro.getConnectionInfo());
            RobotLog.ii("SJH", "I2cAddr %s", Integer.toHexString(gyro.getI2cAddress().get8Bit()));
            RobotLog.ii("SJH", "I2cAddr %s", Integer.toHexString(gyro.getI2cAddress().get7Bit()));
        }
    }

    protected void initCapabilities()
    {
        for (Map.Entry mEnt : capMap.entrySet())
        {
            System.out.println(mEnt.getKey() + " = " + mEnt.getValue());
        }
    }

    private double getTotalGearRatio()
    {
        double gr = 1.0;
        for(double g : DRIVE_GEARS) gr *= g;
        return gr;
    }

    protected void computeCPI()
    {
        CPI = (COUNTS_PER_MOTOR_REV * getTotalGearRatio())/
                      (WHEEL_DIAMETER_INCHES * TUNE * Math.PI);
    }

    public void setDriveDir (DriveDir ddir)
    {
        if(ddir == DriveDir.UNKNOWN)
        {
            RobotLog.ee("SJH", "ERROR - setDriveDir UNKNOWN not allowed.  Setting SWEEPER.");
            ddir = DriveDir.SWEEPER;
        }

        if(calibrationDriveDir == DriveDir.UNKNOWN) calibrationDriveDir = ddir;

        if(this.ddir == ddir)
            return;

        this.ddir = ddir;

        RobotLog.ii("SJH", "Setting Drive Direction to " + ddir);

        switch (ddir)
        {
            case PUSHER:
            {
                leftMotor   = hwMap.dcMotor.get("rightdrive");
                rightMotor  = hwMap.dcMotor.get("leftdrive");
                break;
            }
            case SWEEPER:
            case UNKNOWN:
            {
                leftMotor   = hwMap.dcMotor.get("leftdrive");
                rightMotor  = hwMap.dcMotor.get("rightdrive");
            }
        }

        leftMotor.setDirection(LEFT_DIR);
        rightMotor.setDirection(RIGHT_DIR);
    }

    public DriveDir invertDriveDir()
    {
        DriveDir inDir  = getDriveDir();
        DriveDir outDir = DriveDir.SWEEPER;

        switch(inDir)
        {
            case SWEEPER: outDir = DriveDir.PUSHER;  break;
            case PUSHER:
            case UNKNOWN:
                outDir = DriveDir.SWEEPER; break;
        }

        RobotLog.ii("SJH", "Changing from %s FWD to %s FWD", inDir, outDir);
        setDriveDir(outDir);
        return outDir;
    }

    public void setInitHdg(double initHdg)
    {
        this.initHdg = (int) Math.round(initHdg);
    }

    public boolean calibrateGyro()
    {
        if(gyro == null)
        {
            RobotLog.ee("SJH", "NO GYRO FOUND TO CALIBRATE");
            return false;
        }

        if(calibrationDriveDir == DriveDir.UNKNOWN)
        {
            RobotLog.ii("SJH", "calibrateGyro called without having set a drive Direction. " +
                       "Defaulting to SWEEPER.");
            setDriveDir(DriveDir.SWEEPER);
        }

        RobotLog.ii("SJH", "Starting gyro calibration");
        gyro.calibrate();

        double gyroInitTimout = 5.0;
        boolean gyroCalibTimedout = false;
        ElapsedTime gyroTimer = new ElapsedTime();

        while (!op.isStopRequested() &&
               gyro.isCalibrating())
        {
            op.sleep(50);
            if(gyroTimer.seconds() > gyroInitTimout)
            {
                RobotLog.ii("SJH", "GYRO INIT TIMED OUT!!");
                gyroCalibTimedout = true;
                break;
            }
        }
        RobotLog.ii("SJH", "Gyro calibrated in %4.2f seconds", gyroTimer.seconds());

        boolean gyroReady = !gyroCalibTimedout;
        if(gyroReady) gyro.resetZAxisIntegrator();
        this.gyroReady = gyroReady;
        return gyroReady;
    }

    public void resetGyro()
    {
        if(gyro != null && gyroReady)
        {
            gyro.resetZAxisIntegrator();
        }
    }

    public double getGyroHdg()
    {
        double rawGyroHdg = gyro.getIntegratedZValue();
        return rawGyroHdg;
    }

    public double getGyroFhdg()
    {
        if(imu == null && gyro == null) return 0;
        int dirHdgAdj = 0;
        if(ddir != calibrationDriveDir) dirHdgAdj = 180;

        double rawGyroHdg = getGyroHdg();
        //There have been cases where gyro incorrectly returns 0 for a frame : needs filter
        //Uncomment the following block for a test filter
//        if(rawGyroHdg == 0 &&
//           Math.abs(lastRawGyroHdg) > 30)
//        {
//            rawGyroHdg = lastRawGyroHdg;
//        }
//        else
//        {
//            lastRawGyroHdg = rawGyroHdg;
//        }

        //NOTE:  gyro.getIntegratedZValue is +ve CCW (left turn)
        //WHEN the freaking gyro is mounted upright.
        //Since snowman 2.0 has gyro mounted upside down, we need
        //to negate the value!!
        int gDir = 1;
        if(gyroInverted) gDir = -1;
        double cHdg = gDir * rawGyroHdg + initHdg + dirHdgAdj;

        while (cHdg <= -180) cHdg += 360;
        while (cHdg >   180) cHdg -= 360;

        return cHdg;
    }

    public enum DriveDir
    {
        UNKNOWN,
        SWEEPER,
        PUSHER
    }

    @SuppressWarnings("unused")
    private int getColorPort()
    {
        return colorPort;
    }

    public void turnColorOn()
    {
        if(colorSensor == null) return;
        RobotLog.ii("SJH", "Turning on colorSensor LED");
        colorEnabled = true;
        //colorSensor.getI2cController().registerForI2cPortReadyCallback(colorSensor,
        //        getColorPort());

        op.sleep(50);
        colorSensor.enableLed(true);
    }

    public void turnColorOff()
    {
        if(colorSensor == null) return;
        colorEnabled = false;
        colorSensor.enableLed(false);
        op.sleep(50);
        //colorSensor.getI2cController().deregisterForPortReadyCallback(getColorPort());
    }

    public DriveDir getDriveDir() { return ddir; }

    /***
     *
     * waitForTick implements a periodic delay. However, this acts like a metronome with a regular
     * periodic tick.  This is used to compensate for varying processing times for each cycle.
     * The function looks at the elapsed cycle time, and sleeps for the remaining time interval.
     *
     * @param periodMs  Length of wait cycle in mSec.
     */
    public void waitForTick(long periodMs)
    {
        long  remaining = periodMs - (long)period.milliseconds();

        // sleep for the remaining portion of the regular cycle period.
        if (remaining > 0)
            op.sleep(remaining);

        // Reset the cycle clock for the next pass.
        period.reset();
    }

    //With phone laid flat in portrait mode with screen up:
    //The phone axis is 0,0,0 at Camera (using front camera)
    //X pts to the right side of phone (ZTE volume button edge)
    //Y pts to top of phone (head phone jack edge)
    //Z pts out of camera - initially toward bot up
    //to mount camera on front of bot looking bot fwd,
    //rotate -90 about z, then -90 about x
    //translate 0 in bot x, half bot length in bot y, and ~11" in bot z
    public static OpenGLMatrix phoneOrientation;
    public static OpenGLMatrix phoneLocationOnRobot;
    static
    {
        phoneOrientation = Orientation.getRotationMatrix(
                AxesReference.EXTRINSIC, AxesOrder.XYZ, //ZXY
                AngleUnit.DEGREES, 0, 0, 0);

        phoneLocationOnRobot = OpenGLMatrix
                .translation(CAMERA_X_IN_BOT, CAMERA_Y_IN_BOT, CAMERA_Z_IN_BOT)
                .multiplied(phoneOrientation);
    }
}
