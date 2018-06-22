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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;

class MissionOperatorListener implements WaypointMissionOperatorListener {
    private String TAG = this.getClass().getSimpleName();
    private int WAYPOINT_COUNT = 0;

    @Override
    public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {
        // Example of Download Listener
        if (waypointMissionDownloadEvent.getProgress() != null
                && waypointMissionDownloadEvent.getProgress().isSummaryDownloaded
                && waypointMissionDownloadEvent.getProgress().downloadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
        }
        updateWaypointMissionState();
    }

    @Override
    public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
        // Example of Upload Listener
        //activity.logMessageDJI("Uploaded waypoint " +  waypointMissionUploadEvent.getProgress().uploadedWaypointIndex);
        if (waypointMissionUploadEvent.getProgress() != null
                && waypointMissionUploadEvent.getProgress().isSummaryUploaded
                && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
        }
        updateWaypointMissionState();
    }

    @Override
    public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
        // Example of Execution Listener
        Log.d(TAG,
                (waypointMissionExecutionEvent.getPreviousState() == null
                        ? ""
                        : waypointMissionExecutionEvent.getPreviousState().getName())
                        + ", "
                        + waypointMissionExecutionEvent.getCurrentState().getName()
                        + (waypointMissionExecutionEvent.getProgress() == null
                        ? ""
                        : waypointMissionExecutionEvent.getProgress().targetWaypointIndex));
        updateWaypointMissionState();
    }

    @Override
    public void onExecutionStart() {
        updateWaypointMissionState();
    }

    @Override
    public void onExecutionFinish(@Nullable DJIError djiError) {
        updateWaypointMissionState();
    }

    private void updateWaypointMissionState() {

    }
};