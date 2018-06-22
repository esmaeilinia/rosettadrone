package sq.rogue.rosettadrone.drone;

import dji.sdk.products.Aircraft;

public class Drone extends Aircraft {

    private boolean mSafetyEnabled;
    private boolean mArmed;


    private int mThrottle;

    private int mGCSCommandedMode;

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

    /**
     *
     */
    public Drone() {
        mSafetyEnabled = true;
        mArmed = false;

        mGCSCommandedMode = 0;
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

    /**
     *
     * @return
     */
    public boolean isSafetyEnabled() {
        return mSafetyEnabled;
    }

    /**
     *
     * @param safetyEnabled
     */
    public void setSafetyEnabled(boolean safetyEnabled) {
        this.mSafetyEnabled = safetyEnabled;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region arming
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public boolean isArmed() {
        return mArmed;
    }

    /**
     *
     * @param armed
     */
    public void setArmed(boolean armed) {
        this.mArmed = mArmed;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region commanded mode
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public int getCommandedMode() {
        return mGCSCommandedMode;
    }

    /**
     *
     * @param mode
     */
    public void setGCSCommandedMode(int mode) {
        mGCSCommandedMode = mode;
    }

    //---------------------------------------------------------------------------------------
    //endregion


    //region throttle
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public int getThrottle() {
        return mThrottle;
    }

    /**
     *
     * @param throttle
     */
    public void setThrottle(int throttle) {
        this.mThrottle = throttle;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region temp
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public float getTemp() {
        return mTemp;
    }

    /**
     *
     * @param temp
     */
    public void setTemp(float temp) {
        this.mTemp = temp;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region capacity
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public int getCapacity() {
        return mCapacity;
    }

    /**
     *
     * @param capacity
     */
    public void setCapacity(int capacity) {
        this.mCapacity = capacity;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region charge
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public int getChargeRemaining() {
        return mChargeRemaining;
    }

    /**
     *
     * @param chargeRemaining
     */
    public void setChargeRemaining(int chargeRemaining) {
        this.mChargeRemaining = chargeRemaining;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region current
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public int getCurrent() {
        return mCurrent;
    }

    /**
     *
     * @param current
     */
    public void setCurrent(int current) {
        this.mCurrent = current;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region voltage
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public int getVoltage() {
        return mVoltage;
    }

    /**
     *
     * @param voltage
     */
    public void setVoltage(int voltage) {
        this.mVoltage = voltage;
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region cell voltages
    //---------------------------------------------------------------------------------------

    /**
     *
     * @return
     */
    public int[] getCellVoltages() {
        return mCellVoltages;
    }

    /**
     *
     * @param cellVoltages
     */
    public void setCellVoltages(int[] cellVoltages) {
        this.mCellVoltages = cellVoltages;
    }

    //---------------------------------------------------------------------------------------
    //endregion
}
