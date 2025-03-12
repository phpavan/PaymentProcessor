package com.example;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public  class Payment implements Serializable {
    private static final long serialVersionUID = 1L;
	private String transactionId;
    private String customerId;
    private String customerName;
    private String merchantId;
    private String amountOrBulk;
    private String currency;
    private long timestamp;

    public Payment(String transactionId, String customerId, String merchantId, String amountOrBulk, String currency, long timestamp) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.merchantId = merchantId;
        this.amountOrBulk = amountOrBulk;
        this.currency = currency;
        this.timestamp = timestamp;
    }

    public boolean isBulk() {
        return amountOrBulk.startsWith("[");
    }

    

    public String getTransactionId() { return transactionId; }
    public String getCustomerId() { return customerId; }
    public String getMerchantId() { return merchantId; }
    public double getAmount() { return Double.parseDouble(amountOrBulk); }
    public long getTimestamp() { return timestamp; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    @Override
    public String toString() {
        return "Payment{" +
                "transactionId='" + transactionId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", amount=" + amountOrBulk +
                ", currency='" + currency + '\'' +
                ", timestamp=" + Instant.ofEpochSecond(timestamp) +
                '}';
    }
    
    
    public List<Payment> expandBulk() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> bulkPayments = objectMapper.readValue(amountOrBulk, new TypeReference<List<Map<String, Object>>>() {});
            
            return bulkPayments.stream()
                    .map(txn -> new Payment(
                            (String) txn.get("txn_id"),
                            this.customerId,
                            this.merchantId,
                            String.valueOf(txn.get("amount")),
                            this.currency,
                            this.timestamp))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(this); // Return the original if parsing fails
        }
    }
}