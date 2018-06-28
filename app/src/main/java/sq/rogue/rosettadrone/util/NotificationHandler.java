package sq.rogue.rosettadrone.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;

import sq.rogue.rosettadrone.R;

import static sq.rogue.rosettadrone.util.util.TYPE_DRONE_ID;
import static sq.rogue.rosettadrone.util.util.TYPE_GCS_IP;
import static sq.rogue.rosettadrone.util.util.TYPE_GCS_PORT;
import static sq.rogue.rosettadrone.util.util.TYPE_VIDEO_IP;
import static sq.rogue.rosettadrone.util.util.TYPE_VIDEO_PORT;

public class NotificationHandler {

    public final static int NOTIFICATION_ID = 412;


    public static void notifySnackbar(View view, int resID, int duration) {
        Snackbar snackbar = Snackbar.make(view, resID, duration);
        snackbar.show();
    }

    public static void notifyAlert(Context context, int input, DialogInterface.OnClickListener clickListener,
                                   DialogInterface.OnCancelListener cancelListener) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context, R.style.AppDialog);
        builder.setPositiveButton(android.R.string.ok, clickListener);
        builder.setOnCancelListener(cancelListener);
        switch (input) {
            case TYPE_GCS_IP:
                builder.setMessage("Invalid IP address entered for GCS IP.");
                break;
            case TYPE_GCS_PORT:
                builder.setMessage("Invalid port entered for GCS Port.");
                break;
            case TYPE_VIDEO_IP:
                builder.setMessage("Invalid IP address entered for Video IP.");
                break;
            case TYPE_VIDEO_PORT:
                builder.setMessage("Invalid port entered for Video Port.");
                break;
            case TYPE_DRONE_ID:
                builder.setMessage("Invalid ID entered for Drone ID (1-254).");
                break;
        }
        builder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String createNotificationChannel(Context context) {
        String channelID = "rosetta_service";
        String channelName = "RosettaDrone";
        NotificationChannel chan = new NotificationChannel(channelID,
                channelName, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelID;
    }

}
