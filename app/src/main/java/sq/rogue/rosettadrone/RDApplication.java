package sq.rogue.rosettadrone;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;
//import com.squareup.leakcanary.LeakCanary;

public class RDApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(RDApplication.this);
    }
}
