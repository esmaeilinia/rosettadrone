package sq.rogue.rosettadrone.gcs;

//        Copyright (c) 2018, U.S. Federal Government (in countries where recognized)
//
//        Redistribution and use in source and binary forms, with or without
//        modification, are permitted provided that the following conditions are met:
//
//        * Redistributions of source code must retain the above copyright notice, this
//        list of conditions and the following disclaimer.
//
//        * Redistributions in binary form must reproduce the above copyright notice,
//        this list of conditions and the following disclaimer in the documentation
//        and/or other materials provided with the distribution.
//
//        * Neither the name of the copyright holder nor the names of its
//        contributors may be used to endorse or promote products derived from
//        this software without specific prior written permission.
//
//        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
//        AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
//        IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//        DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
//        FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
//        DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
//        SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
//        CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
//        OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//        OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import android.util.Log;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_altitude;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_autopilot_version;
import com.MAVLink.common.msg_battery_status;
import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_gps_raw_int;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_home_position;
import com.MAVLink.common.msg_mission_ack;
import com.MAVLink.common.msg_mission_count;
import com.MAVLink.common.msg_mission_item;
import com.MAVLink.common.msg_mission_item_reached;
import com.MAVLink.common.msg_mission_request;
import com.MAVLink.common.msg_mission_request_list;
import com.MAVLink.common.msg_power_status;
import com.MAVLink.common.msg_radio_status;
import com.MAVLink.common.msg_rc_channels;
import com.MAVLink.common.msg_statustext;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.common.msg_vfr_hud;
import com.MAVLink.common.msg_vibration;
import com.MAVLink.enums.GPS_FIX_TYPE;
import com.MAVLink.enums.MAV_AUTOPILOT;
import com.MAVLink.enums.MAV_FRAME;
import com.MAVLink.enums.MAV_MISSION_RESULT;
import com.MAVLink.enums.MAV_MISSION_TYPE;
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
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.waypoint.Waypoint;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import sq.rogue.rosettadrone.ArduCopterFlightModes;
import sq.rogue.rosettadrone.drone.Drone;

import static com.MAVLink.enums.MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1;
import static sq.rogue.rosettadrone.util.util.getTimestampMicroseconds;

public class GCSOutbound implements Runnable {
    private final static String TAG = GCSOutbound.class.getSimpleName();

    //Current mavlink version
    private final static int MAVLINK_VERSION = 0x03;

    private IGCSOutbound mGCSOutboundCallback;
    private DatagramSocket mSocket;

    private int mSystemID;

    private InetAddress mGCSAddress;
    private int mGCSPort;

    private long ticks;

    //region constructors
    //---------------------------------------------------------------------------------------

    public GCSOutbound() {
        mGCSAddress = null;
        mGCSPort = -1;
        mSystemID = 0x01;
        ticks = 0;
    }

