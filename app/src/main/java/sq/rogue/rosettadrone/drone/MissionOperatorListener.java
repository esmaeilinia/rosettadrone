package sq.rogue.rosettadrone.drone;


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