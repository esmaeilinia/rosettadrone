package sq.rogue.rosettadrone;

// Acknowledgements:
// IP address validation: https://stackoverflow.com/questions/3698034/validating-ip-in-android/11545229#11545229
// Hide keyboard: https://stackoverflow.com/questions/16495440/how-to-hide-keyboard-by-default-and-show-only-when-click-on-edittext
// MenuItemTetColor: RPP @ https://stackoverflow.com/questions/31713628/change-menuitem-text-color-programmatically

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.MAVLink.Parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;

import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import sq.rogue.rosettadrone.logs.LogFragment;
import sq.rogue.rosettadrone.settings.SettingsActivity;
import sq.rogue.rosettadrone.telemetry.MAVLinkReceiver;
import sq.rogue.rosettadrone.util.util;
import sq.rogue.rosettadrone.video.VideoService;

import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static sq.rogue.rosettadrone.video.VideoService.ACTION_DRONE_CONNECTED;
import static sq.rogue.rosettadrone.video.VideoService.ACTION_DRONE_DISCONNECTED;
import static sq.rogue.rosettadrone.video.VideoService.ACTION_RESTART;
import static sq.rogue.rosettadrone.video.VideoService.ACTION_SET_MODEL;
import static sq.rogue.rosettadrone.video.VideoService.ACTION_START;
import static sq.rogue.rosettadrone.video.VideoService.ACTION_STOP;

public class MainActivity extends AppCompatActivity {

    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private final static int RESULT_SETTINGS = 1001;
    public static boolean FLAG_PREFS_CHANGED = false;

    private static BaseProduct mProduct;
    private final String TAG = "RosettaDrone";
    private final int GCS_TIMEOUT_mSEC = 2000;
    private Handler mDJIHandler;
    private Handler mUIHandler;
    private CheckBox mSafety;

    private FragmentManager fragmentManager;
    private LogFragment logDJI;
    private LogFragment logOutbound;
    private LogFragment logInbound;

    private BottomNavigationView mBottomNavigation;
    private int navState = -1;
    private FloatingActionButton mFab;

    private SharedPreferences prefs;

    private String mNewOutbound = "";
    private String mNewInbound = "";
    private String mNewDJI = "";

    private DatagramSocket socket;

    private DroneModel mModel;
    private MAVLinkReceiver mMavlinkReceiver;
    private Parser mMavlinkParser;
    private GCSCommunicatorAsyncTask mGCSCommunicator;

    private Runnable RunnableUpdateUI = new Runnable() {
        // We have to update UI from here because we can't update the UI from the
        // drone/GCS handling threads

        @Override
        public void run() {

            try {
                if (!mNewDJI.equals("")) {
                    logDJI.appendLogText(mNewDJI);
                    mNewDJI = "";
                }
                if (!mNewOutbound.equals("")) {
                    logOutbound.appendLogText(mNewOutbound);

                    mNewOutbound = "";
                }
                if (!mNewInbound.equals("")) {
                    logInbound.appendLogText(mNewInbound);
                    mNewInbound = "";
                }

            } catch (Exception e) {
                Log.d(TAG, "exception", e);
            }
            mUIHandler.postDelayed(this, 100);
        }
    };
    private Runnable djiUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

//        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String versionName = "";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        getSupportActionBar().setTitle("Rosetta Drone v" + versionName);


        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        requestPermissions();

        deleteApplicationDirectory();

        if (savedInstanceState != null) {
            navState = savedInstanceState.getInt("navigation_state");
        }
        initLogs();
        initBottomNav();
        initFab();


        mModel = new DroneModel(this, null);
        mMavlinkReceiver = new MAVLinkReceiver(this, mModel);
        loadMockParamFile();

        mDJIHandler = new Handler(Looper.getMainLooper());
        mUIHandler = new Handler(Looper.getMainLooper());
        mUIHandler.postDelayed(RunnableUpdateUI, 1000);

