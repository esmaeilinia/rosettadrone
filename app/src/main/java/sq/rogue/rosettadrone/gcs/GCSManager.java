package sq.rogue.rosettadrone.gcs;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_altitude;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_autopilot_version;
import com.MAVLink.common.msg_battery_status;
import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_home_position;
import com.MAVLink.common.msg_power_status;
import com.MAVLink.common.msg_radio_status;
import com.MAVLink.common.msg_rc_channels;
import com.MAVLink.common.msg_statustext;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.common.msg_vfr_hud;
import com.MAVLink.common.msg_vibration;
import com.MAVLink.enums.MAV_AUTOPILOT;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.MAVLink.enums.MAV_PROTOCOL_CAPABILITY;
import com.MAVLink.enums.MAV_RESULT;
import com.MAVLink.enums.MAV_STATE;
import com.MAVLink.enums.MAV_TYPE;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.sdk.flightcontroller.FlightController;
import sq.rogue.rosettadrone.ArduCopterFlightModes;
import sq.rogue.rosettadrone.drone.Drone;

import static com.MAVLink.enums.MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1;

public class GCSManager {

    //Vehicles *generally* have a system ID of 1
    private final static int SYSTEM_ID = 0x01;

    //Current mavlink version
    private final static int  MAVLINK_VERSION = 0x03;

    private IGCSManager mGCSManagerCallback;
    private DatagramSocket mSocket;

    private InetAddress mGCSAddress;
    private int mGCSPort;

    //region constructors
    //---------------------------------------------------------------------------------------

    public GCSManager() {
        mGCSAddress = null;
        mGCSPort = -1;
    }

    public GCSManager(InetAddress gcsAddress, int gcsPort) {
        setGCSAddress(gcsAddress);
        setGCSPort(gcsPort);
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region GCS
    //---------------------------------------------------------------------------------------

    /**
     * Set the IP address of the ground station
     * @param gcsAddress IP Address of the ground station as an InetAddress
     */
    public void setGCSAddress(InetAddress gcsAddress) {
        mGCSAddress = gcsAddress;
    }

    /**
     * Get the currently set IP address for the ground station
     * @return IP address of the ground station as an InetAddress
     */
    public InetAddress getGCSAddress() {
        return mGCSAddress;
    }

    /**
     * Set the port of the ground station
     * @param gcsPort The port of the ground station to send telemetry traffic to
     */
    public void setGCSPort(int gcsPort) {
        mGCSPort = gcsPort;
    }

    /**
     * Get the currently set port of the ground station
     * @return The port of the ground station that is currently used
     */
    public int getGCSPort() {
        return mGCSPort;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region socket
    //---------------------------------------------------------------------------------------

    public void setSocket(DatagramSocket socket) {
        mSocket = socket;
    }

    public DatagramSocket getSocket() {
        return mSocket;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region status messages
    //---------------------------------------------------------------------------------------

    /**
     *
     * @param drone
     */
    public void sendSystemStatus(Drone drone) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }


        msg_sys_status sysStatusMessage = new msg_sys_status();

        if (drone.getCapacity() > 0) {
            sysStatusMessage.battery_remaining = (byte) ((float) drone.getChargeRemaining() / (float) drone.getCapacity() * 100.0);
        } else {
            sysStatusMessage.battery_remaining = 100;
        }

        sysStatusMessage.voltage_battery = drone.getVoltage();
        sysStatusMessage.current_battery = (short) drone.getCurrent();

        sendMessage(sysStatusMessage);
    }

    /**
     *
     */
    public void sendPowerStatus(Drone drone) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        msg_battery_status batteryStatusMessage = new msg_battery_status();

        batteryStatusMessage.current_consumed = drone.getCapacity() - drone.getChargeRemaining();
        batteryStatusMessage.voltages = drone.getCellVoltages();
        batteryStatusMessage.temperature = (short) (drone.getTemp() * 100);
        batteryStatusMessage.current_battery = (short) (drone.getCurrent() * 10);

        sendMessage(batteryStatusMessage);
    }

