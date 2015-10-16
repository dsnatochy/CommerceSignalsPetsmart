package co.poynt.samples.commercesignalspetsmart;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import co.poynt.os.contentproviders.orders.orders.OrdersColumns;
import co.poynt.os.model.Intents;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class PaymentSuccessReceiver extends BroadcastReceiver {

    private static final String TAG = "PaymentSuccessReceiver";


    public PaymentSuccessReceiver() {

        Log.d(TAG, "from constructor");

    }

    private boolean isOrderServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (OrderService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {



        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //throw new UnsupportedOperationException("Not yet implemented");
        Log.d(TAG, "Intent: " + intent);
        //Bundle extras = intent.getExtras();
        Bundle bundle = intent.getExtras();
        // -1 is returned if customerid is not there
//        long customerId = intent.getLongExtra(Intents.INTENT_EXTRAS_CUSTOMERID,-1);
//         Log.d(TAG, "customer id: " + customerId);

        // if OrderService is not running - start it.

        context.startService(new Intent(context, OrderService.class));




    }




}
