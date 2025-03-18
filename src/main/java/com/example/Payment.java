/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>Defines a payment, which may be a single or a group.
 * </p>
 */
public  class Payment {

	private String transactionId;
    private String customerId;
    private String customerName;
    private String merchantId;
    private String amountOrBulk;               //String field
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