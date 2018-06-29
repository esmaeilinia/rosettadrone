package sq.rogue.rosettadrone.gcs;

import android.os.AsyncTask;
import android.util.Log;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Parser;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;
import java.util.TimerTask;

import sq.rogue.rosettadrone.telemetry.MAVLinkReceiver;
import sq.rogue.rosettadrone.MainActivity;
import sq.rogue.rosettadrone.drone.DroneManager;

public class GCSInbound implements Runnable {
    private final static int BUF_SIZE = 512;

    private DatagramSocket mSocket;

    private DroneManager mDroneManager;

    private Parser mMAVLinkParser;
    private MAVLinkReceiver mMAVLinkReceiver;

    //region constructors
    //---------------------------------------------------------------------------------------

    public GCSInbound() {
        mSocket = null;
        mMAVLinkParser = new Parser();
    }

    public GCSInbound(DatagramSocket socket) {
        mSocket = socket;
        mMAVLinkParser = new Parser();
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region MAVLink Receiver
    //---------------------------------------------------------------------------------------

    /**
     *
     * @param droneManager
     */
    public void initMAVLinkReceiver(DroneManager droneManager) {
        mMAVLinkReceiver = new MAVLinkReceiver(droneManager);
    }

    /**
     *
     * @return
     */
    public MAVLinkReceiver getMAVLinkReceiver() {
        return mMAVLinkReceiver;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region socket
    //---------------------------------------------------------------------------------------

    public DatagramSocket getSocket() {
        return mSocket;
    }

    public DatagramSocket setSocket(DatagramSocket socket) {
        return mSocket = socket;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region main loop
    //---------------------------------------------------------------------------------------

    @Override
    public void run() {

        while (!Thread.interrupted()) {
            try {
                byte[] buf = new byte[BUF_SIZE];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);

                mSocket.receive(dp);

                byte[] bytes = dp.getData();
                int[] ints = new int[bytes.length];
                for (int i = 0; i < bytes.length; i++)
                    ints[i] = bytes[i] & 0xff;

                for (int i = 0; i < bytes.length; i++) {
                    MAVLinkPacket packet = mMavlinkParser.mavlink_parse_char(ints[i]);

                    if (packet != null) {
                        MAVLinkMessage msg = packet.unpack();
                        if (mainActivityWeakReference.get().prefs.getBoolean("pref_log_mavlink", false))
                            mainActivityWeakReference.get().logMessageFromGCS(msg.toString());
                        mMAVLinkReceiver.process(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region GCS Timer Task
    //---------------------------------------------------------------------------------------

    private static class GCSSenderTimerTask extends TimerTask {

        private WeakReference<MainActivity> mainActivityWeakReference;

        GCSSenderTimerTask(WeakReference<MainActivity> mainActivityWeakReference) {
            this.mainActivityWeakReference = mainActivityWeakReference;
        }

        @Override
        public void run() {
            mainActivityWeakReference.get().mModel.tick();
        }
    }


    private static class GCSCommunicatorAsyncTask extends AsyncTask<Integer, Integer, Integer> {

        private static final String TAG = GCSSenderTimerTask.class.getSimpleName();
        public boolean request_renew_datalinks = false;
        private Timer timer;
        private WeakReference<MainActivity> mainActivityWeakReference;

        GCSCommunicatorAsyncTask(MainActivity mainActivity) {
            mainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        public void renewDatalinks() {
//            Log.d(TAG, "renewDataLinks");
            request_renew_datalinks = true;
        }

        private void onRenewDatalinks() {
//            Log.d(TAG, "onRenewDataLinks");
            createTelemetrySocket();
//            mainActivityWeakReference.get().sendRestartVideoService();
        }

        @Override
        protected Integer doInBackground(Integer... ints2) {
//            Log.d("RDTHREADS", "doInBackground()");

            try {
                createTelemetrySocket();
                mainActivityWeakReference.get().mMavlinkParser = new Parser();

                GCSSenderTimerTask gcsSender = new GCSSenderTimerTask(mainActivityWeakReference);
                timer = new Timer(true);
                timer.scheduleAtFixedRate(gcsSender, 0, 100);

                while (!isCancelled()) {
                    // Listen for packets
                    try {
                        if (request_renew_datalinks) {
                            request_renew_datalinks = false;
                            onRenewDatalinks();

                        }
                        byte[] buf = new byte[1000];
                        DatagramPacket dp = new DatagramPacket(buf, buf.length);
                        mainActivityWeakReference.get().socket.receive(dp);

                        byte[] bytes = dp.getData();
                        int[] ints = new int[bytes.length];
                        for (int i = 0; i < bytes.length; i++)
                            ints[i] = bytes[i] & 0xff;

                        for (int i = 0; i < bytes.length; i++) {
                            MAVLinkPacket packet = mainActivityWeakReference.get().mMavlinkParser.mavlink_parse_char(ints[i]);

                            if (packet != null) {
                                MAVLinkMessage msg = packet.unpack();
                                if (mainActivityWeakReference.get().prefs.getBoolean("pref_log_mavlink", false))
                                    mainActivityWeakReference.get().logMessageFromGCS(msg.toString());
                                mainActivityWeakReference.get().mMavlinkReceiver.process(msg);
                            }
                        }
                    } catch (IOException e) {
                        //logMessageDJI("IOException: " + e.toString());
                    }
                }

            } catch (Exception e) {
                Log.d(TAG, "exception", e);
            } finally {
                if (mainActivityWeakReference.get().socket.isConnected()) {
                    mainActivityWeakReference.get().socket.disconnect();
                }
                if (timer != null) {
                    timer.cancel();
                }
//                Log.d("RDTHREADS", "doInBackground() complete");

            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            /*
            TODO Not sure what to do here...
             */
            if (mainActivityWeakReference.get() == null || mainActivityWeakReference.get().isFinishing())
                return;

            mainActivityWeakReference.clear();

        }

        @Override
        protected void onCancelled(Integer result) {
            super.onCancelled();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

        }

        protected void close() {
            if (mainActivityWeakReference.get().socket != null) {
                mainActivityWeakReference.get().socket.disconnect();
                mainActivityWeakReference.get().socket.close();
            }
        }
    }

    //---------------------------------------------------------------------------------------
    //endregion

}
