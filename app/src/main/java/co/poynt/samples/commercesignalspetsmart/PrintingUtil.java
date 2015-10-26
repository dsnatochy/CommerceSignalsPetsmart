package co.poynt.samples.commercesignalspetsmart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.ParcelFileDescriptor;
import android.print.pdf.PrintedPdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.ParcelFileDescriptor;
import android.print.pdf.PrintedPdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//import co.poynt.os.common.util.Ln;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;

/**
 * Created by vchau on 10/3/14.
 */
public final class PrintingUtil {

    //Return height of receipt in mils (thousands of an inch)
    private static int calculateHeight(PrintedReceipt receipt) {
        //always give some space for user to tear paper
        int height = ReceiptCanvasInfo.PAPER_TEAR_SPACE;

        if (receipt.getHeaderImage() != null) {
            height += ReceiptCanvasInfo.calculateImageHeight(
                    receipt.getHeaderImage().getHeight());
        }
        if (receipt.getFooterImage() != null) {
            height += ReceiptCanvasInfo.calculateImageHeight(receipt.getFooterImage().getHeight());
        }

        int totalTextLine = 3; //name + address
        if (receipt.getPhone() != null) {
            totalTextLine++;
        }
        if (receipt.getHeader() != null) {
            totalTextLine += receipt.getHeader().size();
        }

        if (receipt.getBody() != null) {
            totalTextLine += receipt.getBody().size();
        }

        if (receipt.getFooter() != null) {
            totalTextLine += receipt.getFooter().size();
        }

        height += ReceiptCanvasInfo.calculateTextHeight(totalTextLine);
        return height;
    }

    private static void fillAddressAndPhone(PrintedReceipt receipt, List<PrintedReceiptLine> content) {
        if (receipt.getStoreAddress() != null) {
            content.add(new PrintedReceiptLine(receipt.getStoreAddress().getLine1(), false));
            content.add(new PrintedReceiptLine(receipt.getStoreAddress().getCity() + " " +
                    receipt.getStoreAddress().getTerritory() + " " +
                    receipt.getStoreAddress().getPostalCode(), false));
        }
        if (receipt.getPhone() != null) {
            content.add(new PrintedReceiptLine(
                    receipt.getPhone().getAreaCode() + "-" + receipt.getPhone()
                            .getLocalPhoneNumber(),false));
        }
        content.add(new PrintedReceiptLine("",false));
        content.add(new PrintedReceiptLine("",false));
    }

    /**
     * Create a bitmap for printing. The media length is based on the
     * amount of content in the receipt. The width is fixed at ReceiptCanvasInfo.PAPER_WIDTH px.
     *
     * @return
     */
    public static Bitmap createPrintableImage(PrintedReceipt receipt) {

        int mediaHeight = calculateHeight(receipt);
//        Ln.d("Height: " + mediaHeight);

        Bitmap bitmap = Bitmap.createBitmap(ReceiptCanvasInfo.PAPER_WIDTH, mediaHeight,
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);

        Paint paint = new Paint();
        int verOffset = 20;

        if (receipt.getHeaderImage() != null) {
            verOffset = PrintingUtil.drawImage(canvas, paint, verOffset,
                    receipt.getHeaderImage());
        }

        List<PrintedReceiptLine> bizInfo = new ArrayList<PrintedReceiptLine>();
        if (receipt.getBusinessName()!=null) {
            bizInfo.add(new PrintedReceiptLine(receipt.getBusinessName(), false));
        }
        fillAddressAndPhone(receipt, bizInfo);
        verOffset = PrintingUtil.drawText(canvas, paint, verOffset, Paint.Align.CENTER,
                bizInfo);

        verOffset = PrintingUtil.drawText(canvas, paint, verOffset, Paint.Align.CENTER,
                receipt.getHeader());

        paint.setTextAlign(Paint.Align.CENTER);
        verOffset = PrintingUtil.drawText(canvas, paint, verOffset, Paint.Align.CENTER,
                receipt.getBody());

        if (receipt.getFooterImage() != null) {
            verOffset = PrintingUtil.drawImage(canvas, paint, verOffset,
                    receipt.getFooterImage());
        }
        return bitmap;
    }

