package sq.rogue.rosettadrone.drone;

import android.support.annotation.NonNull;

import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_RESULT;

import java.util.Arrays;

import dji.common.airlink.SignalQualityCallback;
import dji.common.battery.AggregationState;
import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.product.Model;
import dji.common.remotecontroller.HardwareState;
import dji.common.util.CommonCallbacks;
import dji.sdk.airlink.AirLink;
import dji.sdk.battery.Battery;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.remotecontroller.RemoteController;

import static com.MAVLink.enums.MAV_RESULT.MAV_RESULT_ACCEPTED;
import static com.MAVLink.enums.MAV_RESULT.MAV_RESULT_DENIED;
import static com.MAVLink.enums.MAV_RESULT.MAV_RESULT_FAILED;
import static dji.common.mission.waypoint.WaypointMissionState.EXECUTING;
import static dji.common.mission.waypoint.WaypointMissionState.EXECUTION_PAUSED;
import static dji.common.mission.waypoint.WaypointMissionState.NOT_SUPPORTED;
import static dji.common.mission.waypoint.WaypointMissionState.READY_TO_EXECUTE;


/**
 * Class that acts as an intermediary to control a Drone. This class handles all direct communication
 * between the application and the drone.
 */
public class DroneManager {
    private Drone mDrone;
    private WaypointMissionOperator waypointMissionOperator;

    private IDroneManager mDroneManagerCallback;

    private int mUplinkQuality;
    private int mDownlinkQuality;

    private int mGCSCommandedMode;

    //region constructors
    //---------------------------------------------------------------------------------------

    public DroneManager(Drone drone) {
        mUplinkQuality = 0;
        mDownlinkQuality = 0;

        mGCSCommandedMode = 0;

        mDroneManagerCallback = null;

        if (drone == null) {
            setupDrone(new Drone());
        }
        setupDrone(drone);
    }

    //---------------------------------------------------------------------------------------
    //endregion

    /**
     * Main bootstrapping method for setting up a drone. Calls and sets up all the requisite
     * parameters for the drone.
     * @param drone The Drone to setup.
     * @return true if the setup finished successfully, otherwise false.
     */
    private boolean setupDrone(Drone drone) {
        mDrone = drone;

        if (!setCellVoltages()) {
            return false;
        }
        setCellVoltages();

        if (!setHardwareStateCallback(null)) {
            return false;
        }

        if (!setBatteryStateCallback(null)) {
            return false;
        }

        return setLinkQualityCallbacks(null, null);
    }

    //region setup
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    private boolean setCellVoltages() {
        if (mDrone == null) {
            return false;
        }
        int[] cellVoltages = new int[10];
        Arrays.fill(cellVoltages, 0xffff);
        mDrone.setCellVoltages(cellVoltages);
        return true;
    }

    /**
     * Sets the callback for when RC state changes
     * @param callback The callback to be called whenever RC state changes. Should be null if you want
     *                 the default.
     * @return true if the callback is set successfully. If the callback can't be set it means the
     * RC is null or the Drone was not setup prior to setting up the callback.
     */
    private boolean setHardwareStateCallback(HardwareState.HardwareStateCallback callback) {
        if (mDrone == null) {
            return false;
        }

        RemoteController rc = mDrone.getRemoteController();
        if (rc != null) {
            if (callback == null) {
                rc.setHardwareStateCallback(new HardwareState.HardwareStateCallback() {
                    @Override
                    public void onUpdate(@NonNull HardwareState hardwareState) {
                        mDrone.setThrottle(
                                (hardwareState.getLeftStick().getVerticalPosition() + 660) / 1320);
                    }
                });
            } else {
                rc.setHardwareStateCallback(callback);
            }

            return true;
        }

        return false;

    }


    /**
     * Sets the callback for battery state changes. The M600 uses an aggregation callback due to having
     * multiple batteries, and as a result passing a pre-defined callback when using the M600 is
     * NOT currently supported. Pass in null when using the M600.
     * @param callback The callback to be called whenever battery state changes. Should be null if
     *                 you want the default or are using the M600.
     * @return true if the callback is set successfully. If the callback can't be set it means the
     * battery is null or the Drone was not setup prior to setting the callback.
     */
    private boolean setBatteryStateCallback(BatteryState.Callback callback) {
        //TODO
        if (mDrone == null) {
            return false;
        }

        Battery battery = mDrone.getBattery();

        if (battery != null) {
            if (callback == null) {

                if (mDrone.getModel() == Model.MATRICE_600 || mDrone.getModel() == Model.MATRICE_600_PRO) {
                    Battery.setAggregationStateCallback(new AggregationState.Callback() {
                        @Override
                        public void onUpdate(AggregationState aggregationState) {
                            mDrone.setCapacity(aggregationState.getFullChargeCapacity());
                            mDrone.setChargeRemaining(aggregationState.getChargeRemaining());
                            mDrone.setVoltage(aggregationState.getVoltage());
                            mDrone.setCurrent(Math.abs(aggregationState.getCurrent()));
                            mDrone.setTemp(aggregationState.getHighestTemperature());
                        }
                    });
                } else {
                    battery.setStateCallback(new BatteryState.Callback() {
                        @Override
                        public void onUpdate(BatteryState batteryState) {
                            mDrone.setCapacity(batteryState.getFullChargeCapacity());
                            mDrone.setChargeRemaining(batteryState.getChargeRemaining());
                            mDrone.setVoltage(batteryState.getVoltage());
                            mDrone.setCurrent(Math.abs(batteryState.getCurrent()));
                            mDrone.setTemp(batteryState.getTemperature());
                        }
                    });
                }
            } else {
                battery.setStateCallback(callback);
            }

            return true;
        }

        return false;
    }

