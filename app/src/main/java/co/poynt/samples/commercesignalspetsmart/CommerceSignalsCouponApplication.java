package co.poynt.samples.commercesignalspetsmart;

import android.app.Application;
import android.content.Intent;

/**
 * Created by palavilli on 10/16/15.
 * Modified by dsnatochy on 10/25/15
 */
public class CommerceSignalsCouponApplication extends Application {
    public static CommerceSignalsCouponApplication instance;

    public static CommerceSignalsCouponApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // start the foreground service
        Intent foregroundServiceIntent = new Intent(this, OrderService.class);
        startService(foregroundServiceIntent);
    }


}
