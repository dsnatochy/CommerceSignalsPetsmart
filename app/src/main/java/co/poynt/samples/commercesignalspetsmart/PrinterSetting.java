package co.poynt.samples.commercesignalspetsmart;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Printer information.
 */
public class PrinterSetting {
    public static final String typeBluetoothIF = "BT:";
    public static final String typeUsbIF       = "USB:";
    public static final String typeEthernetIF  = "TCP:";

    public static final String PREF_KEY_DEVICE_NAME  = "pref_key_device_name";
    public static final String PREF_KEY_PRINTER_TYPE = "pref_key_printertype";
    public static final String PREF_KEY_MAC_ADDRESS  = "pref_key_mac_address";

    private Context mContext;

    PrinterSetting(Context context) {
        mContext = context;
    }

    public void write(String portName, String macAddress) {
        write(portName, macAddress, "");
    }

    public void write(String portName, String macAddress, String printerType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        prefs.edit()
                .putString(PREF_KEY_DEVICE_NAME, portName)
                .putString(PREF_KEY_MAC_ADDRESS, macAddress)
                .putString(PREF_KEY_PRINTER_TYPE, printerType)
                .apply();
    }

    public String getPortName() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        String macAddress = prefs.getString(PREF_KEY_MAC_ADDRESS, "");

        // --- Bluetooth ---
        // It can communication used device name(Ex.BT:Star Micronics) at bluetooth.
        // If android device has paired two same name device, can't choose destination target.
        // If used Mac Address(Ex. BT:00:12:3f:XX:XX:XX) at Bluetooth, can choose destination target.
        if (macAddress.startsWith("BT:")) {
            return macAddress;
        }

        return prefs.getString(PREF_KEY_DEVICE_NAME, "");
    }

    public String getDeviceName() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getString(PREF_KEY_DEVICE_NAME, "");
    }

    public String getMacAddress() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getString(PREF_KEY_MAC_ADDRESS, "");
    }

    public String getPrinterType() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getString(PREF_KEY_PRINTER_TYPE, "");
    }

    public Context getContext() {
        return mContext;
    }
}
