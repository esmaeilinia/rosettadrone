package sq.rogue.rosettadrone.drone;

import dji.sdk.products.Aircraft;

public class Drone extends Aircraft {

    private boolean mSafetyEnabled;

    private int mThrottle;

    private int[] mCellVoltages;

    public Drone() {
        mSafetyEnabled = true;
        mThrottle = 0;
        mCellVoltages = new int[10];
    }

    public boolean isSafetyEnabled() {
        return mSafetyEnabled;
    }

    public void setSafetyEnabled(boolean safetyEnabled) {
        this.mSafetyEnabled = safetyEnabled;
    }

    public int getThrottle() {
        return mThrottle;
    }

    public void setThrottle(int throttle) {
        this.mThrottle = throttle;
    }

    public int[] getCellVoltages() {
        return mCellVoltages;
    }

    public void setCellVoltages(int[] cellVoltages) {
        this.mCellVoltages = cellVoltages;
    }
}