    //convert a PDF to a ParcelFileDescriptor provided by the printer driver service.
    public static void writePdfToFile(PrintedPdfDocument pdf, ParcelFileDescriptor pfd) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(pfd.getFileDescriptor());
            pdf.writeTo(fos);

        } catch (Exception e) {
//            Ln.e("Error while writing PDF to ParcelFileDescriptor: " + e.getMessage());
        } finally {
            pdf.close();
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    public static void writeDebugFile(Context context, PrintedPdfDocument pdf, String fileName) {
        FileOutputStream fos = null;
        try {
            File file = new File(context.getFilesDir(), fileName);
            fos = context.openFileOutput(file.getName(), Context.MODE_PRIVATE);
            pdf.writeTo(fos);
        } catch (IOException e) {
//            Ln.e("Failed to write to debug file.");
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    public static int drawText(Canvas canvas, Paint paint, int verticalOffset,
                               Paint.Align alignment,
                               List<PrintedReceiptLine> content) {
        if (content != null) {
            paint.setColor(Color.BLACK);
            paint.setTextAlign(alignment);
            paint.setTextSize(ReceiptCanvasInfo.getFontSize());
            Typeface tf = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
            paint.setTypeface(tf);

            int leftMargin = Paint.Align.CENTER.equals(alignment) ? canvas.getWidth() / 2 :
                    ReceiptCanvasInfo.getLeftMargin();

            for (PrintedReceiptLine line : content) {
                paint.setColor(Color.BLACK);
                if (line != null) {
                    int margin = 5; //5pixel margin on left and right.
                    String str = line.getText();
                    if (str.length() > ReceiptCanvasInfo.getMaxTextLineLength()) {
                        str = str.substring(0, ReceiptCanvasInfo.getMaxTextLineLength());
                    }

                    if (line.invertColor()) {
                        float backLeftMargin = Paint.Align.CENTER.equals(alignment) ?
                                leftMargin - paint.measureText(str) / 2 : leftMargin;
                        float backTopMargin = verticalOffset - ReceiptCanvasInfo.getLineSpacing();
                        canvas.drawRect(
                                backLeftMargin - margin,
                                backTopMargin + margin,
                                backLeftMargin + paint.measureText(str) + margin,
                                backTopMargin + ReceiptCanvasInfo.getLineSpacing()
                                        + margin, paint);
                        paint.setColor(Color.WHITE);
                    }
                    canvas.drawText(str,
                            leftMargin,
                            verticalOffset, paint);

                    verticalOffset += ReceiptCanvasInfo.getLineSpacing();
                }
            }
        }

        return verticalOffset;
    }

    public static int drawImage(Canvas canvas, Paint paint, int verticalOffset,
                                Bitmap bm) {
        Bitmap scaledBm = bm;
        float scaleFactor = 1.f;
        if (bm.getWidth() > ReceiptCanvasInfo.getMaxImageWidth() || bm.getHeight() >
                ReceiptCanvasInfo.getMaxImageHeight()) {
            if ((bm.getWidth() - ReceiptCanvasInfo.getMaxImageWidth()) > (bm.getHeight() - ReceiptCanvasInfo.getMaxImageHeight())) {
                //scale width
                scaleFactor = (1.F * ReceiptCanvasInfo.getMaxImageWidth()) / bm.getWidth();
            } else {
                scaleFactor = (1.F * ReceiptCanvasInfo.getMaxImageHeight()) / bm.getHeight();
            }
            //scale down
            int scaledWidth = (int) (scaleFactor * bm.getWidth());
            int scaledHeight = (int) (scaleFactor * bm.getHeight());
            scaledBm = Bitmap.createScaledBitmap(bm, scaledWidth, scaledHeight, true);

        }

        //forces everything to draw in monochrome
        ColorMatrix ma = new ColorMatrix();
        ma.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(ma));
        paint.setColor(Color.BLACK);

        canvas.drawBitmap(scaledBm, (canvas.getWidth() / 2) - (scaledBm.getWidth() / 2),
                verticalOffset,
                paint);
        verticalOffset += scaledBm.getHeight() * ReceiptCanvasInfo.TVDPI_FACTOR;

        return verticalOffset;
    }

}
