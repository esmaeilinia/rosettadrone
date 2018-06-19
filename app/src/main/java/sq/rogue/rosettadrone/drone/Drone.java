package sq.rogue.rosettadrone.drone;

import dji.sdk.products.Aircraft;

public class Drone extends Aircraft {

    private boolean mSafetyEnabled = true;


    public boolean ismSafetyEnabled() {
        return mSafetyEnabled;
    }

    public void setmSafetyEnabled(boolean mSafetyEnabled) {
        this.mSafetyEnabled = mSafetyEnabled;
    }
}
