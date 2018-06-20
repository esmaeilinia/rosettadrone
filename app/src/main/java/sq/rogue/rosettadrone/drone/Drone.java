package sq.rogue.rosettadrone.drone;

import dji.sdk.products.Aircraft;

public class Drone extends Aircraft {

    private boolean mSafetyEnabled;
    private boolean mArmed;


    private int mThrottle;

    //Voltage in mV
    private int mVoltage;

    //Current in mA
    private int mCurrent;

    //Charge in mAh
    private int mChargeRemaining;

    //Charge capacity in mAh
    private int mCapacity;

    //Temp in celsius
    private float mTemp;

    private int[] mCellVoltages;

    //region constructors
    //---------------------------------------------------------------------------------------

    public Drone() {
        mSafetyEnabled = true;
        mArmed = false;

        mThrottle = 0;

        mTemp = 0;
        mCapacity = 0;
        mChargeRemaining = 0;
        mCurrent = 0;
        mVoltage = 0;

        mCellVoltages = new int[10];
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region safety
    //---------------------------------------------------------------------------------------

    public boolean isSafetyEnabled() {
        return mSafetyEnabled;
    }

    public void setSafetyEnabled(boolean safetyEnabled) {
        this.mSafetyEnabled = safetyEnabled;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region arming
    //---------------------------------------------------------------------------------------

    public boolean isArmed() {
        return mArmed;
    }

    public void setArmed(boolean armed) {
        this.mArmed = armed;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region throttle
    //---------------------------------------------------------------------------------------

    public int getThrottle() {
        return mThrottle;
    }

    public void setThrottle(int throttle) {
        this.mThrottle = throttle;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region temp
    //---------------------------------------------------------------------------------------

    public float getTemp() {
        return mTemp;
    }

    public void setTemp(float temp) {
        this.mTemp = temp;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region capacity
    //---------------------------------------------------------------------------------------

    public int getCapacity() {
        return mCapacity;
    }

    public void setCapacity(int capacity) {
        this.mCapacity = capacity;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region charge
    //---------------------------------------------------------------------------------------

    public int getChargeRemaining() {
        return mChargeRemaining;
    }

    public void setChargeRemaining(int chargeRemaining) {
        this.mChargeRemaining = chargeRemaining;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region current
    //---------------------------------------------------------------------------------------

    public int getCurrent() {
        return mCurrent;
    }

    public void setCurrent(int current) {
        this.mCurrent = current;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region voltage
    //---------------------------------------------------------------------------------------

    public int getVoltage() {
        return mVoltage;
    }

    public void setVoltage(int voltage) {
        this.mVoltage = voltage;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region cell voltages
    //---------------------------------------------------------------------------------------

    public int[] getCellVoltages() {
        return mCellVoltages;
    }

    public void setCellVoltages(int[] cellVoltages) {
        this.mCellVoltages = cellVoltages;
    }

    //---------------------------------------------------------------------------------------
    //endregion
}
