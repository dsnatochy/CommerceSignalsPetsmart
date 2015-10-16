package co.poynt.samples.commercesignalspetsmart;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import co.poynt.api.model.Order;
import co.poynt.os.contentproviders.orders.orders.OrdersColumns;
import co.poynt.os.contentproviders.orders.orders.OrdersCursor;
import co.poynt.os.model.PoyntError;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.services.v1.IPoyntOrderService;
import co.poynt.os.services.v1.IPoyntOrderServiceListener;
import co.poynt.os.services.v1.IPoyntReceiptPrintingService;
import co.poynt.os.services.v1.IPoyntReceiptPrintingServiceListener;

public class OrderService extends Service {
    private final String TAG = OrderService.class.getSimpleName();
    private MyObserver observer;
    private ContentResolver resolver;
    private Handler handler;

    private IPoyntOrderService mOrderService;

    private ServiceConnection mOrderConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "IPoyntOrderService is now connected");
            mOrderService = IPoyntOrderService.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "IPoyntOrderService has unexpectedly disconnected");
            mOrderService = null;
        }
    };
    private IPoyntOrderServiceListener poyntOrderServiceListener = new IPoyntOrderServiceListener.Stub() {
        @Override
        public void orderResponse(Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "Order Info: " + order.toString());
        }
    };


    /**** receipt printing service setup ******/
    private IPoyntReceiptPrintingService mReceiptPrintingService;
    private ServiceConnection mReceiptPrintingConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "IPoyntReceiptPrintingService is now connected");
            mReceiptPrintingService = IPoyntReceiptPrintingService.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "IPoyntReceiptPrintingService has unexpectedly disconnected");
            mReceiptPrintingService = null;
        }
    };
    private IPoyntReceiptPrintingServiceListener ipoyntReceiptPrintingServiceListener = new IPoyntReceiptPrintingServiceListener.Stub(){
        @Override
        public void printQueued() throws RemoteException {
            Log.d(TAG, "Receipt queued");
        }

        @Override
        public void printFailed() throws RemoteException {
            Log.d(TAG, "Receipt printing failed");
        }
    };

    private class PrintingTask extends AsyncTask<Void,Void,Void> {
        private IPoyntReceiptPrintingService printingService;
        String printJobId;
        PrintedReceipt printedReceipt;
        @Override
        protected Void doInBackground(Void... voids) {

            // need to wait to make sure printing service has connected
            while (printingService == null){
                try {
                    Thread.currentThread().sleep(1000);
                }catch(InterruptedException e){ /*do nothing */}
            }

            try {
                mReceiptPrintingService.printReceipt(printJobId,printedReceipt, ipoyntReceiptPrintingServiceListener);
            } catch (RemoteException e) { e.printStackTrace(); }

            return null;
        }

        public PrintingTask (IPoyntReceiptPrintingService service, PrintedReceipt receipt){
            this.printingService = service;
            printJobId = UUID.randomUUID().toString();
            this.printedReceipt = receipt;

        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        PrintedReceipt printedReceipt = new PrintedReceipt();
        List<PrintedReceiptLine> header = new ArrayList<PrintedReceiptLine>();
        PrintedReceiptLine headerLine1 = new PrintedReceiptLine();
        headerLine1.setText("JAMBA JUICE");
        PrintedReceiptLine headerLine2 = new PrintedReceiptLine();
        headerLine2.setText("$2 Fruit and Veggie Smoothies");
        PrintedReceiptLine headerLine3 = new PrintedReceiptLine();
        headerLine3.setText("");
        PrintedReceiptLine headerLine4 = new PrintedReceiptLine();
        headerLine4.setText("For demo purposes only,");
        PrintedReceiptLine headerLine5 = new PrintedReceiptLine();
        headerLine5.setText("not a redeemable offer");
        header.add(headerLine1);
        header.add(headerLine2);
        header.add(headerLine3);
        header.add(headerLine4);
        header.add(headerLine5);


        printedReceipt.setHeader(header);
        printedReceipt.setFooterImage(Utils.generateBarcode("twodollarsoff"));

        new PrintingTask(mReceiptPrintingService, printedReceipt).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);

        // 0=OK?
        return 0;
    }

    public OrderService() {
    }



    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        observer = new MyObserver(handler);
        resolver = getContentResolver();
        resolver.registerContentObserver(OrdersColumns.CONTENT_URI, true, observer);
        Log.d(TAG, "from onCreate");

        bindService(new Intent(IPoyntReceiptPrintingService.class.getName()),
                mReceiptPrintingConnection, Context.BIND_AUTO_CREATE);
//        bindService(new Intent(IPoyntOrderService.class.getName()),
//                mOrderConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("OrderService", "from onDestroy");
//        unbindService(mOrderConnection);
        unbindService(mReceiptPrintingConnection);

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    class MyObserver extends ContentObserver {
        public MyObserver(Handler handler) {
            super(handler);
            Log.d(TAG, "From MyObserver Constructor");
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
            Log.d(TAG, "from onChange");
        }


        @Override
        public void onChange(boolean selfChange, Uri uri) {
        /*
            Log.d(TAG, "onChange(boolean selfChange, Uri uri)");
            Log.d(TAG, "selfChange: " + selfChange + "\nUri: " +uri);
            // onChange() get called when an order gets deleted during sync
            // we can ignore and return from this method if order id is not present
            if ("orders".equals(uri.getLastPathSegment())) return;


            String orderId = uri.getLastPathSegment();
            Log.d(TAG, "Order id:" + orderId);

//            try {
//                mOrderService.getOrder(uri.getLastPathSegment(), UUID.randomUUID().toString(), poyntOrderServiceListener);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
            // String [] mProjection = {OrdersColumns.ORDERID, OrdersColumns.CUSTOMERUSERID, OrdersColumns.CREATEDAT};
            String mSelectionClause = OrdersColumns.ORDERID + "= ?";
            String [] mSelectionArgs = {orderId};
            String mSortOrder = null;
//            Cursor cursor = getContentResolver().query(OrdersColumns.CONTENT_URI,mProjection, mSelectionClause, mSelectionArgs, mSortOrder);
            OrdersCursor cursor = (OrdersCursor)getContentResolver().query(OrdersColumns.CONTENT_URI,
                    OrdersColumns.FULL_PROJECTION, mSelectionClause, mSelectionArgs, mSortOrder);
            if (cursor !=null){
                if (cursor.moveToNext()){
//                    Log.d(TAG, "order id: " + cursor.getString(0));
//                    Log.d(TAG, "customer id: " + cursor.getString(1));
//                    Log.d(TAG, "created at: " + cursor.getString(2));
                    Log.d(TAG, "order id: " + cursor.getOrderid());
                    Log.d(TAG, "customer id: " + cursor.getCustomeruserid());
                    Log.d(TAG, "created at: " + cursor.getCreatedat());
                    Log.d(TAG, "updated at: " + cursor.getUpdatedat());
                    Log.d(TAG, "order #: " + cursor.getOrdernumber());
                    Log.d(TAG, "Notes: " + cursor.getNotes());


                }

            }
*/
            // do s.th.
            // depending on the handler you might be on the UI
            // thread, so be cautious!
        }
    }
}

