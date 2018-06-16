package sq.rogue.rosettadrone.video;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import dji.common.product.Model;
import dji.sdk.camera.VideoFeeder;
import sq.rogue.rosettadrone.IVideoService;
import sq.rogue.rosettadrone.R;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

public class VideoService extends Service {

    public static final String ACTION_START = "VIDEO.START";
    public static final String ACTION_STOP = "VIDEO.STOP";
    public static final String ACTION_RESTART = "VIDEO.RESTART";
    public static final String ACTION_UPDATE = "VIDEO.UPDATE";
    public static final String ACTION_DRONE_CONNECTED = "VIDEO.DRONE_CONNECTED";
    public static final String ACTION_DRONE_DISCONNECTED = "VIDEO.DRONE_DISCONNECTED";
    public static final String ACTION_SET_MODEL = "VIDEO.SET_MODEL";
    public static final String ACTION_SEND_NAL = "VIDEO.SEND_NAL";
    private static final String TAG = VideoService.class.getSimpleName();
    protected H264Packetizer mPacketizer;
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    protected Model mModel;
    protected SharedPreferences sharedPreferences;
    protected Thread thread;
    private boolean isRunning = false;

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "oncreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (intent != null) {
            if (intent.getAction() != null) {
                Log.d(TAG, intent.getAction());
                switch (intent.getAction()) {
                    case ACTION_START:
                        break;
                    case ACTION_STOP:
                        break;
                    case ACTION_RESTART:
                        if (isRunning) {
                            setActionDroneDisconnected();
                            spinThread();
                        }
                        break;
                    case ACTION_UPDATE:
                        break;
                    case ACTION_DRONE_CONNECTED:
                        mModel = (Model) intent.getSerializableExtra("model");
                        spinThread();
                        break;
                    case ACTION_DRONE_DISCONNECTED:
                        setActionDroneDisconnected();
                        break;
                    case ACTION_SET_MODEL:
                        break;
                    case ACTION_SEND_NAL:
                        break;
                    default:
                        break;
                }
            }
        }

        return START_STICKY;
    }

    public void spinThread() {
        thread = new Thread() {
            @Override
            public void run() {
                setActionDroneConnected();
            }
        };
        thread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    public void setActionDroneConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelID = createNotificationChannel();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
            Notification notification = builder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(PRIORITY_MIN)
                    .build();
            startForeground(1, notification);
        }
        initVideoStreamDecoder();
        initPacketizer();

        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                //Log.d(TAG, "camera recv video data size: " + size);
                //sendNAL(videoBuffer);
                DJIVideoStreamDecoder.getInstance().parse(videoBuffer, size);
            }
        };
        if (!mModel.equals(Model.UNKNOWN_AIRCRAFT)) {
            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
            }
        }

        isRunning = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelID = "video_service";
        String channelName = "RosettaDrone Video Service";
        NotificationChannel chan = new NotificationChannel(channelID,
                channelName, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelID;
    }

    private void setActionDroneDisconnected() {
        stopForeground(true);
        isRunning = false;
        if (mPacketizer != null) {
            mPacketizer.getRtpSocket().close();
            mPacketizer.stop();
        }


        DJIVideoStreamDecoder.getInstance().stop();

        thread.interrupt();
        thread = null;
//        mGCSCommunicator.cancel(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private IVideoService.Stub mBinder = new IVideoService.Stub() {

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

    };
}
