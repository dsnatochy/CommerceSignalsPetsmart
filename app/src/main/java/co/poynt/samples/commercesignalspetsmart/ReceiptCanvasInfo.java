package co.poynt.samples.commercesignalspetsmart;

public class ReceiptCanvasInfo {
    public static final int MILS_OF_INCH = 1000;
    public static final int MDPI = 160;
    public static final int TVDPI = 213; //1.33 * MDPI

    public static final float TVDPI_FACTOR = 1.33f;

    public static final int PRINTER_DPI_X = 203;
    public static final int PRINTER_DPI_Y = 203;

    public static final int PAPER_WIDTH = 400; //in Pixels

    private static final int FONT_SIZE = 20;
    private static final int LINE_SPACING = 24;
    private static final int MAX_TEXT_LINE_LENGTH = 31;
    private static final String BLANK_LINE = "                               ";

    private static final int LEFT_MARGIN = 0;//8;

    private static final int MAX_IMAGE_WIDTH_PX = 240;
    private static final int MAX_IMAGE_HEIGHT_PX = 200;
    public static final int PAPER_TEAR_SPACE = LINE_SPACING * 4;

    public static int calculateTextHeight(int lines) {
        return lines * LINE_SPACING;
    }

    public static int calculateImageHeight(int heightPx) {
        return (int) (heightPx * TVDPI_FACTOR);
    }

    public static int getFontSize() {
        return FONT_SIZE;
    }

    public static int getLineSpacing() {
        return LINE_SPACING;
    }

    public static int getMaxTextLineLength() {
        return MAX_TEXT_LINE_LENGTH;
    }

    public static int getLeftMargin() {
        return LEFT_MARGIN;
    }

    public static int getMaxImageWidth() {
        return MAX_IMAGE_WIDTH_PX;
    }

    public static int getMaxImageHeight() {
        return MAX_IMAGE_HEIGHT_PX;
    }

    public static String getBlankLine() {
        return BLANK_LINE;
    }
}