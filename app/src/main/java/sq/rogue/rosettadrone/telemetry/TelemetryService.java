package sq.rogue.rosettadrone.telemetry;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import sq.rogue.rosettadrone.ITelemetryService;
import sq.rogue.rosettadrone.drone.Drone;
import sq.rogue.rosettadrone.drone.DroneManager;
import sq.rogue.rosettadrone.gcs.GCSOutbound;
import sq.rogue.rosettadrone.managers.DJIManager;

public class TelemetryService extends Service implements DJIManager.IDJIListener{

    private static final String TAG = TelemetryService.class.getSimpleName();

    protected SharedPreferences sharedPreferences;

    private DroneManager mDroneManager;

    private boolean droneConnected = false;
    private boolean isRunning = false;

    private GCSOutbound gcsOutbound;

    private HandlerThread handlerThread;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {

        return START_STICKY;
    }

    private void start() {
        handlerThread = new HandlerThread("TickThread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        handler = new Handler(looper);
    }

    private void stop() {

    }

    private void restart() {

    }

    private void tick() {
        handler.post(() -> gcsOutbound.tick(mDroneManager.getDrone(), mDroneManager.));
    }

    //region service binding
    //---------------------------------------------------------------------------------------

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        DJIManager.getInstance().addDJIListener(this);

        return mBinder;
    }

    private ITelemetryService.Stub mBinder = new ITelemetryService.Stub() {

        @Override
        public boolean start() throws RemoteException {
            if (!droneConnected || isRunning) {
                return false;
            }

            TelemetryService.this.start();

            return true;
        }

        @Override
        public boolean stop() throws RemoteException {
            if (!isRunning) {
                return false;
            }

            TelemetryService.this.stop();
            return true;
        }

        @Override
        public boolean restart() throws RemoteException {
            if (droneConnected) {
                if (!isRunning) {
                    TelemetryService.this.start();
                } else {
                    TelemetryService.this.restart();
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean update() throws RemoteException {
            return false;
        }

    };

    //---------------------------------------------------------------------------------------
    //endregion

    //region DJI Manager interface
    //---------------------------------------------------------------------------------------

    @Override
    public void onDroneConnected(Drone drone) {
        mDroneManager = new DroneManager(mDrone);
        droneConnected = true;
    }

    @Override
    public void onDroneDisconnected() {
        droneConnected = false;
    }

    @Override
    public void onStatusChange() {

    }

    @Override
    public void onRegistration(boolean result) {

    }

    //---------------------------------------------------------------------------------------
    //endregion
}
