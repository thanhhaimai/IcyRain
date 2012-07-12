/**
 */
package mai.icyrain;

import java.util.Arrays;
import java.util.List;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import com.mapquest.android.maps.RouteResponse;
import com.mapquest.android.maps.RouteResponse.Route.Leg;
import com.mapquest.android.maps.RouteResponse.Route.Leg.Maneuver;

/**
 * @author achalddave@live.com (Achal Dave)
 */
public class RouteLocationListener implements LocationListener {

  private Maneuver currManeuver;
  private int currManeuverIndex;
  private List<Maneuver> maneuvers;

  public RouteLocationListener(RouteResponse routeResponse) {
    List<Leg> legs = routeResponse.route.legs;
    if (legs != null) {
      for (Leg leg : legs) {
        List<Maneuver> maneuvers = leg.maneuvers;
        this.maneuvers = maneuvers;
        if (maneuvers != null) {
          int i = 1;
          for (Maneuver maneuver : maneuvers) {
            // Log.d("pullLocation", "maneuver " + i + " is " +
            // maneuver.startPoint.toString());
            if (!maneuver.directionName.equals("")) {
              Log.d("pullLocation",
                "turn " + maneuver.directionName + " at " + maneuver.startPoint.getLatitude()
                  + ", " + maneuver.startPoint.getLongitude());
            }
          }
          currManeuverIndex = 0;
          currManeuver = maneuvers.get(currManeuverIndex);
        }
      }
    } else {
      Log.d("pullLocation", "legs is null");
    }
  }

  @Override
  public void onLocationChanged(Location arg0) {
    double currLat = arg0.getLatitude();
    double currLng = arg0.getLongitude();
    Log.d("pullLocation", "My location is now " + currLat + ", " + currLng);

    double maneuverLat = currManeuver.startPoint.getLatitude();
    double maneuverLng = currManeuver.startPoint.getLongitude();
    float[] resultArray = new float[1];

    Log.d("pullLocation", "Maneuver location is " + currManeuver.startPoint.toString());
    Location.distanceBetween(currLat, currLng, maneuverLat, maneuverLng, resultArray);
    float distance = resultArray[0];
    Log.d("pullLocation", "Distance between is " + distance);
    if (distance < 1) {
      // TODO HAI: IMPLEMENT SENDING ARDUINO NOTIFICATIONS
      // it's time...
      int maneuverTurn = currManeuver.turnType;
      int direction = turnToDirectionInt(maneuverTurn);

      // vibrate the arduino
      switch (direction) {
      case 0: // forward
        // send arduino a "go forward" vibrate
        break;
      case 1: // right
        // send arduino a "go right" vibrate
        break;
      case 2: // down
        // send arduino a "go backward" vibrate
        break;
      case 3: // left
        // send arduino a "go left" vibrate
        break;
      }
    }
  }

  @Override
  public void onProviderDisabled(String arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onProviderEnabled(String arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
    // TODO Auto-generated method stub

  }

  // returns int between 0 and 3
  // forward: 0, right: 1, back: 2, left: 3
  private int turnToDirectionInt(int turnType) {
    /*
     * straight = 0
     * slight right = 1
     * right = 2
     * sharp right = 3
     * reverse = 4
     * sharp left = 5
     * left = 6
     * slight left = 7
     * right u-turn = 8
     * left u-turn =9
     * right merge =10
     * left merge = 11
     * right on ramp = 12
     * left on ramp = 13
     * right off ramp = 14
     * left off ramp = 15
     * right fork = 16
     * left fork = 17
     * straight fork = 18
     */
    int[] rightTurns = { 1, 2, 3, 8, 10, 12, 14, 16 };
    int[] leftTurns = { 5, 6, 7, 9, 11, 13, 15, 17 };
    int[] forward = { 0, 18 };
    int[] backward = { 4 };
    if (Arrays.asList(forward).contains(turnType)) {
      return 0;
    } else if (Arrays.asList(rightTurns).contains(turnType)) {
      return 1;
    } else if (Arrays.asList(backward).contains(turnType)) {
      return 2;
    } else if (Arrays.asList(leftTurns).contains(turnType)) {
      return 3;
    } else {
      return -1;
    }

  }

}
