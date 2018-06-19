package sq.rogue.rosettadrone.drone;

import android.support.annotation.NonNull;

import java.util.Arrays;

import dji.common.airlink.SignalQualityCallback;
import dji.common.battery.BatteryState;
import dji.common.remotecontroller.HardwareState;
import dji.sdk.airlink.AirLink;
import dji.sdk.battery.Battery;
import dji.sdk.remotecontroller.RemoteController;

/**
 * Class that acts as an intermediary to control a Drone
 */
public class DroneManager {
    private Drone mDrone;

    private int mUplinkQuality;
    private int mDownlinkQuality;

    public DroneManager(Drone drone) {
        mUplinkQuality = 0;
        mDownlinkQuality = 0;

        if (drone == null) {
            setupDrone(new Drone());
        }
        setupDrone(drone);
    }

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

        if (!setLinkQualityCallbacks(null, null)) {
            return false;
        }

        return true;
    }

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
     *
     * @param callback
     * @return
     */
    private boolean setBatteryStateCallback(BatteryState.Callback callback) {
        //TODO
        if (mDrone == null) {
            return false;
        }

        Battery battery = mDrone.getBattery();

        if (battery != null) {
            if (callback == null) {
                battery.setStateCallback(new BatteryState.Callback() {
                    @Override
                    public void onUpdate(BatteryState batteryState) {

                    }
                });
            } else {
                battery.setStateCallback(callback);
            }

            return true;
        }

        return false;
    }

    /**
     *
     * @param downlinkCallback
     * @param uplinkCallback
     * @return
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



}
