package co.poynt.samples.commercesignalspetsmart;


import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

/**
 * Created by dennis on 10/7/15.
 */
public class Utils {
    private final static String TAG = "co.poynt.samples.Utils";
    public static Bitmap generateBarcode(String contents) {

        int WHITE = 0xFFFFFFFF;
        int BLACK = 0xFF000000;

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            int qrSize = 400;
            result = writer.encode(contents, BarcodeFormat.QR_CODE, qrSize, qrSize, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate barcode: " + e.getMessage());
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
