package sq.rogue.rosettadrone.telemetry;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import dji.common.product.Model;
import sq.rogue.rosettadrone.ITelemetryService;
import sq.rogue.rosettadrone.video.VideoService;

public class TelemetryService extends Service implements ITelemetryService {

    private static final String TAG = TelemetryService.class.getSimpleName();

    protected SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        return START_STICKY;
    }

    @Override
    public boolean start() throws RemoteException {
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

    @Override
    public IBinder asBinder() {
        return null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