    public GCSOutbound(InetAddress gcsAddress, int gcsPort) {
        setGCSAddress(gcsAddress);
        setGCSPort(gcsPort);
        mSystemID = 0x01;
        ticks = 0;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region GCS
    //---------------------------------------------------------------------------------------

    /**
     * Get the currently set IP address for the ground station
     *
     * @return IP address of the ground station as an InetAddress
     */
    public InetAddress getGCSAddress() {
        return mGCSAddress;
    }

    /**
     * Set the IP address of the ground station
     *
     * @param gcsAddress IP Address of the ground station as an InetAddress
     */
    public void setGCSAddress(InetAddress gcsAddress) {
        mGCSAddress = gcsAddress;
    }

    /**
     * Get the currently set port of the ground station
     *
     * @return The port of the ground station that is currently used
     */
    public int getGCSPort() {
        return mGCSPort;
    }

    /**
     * Set the port of the ground station
     *
     * @param gcsPort The port of the ground station to send telemetry traffic to
     */
    public void setGCSPort(int gcsPort) {
        mGCSPort = gcsPort;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region socket
    //---------------------------------------------------------------------------------------

    public DatagramSocket getSocket() {
        return mSocket;
    }

    public void setSocket(DatagramSocket socket) {
        mSocket = socket;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region system id
    //---------------------------------------------------------------------------------------

    public int getSystemID() {
        return mSystemID;
    }

    public void setSystemID(int systemID) {
        mSystemID = systemID;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region main loop
    //---------------------------------------------------------------------------------------

    @Override
    public void run() {

    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region tick
    //---------------------------------------------------------------------------------------

    /**
     * @param drone
     * @param linkQuality
     */
    public void tick(Drone drone, int linkQuality) {
        ticks += 100;

        if (drone == null) {
            return;
        }

        try {
            if (ticks % 100 == 0) {
                sendAttitude(drone);
                sendAltitude(drone);
                sendVibration();
                sendVFRHud(drone);
            }
            if (ticks % 300 == 0) {
                sendGlobalPositionInt(drone);
                sendGPSRawInt(drone);
                sendRadioStatus();
                sendRCChannels(linkQuality);
            }
            if (ticks % 1000 == 0) {
                sendHeartbeat(drone);
                sendSystemStatus(drone);
                sendPowerStatus(drone);
                sendBatteryStatus();
            }
            if (ticks % 5000 == 0) {
                sendHomePosition(drone);
            }

        } catch (Exception e) {
            Log.d(TAG, "exception", e);
        }
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region status messages
    //---------------------------------------------------------------------------------------

    /**
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

    //region location
    //---------------------------------------------------------------------------------------

    /**
     * @param drone
     */
    public void sendGPSRawInt(Drone drone) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        FlightController flightController = drone.getFlightController();

        if (flightController == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        LocationCoordinate3D coordinate3D = flightController.getState().getAircraftLocation();

        msg_gps_raw_int gpsRawIntMessage = new msg_gps_raw_int();

        gpsRawIntMessage.time_usec = getTimestampMicroseconds();
        gpsRawIntMessage.lat = (int) (coordinate3D.getLatitude() * Math.pow(10, 7));
        gpsRawIntMessage.lon = (int) (coordinate3D.getLongitude() * Math.pow(10, 7));
        // TODO msg.alt
        // TODO msg.eph
        // TODO msg.epv
        // TODO msg.vel
        // TODO msg.cog
        gpsRawIntMessage.satellites_visible = (short) flightController.getState().getSatelliteCount();

        // DJI reports signal quality on a scale of 1-5
        // Mavlink has separate codes for fix type.
        GPSSignalLevel gpsSignalLevel = flightController.getState().getGPSSignalLevel();

        switch (gpsSignalLevel) {
            case NONE:
            case LEVEL_0:
            case LEVEL_1:
                gpsRawIntMessage.fix_type = GPS_FIX_TYPE.GPS_FIX_TYPE_NO_FIX;
                break;
            case LEVEL_2:
                gpsRawIntMessage.fix_type = GPS_FIX_TYPE.GPS_FIX_TYPE_2D_FIX;
                break;
            case LEVEL_3:
            case LEVEL_4:
            case LEVEL_5:
                gpsRawIntMessage.fix_type = GPS_FIX_TYPE.GPS_FIX_TYPE_3D_FIX;
                break;
        }

        sendMessage(gpsRawIntMessage);
    }

    /**
     * @param drone
     */
    public void sendGlobalPositionInt(Drone drone) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        FlightController flightController = drone.getFlightController();

        if (flightController == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        msg_global_position_int globalPositionIntMessage = new msg_global_position_int();

        LocationCoordinate3D coordinate3D = flightController.getState().getAircraftLocation();
        globalPositionIntMessage.lat = (int) (coordinate3D.getLatitude() * Math.pow(10, 7));
        globalPositionIntMessage.lon = (int) (coordinate3D.getLongitude() * Math.pow(10, 7));

        // NOTE: Commented out this field, because msg.relative_alt seems to be intended for altitude above the current terrain,
        // but DJI reports altitude above home point.
        // Mavlink: Millimeters above ground (unspecified: presumably above home point?)
        // DJI: relative altitude of the aircraft relative to take off location, measured by barometer, in meters.
        globalPositionIntMessage.relative_alt = (int) (coordinate3D.getAltitude() * 1000);

        // Mavlink: Millimeters AMSL
        // msg.alt = ??? No method in SDK for obtaining MSL altitude.
        // djiAircraft.getFlightController().getState().getHomePointAltitude()) seems promising, but always returns 0

        // Mavlink: m/s*100
        // DJI: m/s
        globalPositionIntMessage.vx = (short) (flightController.getState().getVelocityX() * 100); // positive values N
        globalPositionIntMessage.vy = (short) (flightController.getState().getVelocityY() * 100); // positive values E
        globalPositionIntMessage.vz = (short) (flightController.getState().getVelocityZ() * 100); // positive values down

        // DJI=[-180,180] where 0 is true north, Mavlink=degrees
        // TODO unspecified in Mavlink documentation whether this heading is true or magnetic
        double yaw = flightController.getState().getAttitude().yaw;
        if (yaw < 0)
            yaw += 360;
        globalPositionIntMessage.hdg = (int) (yaw * 100);

        sendMessage(globalPositionIntMessage);
    }

    /**
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

        homePositionMessage.latitude = (int) (droneLat * Math.pow(10, 7));
        homePositionMessage.longitude = (int) (droneLong * Math.pow(10, 7));
        homePositionMessage.altitude = (int) (flightController.getState().getHomePointAltitude());

        sendMessage(homePositionMessage);
    }


    /**
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

    //---------------------------------------------------------------------------------------
    //endregion

    /**
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

        switch (flightMode) {
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
     */
    public void sendAutopilotVersion() {
        msg_autopilot_version autopilotVersionMessage = new msg_autopilot_version();
        autopilotVersionMessage.capabilities = MAV_PROTOCOL_CAPABILITY.MAV_PROTOCOL_CAPABILITY_COMMAND_INT | MAV_PROTOCOL_CAPABILITY.MAV_PROTOCOL_CAPABILITY_MISSION_INT;

        sendMessage(autopilotVersionMessage);
    }

    /**
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

    //region mission
    //---------------------------------------------------------------------------------------

    /**
     * @param drone
     * @param seq
     */
    public void sendMissionItem(Drone drone, int seq) {
        if (drone == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        FlightController flightController = drone.getFlightController();

        if (flightController == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        msg_mission_item missionItemMessage = new msg_mission_item();

        WaypointMissionOperator waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();

        if (seq == 0) {
            missionItemMessage.x = (float) (flightController.getState().getHomeLocation().getLatitude());
            missionItemMessage.y = (float) (flightController.getState().getHomeLocation().getLongitude());
            missionItemMessage.z = 0;
        } else {
            Waypoint wp = waypointMissionOperator.getLoadedMission().getWaypointList().get(seq - 1);
            missionItemMessage.x = (float) (wp.coordinate.getLatitude());
            missionItemMessage.y = (float) (wp.coordinate.getLongitude());
            missionItemMessage.z = wp.altitude;
        }

        missionItemMessage.seq = seq;
        missionItemMessage.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
        sendMessage(missionItemMessage);
    }

    /**
     * @param seq
     */
    public void sendMissionItemReached(int seq) {
        msg_mission_item_reached msg = new msg_mission_item_reached();
        msg.seq = seq;
        sendMessage(msg);
    }

    /**
     *
     */
    public void sendMissionCount() {
        msg_mission_count missionCountMessage = new msg_mission_count();

        missionCountMessage.mission_type = MAV_MISSION_TYPE.MAV_MISSION_TYPE_MISSION;

        sendMessage(missionCountMessage);
    }

    /**
     * @param seq
     */
    public void requestMissionItem(int seq) {
        msg_mission_request requestMissionItemMessage = new msg_mission_request();

        requestMissionItemMessage.seq = seq;
        requestMissionItemMessage.mission_type = MAV_MISSION_TYPE.MAV_MISSION_TYPE_MISSION;

        sendMessage(requestMissionItemMessage);
    }

    /**
     *
     */
    public void requestMissionList() {
        msg_mission_request_list requestMissionListMessage = new msg_mission_request_list();
        sendMessage(requestMissionListMessage);
    }

    /**
     *
     */
    public void sendMissionACK() {
        msg_mission_ack missionACKMessage = new msg_mission_ack();

        missionACKMessage.type = MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED;
        missionACKMessage.mission_type = MAV_MISSION_TYPE.MAV_MISSION_TYPE_MISSION;

        sendMessage(missionACKMessage);
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region send message
    //---------------------------------------------------------------------------------------

    /**
     * Send a telemetry packet to the ground station
     *
     * @param message The {@link MAVLinkMessage} to send.
     */
    private void sendMessage(MAVLinkMessage message) {
        if (mSocket == null || message == null) {
            makeCallback(MAV_RESULT.MAV_RESULT_FAILED);
            return;
        }

        MAVLinkPacket packet = message.pack();

        packet.sysid = mSystemID;
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
        if (mGCSOutboundCallback != null) {
            mGCSOutboundCallback.onResult(value);
        }
    }

    /**
     * Hook into the callback interface. Only one hook can be acquired at a time. If multiple calls
     * are made only the most recent is sent a callback.
     *
     * @param gcsOutboundCallback
     */
    public void setGCSOutboundCallback(IGCSOutbound gcsOutboundCallback) {
        this.mGCSOutboundCallback = gcsOutboundCallback;
    }

    /**
     * Remove the callback hook.
     */
    public void removeDroneManagerCallback() {
        this.mGCSOutboundCallback = null;
    }

    /**
     * The callback interface. Called whenever an operation relating to drone operation fails or succeeds.
     */
    public interface IGCSOutbound {
        void onResult(int result);
    }

    //---------------------------------------------------------------------------------------
    //endregion

}
