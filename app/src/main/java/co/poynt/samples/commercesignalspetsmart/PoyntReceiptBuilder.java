package co.poynt.samples.commercesignalspetsmart;

import android.graphics.Bitmap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import co.poynt.api.model.Address;
import co.poynt.api.model.CardType;
import co.poynt.api.model.Discount;
import co.poynt.api.model.EMVData;
import co.poynt.api.model.FundingSourceType;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.Phone;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.model.PaymentSettings;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;

import android.graphics.Bitmap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import co.poynt.api.model.Address;
import co.poynt.api.model.CardType;
import co.poynt.api.model.Discount;
import co.poynt.api.model.EMVData;
import co.poynt.api.model.FundingSourceType;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.Phone;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.model.PaymentSettings;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;

/**
 * Created by vchau on 10/6/14.
 */
public class PoyntReceiptBuilder {
    private Bitmap businessReceiptHeaderImage;
    private Address storeAddress;
    private Phone phone;
    private String businessName;
    private String merchantId;
    private String terminalId;
    private PaymentSettings paymentSettings;


    private final PrintedReceiptLine EMPTY_LINE = new PrintedReceiptLine("", false);
    private final PrintedReceiptLine DOTTED_LINE = new PrintedReceiptLine("----------------------------------------", false);

    public PoyntReceiptBuilder(PaymentSettings paymentSettings) {
        this.paymentSettings = paymentSettings;
    }

    public Bitmap getBusinessReceiptHeaderImage() {
        return businessReceiptHeaderImage;
    }

    public void setBusinessReceiptHeaderImage(Bitmap businessReceiptHeaderImage) {
        this.businessReceiptHeaderImage = businessReceiptHeaderImage;
    }

    public Address getStoreAddress() {
        return storeAddress;
    }

    public void setStoreAddress(Address storeAddress) {
        this.storeAddress = storeAddress;
    }

    public Phone getPhone() {
        return phone;
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    protected static PrintedReceiptLine makeLine(String label, String value, boolean invertColor) {
        StringBuffer line = new StringBuffer(ReceiptCanvasInfo.getBlankLine());
        if (value == null) {
            value = "";
        } else if (value.length() > 31) {
            value = value.substring(0, value.length() - 10);
        }
        int amtStartIndex = ReceiptCanvasInfo.getMaxTextLineLength() - value.length();
        line.replace(amtStartIndex, line.length(), value);
        if (label.length() > (amtStartIndex - 3)) {
            label = label.substring(0, amtStartIndex - 3);
        }
        line.replace(0, label.length(), label);

        return new PrintedReceiptLine(line.toString(), invertColor);
    }

    protected static PrintedReceiptLine makeLine(String label, String value) {
        return makeLine(label, value, false);
    }

    public static String convertToShortId(String uuid) {
        return "#" + uuid.substring(0, uuid.indexOf("-"));
    }
    public static final boolean notEmpty(final String s)
    {
        return !isEmpty(s);
    }
    protected void addOrderDetails(Order order, PrintedReceipt receipt) {
        if (receipt.getBody() == null) {
            receipt.setBody(new ArrayList<PrintedReceiptLine>());
        }
        List<PrintedReceiptLine> content = receipt.getBody();

        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MM/dd/yyyy h:mm a");
        sdf.setTimeZone(tz);

        String txnTime = sdf.format(cal.getTime());

        content.add(makeLine("TIME: " + txnTime, "", false));
        content.add(makeLine("Order ID: " + convertToShortId(order.getId().toString()), ""));
        if (notEmpty(order.getOrderNumber())) {
            content.add(makeLine("Order Number: " + order.getOrderNumber(), ""));
        }
        content.add(DOTTED_LINE);
        Float tmpTotal = 0.f;
        Float tmpTax = 0.f;
        Float tmpDiscount = 0.f;
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String qtyAndPrice;
                tmpTotal += item.getUnitPrice() * item.getQuantity();
                if (item.getTax() != null) {
                    tmpTax += item.getTax();
                }
                if (item.getDiscount() != null) {
                    tmpDiscount += item.getDiscount();
                }
                if (item.getQuantity() > 1.0) {
                    qtyAndPrice = item.getQuantity() + "@ " + String.format("%.2f",
                            item.getUnitPrice() / 100.f);
                } else {
                    qtyAndPrice = String.format("%.2f", item.getUnitPrice() / 100.f);
                }
                String label = String.format("%s", item.getName());
                content.add(makeLine(label, qtyAndPrice));
            }
            content.add(DOTTED_LINE);
        }
        // add discounts if any
        if (order.getDiscounts() != null && order.getDiscounts().size() > 0) {
            for (Discount discount : order.getDiscounts()) {
                String label = String.format("%s", discount.getCustomName());
                String discountValue = String.format("%.2f", discount.getAmount() / 100.f);
                content.add(makeLine(label, discountValue));
            }
            content.add(DOTTED_LINE);
        }
        content.add(makeLine("Total", String.format("%.2f", order.getAmounts().getSubTotal() / 100.f)));
        if (order.getAmounts().getTaxTotal() != null && order.getAmounts().getTaxTotal() > 0) {
            content.add(makeLine("Tax", String.format("%.2f", order.getAmounts().getTaxTotal() / 100.f)));
        }