    /**
     *
     */
    public void sendBatteryStatus() {
        msg_power_status powerStatusMessage = new msg_power_status();

        sendMessage(powerStatusMessage);
    }

    /**
     *
     */
    public void sendRadioStatus() {
        msg_radio_status radioStatusMessage = new msg_radio_status();

        radioStatusMessage.rssi = 0; // TODO: work out units conversion (see issue #1)
        radioStatusMessage.remrssi = 0; // TODO: work out units conversion (see issue #1)

        sendMessage(radioStatusMessage);
    }

    /**
     *
     */
    public void sendStatusText(String text, int severity) {
        msg_statustext statusTextMessage = new msg_statustext();

        statusTextMessage.text = text.getBytes();
        statusTextMessage.severity = (short) severity;

        sendMessage(statusTextMessage);
    }

    //---------------------------------------------------------------------------------------
    //endregion

    /**
     *
     * @param drone
     */
    public void sendHeartbeat(Drone drone) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        FlightController flightcontroller = drone.getFlightController();

        if (flightcontroller == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        FlightMode flightMode = flightcontroller.getState().getFlightMode();

        if (flightMode == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        msg_heartbeat heartbeatMessage = new msg_heartbeat();

        heartbeatMessage.type = MAV_TYPE.MAV_TYPE_QUADROTOR;
        heartbeatMessage.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_ARDUPILOTMEGA;

        heartbeatMessage.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED;

        switch(flightMode) {
            case MANUAL:
                heartbeatMessage.custom_mode = ArduCopterFlightModes.STABILIZE;
                break;
            case ATTI:
                heartbeatMessage.custom_mode = ArduCopterFlightModes.LOITER;
                break;
            case ATTI_COURSE_LOCK:
                break;
            case GPS_ATTI:
                break;
            case GPS_COURSE_LOCK:
                break;
            case GPS_HOME_LOCK:
                break;
            case GPS_HOT_POINT:
                break;
            case ASSISTED_TAKEOFF:
                break;
            case AUTO_TAKEOFF:
                heartbeatMessage.custom_mode = ArduCopterFlightModes.GUIDED;
                heartbeatMessage.base_mode |= MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED;
                break;
            case AUTO_LANDING:
                heartbeatMessage.custom_mode = ArduCopterFlightModes.LAND;
                heartbeatMessage.base_mode |= MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED;
                break;
            case GPS_WAYPOINT:
                heartbeatMessage.custom_mode = ArduCopterFlightModes.AUTO;
                heartbeatMessage.base_mode |= MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED;
                break;
            case GO_HOME:
                heartbeatMessage.custom_mode = ArduCopterFlightModes.RTL;
                heartbeatMessage.base_mode |= MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED;
                break;
            case JOYSTICK:
                break;
            case GPS_ATTI_WRISTBAND:
                break;
            case DRAW:
                heartbeatMessage.base_mode |= MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED;
                break;
            case GPS_FOLLOW_ME:
                heartbeatMessage.custom_mode = ArduCopterFlightModes.GUIDED;
                heartbeatMessage.base_mode |= MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED;
                break;
            case ACTIVE_TRACK:
                heartbeatMessage.custom_mode = ArduCopterFlightModes.GUIDED;
                heartbeatMessage.base_mode |= MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED;
                break;
            case TAP_FLY:
                heartbeatMessage.custom_mode = ArduCopterFlightModes.GUIDED;
                break;
            case GPS_SPORT:
                break;
            case GPS_NOVICE:
                break;
            case UNKNOWN:
                break;
            case CONFIRM_LANDING:
                break;
            case TERRAIN_FOLLOW:
                break;
            case TRIPOD:
                break;
            case TRACK_SPOTLIGHT:
                break;
            case MOTORS_JUST_STARTED:
                break;
        }

        if (drone.getCommandedMode() == ArduCopterFlightModes.GUIDED) {
            heartbeatMessage.custom_mode = ArduCopterFlightModes.GUIDED;
        } else if (drone.getCommandedMode() == ArduCopterFlightModes.BRAKE) {
            heartbeatMessage.custom_mode = ArduCopterFlightModes.BRAKE;
        }

        if (drone.isArmed()) {
            heartbeatMessage.base_mode |= MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED;
        }

        // Catches manual landings
        // Automatically disarm motors if aircraft is on the ground and a takeoff is not in progress
        if (flightcontroller.getState().isFlying() && drone.getCommandedMode() != ArduCopterFlightModes.GUIDED) {
            drone.setArmed(false);
        }

        // Catches manual takeoffs
        if (flightcontroller.getState().areMotorsOn()) {
            drone.setArmed(true);
        }

        heartbeatMessage.system_status = MAV_STATE.MAV_STATE_ACTIVE;
        heartbeatMessage.mavlink_version = MAVLINK_VERSION;
        sendMessage(heartbeatMessage);
    }

    /**
     *
     * @param uplinkQuality
     */
    public void sendRCChannels(int uplinkQuality) {
        msg_rc_channels channelsMessage = new msg_rc_channels();

        channelsMessage.rssi = (short) uplinkQuality;

        sendMessage(channelsMessage);
    }

    /**
     *
     */
    public void sendVibration() {
        msg_vibration vibrationMessage = new msg_vibration();

        sendMessage(vibrationMessage);
    }

    /**
     *
     * @param drone
     */
    public void sendHomePosition(Drone drone) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        FlightController flightController = drone.getFlightController();

        if (flightController == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        msg_home_position homePositionMessage = new msg_home_position();

        double droneLat = flightController.getState().getHomeLocation().getLatitude();
        double droneLong = flightController.getState().getHomeLocation().getLongitude();

        homePositionMessage.latitude = (int) (droneLat * Math.pow(10,7));
        homePositionMessage.longitude = (int) (droneLong * Math.pow(10,7));
        homePositionMessage.altitude = (int) (flightController.getState().getHomePointAltitude());

        sendMessage(homePositionMessage);
    }

    /**
     *
     */
    public void sendAutopilotVersion() {
        msg_autopilot_version autopilotVersionMessage = new msg_autopilot_version();
        autopilotVersionMessage.capabilities = MAV_PROTOCOL_CAPABILITY.MAV_PROTOCOL_CAPABILITY_COMMAND_INT | MAV_PROTOCOL_CAPABILITY.MAV_PROTOCOL_CAPABILITY_MISSION_INT;

        sendMessage(autopilotVersionMessage);
    }

    /**
     *
     * @param drone
     */
    public void sendAttitude(Drone drone) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        FlightController flightController = drone.getFlightController();

        if (flightController == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }
        Attitude attitude = flightController.getState().getAttitude();

        msg_attitude msgAttitude = new msg_attitude();

        msgAttitude.roll = (float) (attitude.roll * Math.PI / 180);
        msgAttitude.pitch = (float) (attitude.pitch * Math.PI / 180);
        msgAttitude.yaw = (float) (attitude.yaw * Math.PI / 180);
        // TODO msg.rollspeed = 0;
        // TODO msg.pitchspeed = 0;
        // TODO msg.yawspeed = 0;
        sendMessage(msgAttitude);
    }

