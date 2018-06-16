package sq.rogue.rosettadrone;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;


/**
 * Class to manage the telemetry and video services.
 */
public class ServiceManager {
    private static final String TAG = ServiceManager.class.getSimpleName();

    private boolean isVideoServiceRunning = false;
    private boolean isTelemetryServiceRunning = false;

    public ServiceManager() {

    }

    //region services and connections
    //---------------------------------------------------------------------------------------

    private IVideoService iVideoService;
    private ServiceConnection mVideoConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "VIDEO SERVICE CONNECTED");
            iVideoService = IVideoService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            iVideoService = null;
        }

    };

    private ITelemetryService iTelemetryService;
    private ServiceConnection mTelemetryConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "TELEMETRY SERVICE CONNECTED");
            iTelemetryService = ITelemetryService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            iTelemetryService = null;
        }

    };

    //---------------------------------------------------------------------------------------
    //endregion

    //region telemetry interface
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return true if the call to the video service is successful otherwise false.
     */
    protected boolean startTelemetryService() {
        if (iTelemetryService != null) {
            try {
                isTelemetryServiceRunning = iTelemetryService.start();

                return isTelemetryServiceRunning;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     *
     * @return true if the call to the video service is successful otherwise false.
     */
    protected boolean stopTelemetryService() {
        if (iTelemetryService != null) {
            try {
                isTelemetryServiceRunning = !iTelemetryService.stop();

                return !isTelemetryServiceRunning;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     *
     * @return true if the call to the video service is successful otherwise false.
     */
    protected boolean restartTelemetryService() {
        if (iTelemetryService != null) {
            try {
                isTelemetryServiceRunning =  iTelemetryService.restart();

                return isTelemetryServiceRunning;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     *
     * @return true if the call to the video service is successful otherwise false.
     */
    protected boolean updateTelemetryService() {
        if (iTelemetryService != null) {
            try {
                return iTelemetryService.update();
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }
    //---------------------------------------------------------------------------------------
    //endregion

    //region video interface
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return true if the call to the video service is successful otherwise false.
     */
    protected boolean startVideoService() {
        if (iVideoService != null) {
            try {
                isVideoServiceRunning = iVideoService.start();

                return isVideoServiceRunning;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }

    }

    /**
     *
     * @return true if the call to the video service is successful otherwise false.
     */
    protected boolean stopVideoService() {
        if (iVideoService != null) {
            try {
                isVideoServiceRunning = !iVideoService.stop();

                return !isVideoServiceRunning;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     *
     * @return true if the call to the video service is successful otherwise false.
     */
    protected boolean restartVideoService() {
        if (iVideoService != null) {
            try {
                isVideoServiceRunning = iVideoService.restart();

                return isVideoServiceRunning;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     *
     * @return true if the call to the video service is successful otherwise false.
     */
    protected boolean updateVideoService() {
        if (iVideoService != null) {
            try {
                return iVideoService.update();
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }
    //---------------------------------------------------------------------------------------
    //endregion

    //region bindings
    //---------------------------------------------------------------------------------------

    /**
     * Binds the {@link ServiceManager} to both the {@link sq.rogue.rosettadrone.video.VideoService}
     * and {@link sq.rogue.rosettadrone.telemetry.TelemetryService}. If either service fails to bind
     * successfully both services will be unbound and closed. Note that binding to either service is
     * an asynchronous call and as such either connection will not be successfully established prior
     * to the value being returned. If you need to bind only one service use the {@link ServiceManager#bindTelemetryService()}
     * and {@link ServiceManager#bindVideoService()} methods.
     * @return True if both services bind successfully otherwise returns false.
     */
    protected boolean bindServices() {
        //Telemetry binding
        boolean telemetryResult = bindTelemetryService();

        if (!telemetryResult) {
            return false;
        }

        //Video binding
        boolean videoResult = bindVideoService();

        if (!videoResult) {
            return false;
        }

        //Both video and telemetry services were bound successfully.
        return true;
    }

    /**
     * Binds the ServiceManager to the {@link sq.rogue.rosettadrone.telemetry.TelemetryService}.
     * @return True if the binding is successfully otherwise false.
     */
    protected boolean bindTelemetryService() {
        Intent telemetryIntent = new Intent();

        telemetryIntent.setComponent(new ComponentName("sq.rogue.rosettadrone",
                "sq.rogue.rosettadrone.telemetry.TelemetryService"));

        return bindService(telemetryIntent, mTelemetryConnection, BIND_AUTO_CREATE);
    }

    /**
     * Binds the ServiceManager to the {@link sq.rogue.rosettadrone.video.VideoService}.
     * @return True if the binding is successfully otherwise false.
     */
    protected boolean bindVideoService() {

        Intent videoIntent = new Intent();

        videoIntent.setComponent(new ComponentName("sq.rogue.rosettadrone",
                "sq.rogue.rosettadrone.video.VideoService"));

        return bindService(videoIntent, mVideoConnection, BIND_AUTO_CREATE);
    }

    //---------------------------------------------------------------------------------------
    //endregion

}