    /**
     * Sets the callbacks for when WiFi signal quality changes.
     * @param downlinkCallback The callback to be called when the downlink quality changes. Should be
     *                         null if the default is to be set.
     * @param uplinkCallback The callback to be called when the uplink quality changes. Should be null
     *                       if the default is to be set.
     * @return true if the callbacks are successfully set. false if either the airlink is null or the
     * drone has not be set prior to calling this method.
     */
    private boolean setLinkQualityCallbacks(SignalQualityCallback downlinkCallback,
                                            SignalQualityCallback uplinkCallback) {
        if (mDrone == null) {
            return false;
        }

        AirLink airLink = mDrone.getAirLink();

        if (airLink != null) {

            if (downlinkCallback == null) {
                airLink.setDownlinkSignalQualityCallback(new SignalQualityCallback() {
                    @Override
                    public void onUpdate(int i) {
                        mDownlinkQuality = i;
                    }
                });
            } else {
                airLink.setDownlinkSignalQualityCallback(downlinkCallback);
            }

            if (uplinkCallback == null) {
                airLink.setUplinkSignalQualityCallback(new SignalQualityCallback() {
                    @Override
                    public void onUpdate(int i) {
                        mUplinkQuality = i;
                    }
                });
            } else {
                airLink.setUplinkSignalQualityCallback(uplinkCallback);
            }

            return true;
        }

        return false;
    }


    //---------------------------------------------------------------------------------------
    //endregion


    //region commands
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public int sendTakeoff() {
        if (mDrone == null) {
            makeCallback(MAV_RESULT_FAILED);
            return MAV_RESULT_FAILED;
        }

        if (mDrone.isSafetyEnabled()) {
            makeCallback(MAV_RESULT_DENIED);
            return MAV_RESULT_DENIED;
        }

        if (waypointMissionOperator.getCurrentState() == READY_TO_EXECUTE) {
            int waypointResult = startWaypointMission();
            if (waypointResult == MAV_RESULT_FAILED || waypointResult == MAV_RESULT_DENIED) {
                makeCallback(waypointResult);
                return waypointResult;
            }

            makeCallback(MAV_RESULT_ACCEPTED);
            return MAV_RESULT_ACCEPTED;
        } else {
            FlightController flightController = mDrone.getFlightController();

            if (flightController == null) {
                makeCallback(MAV_RESULT_FAILED);
                return MAV_RESULT_FAILED;
            }

            flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        makeCallback(MAV_RESULT_FAILED);
                    } else {
                        makeCallback(MAV_RESULT_ACCEPTED);
                        mGCSCommandedMode = -1;
                    }
                }
            });
        }

        makeCallback(MAV_RESULT_ACCEPTED);
        return MAV_RESULT_ACCEPTED;
    }

    /**
     * Starts a previously uploaded waypoint mission. Both the Drone and WaypointMissionOperator must
     * be setup prior to calling this method.
     * @return {@link MAV_RESULT#MAV_RESULT_ACCEPTED} if the mission starts successfully,
     * {@link MAV_RESULT#MAV_RESULT_DENIED if the safety is still on}, and {@link MAV_RESULT#MAV_RESULT_FAILED}
     * if either the mission fails or one of the prerequisites was not met.
     */
    public int startWaypointMission() {
        if (mDrone == null) {
            return MAV_RESULT_FAILED;
        }

        if (waypointMissionOperator == null) {
            return MAV_RESULT_FAILED;
        }

        if (mDrone.isSafetyEnabled()) {
            return MAV_RESULT_DENIED;
        }

        if (waypointMissionOperator == null) {
            return MAV_RESULT_FAILED;
        }

        if (waypointMissionOperator.getCurrentState() != READY_TO_EXECUTE) {
            return MAV_RESULT_FAILED;
        }

        waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    makeCallback(MAV_RESULT_FAILED);
                }
            }
        });

        return MAV_RESULT_ACCEPTED;
    }

    /**
     *
     * @return
     */
    public int stopWaypointMission() {

    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region callback
    //---------------------------------------------------------------------------------------

    /**
     *
     * @param value
     */
    private void makeCallback(int value) {
        if (mDroneManagerCallback != null) {
            mDroneManagerCallback.onResult(value);
        }
    }

    /**
     *
     */
    public interface IDroneManager {
        void onResult(int result);
    }

    /**
     *
     * @param droneManagerCallback
     */
    public void setDroneManagerCallback(IDroneManager droneManagerCallback) {
        this.mDroneManagerCallback = droneManagerCallback;
    }

    /**
     *
     */
    public void removeDroneManagerCallback() {
        this.mDroneManagerCallback = null;
    }

    //---------------------------------------------------------------------------------------
    //endregion
}