    /**
     *
     * @param drone
     */
    public void sendAltitude(Drone drone) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        FlightController flightController = drone.getFlightController();

        if (flightController == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        LocationCoordinate3D locationCoordinate3D = flightController.getState().getAircraftLocation();

        msg_altitude msgAltitude = new msg_altitude();
        msgAltitude.altitude_relative = (int) (locationCoordinate3D.getAltitude() * 1000);
        sendMessage(msgAltitude);
    }

    /**
     *
     * @param drone
     */
    public void sendVFRHud(Drone drone) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        FlightController flightController = drone.getFlightController();

        if (flightController == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        msg_vfr_hud vfrHudMessage = new msg_vfr_hud();

        vfrHudMessage.airspeed = (float) (Math.sqrt(Math.pow(flightController.getState().getVelocityX(), 2) +
                Math.pow(flightController.getState().getVelocityY(), 2)));

        // Mavlink: Current ground speed in m/s. For now, just echoing airspeed.
        //TODO
        vfrHudMessage.groundspeed = vfrHudMessage.airspeed;

        // Mavlink: Current heading in degrees, in compass units (0..360, 0=north)
        // TODO: unspecified in Mavlink documentation whether this heading is true or magnetic
        // DJI=[-180,180] where 0 is true north, Mavlink=degrees
        double yaw = flightController.getState().getAttitude().yaw;
        if (yaw < 0) {
            yaw += 360;
        }
        vfrHudMessage.heading = (short) yaw;

        // Mavlink: Current throttle setting in integer percent, 0 to 100
        vfrHudMessage.throttle = drone.getThrottle();

        // Mavlink: Current altitude (MSL), in meters
        // DJI: relative altitude is altitude of the aircraft relative to take off location, measured by barometer, in meters.
        // DJI: home altitude is home point's altitude. Units unspecified in DJI SDK documentation. Presumably meters AMSL.
        LocationCoordinate3D coord = flightController.getState().getAircraftLocation();
        vfrHudMessage.alt = (int) (coord.getAltitude());

        // Mavlink: Current climb rate in meters/second
        // DJI: m/s, positive values down
        vfrHudMessage.climb = -(short) (flightController.getState().getVelocityZ());

        sendMessage(vfrHudMessage);
    }

