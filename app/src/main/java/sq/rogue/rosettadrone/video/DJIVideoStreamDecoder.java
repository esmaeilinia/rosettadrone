package sq.rogue.rosettadrone.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import dji.common.product.Model;
import dji.log.DJILog;
import dji.midware.data.model.P3.DataCameraGetPushStateInfo;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import sq.rogue.rosettadrone.R;

public class DJIVideoStreamDecoder implements NativeHelper.NativeDataListener {
    public static final String VIDEO_ENCODING_FORMAT = "video/avc";
    private static final String TAG = DJIVideoStreamDecoder.class.getSimpleName();
    private static final int BUF_QUEUE_SIZE = 30;
    private static final int MSG_FRAME_QUEUE_IN = 1;
    private static final int MSG_DECODE_FRAME = 2;
    private static final int MSG_YUV_DATA = 3;
    private static DJIVideoStreamDecoder instance;
    private final boolean DEBUG = false;
    public int frameIndex = -1;
    public int width;
    public int height;
    private HandlerThread handlerThreadNew;
    private Handler handlerNew;
    private Queue<DJIFrame> frameQueue;
    private HandlerThread dataHandlerThread;
    private Handler dataHandler;
    private Context context;
    private long currentTime;
    private boolean hasIFrameInQueue = false;
    private IFrameDataListener frameDataListener;