        if (order.getAmounts().getDiscountTotal() != null && order.getAmounts().getDiscountTotal() != 0) {
            content.add(makeLine("Discounts", String.format("%.2f", order.getAmounts().getDiscountTotal() / 100.f)));
        }
        content.add(DOTTED_LINE);
        Currency currency = null;
        try {
            currency = Currency.getInstance(order.getAmounts().getCurrency());
        } catch (IllegalArgumentException e) {
            Ln.e(e, "Invalid currencyCode (%s)", order.getAmounts().getCurrency());
            if (currency == null) {
                // default to en-us
                currency = Currency.getInstance(Locale.US);
            }
        }
        content.add(makeLine("Grand Total", String.format("%s%.2f",
                currency.getSymbol(),
                (order.getAmounts().getNetTotal() / 100.f))));
        if (order.getTransactions() != null) {
            addBriefFundingSourceDetails(order.getTransactions(), content);
        }
        content.add(EMPTY_LINE);
        content.add(EMPTY_LINE);

        receipt.setBody(content);

    }

    protected void addTransactionDetails(Transaction txn, PrintedReceipt receipt) {
        if (receipt.getBody() == null) {
            receipt.setBody(new ArrayList<PrintedReceiptLine>());
        }
        List<PrintedReceiptLine> content = receipt.getBody();

        addMerchantInfo(content);
        addLine(content);

        boolean isEmv = addFundingSourceDetails(txn, content);

        Currency currency = null;
        try {
            currency = Currency.getInstance(txn.getAmounts().getCurrency());
        } catch (IllegalArgumentException e) {
            Ln.e(e, "Invalid currencyCode (%s)", txn.getAmounts().getCurrency());
            if (currency == null) {
                // default to en-us
                currency = Currency.getInstance(Locale.US);
            }
        }
        content.add(makeLine("Amount:", String.format("%s%.2f",
                currency.getSymbol(),
                txn.getAmounts().getOrderAmount() / 100.f)));

        //TODO: figure out how to determine if PIN was used. It is suppose to be tag 0x8E
        boolean isPINVerified = false;

        boolean displayedTipAndTotal = false;

        if (txn.getAmounts().getTipAmount() != null && txn.getAmounts().getTipAmount() > 0) {
            content.add(makeLine("Tip:", String.format("%.2f",
                    txn.getAmounts().getTipAmount() / 100.f)));
        } else if (TransactionStatus.AUTHORIZED.equals(txn.getStatus())) {
            if (paymentSettings != null && paymentSettings.isGratuityEnabled()) {
                displayedTipAndTotal = true;
                content.add(EMPTY_LINE);
                content.add(makeLine("Tip:", "____________"));
                content.add(EMPTY_LINE);
                content.add(makeLine("Total:", "____________"));
            }
        }

        if (isPINVerified) {
            content.add(makeLine("PIN Verified", ""));
        }

        if (!displayedTipAndTotal) {
            content.add(DOTTED_LINE);
            String actionLabel = TransactionAction.REFUND.equals(txn.getAction()) ? "Refunded:" :
                    "Paid:";
            content.add(makeLine(actionLabel, String.format("%.2f",
                    txn.getAmounts().getTransactionAmount() / 100.f)));
            if (txn.getAmounts().getCashbackAmount() != null && txn.getAmounts().getCashbackAmount()
                    > 0) {
                content.add(makeLine("Cashback:", String.format("-%.2f",
                        txn.getAmounts().getCashbackAmount() / 100.f)));
            }
        }

        if (txn.getProcessorResponse() != null && txn.getProcessorResponse().getRemainingBalance() != null) {
            content.add(DOTTED_LINE);
            content.add(makeLine("Remaining Balance:", String.format("%.2f",
                    txn.getProcessorResponse().getRemainingBalance() / 100.f)));
            content.add(DOTTED_LINE);
        }


        if (TransactionStatus.AUTHORIZED.equals(txn.getStatus())) {
            content.add(EMPTY_LINE);
            content.add(EMPTY_LINE);
            if (txn.isSignatureCaptured() == Boolean.TRUE) {
                content.add(EMPTY_LINE);
                content.add(new PrintedReceiptLine("Signature:______________________________", false));
            }
        }

        content.add(EMPTY_LINE);
        content.add(EMPTY_LINE);
        content.add(EMPTY_LINE);
        content.add(EMPTY_LINE);
        content.add(EMPTY_LINE);

    }

    protected void addCombinedOrderTransactionDetails(Order order, Transaction transaction, PrintedReceipt receipt, long tipAmount, boolean signatureCollected) {
        if (receipt.getBody() == null) {
            receipt.setBody(new ArrayList<PrintedReceiptLine>());
        }
        List<PrintedReceiptLine> content = receipt.getBody();

        addMerchantInfo(content);
        if (order != null) {
            content.add(makeLine("Order ID: " + PoyntUtil.convertToShortId(order.getId().toString()), ""));
            if (Strings.notEmpty(order.getOrderNumber())) {
                content.add(makeLine("Order Number: " + order.getOrderNumber(), ""));
            }
            addLine(content);
            // add items
            addItems(order.getItems(), content);
            // add discounts if any
            addDiscounts(order.getDiscounts(), content);
            // add totals
            content.add(makeLine("Sub Total", String.format("%.2f", order.getAmounts().getSubTotal() / 100.f)));
            if (order.getAmounts().getTaxTotal() != null && order.getAmounts().getTaxTotal() > 0) {
                content.add(makeLine("Tax", String.format("%.2f", order.getAmounts().getTaxTotal() / 100.f)));
            }
            if (order.getAmounts().getDiscountTotal() != null && order.getAmounts().getDiscountTotal() != 0) {
                content.add(makeLine("Discounts", String.format("%.2f", order.getAmounts().getDiscountTotal() / 100.f)));
            }
        } else {
            addFundingSourceDetails(transaction, content);
            content.add(makeLine("Sub Total", String.format("%.2f", transaction.getAmounts().getOrderAmount() / 100.f)));
        }
        boolean displayedTip = false;
        if (transaction.getAmounts().getTipAmount() != null && transaction.getAmounts().getTipAmount() > 0) {
            content.add(makeLine("Tip:", String.format("%.2f",
                    transaction.getAmounts().getTipAmount() / 100.f)));
            displayedTip = true;
        } else if (tipAmount > 0) {
            content.add(makeLine("Tip:", String.format("%.2f",
                    tipAmount / 100.f)));
            displayedTip = true;
        }
        addLine(content);
        // grand total
        long grandTotal;
        if (order != null) {
            grandTotal = order.getAmounts().getNetTotal() + tipAmount;
        } else {
            if (transaction.getStatus() == TransactionStatus.AUTHORIZED) {
                grandTotal = transaction.getAmounts().getTransactionAmount() + tipAmount;
            } else {
                grandTotal = transaction.getAmounts().getTransactionAmount();
            }
        }
        boolean invertColor = false;
        if (transaction.getStatus() == TransactionStatus.VOIDED
                || transaction.getStatus() == TransactionStatus.REFUNDED) {
            invertColor = true;
        }
        if (displayedTip && signatureCollected) {
            content.add(makeLine("Total", String.format("%.2f",
                    (grandTotal / 100.f)), invertColor));
        } else {
            content.add(makeLine("Total", String.format("%.2f",
                    (grandTotal / 100.f)), invertColor));
            if (transaction.getStatus() == TransactionStatus.AUTHORIZED) {
                if (!displayedTip
                        && paymentSettings != null
                        && paymentSettings.isGratuityEnabled()) {
                    content.add(EMPTY_LINE);
                    content.add(makeLine("Tip:", "____________"));
                    content.add(EMPTY_LINE);
                    content.add(makeLine("Grand Total:", "____________"));
                }
                content.add(EMPTY_LINE);
                content.add(EMPTY_LINE);
                content.add(EMPTY_LINE);
                content.add(new PrintedReceiptLine("Signature:______________________________", false));
                content.add(EMPTY_LINE);
                content.add(EMPTY_LINE);
            }
        }
        if (transaction.getAmounts().getCashbackAmount() != null && transaction.getAmounts().getCashbackAmount()
                > 0) {
            content.add(makeLine("Cashback:", String.format("-%.2f",
                    transaction.getAmounts().getCashbackAmount() / 100.f)));
        }

        if (transaction.getProcessorResponse() != null && transaction.getProcessorResponse().getRemainingBalance() != null) {
            addLine(content);
            content.add(makeLine("Remaining Balance:", String.format("%.2f",
                    transaction.getProcessorResponse().getRemainingBalance() / 100.f)));
            addLine(content);
        }

        if (order != null) {
            addFundingSourceDetails(transaction, content);
        }

        content.add(EMPTY_LINE);
        content.add(EMPTY_LINE);

        receipt.setBody(content);

    }

    private void addLine(List<PrintedReceiptLine> content) {
        content.add(DOTTED_LINE);
    }

    private void addItems(List<OrderItem> items, List<PrintedReceiptLine> content) {
        Float tmpTotal = 0.f;
        Float tmpTax = 0.f;
        Float tmpDiscount = 0.f;
        if (items != null) {
            for (OrderItem item : items) {
                String qtyAndPrice;
                tmpTotal += item.getUnitPrice() * item.getQuantity();
                if (item.getTax() != null) {
                    tmpTax += item.getTax();
                }
                if (item.getDiscount() != null) {
                    tmpDiscount += item.getDiscount();
                }
                if (item.getQuantity() > 1.0) {
                    qtyAndPrice = item.getQuantity() + "@ " + String.format("%.2f",
                            item.getUnitPrice() / 100.f);
                } else {
                    qtyAndPrice = String.format("%.2f", item.getUnitPrice() / 100.f);
                }
                String label = String.format("%s", item.getName());
                content.add(makeLine(label, qtyAndPrice));
            }
            addLine(content);
        }
    }

    private void addMerchantInfo(List<PrintedReceiptLine> content) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MM/dd/yyyy h:mm a");
        sdf.setTimeZone(tz);

        String txnTime = sdf.format(cal.getTime());
        content.add(makeLine("TIME: " + txnTime, ""));
        content.add(makeLine("Merchant ID: ", getMerchantId()));
        content.add(makeLine("Terminal ID: ", getTerminalId()));
    }

    private void addDiscounts(List<Discount> discounts, List<PrintedReceiptLine> content) {
        if (discounts != null && discounts.size() > 0) {
            for (Discount discount : discounts) {
                String label = String.format("%s", discount.getCustomName());
                String discountValue = String.format("%.2f", discount.getAmount() / 100.f);
                content.add(makeLine(label, discountValue));
            }
            addLine(content);
        }
    }

    protected boolean addFundingSourceDetails(Transaction txn, List<PrintedReceiptLine> content) {
        content.add(makeLine("Transaction ID: ", PoyntUtil.convertToShortId(txn.getId().toString())));
        content.add(makeLine("Type: ",
                (FundingSourceType.CREDIT_DEBIT.equals(txn.getFundingSource().getType()) ?
                        "CARD" : txn.getFundingSource().getType().toString())));
        content.add(EMPTY_LINE);

        boolean isEmv = false;
        if (FundingSourceType.CREDIT_DEBIT.equals(txn.getFundingSource().getType())) {
            String entryMode = "Other";
            if (txn.getFundingSource().getEntryDetails().getEntryMode() != null) {
                switch (txn.getFundingSource().getEntryDetails().getEntryMode()) {
                    case KEYED:
                        entryMode = "Manual";
                        break;
                    case TRACK_DATA_FROM_MAGSTRIPE:
                        //While the FD requirement is "Magstripe', most receipt has "Swiped"
                        entryMode = "Swiped";
                        break;
                    case CONTACTLESS_MAGSTRIPE:
                        entryMode = "Contactless";
                        isEmv = true;
                        break;
                    case INTEGRATED_CIRCUIT_CARD:
                        isEmv = true;
                        entryMode = "Contact";
                        break;
                    case CONTACTLESS_INTEGRATED_CIRCUIT_CARD:
                        isEmv = true;
                        entryMode = "Contactless";
                        break;
                    default:
                        entryMode = "Other";
                }
            }
            content.add(makeLine("Entry Mode: ", entryMode));
            String cardType = CardType.OTHER.name();
            if (txn.getFundingSource().getCard() != null
                    && txn.getFundingSource().getCard().getType() != null) {
                cardType = txn.getFundingSource().getCard().getType().toString().toLowerCase();
                cardType = cardType.toUpperCase();
                content.add(makeLine("Card Type: ", cardType));
            }
            content.add(makeLine("Card Number: ", "XXXXXXXXXXXX" + txn.getFundingSource().getCard()
                    .getNumberLast4()));
            if (isEmv) {
                addEmvDetails(txn, content);
            }
            if (txn.getProcessorResponse() != null) {
                content.add(makeLine("Approval Code: ", txn.getProcessorResponse().getApprovalCode()));
            }

            if (TransactionAction.REFUND.equals(txn.getAction())) {
                content.add(makeLine(cardType + " Refund", ""));
            } else if (TransactionStatus.VOIDED.equals(txn.getStatus())) {
                content.add(makeLine(cardType + " Void", ""));
            } else if (TransactionStatus.CAPTURED.equals(txn.getStatus())) {
                content.add(makeLine(cardType + " Sale", ""));
            } else if (TransactionStatus.AUTHORIZED.equals(txn.getStatus())) {
                content.add(makeLine(cardType + " Authorized", ""));
            } else if (TransactionStatus.DECLINED.equals(txn.getStatus())) {
                content.add(makeLine(cardType + " Declined", ""));
            } else if (TransactionStatus.REFUNDED.equals(txn.getStatus())) {
                content.add(makeLine(cardType + " Refund", ""));
            } else {
                content.add(makeLine(cardType + " Sale", ""));
            }
            content.add(EMPTY_LINE);
        } else {
            // just add the transaction status
            if (TransactionAction.REFUND.equals(txn.getAction())) {
                content.add(makeLine("Refund", ""));
            } else if (TransactionStatus.VOIDED.equals(txn.getStatus())) {
                content.add(makeLine("Void", ""));
            } else if (TransactionStatus.CAPTURED.equals(txn.getStatus())) {
                content.add(makeLine("Sale", ""));
            } else if (TransactionStatus.AUTHORIZED.equals(txn.getStatus())) {
                content.add(makeLine("Authorized", ""));
            } else if (TransactionStatus.DECLINED.equals(txn.getStatus())) {
                content.add(makeLine("Declined", ""));
            } else if (TransactionStatus.REFUNDED.equals(txn.getStatus())) {
                content.add(makeLine("Refund", ""));
            } else {
                content.add(makeLine("Sale", ""));
            }
            content.add(EMPTY_LINE);
        }

        return isEmv;
    }

    protected void addEmvDetails(Transaction txn, List<PrintedReceiptLine> content) {
        if (txn.getFundingSource() != null) {
            Map<String, String> emvTags = null;
            if (txn.getProcessorResponse() != null &&
                    txn.getProcessorResponse().getEmvTags() != null) {
                emvTags = txn.getProcessorResponse().getEmvTags();
            } else {
                EMVData emvData = txn.getFundingSource().getEmvData();
                if (emvData != null) {
                    emvTags = emvData.getEmvTags();
                }
            }
            //EMVData emvData = txn.getFundingSource().getEmvData();
            if (emvTags != null) {
//                content.add(makeLine("CHIP Read", ""));
//                if (emvTags.containsKey("0x9F12")) {
//                    try {
//                        content.add(makeLine("AID: ",
//                                new String(BytesUtils.fromString(emvTags.get("0x9F12")), "UTF-8")));
//                    } catch (UnsupportedEncodingException e) {
//                        Ln.e(e);
//                    }
//                }
                if (emvTags.containsKey("0x9F06")) {
                    content.add(makeLine("AID: ", emvTags.get("0x9F06")));
                }
//                if (emvTags.containsKey("0x4F")) {
//                    content.add("AppID: " + emvTags.get("0x4F"));
//                }
            }
        }
    }

    // for credit card txns we have to print capture txn if available - otherwise fall back to auth txn
    protected void addBriefFundingSourceDetails(List<Transaction> transactionList,
                                                List<PrintedReceiptLine> content) {
        for (Transaction txn : transactionList) {
            if (FundingSourceType.CREDIT_DEBIT.equals(txn.getFundingSource().getType())) {
                // only captures, and refunds will have parentId
                // and if not we will only print auth txns that haven't been captured yet
                if (txn.getParentId() != null
                        || txn.getStatus() == TransactionStatus.AUTHORIZED) {
                    content.add(makeLine("Card: XXXXXXXXXXXX" + txn.getFundingSource()
                            .getCard()
                            .getNumberLast4(), String.format("%.2f",
                            txn.getAmounts().getTransactionAmount() / 100.f)));
                    // add tip if present
                    if (txn.getAmounts().getTipAmount() != null && txn.getAmounts().getTipAmount() > 0) {
                        content.add(makeLine("Tip",
                                String.format("%.2f",
                                        txn.getAmounts().getTipAmount() / 100.f)));
                    }
                }
            } else {
                content.add(makeLine(txn.getFundingSource().getType().toString(),
                        String.format("%.2f",
                                txn.getAmounts().getTransactionAmount() / 100.f)));
            }

            if (txn.getAmounts().getCashbackAmount() != null && txn.getAmounts().getCashbackAmount()
                    > 0) {
                content.add(makeLine("Cashback:", String.format("-%.2f",
                        txn.getAmounts().getCashbackAmount() / 100.f)));
            }
        }
    }

    @Override
    public PrintedReceipt build(boolean isPreview, Transaction transaction) {
        PrintedReceipt receipt = new PrintedReceipt();

        receipt.setBusinessName(getBusinessName());

        receipt.setHeaderImage(getBusinessReceiptHeaderImage());

        receipt.setStoreAddress(getStoreAddress());
        receipt.setPhone(getPhone());

        addTransactionDetails(transaction, receipt);
        return receipt;
    }

    @Override
    public PrintedReceipt build(boolean isPreview, Order order) {
        PrintedReceipt receipt = new PrintedReceipt();

        receipt.setBusinessName(getBusinessName());

        receipt.setHeaderImage(getBusinessReceiptHeaderImage());

        receipt.setStoreAddress(getStoreAddress());
        receipt.setPhone(getPhone());

        addOrderDetails(order, receipt);
        return receipt;
    }

    @Override
    public PrintedReceipt build(boolean isPreview, Order order, Transaction transaction, long tipAmount, boolean signatureCollected) {
        if (order == null && transaction == null) {
            return null;
        }
        PrintedReceipt receipt = new PrintedReceipt();

        receipt.setBusinessName(getBusinessName());

        receipt.setHeaderImage(getBusinessReceiptHeaderImage());

        receipt.setStoreAddress(getStoreAddress());
        receipt.setPhone(getPhone());

        addCombinedOrderTransactionDetails(order, transaction, receipt, tipAmount, signatureCollected);
        List<PrintedReceiptLine> content = new ArrayList<>();
        content.add(EMPTY_LINE);
        content.add(EMPTY_LINE);
        content.add(EMPTY_LINE);
        content.add(EMPTY_LINE);
        receipt.setFooter(content);
        return receipt;
    }
}
