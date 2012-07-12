package mai.icyrain;

import android.os.Handler;

/**
 * Currently this gesture handler is abstracted away from how the data from the glove is actually
 * received.
 * However the data is acquired, simply pass in pitch roll and yaw into the methods.
 * 
 * @author https://github.com/ohtrahddis (Siddhartho Bhattacharya)
 */
public class GestureHandler extends Handler {

  // set these to determine the leeway for how far apart degree measurements are to be considered
  // "the same" to account for jitter
  static double PITCH_LEEWAY = 1.0;
  static double ROLL_LEEWAY = 1.0;
  static double YAW_LEEWAY = 1.0;
  static double UPRIGHT_PITCH_THRESHOLD = 45.0;

  boolean handIsStill;
  boolean isWaving;

  boolean isFirstWavePointSet = true;
  boolean isSecondWavePointSet = false;

  boolean rollIncreasing;
  boolean rollDecreasing;
  boolean pitchIncreasing;
  boolean pitchDecreasing;
  boolean yawIncreasing;
  boolean yawDecreasing;

  double oldPitch;
  double oldRoll;
  double oldYaw;
  double pitch;
  double roll;
  double yaw;

  public GestureHandler() {
    this(0, 0, 0);
  }

  // some error
  // "Implicit super constructor Object() is undefined. Must explicitly invoke another constructor"
  // something to do with jre lib or android SDK not sure, normal java this constructor should be
  // fine.
  public GestureHandler(double p, double r, double y) {
    oldPitch = 0.0;
    oldRoll = 0.0;
    oldYaw = 0.0;
    pitch = p;
    roll = r;
    yaw = y;
  }

  /**
   * ideally, this method should be the one that actually grabs the data from the bluetooth handler
   * for now, it requires manually passing in the data.
   * however it is done, make sure the variables below are set.
   */
  public void UpdateData(double p, double r, double y) {
    oldPitch = pitch;
    oldRoll = roll;
    oldYaw = yaw;
    pitch = p;
    roll = r;
    yaw = y;

    if (pitch < oldPitch && Math.abs(oldPitch - pitch) > PITCH_LEEWAY) {
      pitchDecreasing = true;
      pitchIncreasing = false;
    } else if (pitch > oldPitch && Math.abs(oldPitch - pitch) > PITCH_LEEWAY) {
      pitchIncreasing = true;
      pitchDecreasing = false;
    } else {
      pitchDecreasing = false;
      pitchIncreasing = false;
    }

    if (roll < oldRoll && Math.abs(oldRoll - roll) > ROLL_LEEWAY) {
      rollDecreasing = true;
      rollIncreasing = false;
    } else if (roll > oldRoll && Math.abs(oldRoll - roll) > ROLL_LEEWAY) {
      rollIncreasing = true;
      rollDecreasing = false;
    } else {
      rollDecreasing = false;
      rollIncreasing = false;
    }

    if (yaw < oldYaw && Math.abs(oldYaw - yaw) > YAW_LEEWAY) {
      yawDecreasing = true;
      yawIncreasing = false;
    } else if (yaw > oldYaw && Math.abs(oldYaw - yaw) > YAW_LEEWAY) {
      yawIncreasing = true;
      yawDecreasing = false;
    } else {
      yawDecreasing = false;
      yawIncreasing = false;
    }

  }

  /*
   * This method is currently unused, but can be used to auto-set the leeway variables
   * for pitch roll and yaw. This should be run while the user lifts the hand to a
   * raised and stationary position, and the variables will be set for any natural wavering/jitter.
   * Sets variables to standard deviation of measured data.
   */
  public void CalibrateUpright() {
    int calibrationCountMax = 10; // increase to have a greater precision
    int count = 1;
    double[] pitchValues = new double[calibrationCountMax];
    double[] rollValues = new double[calibrationCountMax];
    double[] yawValues = new double[calibrationCountMax];

    double pitchTotal = 0.0;
    double rollTotal = 0.0;
    double yawTotal = 0.0;

    while (count <= calibrationCountMax) {
      // WARNING - this line won't work under current implementation of UpdateData()
      // UpdateData(); // UpdateData should grab the next pile of data from bluetooth received data
      pitchValues[count] = pitch;
      pitchTotal += pitch;

      rollValues[count] = roll;
      rollTotal += roll;

      yawValues[count] = yaw;
      yawTotal += yaw;

      count++;
    }

    double pitchMean = pitchTotal / calibrationCountMax;
    double rollMean = rollTotal / calibrationCountMax;
    double yawMean = yawTotal / calibrationCountMax;
    pitchTotal = 0;
    rollTotal = 0;
    yawTotal = 0;

    for (int i = 0; i < calibrationCountMax; i++) {
      pitchValues[i] = (pitchValues[i] - pitchMean) * (pitchValues[i] - pitchMean);
      pitchTotal += pitchValues[i];

      rollValues[i] = (rollValues[i] - rollMean) * (rollValues[i] - rollMean);
      rollTotal += rollValues[i];

      yawValues[i] = (yawValues[i] - yawMean) * (yawValues[i] - yawMean);
      yawTotal += yawValues[i];
    }

    PITCH_LEEWAY = Math.sqrt(pitchTotal / (calibrationCountMax - 1));
    ROLL_LEEWAY = Math.sqrt(rollTotal / (calibrationCountMax - 1));
    YAW_LEEWAY = Math.sqrt(yawTotal / (calibrationCountMax - 1));

  }

  public boolean CheckStillHand(double p, double r, double y) {
    UpdateData(p, r, y); // once UpdateData is implemented accordingly, delete this line so that
                         // Update is called once and only once for gesture checking

    if (Math.abs(pitch - oldPitch) < PITCH_LEEWAY && Math.abs(roll - oldRoll) < ROLL_LEEWAY
      && Math.abs(yaw - oldYaw) < YAW_LEEWAY) {
      handIsStill = true;
      return true;
    } else {
      handIsStill = false;
      return false;
    }
  }

  public boolean CheckUprightHand(double p, double r, double y) {
    UpdateData(p, r, y); // once UpdateData is implemented accordingly, delete this line so that
                         // Update is called once and only once for gesture checking

    if (pitch > UPRIGHT_PITCH_THRESHOLD && Math.abs(yaw) < YAW_LEEWAY) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * This method relies on a wave being defined as an oscillation of the wrist rolling between two
   * positions
   * with the hand being upright.
   */
  public boolean CheckWave(double p, double r, double y) {

    boolean prevIncreasing = rollIncreasing;
    boolean prevDecreasing = rollDecreasing;

    // UpdateData(p, r, y); after update is implemented, this line should be called to look for
    // changes in roll through updating rollIncreasing/Decreasing
    if (!CheckUprightHand(p, r, y)) { // update is getting called here, changing
                                      // rollIncreasing/Decreasing - this is important (see above
                                      // comment)
      isWaving = false;
      isFirstWavePointSet = false;
      isSecondWavePointSet = false;
      return false;
    }
    if (handIsStill) {
      isFirstWavePointSet = false;
      isSecondWavePointSet = false;
      isWaving = false;
      return false;
    }

    if (prevIncreasing != rollIncreasing || prevDecreasing != rollDecreasing) {
      if (!isFirstWavePointSet) {
        isFirstWavePointSet = true;
      } else if (isFirstWavePointSet) {
        isSecondWavePointSet = true;
      }
    }

    if (isFirstWavePointSet && isSecondWavePointSet) {
      isWaving = true;
      return true;
    } else {
      isWaving = false;
      return false;
    }

  }

}