    private DJIVideoStreamDecoder() {
        frameQueue = new ArrayBlockingQueue<DJIFrame>(BUF_QUEUE_SIZE);
        startDataHandler();
        handlerThreadNew = new HandlerThread("native parser thread");
        handlerThreadNew.start();
        handlerNew = new Handler(handlerThreadNew.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                byte[] buf = (byte[]) msg.obj;
                NativeHelper.getInstance().parse(buf, msg.arg1);
                return false;
            }
        });
    }

    public static DJIVideoStreamDecoder getInstance() {
        if (instance == null) {
            instance = new DJIVideoStreamDecoder();
        }
        return instance;
    }

    public void setFrameDataListener(IFrameDataListener frameDataListener) {
        this.frameDataListener = frameDataListener;
    }

    private void logd(String tag, String log) {
        if (!DEBUG) {
            return;
        }
        Log.d(tag, log);
    }

    private void loge(String tag, String log) {
        if (!DEBUG) {
            return;
        }
        Log.e(tag, log);
    }

    private void logd(String log) {
        logd(TAG, log);
    }

    private void loge(String log) {
        loge(TAG, log);
    }

    public void init(Context context, Surface surface) {
        this.context = context;
        NativeHelper.getInstance().setDataListener(this);
    }

    /**
     * Framing the raw data from the camera.
     *
     * @param buf  Raw data from camera.
     * @param size Data length
     */
    public void parse(byte[] buf, int size) {
        Message message = handlerNew.obtainMessage();
        message.obj = buf;
        message.arg1 = size;
        handlerNew.sendMessage(message);
    }

    /**
     * Get the resource ID of the IDR frame.
     *
     * @param pModel Product model of connecting DJI product.
     * @param width  Width of current video stream.
     * @return Resource ID of the IDR frame
     */
    public int getIframeRawId(Model pModel, int width) {
        int iframeId = R.raw.iframe_1280x720_ins;

        switch (pModel) {
            case PHANTOM_3_ADVANCED:
            case PHANTOM_3_STANDARD:
                if (width == 960) {
                    //for photo mode, 960x720, GDR
                    iframeId = R.raw.iframe_960x720_3s;
                } else {
                    //for record mode, 1280x720, GDR
                    iframeId = R.raw.iframe_1280x720_3s;
                }
                break;

            case Phantom_3_4K:
                switch (width) {
                    case 640:
                        //for P3-4K with resolution 640*480
                        iframeId = R.raw.iframe_640x480;
                        break;
                    case 848:
                        //for P3-4K with resolution 848*480
                        iframeId = R.raw.iframe_848x480;
                        break;
                    default:
                        iframeId = R.raw.iframe_1280x720_3s;
                        break;
                }
                break;

            case OSMO_PRO:
            case OSMO:
                iframeId = -1;
                break;

            case PHANTOM_4:
                iframeId = R.raw.iframe_1280x720_p4;
                break;
            case PHANTOM_4_PRO: // p4p
                switch (width) {
                    case 1280:
                        iframeId = R.raw.iframe_p4p_720_16x9;
                        break;
                    case 960:
                        iframeId = R.raw.iframe_p4p_720_4x3;
                        break;
                    case 1088:
                        iframeId = R.raw.iframe_p4p_720_3x2;
                        break;
                    case 1344:
                        iframeId = R.raw.iframe_p4p_1344x720;
                        break;
                    default:
                        iframeId = R.raw.iframe_p4p_720_16x9;
                        break;
                }
                break;
            case INSPIRE_2: //inspire2
                DataCameraGetPushStateInfo.CameraType cameraType = DataCameraGetPushStateInfo.getInstance().getCameraType();
                if (cameraType == DataCameraGetPushStateInfo.CameraType.DJICameraTypeGD600) {
                    iframeId = R.raw.iframe_1080x720_gd600;
                } else {
                    if (width == 640 && height == 368) {
                        DJILog.i(TAG, "Selected Iframe=iframe_640x368_wm620");
                        iframeId = R.raw.iframe_640x368_wm620;
                    }
                    if (width == 608 && height == 448) {
                        DJILog.i(TAG, "Selected Iframe=iframe_608x448_wm620");
                        iframeId = R.raw.iframe_608x448_wm620;
                    } else if (width == 720 && height == 480) {
                        DJILog.i(TAG, "Selected Iframe=iframe_720x480_wm620");
                        iframeId = R.raw.iframe_720x480_wm620;
                    } else if (width == 1280 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1280x720_wm620");
                        iframeId = R.raw.iframe_1280x720_wm620;
                    } else if (width == 1080 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1080x720_wm620");
                        iframeId = R.raw.iframe_1080x720_wm620;
                    } else if (width == 960 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_960x720_wm620");
                        iframeId = R.raw.iframe_960x720_wm620;
                    } else if (width == 1360 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1360x720_wm620");
                        iframeId = R.raw.iframe_1360x720_wm620;
                    } else if (width == 1344 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1344x720_wm620");
                        iframeId = R.raw.iframe_1344x720_wm620;
                    } else if (width == 1760 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1760x720_wm620");
                        iframeId = R.raw.iframe_1760x720_wm620;
                    } else if (width == 1920 && height == 800) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1920x800_wm620");
                        iframeId = R.raw.iframe_1920x800_wm620;
                    } else if (width == 1920 && height == 1024) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1920x1024_wm620");
                        iframeId = R.raw.iframe_1920x1024_wm620;
                    } else if (width == 1920 && height == 1088) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1920x1080_wm620");
                        iframeId = R.raw.iframe_1920x1088_wm620;
                    } else if (width == 1920 && height == 1440) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1920x1440_wm620");
                        iframeId = R.raw.iframe_1920x1440_wm620;
                    }
                }
                break;
            default: //for P3P, Inspire1, etc/
                iframeId = R.raw.iframe_1280x720_ins;
                break;
        }
        return iframeId;
    }

    /**
     * Get default black IDR frame.
     *
     * @param width Width of current video stream.
     * @return IDR frame data
     * @throws IOException
     */
    private byte[] getDefaultKeyFrame(int width) throws IOException {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product == null || product.getModel() == null) {
            return null;
        }
        int iframeId = getIframeRawId(product.getModel(), width);
        if (iframeId >= 0) {

            InputStream inputStream = context.getResources().openRawResource(iframeId);
            int length = inputStream.available();
            logd("iframeId length=" + length);
            byte[] buffer = new byte[length];
            inputStream.read(buffer);
            inputStream.close();

            return buffer;
        }
        return null;
    }


    private void startDataHandler() {
        if (dataHandlerThread != null && dataHandlerThread.isAlive()) {
            return;
        }
        dataHandlerThread = new HandlerThread("frame data handler thread");
        dataHandlerThread.start();
        dataHandler = new Handler(dataHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_FRAME_QUEUE_IN:
                        try {
                            onFrameQueueIn(msg);
                        } catch (Exception e) {
                            loge("queue in frame error: " + e);
                            e.printStackTrace();
                        }

                        if (!hasMessages(MSG_DECODE_FRAME)) {
                            sendEmptyMessage(MSG_DECODE_FRAME);
                        }
                        break;
                    case MSG_DECODE_FRAME:
                        try {
                            decodeFrame();
                        } catch (Exception e) {
                            loge("handle frame error: " + e);
                            if (e instanceof MediaCodec.CodecException) {
                            }
                            e.printStackTrace();
                        } finally {
                            if (frameQueue.size() > 0) {
                                sendEmptyMessage(MSG_DECODE_FRAME);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    /**
     * Stop the data processing thread
     */
    private void stopDataHandler() {
        if (dataHandlerThread == null || !dataHandlerThread.isAlive()) {
            return;
        }
        if (dataHandler != null) {
            dataHandler.removeCallbacksAndMessages(null);
        }
        dataHandlerThread.quitSafely();

        try {
            dataHandlerThread.join(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        dataHandler = null;
    }


    /**
     * Queue in the frame.
     *
     * @param msg
     */
    private void onFrameQueueIn(Message msg) {
        DJIFrame inputFrame = (DJIFrame) msg.obj;
        if (inputFrame == null) {
            return;
        }
        if (!hasIFrameInQueue) { // check the I frame flag
            if (inputFrame.frameNum != 1 && !inputFrame.isKeyFrame) {
                loge("the timing for setting iframe has not yet come.");
                return;
            }
            byte[] defaultKeyFrame = null;
            try {
                defaultKeyFrame = getDefaultKeyFrame(inputFrame.width); // Get I frame data
            } catch (IOException e) {
                loge("get default key frame error: " + e.getMessage());
            }
            if (defaultKeyFrame != null) {
                DJIFrame iFrame = new DJIFrame(
                        defaultKeyFrame,
                        defaultKeyFrame.length,
                        inputFrame.pts,
                        System.currentTimeMillis(),
                        inputFrame.isKeyFrame,
                        0,
                        inputFrame.frameIndex - 1,
                        inputFrame.width,
                        inputFrame.height
                );
                frameQueue.clear();
                frameQueue.offer(iFrame); // Queue in the I frame.
                logd("add iframe success!!!!");
                hasIFrameInQueue = true;
            } else if (inputFrame.isKeyFrame) {
                logd("onFrameQueueIn no need add i frame!!!!");
                hasIFrameInQueue = true;
            } else {
                loge("input key frame failed");
            }
        }
        if (inputFrame.width != 0 && inputFrame.height != 0 &&
                inputFrame.width != this.width &&
                inputFrame.height != this.height) {
            this.width = inputFrame.width;
            this.height = inputFrame.height;
        }
        // Queue in the input frame.
        if (this.frameQueue.offer(inputFrame)) {
            logd("put a frame into the Extended-Queue with index=" + inputFrame.frameIndex);
        } else {
            // If the queue is full, drop a frame.
            DJIFrame dropFrame = frameQueue.poll();
            this.frameQueue.offer(inputFrame);
            loge("Drop a frame with index=" + dropFrame.frameIndex + " and append a frame with index=" + inputFrame.frameIndex);
        }
    }

    /**
     * Dequeue the frames from the queue and decode them using the hardware decoder.
     *
     * @throws Exception
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void decodeFrame() throws Exception {
        DJIFrame inputFrame = frameQueue.poll();
        if (inputFrame == null) {
            return;
        }

        //----------
        // Added: send H264 (including i-frames) to the Video Service for RTP packing and transmission
        if (frameDataListener != null)
            frameDataListener.onFrameDataReceived(inputFrame.videoBuffer, width, height);
        //-----------
    }

    /**
     * Stop the decoding process.
     */
    public void stop() {
        if (dataHandler != null) {
            dataHandler.removeCallbacksAndMessages(null);
        }

        if (frameQueue != null) {
            frameQueue.clear();
            hasIFrameInQueue = false;
        }
        stopDataHandler();
    }

    public void resume() {
        startDataHandler();
    }

    public void destroy() {
    }

    @Override
    public void onDataRecv(byte[] data, int size, int frameNum, boolean isKeyFrame, int width, int height) {
        if (dataHandler == null || dataHandlerThread == null || !dataHandlerThread.isAlive()) {
            return;
        }
        if (data.length != size) {
            loge("recv data size: " + size + ", data lenght: " + data.length);
        } else {
            logd("recv data size: " + size + ", frameNum: " + frameNum + ", isKeyframe: " + isKeyFrame + "," +
                    " width: " + width + ", height: " + height);
            currentTime = System.currentTimeMillis();
            frameIndex++;
            DJIFrame newFrame = new DJIFrame(data, size, currentTime, currentTime, isKeyFrame,
                    frameNum, frameIndex, width, height);


            dataHandler.obtainMessage(MSG_FRAME_QUEUE_IN, newFrame).sendToTarget();

        }
    }


    public interface IFrameDataListener {
        void onFrameDataReceived(byte[] frame, int width, int height);
    }

    /**
     * A data structure for containing the frames.
     */
    private static class DJIFrame {
        public byte[] videoBuffer;
        public int size;
        public long pts;
        public long incomingTimeMs;
        public boolean isKeyFrame;
        public int frameNum;
        public long frameIndex;
        public int width;
        public int height;

        public DJIFrame(byte[] videoBuffer, int size, long pts, long incomingTimeUs, boolean isKeyFrame,
                        int frameNum, long frameIndex, int width, int height) {
            this.videoBuffer = videoBuffer;
            this.size = size;
            this.pts = pts;
            this.incomingTimeMs = incomingTimeUs;
            this.isKeyFrame = isKeyFrame;
            this.frameNum = frameNum;
            this.frameIndex = frameIndex;
            this.width = width;
            this.height = height;
        }
    }
}