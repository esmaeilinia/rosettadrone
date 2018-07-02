package sq.rogue.rosettadrone.drone;

//        Copyright (c) 2018, U.S. Federal Government (in countries where recognized)
//
//        Redistribution and use in source and binary forms, with or without
//        modification, are permitted provided that the following conditions are met:
//
//        * Redistributions of source code must retain the above copyright notice, this
//        list of conditions and the following disclaimer.
//
//        * Redistributions in binary form must reproduce the above copyright notice,
//        this list of conditions and the following disclaimer in the documentation
//        and/or other materials provided with the distribution.
//
//        * Neither the name of the copyright holder nor the names of its
//        contributors may be used to endorse or promote products derived from
//        this software without specific prior written permission.
//
//        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
//        AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
//        IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//        DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
//        FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
//        DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
//        SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
//        CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
//        OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//        OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


import dji.common.product.Model;
import dji.sdk.airlink.AirLink;
import dji.sdk.battery.Battery;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;

public class Drone {

    //Have to use composition rather than inheritance since we can't construct a drone from an aircraft
    private Aircraft mAircraft;

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
     * Create a drone from an existing DJI aircraft
     */
    public Drone(Aircraft aircraft) {
        mAircraft = aircraft;
        init();
    }

    /**
     * Create a drone from a new aircraft
     */
    public Drone() {
        mAircraft = new Aircraft();
        init();
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region init
    //---------------------------------------------------------------------------------------

    /**
     * Initializes/bootstraps drone wrapper for a DJI aircraft
     */
    private void init() {
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

    //region aircraft calls
    //---------------------------------------------------------------------------------------

    /**
     * Set the aircraft base for the drone
     * @param aircraft A non-null {@link Aircraft}
     */
    public void setAircraft(Aircraft aircraft) {
        mAircraft = aircraft;
    }

    /**
     * Gets the aircraft base for the drone
     * @return The {@link Aircraft} if it is set, otherwise null
     */
    public Aircraft getAircraft() {
        return mAircraft;
    }

    /**
     * Gets the RC for the drone from the {@link Aircraft}
     * @return The {@link RemoteController} if the aircraft is set, otherwise null.
     */
    public RemoteController getRemoteController() {
        if (mAircraft != null) {
            return mAircraft.getRemoteController();
        } else {
            return null;
        }
    }

    /**
     * Gets the Battery for the drone from the {@link Aircraft}
     * @return The {@link Battery} if the aircraft is set, otherwise null.
     */
    public Battery getBattery() {
        if (mAircraft != null) {
            return mAircraft.getBattery();
        } else {
            return null;
        }
    }

    /**
     * Gets the Model of the drone from the {@link Aircraft}
     * @return The {@link Model} if the aircraft is set, otherwise null.
     */
    public Model getModel() {
        if (mAircraft != null) {
            return mAircraft.getModel();
        } else {
            return null;
        }    }

    /**
     * Gets the AirLink for the drone from the {@link Aircraft}
     * @return The {@link AirLink} if the aircraft is set, otherwise null.
     */
    public AirLink getAirLink() {
        if (mAircraft != null) {
            return mAircraft.getAirLink();
        } else {
            return null;
        }
    }

    /**
     * Gets the FlightController for the drone from the {@link Aircraft}
     * @return The {@link FlightController} if the aircraft is set, otherwise null.
     */
    public FlightController getFlightController() {
        if (mAircraft != null) {
            return mAircraft.getFlightController();
        } else {
            return null;
        }
    }

    //---------------------------------------------------------------------------------------
    //endregion

    //region safety
    //---------------------------------------------------------------------------------------

    /**
     * @return
     */
    public boolean isSafetyEnabled() {
        return mSafetyEnabled;
    }

    /**
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
     * @return
     */
    public boolean isArmed() {
        return mArmed;
    }

    /**
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
     * @return
     */
    public int getCommandedMode() {
        return mGCSCommandedMode;
    }

    /**
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
     * @return
     */
    public int getThrottle() {
        return mThrottle;
    }

    /**
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
     * @return
     */
    public float getTemp() {
        return mTemp;
    }

    /**
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
     * @return
     */
    public int getCapacity() {
        return mCapacity;
    }

    /**
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
     * @return
     */
    public int getChargeRemaining() {
        return mChargeRemaining;
    }

    /**
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
     * @return
     */
    public int getCurrent() {
        return mCurrent;
    }

    /**
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
     * @return
     */
    public int getVoltage() {
        return mVoltage;
    }

    /**
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
     * @return
     */
    public int[] getCellVoltages() {
        return mCellVoltages;
    }

    /**
     * @param cellVoltages
     */
    public void setCellVoltages(int[] cellVoltages) {
        this.mCellVoltages = cellVoltages;
    }

    //---------------------------------------------------------------------------------------
    //endregion

}