        Intent aoaIntent = getIntent();
        if (aoaIntent != null) {
            String action = aoaIntent.getAction();
            if (action == (UsbManager.ACTION_USB_ACCESSORY_ATTACHED)) {
                Intent attachedIntent = new Intent();

                attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
                sendBroadcast(attachedIntent);
            }
        }
    }

    /**
     *
     */
    private void initLogs() {
        fragmentManager = getSupportFragmentManager();

        //Adapters in order: DJI, Outbound to GCS, Inbound to GCS
//        logPagerAdapter = new LogPagerAdapter(fragmentManager,
//                new LogFragment(), new LogFragment(), new LogFragment());

        logDJI = new LogFragment();
        logOutbound = new LogFragment();
        logInbound = new LogFragment();


        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.add(R.id.fragment_container, logDJI);
        fragmentTransaction.add(R.id.fragment_container, logOutbound);
        fragmentTransaction.add(R.id.fragment_container, logInbound);

//        Log.d(TAG, "initLOGS navState : " + navState);
        switch (navState) {
            case R.id.action_gcs_up:
                fragmentTransaction.hide(logDJI);
                fragmentTransaction.hide(logInbound);
                break;
            case R.id.action_gcs_down:
                fragmentTransaction.hide(logDJI);
                fragmentTransaction.hide(logOutbound);
                break;
            default:
                fragmentTransaction.hide(logOutbound);
                fragmentTransaction.hide(logInbound);
                break;
        }
        fragmentTransaction.commit();


    }

    /**
     *
     */
    private void initBottomNav() {
        mBottomNavigation = findViewById(R.id.navigationView);

//        /**
//         * Added two lines
//         */
//        bottomNavigationView.setItemIconTintList(null);
//        bottomNavigationView.setItemBackgroundResource(R.drawable.menubackground);


        mBottomNavigation.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        switch (item.getItemId()) {
                            case R.id.action_dji:
                                fragmentTransaction.show(logDJI);
                                fragmentTransaction.hide(logOutbound);
                                fragmentTransaction.hide(logInbound);
                                break;
                            case R.id.action_gcs_up:
                                fragmentTransaction.hide(logDJI);
                                fragmentTransaction.show(logOutbound);
                                fragmentTransaction.hide(logInbound);
                                break;
                            case R.id.action_gcs_down:
                                fragmentTransaction.hide(logDJI);
                                fragmentTransaction.hide(logOutbound);
                                fragmentTransaction.show(logInbound);
                                break;
                        }
                        fragmentTransaction.commit();
                        return true;
                    }
                }
        );

        ViewGroup navigationMenuView = (ViewGroup) mBottomNavigation.getChildAt(0);
        ViewGroup gcsUpMenuItem = (ViewGroup) navigationMenuView.getChildAt(1);
        ViewGroup gcsDownMenuItem = (ViewGroup) navigationMenuView.getChildAt(2);


        gcsUpMenuItem.setLongClickable(true);
        gcsUpMenuItem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClickGCSUp();
                return true;
            }
        });

        gcsDownMenuItem.setLongClickable(true);
        gcsDownMenuItem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClickGCSDown();
                return true;
            }
        });


        if (navState != -1) {
            mBottomNavigation.setSelectedItemId(navState);
        }
    }

    private void initFab() {
        mFab = findViewById(R.id.fab);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName;
                int dataID;
                switch (mBottomNavigation.getSelectedItemId()) {
                    case R.id.action_gcs_up:
                        fileName = android.text.format.DateFormat.format("yyyy-MM-dd-hh:mm:ss", new java.util.Date())
                        + "_gcs_up.txt";
                        dataID = 1;
                        break;
                    case R.id.action_gcs_down:
                        fileName = android.text.format.DateFormat.format("yyyy-MM-dd-hh:mm:ss", new java.util.Date())
                                + "_gcs_down.txt";
                        dataID = 2;
                        break;
                    default:
                        fileName = android.text.format.DateFormat.format("yyyy-MM-dd-hh:mm:ss", new java.util.Date())
                                + "_dji.txt";
                        dataID = 0;
                        break;
                }

                File directory = new File(Environment.getExternalStorageDirectory().getPath()
                        + File.separator + "rosettadrone");
                File dataFile = new File(directory, fileName);

                if (!directory.exists()) {
                    directory.mkdir();
                }

                BufferedWriter bufferedWriter = null;
                try {
                    bufferedWriter = new BufferedWriter(new FileWriter(dataFile, false));
                    switch (dataID) {
                        case 0:
                            bufferedWriter.write(logDJI.getLogText());
                            Log.d(TAG, "LOG DJI");
                            break;
                        case 1:
                            bufferedWriter.write(logOutbound.getLogText());
                            Log.d(TAG, "LOG UP");

                            break;
                        case 2:
                            bufferedWriter.write(logInbound.getLogText());
                            Log.d(TAG, "LOG DOWN");

                            break;
                        default:
                            break;
                    }
                    Log.d(TAG, logDJI.getLogText());
                    Log.d(TAG, directory.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bufferedWriter != null) {
                            bufferedWriter.flush();
                            bufferedWriter.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        navState = mBottomNavigation.getSelectedItemId();
        outState.putInt("navigation_state", navState);
//        Log.d(TAG, "SAVED NAVSTATE: " + navState);

    }

    private void deleteApplicationDirectory() {
//        Log.d("RosettaDrone", "deleteApplicationDirectory()");
        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            String s = p.applicationInfo.dataDir;
            Log.d(TAG, s);
            File dir = new File(s);
            if (dir.isDirectory()) {
//                Log.d("RosettaDrone", "yes, is directory");
                String[] children = dir.list();
                for (String aChildren : children) {
                    new File(dir, aChildren).delete();
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "exception", e);
        }
        //File dir = new File(Environment.getExternalStorageDirectory()+"DJI/sq.rogue.rosettadrone");

    }

    private void requestPermissions() {
        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        DJISDKManager.getInstance().registerApp(this, mDJISDKManagerCallback);
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
//        Log.d(TAG, "onResume()");
        super.onResume();

    }

    @Override
    protected void onPause() {
//        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
//        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
//        Log.i(TAG, "onDestroy");
//        logMessageDJI("onDestroy()");
        closeGCSCommunicator();

        mUIHandler.removeCallbacksAndMessages(null);
        mDJIHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
//        Log.d(TAG, "onActivityResult");
        super.onActivityResult(reqCode, resCode, data);
        if (reqCode == RESULT_SETTINGS && mGCSCommunicator != null && FLAG_PREFS_CHANGED) {
            mGCSCommunicator.renewDatalinks();
            sendRestartVideoService();
            FLAG_PREFS_CHANGED = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);

        MenuItem safetyItem = menu.findItem(R.id.action_safety);
        mSafety = (CheckBox) safetyItem.getActionView();
        mSafety.setButtonDrawable(R.color.safety);
        mSafety.setPadding(mSafety.getPaddingLeft(),
                mSafety.getPaddingTop(),
                mSafety.getPaddingLeft() + (int)(10.0f * getResources().getDisplayMetrics().density + 0.5f),
                mSafety.getPaddingBottom());

        //Make sure default is safety enabled
        mModel.setSafetyEnabled(true);
        mSafety.setChecked(true);

        mSafety.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mModel.setSafetyEnabled(isChecked);
                util.NotificationHandler.notifySnackbar(findViewById(R.id.snack),
                        (mModel.isSafetyEnabled()) ? R.string.safety_on : R.string.safety_off, LENGTH_LONG);
            }
        });

        return true;
    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        boolean result = super.onPrepareOptionsMenu(menu);
