package co.poynt.samples.commercesignalspetsmart;

import android.accounts.Account;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

import com.starmicronics.starioextension.commandbuilder.Bitmap.SCBBitmapConverter;
import com.starmicronics.starioextension.commandbuilder.ISCBBuilder;
import com.starmicronics.starioextension.commandbuilder.SCBFactory;
import com.starmicronics.starioextension.starioextmanager.StarIoExtManager;
import com.starmicronics.starioextension.starioextmanager.StarIoExtManagerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import co.poynt.api.model.Address;
import co.poynt.api.model.Business;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderAmounts;
import co.poynt.api.model.OrderItem;
import co.poynt.os.contentproviders.orders.orders.OrdersColumns;
import co.poynt.os.contentproviders.orders.orders.OrdersCursor;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PaymentSettings;
import co.poynt.os.model.PoyntError;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;
import co.poynt.os.services.v1.IPoyntBusinessService;
import co.poynt.os.services.v1.IPoyntOrderService;
import co.poynt.os.services.v1.IPoyntOrderServiceListener;
import co.poynt.os.services.v1.IPoyntReceiptPrintingService;
import co.poynt.os.services.v1.IPoyntReceiptPrintingServiceListener;

public class OrderService extends Service {
    public static final int COMMERCE_SIGNALS_NOTIFICATION_ID = 13049;// doesn't matter what the id is it just needs to be > 0


    private final String TAG = OrderService.class.getSimpleName();
    private MyObserver observer;
    private ContentResolver resolver;
    private Handler handler;
    private Business mBusiness;

    /***
     * setting up business service
     ***/
    private IPoyntBusinessService mBusinessService;
    private ServiceConnection mBusinessConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "IPoyntBusinessService is now connected");
            mBusinessService = IPoyntBusinessService.Stub.asInterface(iBinder);

            // first load business and business users to make sure the device resolves to a business
            // invoke the api to get business details
            try {
                mBusinessService.getBusiness(mBusinessReadListener);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to connect to business service to resolve the business this terminal belongs to!");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "IPoyntBusinessService has unexpectedly disconnected");
            mBusinessService = null;
        }
    };

    /***
     * setting up order service
     ***/
    private IPoyntOrderService mOrderService;

    private IPoyntBusinessReadListener mBusinessReadListener = new IPoyntBusinessReadListener.Stub() {

        @Override
        public void onResponse(Business business, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "Got Business");
            mBusiness = business;
        }
    };
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
//            Log.d(TAG, "Order Info: " + order.toString());

            if (order == null) {
                return;
            } else {
                getStaticReceipt(order);

                return;
            }
        }
    };

    /****
     * receipt printing service setup
     ******/
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
    private IPoyntReceiptPrintingServiceListener ipoyntReceiptPrintingServiceListener = new IPoyntReceiptPrintingServiceListener.Stub() {
        @Override
        public void printQueued() throws RemoteException {
            Log.d(TAG, "Receipt queued");
        }

        @Override
        public void printFailed() throws RemoteException {
            Log.d(TAG, "Receipt printing failed");
        }
    };

