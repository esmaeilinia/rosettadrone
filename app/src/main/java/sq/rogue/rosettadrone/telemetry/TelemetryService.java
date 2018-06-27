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

public class TelemetryService extends Service {

    private static final String TAG = TelemetryService.class.getSimpleName();

    protected SharedPreferences sharedPreferences;

    private DroneManager droneManager;
    private Drone drone;

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

    private void startThread() {
        handlerThread = new HandlerThread("TickThread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        handler = new Handler(looper);
    }

    private void stopThread() {

    }

    private void tick() {
        handler.post(() -> gcsOutbound.tick());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        droneManager = new DroneManager();

        return mBinder;
    }

    private ITelemetryService.Stub mBinder = new ITelemetryService.Stub() {

        @Override
        public boolean start() throws RemoteException {
            this();

            return false;
        }

        @Override
        public boolean stop() throws RemoteException {

            return false;
        }

        @Override
        public boolean restart() throws RemoteException {
            return false;
        }

        @Override
        public boolean update() throws RemoteException {
            return false;
        }

    };
}
