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
public class ServiceManager extends Service {
    private static final String TAG = ServiceManager.class.getSimpleName();


    private IVideoService iVideoService;
    private ServiceConnection mVideoConnection = new ServiceConnection() {


        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

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
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            Log.d(TAG, "VIDEO SERVICE CONNECTED");
            iTelemetryService = ITelemetryService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            iTelemetryService = null;
        }

    };

    /**
     * Binds the {@link ServiceManager} to both the {@link sq.rogue.rosettadrone.video.VideoService}
     * and {@link sq.rogue.rosettadrone.telemetry.TelemetryService}. If either service fails to bind
     * successfully both services will be unbound and closed. Note that binding to either service is
     * an asynchronous call and as such either connection will not be successfully established prior
     * to the value being returned. If you need to bind only one service use the {@link ServiceManager#bindTelemetryService()}
     * and {@link ServiceManager#bindVideoService()} methods.
     * @return True if both services bind successfully otherwise returns false.
     */
    private boolean bindServices() {
        //Telemetry binding
        Intent telemetryIntent = new Intent();

        telemetryIntent.setComponent(new ComponentName("sq.rogue.rosettadrone",
                "sq.rogue.rosettadrone.telemetry.TelemetryService"));

        boolean telemetryResult = bindService(telemetryIntent, mTelemetryConnection, BIND_AUTO_CREATE);

        if (!telemetryResult) {
            return false;
        }
        //Video binding
        Intent videoIntent = new Intent();

        videoIntent.setComponent(new ComponentName("sq.rogue.rosettadrone",
                "sq.rogue.rosettadrone.video.VideoService"));

        boolean videoResult = bindService(videoIntent, mVideoConnection, BIND_AUTO_CREATE);

        if (!videoResult) {
            return false;
        }

        //Both video and telemetry services were bound successfully.
        return true;
    }

    /**
     * Binds the ServiceManager to the {@link sq.rogue.rosettadrone.telemetry.TelemetryService}.
     * @return True if the binding is successfuly otherwise false.
     */
    private boolean bindTelemetryService() {
        Intent telemetryIntent = new Intent();

        telemetryIntent.setComponent(new ComponentName("sq.rogue.rosettadrone",
                "sq.rogue.rosettadrone.telemetry.TelemetryService"));

        return bindService(telemetryIntent, mTelemetryConnection, BIND_AUTO_CREATE);
    }

    /**
     * Binds the ServiceManager to the {@link sq.rogue.rosettadrone.video.VideoService}.
     * @return True if the binding is successfuly otherwise false.
     */
    private boolean bindVideoService() {

        Intent videoIntent = new Intent();

        videoIntent.setComponent(new ComponentName("sq.rogue.rosettadrone",
                "sq.rogue.rosettadrone.video.VideoService"));

        return bindService(videoIntent, mVideoConnection, BIND_AUTO_CREATE);
    }







    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
