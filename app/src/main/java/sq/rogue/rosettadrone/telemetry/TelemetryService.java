package sq.rogue.rosettadrone.telemetry;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import sq.rogue.rosettadrone.ITelemetryService;
import sq.rogue.rosettadrone.R;
import sq.rogue.rosettadrone.drone.Drone;
import sq.rogue.rosettadrone.drone.DroneManager;
import sq.rogue.rosettadrone.gcs.GCSOutbound;
import sq.rogue.rosettadrone.managers.DJIManager;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;
import static sq.rogue.rosettadrone.util.NotificationHandler.NOTIFICATION_ID;
import static sq.rogue.rosettadrone.util.NotificationHandler.createNotificationChannel;

public class TelemetryService extends Service implements DJIManager.IDJIListener{

    private static final String TAG = TelemetryService.class.getSimpleName();

    //in ms
    private static final int SOCKET_TIMEOUT = 1000;

    private static final int DEFAULT_PORT = 14550;
    private static final String DEFAULT_IP = "127.0.0.1";

    protected SharedPreferences sharedPreferences;

    private DroneManager mDroneManager;

    private boolean droneConnected = false;
    private boolean isRunning = false;

    private GCSOutbound gcsOutbound;

    private HandlerThread handlerThread;
    private Handler handler;
    private Thread telemetryThread;

    private DatagramSocket mSocket;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {

        return START_STICKY;
    }

    private void start() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelID = createNotificationChannel(this);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
            Notification notification = builder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(PRIORITY_MIN)
                    .build();
            startForeground(NOTIFICATION_ID, notification);
        }


        telemetryThread = new Thread(this::run);
        telemetryThread.start();
    }

    private void stop() {


        stopForeground(true);
    }

    private void restart() {

    }

    //region socket
    //---------------------------------------------------------------------------------------

    /**
     *
     */
    private void createSocket() {
        if (mSocket != null) {
            cleanupSocket();
        }

        try {
            mSocket = new DatagramSocket();
            mSocket.connect(getIP(), getPort());
            mSocket.setSoTimeout(SOCKET_TIMEOUT);
//            mainActivityWeakReference.get().logMessageDJI("Starting GCS telemetry link: " + gcsIPString + ":" + String.valueOf(telemIPPort));
        } catch (SocketException e) {
            Log.d(TAG, "createTelemetrySocket() - socket exception");
            Log.d(TAG, "exception", e);
//            mainActivityWeakReference.get().logMessageDJI("Telemetry socket exception: " + gcsIPString + ":" + String.valueOf(telemIPPort));
        } // TODO
        catch (UnknownHostException e) {
            Log.d(TAG, "createTelemetrySocket() - unknown host exception");
            Log.d(TAG, "exception", e);
//            mainActivityWeakReference.get().logMessageDJI("Unknown telemetry host: " + gcsIPString + ":" + String.valueOf(telemIPPort));
        } // TODO

//            Log.d(TAG, mainActivityWeakReference.get().socket.getInetAddress().toString());
//            Log.d(TAG, mainActivityWeakReference.get().socket.getLocalAddress().toString());
//            Log.d(TAG, String.valueOf(mainActivityWeakReference.get().socket.getPort()));
//            Log.d(TAG, String.valueOf(mainActivityWeakReference.get().socket.getLocalPort()));

    }

    /**
     *
     */
    private void cleanupSocket() {
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.close();
            mSocket = null;
        }
    }

    /**
     * Gets the telemetry IP address from shared preferences
     * @return the telemetry IP set in the settings, otherwise {@link #DEFAULT_IP}
     * @throws UnknownHostException if the IP address is not a valid InetAddress
     */
    private InetAddress getIP() throws UnknownHostException {
        String gcsIPString = DEFAULT_IP;

        if (sharedPreferences != null) {
            if (sharedPreferences.getBoolean("pref_external_gcs", false)) {
                gcsIPString = sharedPreferences.getString("pref_gcs_ip", DEFAULT_IP);
            }
        }

        return InetAddress.getByName(gcsIPString);
    }

    /**
     * Gets the telemetry port from shared preferences
     * @return the telemetry port set in the settings, otherwise {@link #DEFAULT_PORT}
     */
    private int getPort() {
        if (sharedPreferences != null) {
            return Integer.parseInt(sharedPreferences.getString("pref_telem_port", String.valueOf(DEFAULT_PORT)));
        }
        return DEFAULT_PORT;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region tick
    //---------------------------------------------------------------------------------------

    /**
     *
     */
    private void startTicks() {
        handlerThread = new HandlerThread("TickThread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        handler = new Handler(looper);

        handler.post(tick);
    }

    /**
     *
     */
    private void stopTicks() {
        handler.removeCallbacksAndMessages(null);
        handlerThread.quit();

        handlerThread = null;
        handler = null;
    }

    /**
     *
     */
    private Runnable tick = new Runnable() {
        @Override
        public void run() {
            gcsOutbound.tick(mDroneManager.getDrone(), mDroneManager.getUplinkQuality());

            handler.postDelayed(tick, 100);
        }
    };

    //---------------------------------------------------------------------------------------
    //endregion

    //region main loop
    //---------------------------------------------------------------------------------------

    private void run() {

        gcsOutbound = new GCSOutbound()

        while (!Thread.interrupted()) {

        }
    }

    //---------------------------------------------------------------------------------------
    //endregion

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
        mDroneManager = new DroneManager(drone);
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
