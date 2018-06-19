package sq.rogue.rosettadrone.drone;

import android.support.annotation.NonNull;

import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_RESULT;

import java.util.Arrays;

import dji.common.airlink.SignalQualityCallback;
import dji.common.battery.AggregationState;
import dji.common.battery.BatteryState;
import dji.common.product.Model;
import dji.common.remotecontroller.HardwareState;
import dji.sdk.airlink.AirLink;
import dji.sdk.battery.Battery;
import dji.sdk.remotecontroller.RemoteController;


/**
 * Class that acts as an intermediary to control a Drone. This class handles all direct communication
 * between the application and the drone.
 */
public class DroneManager {
    private Drone mDrone;

    private int mUplinkQuality;
    private int mDownlinkQuality;

    //region constructors
    //---------------------------------------------------------------------------------------

    public DroneManager(Drone drone) {
        mUplinkQuality = 0;
        mDownlinkQuality = 0;

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

    public int sendTakeoff() {
        if (mDrone.isSafetyEnabled()) {
            return MAV_RESULT.MAV_RESULT_DENIED;
        }

        return MAV_RESULT.MAV_RESULT_ACCEPTED;
    }

    //---------------------------------------------------------------------------------------
    //endregion
}