    //region acknowledgement
    //---------------------------------------------------------------------------------------

    /**
     *
     * @param messageID
     * @param result
     */
    public void sendACK(int messageID, int result) {
        msg_command_ack ack = new msg_command_ack();
        ack.command = messageID;
        ack.result = (short) result;
        sendMessage(ack);
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region send message
    //---------------------------------------------------------------------------------------

    /**
     * Send a telemetry packet to the ground station
     * @param message The {@link MAVLinkMessage} to send.
     */
    private void sendMessage(MAVLinkMessage message) {
        if (mSocket == null || message == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        MAVLinkPacket packet = message.pack();

        packet.sysid = SYSTEM_ID;
        packet.compid = MAV_COMP_ID_AUTOPILOT1;

        byte[] encodedPacket = packet.encodePacket();

        try {
          DatagramPacket datagramPacket = new DatagramPacket(encodedPacket, encodedPacket.length, mGCSAddress, mGCSPort);
          mSocket.send(datagramPacket);

          makeCallback(MAV_RESULT.MAV_RESULT_ACCEPTED);

        } catch (IOException e) {
            e.printStackTrace();
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
        }

    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region callback
    //---------------------------------------------------------------------------------------

    /**
     * Helper class to make a callback with the desired result code.
     *
     * @param value A {@link MAV_RESULT} code.
     */
    private void makeCallback(int value) {
        if (mGCSManagerCallback != null) {
            mGCSManagerCallback.onResult(value);
        }
    }

    /**
     * Hook into the callback interface. Only one hook can be acquired at a time. If multiple calls
     * are made only the most recent is sent a callback.
     *
     * @param gcsManagerCallback
     */
    public void setDroneManagerCallback(IGCSManager gcsManagerCallback) {
        this.mGCSManagerCallback = gcsManagerCallback;
    }

    /**
     * Remove the callback hook.
     */
    public void removeDroneManagerCallback() {
        this.mGCSManagerCallback = null;
    }

    /**
     * The callback interface. Called whenever an operation relating to drone operation fails or succeeds.
     */
    public interface IGCSManager {
        void onResult(int result);
    }

    //---------------------------------------------------------------------------------------
    //endregion

}