//
//        Menu bottomNavMenu = bottomNavigationView.getMenu();
//        MenuItem dji = bottomNavMenu.findItem(R.id.action_dji);
//        MenuItem gcsDown = bottomNavMenu.findItem(R.id.action_gcs_down);
//        MenuItem gcsUp = bottomNavMenu.findItem(R.id.action_gcs_up);
//
//        if (dji != null) {
//            if (mProduct instanceof Aircraft) {
//                dji.setIcon(R.drawable.ic_drone_green_24dp);
//            }
//            else {
//                dji.setIcon(R.drawable.ic_drone_red_24dp);
//            }
//        }
//
//        if (gcsDown != null) {
//            if (System.currentTimeMillis() - mMavlinkReceiver.getTimestampLastGCSHeartbeat() <= GCS_TIMEOUT_mSEC) {
//                gcsUp.setIcon(R.drawable.ic_up_arrow_green_24dp);
//                gcsDown.setIcon(R.drawable.ic_down_arrow_green_24dp);
//            }
//            else {
//                gcsUp.setIcon(R.drawable.ic_up_arrow_red_24dp);
//                gcsDown.setIcon(R.drawable.ic_down_arrow_red_24dp);
//            }
//        }
//
//        return result;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        Log.d(TAG, "menu item selected");
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_safety_switch:
//                Log.d(TAG, "ACTION_SAFETY_SWITCH");
                return true;
            case R.id.action_safety:
//                Log.d(TAG, "ACTION_SAFETY");
//                NotificationHandler.notifySnackbar(bottomNavigationView, R.string.safety, LENGTH_LONG);
                return true;
            case R.id.action_settings:
                onClickSettings();
            default:
