package sq.rogue.rosettadrone.managers;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import sq.rogue.rosettadrone.DroneModel;

import static sq.rogue.rosettadrone.util.safeSleep;

public class DJIManager {
    private final String TAG = this.getClass().getSimpleName();

    private IDJIListener mDJIListener;

    private static BaseProduct mProduct;

    public static BaseProduct getProductInstance() {
        if (mProduct == null) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static void updateProduct(BaseProduct product) {
        mProduct = product;
    }

    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {

        }
    };
    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if (newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            mDJIListener.onStatusChange();
        }

        @Override
        public void onConnectivityChange(boolean isConnected) {
            if (isConnected)
                onDroneConnected();
            else
                onDroneDisconnected();

            mDJIListener.onStatusChange();
        }
    };

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError error) {
//            Log.d(TAG, error == null ? "success" : error.getDescription());
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                mDJIListener.onRegistration(true);
            } else {
                mDJIListener.onRegistration(false);
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
                logMessageDJI("No DJI drone detected");
                onDroneDisconnected();
            } else {
                mProduct = newProduct;
                mProduct.setBaseProductListener(mDJIBaseProductListener);

                if (mProduct instanceof Aircraft) {
                    logMessageDJI("DJI aircraft detected");
                    onDroneConnected();
                } else {
                    logMessageDJI("DJI non-aircraft product detected");
                    onDroneDisconnected();
                }
            }

            mDJIListener.onStatusChange();
        }
    };

    private void notifyStatusChange() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                refreshModel();
            }
        });
    }

    private void refreshModel() {
        if (mProduct != null && mProduct.isConnected()) {

        }
    }

    private void onDroneConnected() {

        // Multiple tries and a timeout are necessary because of a bug that causes all the
        // components of mProduct to be null sometimes.
        int tries = 0;
        while (!mModel.setDjiAircraft((Aircraft) mProduct)) {
            safeSleep(1000);
            logMessageDJI("Connecting to drone...");
            tries++;
            if (tries == 5) {
                Toast.makeText(this, "Oops, DJI's SDK just glitched. Please restart the app.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        while (!mModel.loadParamsFromDJI()) {

        }

        sendDroneConnected();

    }

    private void onDroneDisconnected() {
        logMessageDJI("Drone disconnected");
        mModel.setDjiAircraft(null);
        closeGCSCommunicator();

        sendDroneDisconnected();
    }

    /**
     *
     * @param listener
     */
    public void setDJIListener(IDJIListener listener) {
        mDJIListener = listener;
    }

    /**
     *
     */
    public void removeDJIListener() {
        mDJIListener = null;
    }

    /**
     *
     */
    public interface IDJIListener {

        /**
         *
         */
        void onStatusChange();

        void onRegistration(boolean result);
    }
}
