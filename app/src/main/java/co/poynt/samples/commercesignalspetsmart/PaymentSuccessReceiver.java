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

import java.util.UUID;

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

        Log.d(TAG, "Intent: " + intent);
        Bundle extras = intent.getExtras();
        UUID objUUID = (UUID)extras.get(Intents.INTENT_EXTRAS_ORDER_ID);

        String orderId = objUUID.toString();

        // -1 is returned if customerid is not there
//        long customerId = intent.getLongExtra(Intents.INTENT_EXTRAS_CUSTOMERID,-1);
//         Log.d(TAG, "customer id: " + customerId);

        // if OrderService is not running - start it.
        Intent orderServiceIntent = new Intent(context, OrderService.class);
        orderServiceIntent.putExtra(Intents.INTENT_EXTRAS_ORDER_ID, orderId);

        context.startService(orderServiceIntent);




    }




}