//                Log.d(TAG, String.valueOf(item.getItemId()));
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("ResourceAsColor")
    private void onLongClickGCSUp() {
//        Log.d(TAG, "onLongClickGCSUp()");
        mModel.startWaypointMission();
    }

    private void onLongClickGCSDown() {
//        Log.d(TAG, "onLongClickGCSDown()");
        mModel.echoLoadedMission();
    }

    private void onClickSettings() {
//        Log.d(TAG, "onClickSettings()");
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivityForResult(intent, RESULT_SETTINGS);
    }



    private void closeGCSCommunicator() {
        if (mGCSCommunicator != null) {
            mGCSCommunicator.cancel(true);
            mGCSCommunicator = null;
        }
    }

    private void notifyStatusChange() {
        mDJIHandler.removeCallbacks(djiUpdateRunnable);
//        Log.d(TAG, "notifyStatusChange()");
        mDJIHandler.postDelayed(djiUpdateRunnable, 500);
    }

    private void loadMockParamFile() {
        mModel.getParams().clear();
        try {

            AssetManager am = getAssets();
            InputStream is = am.open("DJIMock.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(inputStreamReader);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;
                String[] paramData = line.split("\t");
                String paramName = paramData[2];
                Float paramValue = Float.valueOf(paramData[3]);
                short paramType = Short.valueOf(paramData[4]);

                mModel.getParams().add(new MAVParameter(paramName, paramValue, paramType));
            }
        } catch (IOException e) {
            Log.d(TAG, "exception", e);
        }
    }

    public void logMessageToGCS(String msg) {
        if (prefs.getBoolean("pref_log_mavlink", false))
            mNewOutbound += "\n" + msg;
    }

    public void logMessageFromGCS(String msg) {
        if (prefs.getBoolean("pref_log_mavlink", false))
            mNewInbound += "\n" + msg;
    }

    public void logMessageDJI(String msg) {
        mNewDJI += "\n" + msg;
    }

    //region Interface to VideoService
    //---------------------------------------------------------------------------------------

    /**
     *
     */
    private void sendStartVideoService() {
        Intent intent = setupIntent(ACTION_START);
        sendIntent(intent);
    }

    /**
     *
     */
    private void sendStopVideoService() {
        Intent intent = setupIntent(ACTION_STOP);
        sendIntent(intent);
    }

    /**
     *
     */
    private void sendRestartVideoService() {
        String videoIP = getVideoIP();

        int videoPort = Integer.parseInt(prefs.getString("pref_video_port", "5600"));

        logMessageDJI("Restarting Video link to " + videoIP + ":" + videoPort);
        Intent intent = setupIntent(ACTION_RESTART);
        sendIntent(intent);
    }

    /**
     *
     */
    private void sendSetBackingMode() {
        Intent intent = setupIntent(ACTION_SET_MODEL);
        sendIntent(intent);
    }

    /**
     *
     */
    private void sendDroneConnected() {
        String videoIP = getVideoIP();

        int videoPort = Integer.parseInt(prefs.getString("pref_video_port", "5600"));

        logMessageDJI("Starting Video link to " + videoIP + ":" + videoPort);

        Intent intent = setupIntent(ACTION_DRONE_CONNECTED);
        intent.putExtra("model", mProduct.getModel());
        sendIntent(intent);
    }

    /**
     *
     */
    private void sendDroneDisconnected() {
        Intent intent = setupIntent(ACTION_DRONE_DISCONNECTED);
        sendIntent(intent);
    }

    private String getVideoIP() {
        String videoIP = "127.0.0.1";

        if (prefs.getBoolean("pref_external_gcs", false)) {
            if (!prefs.getBoolean("pref_combined_gcs", false)) {
                videoIP = prefs.getString("pref_gcs_ip", "127.0.0.1");
            } else {
                videoIP = prefs.getString("pref_video_ip", "127.0.0.1");
            }
        }

        return videoIP;
    }

    /**
     * @param action
     * @param extras
     * @return
     */
    private Intent setupIntent(String action, Object... extras) {
        Intent intent = new Intent(this, VideoService.class);
        intent.setAction(action);

        return intent;
    }

    /**
     * @param intent
     */
    private void sendIntent(Intent intent) {
        if (intent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    startService(intent);
                } catch (IllegalStateException e) {
                    startForegroundService(intent);
                }
            } else {
                startService(intent);
            }
        }
    }

    //---------------------------------------------------------------------------------------
    //endregion

}