//    /*** order service setup ***/
//    private IPoyntOrderService mOrderService;
//    private ServiceConnection mOrderServiceConnection = new ServiceConnection(){
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder service) {
//            Log.d(TAG, "IPoyntOrderService is now connected");
//            mOrderService = IPoyntOrderService.Stub.asInterface(service);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            Log.d(TAG, "IPoyntOrderService has unexpectedly disconnected");
//            mOrderService = null;
//        }
//    };

    private class PoyntPrintingTask extends AsyncTask<Void, Void, Void> {
        //private IPoyntReceiptPrintingService printingService;
        String printJobId;
        PrintedReceipt printedReceipt;

        @Override
        protected Void doInBackground(Void... voids) {

            // need to wait to make sure printing service has connected
            while (mReceiptPrintingService == null) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) { /*do nothing */}
            }
            try {
                mReceiptPrintingService.printReceipt(printJobId, printedReceipt, ipoyntReceiptPrintingServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }

        public PoyntPrintingTask(IPoyntReceiptPrintingService service, PrintedReceipt receipt) {
            //this.printingService = service;
            printJobId = UUID.randomUUID().toString();
            this.printedReceipt = receipt;

        }
    }

    private class mPOPPrintingTask extends AsyncTask<Void, Void, Void> {
        private String orderId;

        @Override
        protected Void doInBackground(Void... voids) {

            while (mOrderService == null) {
                try {
                    Thread.currentThread().sleep(1000l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                mOrderService.getOrder(orderId, UUID.randomUUID().toString(), poyntOrderServiceListener);

                Bitmap bitmap = Utils.generateBarcode("2OFF");

            } catch (RemoteException e) {
                e.printStackTrace();
            }


            return null;
        }


        public mPOPPrintingTask(String orderId) {
            this.orderId = orderId;
        }

    }

    private void printPosImage(Bitmap image) {

        byte[] data = getPOSImage(400, image, SCBBitmapConverter.Rotation.Normal);
        Comunication.Result result;

        result = Comunication.sendCommands(data,
                OrderService.this.printerBtMac,
                "BT:", 10000, OrderService.this);     // 10000mS!!!

    }

    final byte[] getPOSImage(int width, Bitmap bitmap, SCBBitmapConverter.Rotation rotation) {
        CommandDataList commands = new CommandDataList();

        ISCBBuilder builder = SCBFactory.createBuilder(SCBFactory.Emulation.Star);

        builder.appendBitmap(bitmap, false, width, rotation);

        List<byte[]> listBuf = builder.getBuffer();

        for (byte[] buf : listBuf) {
            commands.add(buf);
        }

        commands.add(0x1b, 0x64, 0x03);                 // Cut Paper

        return commands.getByteArray();
    }

    private void getStaticReceipt(Order order) {

        PoyntReceiptBuilder builder = new PoyntReceiptBuilder(new PaymentSettings());

        if (mBusiness != null) {
            String bizName = mBusiness.getDoingBusinessAs();
            Address address = mBusiness.getAddress();
            String addr1 = address.getLine1();
            String city = address.getCity();
            String state = address.getTerritory();
            String zip = address.getPostalCode();
            String phone = "(" + mBusiness.getPhone().getAreaCode() + ")" + mBusiness.getPhone().getLocalPhoneNumber();

            Log.d(TAG, bizName);
            Log.d(TAG, addr1);
            Log.d(TAG, city + " " + state + ", " + zip);
            Log.d(TAG, phone);

            builder.setBusinessName(bizName);
            builder.setPhone(mBusiness.getPhone());
            builder.setStoreAddress(address);

            PrintedReceipt receipt = builder.build(false, order);
            List<PrintedReceiptLine> bodyList = receipt.getBody();

            PrintedReceiptLine emptyLine = new PrintedReceiptLine();
            PrintedReceiptLine headerLine1 = new PrintedReceiptLine();
            headerLine1.setText("Lucky Dog Bar and Grill");
            PrintedReceiptLine headerLine2 = new PrintedReceiptLine();
            headerLine2.setText("$2 OFF AN ENTREE");
            PrintedReceiptLine headerLine3 = new PrintedReceiptLine();
            headerLine3.setText("Coupon code: 2OFF");
            emptyLine.setText(" ");
            PrintedReceiptLine headerLine5 = new PrintedReceiptLine();
            headerLine5.setText("For demo purposes only,");
            PrintedReceiptLine headerLine6 = new PrintedReceiptLine();
            headerLine6.setText("not a redeemable offer");

            bodyList.add(emptyLine);
            bodyList.add(emptyLine);
            bodyList.add(emptyLine);
            bodyList.add(emptyLine);
            bodyList.add(headerLine1);
            bodyList.add(headerLine2);
            bodyList.add(headerLine3);
            bodyList.add(emptyLine);
            bodyList.add(emptyLine);
            bodyList.add(headerLine5);
            bodyList.add(headerLine6);

            receipt.setBody(bodyList);
            receipt.setFooterImage(Utils.generateBarcode("2OFF"));

            Bitmap bitmap =
                    PrintingUtil.createPrintableImage(receipt);
            printPosImage(bitmap);
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(COMMERCE_SIGNALS_NOTIFICATION_ID, getNotification());


        //new PoyntPrintingTask(mReceiptPrintingService, getStaticReceipt()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);

        String orderId = intent.getStringExtra(Intents.INTENT_EXTRAS_ORDER_ID);
        new mPOPPrintingTask(orderId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);


        return START_STICKY;

//        // 0=OK?
//        return 0;
    }

    //    STAR mPOP-H0427 |
    private final String printerBtMac = "BT:00:12:F3:2B:5F:41";
    private StarIoExtManager mStarIoExtManager = null;
    private StarIoExtManagerListener mStarIoExtManagerListener = new StarIoExtManagerListener() {
        public void didPrinterImpossible() {
            Log.d(TAG, " Printer you are Impossible ;) ");
        }

        public void didPrinterOnline() {
            Log.d(TAG, " didPrinterOnline ;) ");
        }

        public void didPrinterOffline() {
            Log.d(TAG, " didPrinterOffline ;) ");
        }

        public void didPrinterPaperReady() {
            Log.d(TAG, " didPrinterPaperReady ;) ");
        }

        public void didPrinterPaperNearEmpty() {
            Log.d(TAG, " didPrinterNealyHungry ;) ");
        }

        public void didPrinterPaperEmpty() {
            Log.d(TAG, " didPrinterHungry ;) ");
        }

        public void didPrinterCoverOpen() {
            Log.d(TAG, " didPrinterExposed ;) ");
        }

        public void didPrinterCoverClose() {
            Log.d(TAG, " didPrinterCoverUP ;) ");
        }


    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        observer = new MyObserver(handler);
        resolver = getContentResolver();
        resolver.registerContentObserver(OrdersColumns.CONTENT_URI, true, observer);
        Log.d(TAG, "from onCreate");


        PrinterSetting setting = new PrinterSetting(this);
        mStarIoExtManager = new StarIoExtManager(StarIoExtManager.Type.Standard,
                printerBtMac, "BT:",
                10000, this); // 10000mS!!!
        mStarIoExtManager.setListener(mStarIoExtManagerListener);

        new mPOPPrintingTask("blah").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);


        bindService(new Intent(IPoyntReceiptPrintingService.class.getName()),
                mReceiptPrintingConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(IPoyntOrderService.class.getName()),
                mOrderConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(IPoyntBusinessService.class.getName()),
                mBusinessConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("OrderService", "from onDestroy");
        unbindService(mOrderConnection);
        unbindService(mReceiptPrintingConnection);
        unbindService(mBusinessConnection);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * Creates a notification to show in the notification bar
     *
     * @return a new {@link android.app.Notification}
     */
    private Notification getNotification() {

        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                new Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle(getResources().getString(R.string.app_name))
                .setSmallIcon(R.drawable.comm_sig_logo_big)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .build();

        return notification;
    }


    // NOT USED
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

