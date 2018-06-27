package sq.rogue.rosettadrone.managers;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import sq.rogue.rosettadrone.drone.Drone;

public class DJIManager {
    private static DJIManager instance;
    private static BaseProduct mProduct;
    private final String TAG = this.getClass().getSimpleName();
    private List<IDJIListener> mDJIListeners = new ArrayList<>();

    //region DJI Listeners/Callbacks
    //---------------------------------------------------------------------------------------

    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if (newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            Log.d(TAG,
                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                            key,
                            oldComponent,
                            newComponent));

            notifyStatusChange();
        }

        @Override
        public void onConnectivityChange(boolean isConnected) {
            if (isConnected)
                notifyDroneConnected();
            else
                notifyDroneDisconnected();

            notifyStatusChange();
        }
    };

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError error) {
//            Log.d(TAG, error == null ? "success" : error.getDescription());
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();

                notifyRegistration(true);
            } else {
                notifyRegistration(false);
            }
            if (error != null) {
                Log.e(TAG, error.toString());
            }
        }

        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
//            Log.d(TAG, "onProductChange()");
//            logMessageDJI("onProductChange()");


            if (mProduct == null) {
//                logMessageDJI("No DJI drone detected");
                notifyDroneDisconnected();
            } else {
                mProduct = newProduct;
                mProduct.setBaseProductListener(mDJIBaseProductListener);

                if (mProduct instanceof Aircraft) {
//                    logMessageDJI("DJI aircraft detected");
                    notifyDroneConnected();
                } else {
//                    logMessageDJI("DJI non-aircraft product detected");
                    notifyDroneDisconnected();
                }
            }

            notifyStatusChange();
        }
    };

    //---------------------------------------------------------------------------------------
    //endregion

    //region getInstance
    //---------------------------------------------------------------------------------------

    public static DJIManager getInstance() {
        if (instance == null) {
            instance = new DJIManager();
        }
        return instance;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region notify
    //---------------------------------------------------------------------------------------

    /**
     * @param result
     */
    private void notifyRegistration(boolean result) {
        for (IDJIListener listener : mDJIListeners) {
            listener.onRegistration(result);
        }
    }

    /**
     *
     */
    private void notifyStatusChange() {
        for (IDJIListener listener : mDJIListeners) {
            listener.onStatusChange();
        }
//        Handler handler = new Handler(Looper.getMainLooper());
//        handler.post(this::refreshModel);
    }

//    private void refreshModel() {
//        if (mProduct != null && mProduct.isConnected()) {
//        }
//    }

    /**
     *
     */
    private void notifyDroneConnected() {
        Drone drone = new Drone((Aircraft) mProduct);

        for (IDJIListener listener : mDJIListeners) {
            listener.onDroneConnected(drone);
        }
//        // Multiple tries and a timeout are necessary because of a bug that causes all the
//        // components of mProduct to be null sometimes.
//        int tries = 0;
//        while (!mModel.setDjiAircraft((Aircraft) mProduct)) {
//            safeSleep(1000);
//            logMessageDJI("Connecting to drone...");
//            tries++;
//            if (tries == 5) {
//                Toast.makeText(this, "Oops, DJI's SDK just glitched. Please restart the app.",
//                        Toast.LENGTH_LONG).show();
//                return;
//            }
//        }
//        while (!mModel.loadParamsFromDJI()) {
//
//        }
//
//        sendDroneConnected();

    }

    /**
     *
     */
    private void notifyDroneDisconnected() {
        for (IDJIListener listener : mDJIListeners) {
            listener.onDroneDisconnected();
        }
//        logMessageDJI("Drone disconnected");
//        mModel.setDjiAircraft(null);
//        closeGCSCommunicator();
//
//        sendDroneDisconnected();
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region listener interface
    //---------------------------------------------------------------------------------------

    /**
     * @param listener
     */
    public void addDJIListener(IDJIListener listener) {
        if (!mDJIListeners.contains(listener)) {
            mDJIListeners.add(listener);
        }
    }

    /**
     *
     */
    public void removeDJIListener(IDJIListener listener) {
        mDJIListeners.remove(listener);
    }

    /**
     *
     */
    public interface IDJIListener {

        /**
         *
         */
        void onDroneConnected(Drone drone);

        /**
         *
         */
        void onDroneDisconnected();

        /**
         *
         */
        void onStatusChange();

        /**
         * @param result
         */
        void onRegistration(boolean result);
    }

    //---------------------------------------------------------------------------------------
    //endregion
}
