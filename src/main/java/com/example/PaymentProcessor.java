package com.example;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.map.IMap;

public class PaymentProcessor {
    public static void main(String[] args) {
        // Initialize Hazelcast Jet
    	Config config = new Config();
        JetConfig jetConfig = new JetConfig();
        jetConfig.setEnabled(true);
        config.setJetConfig(jetConfig);
        HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
        JetService jet = hz.getJet();
        
        // Load customer enrichment data into IMap
        IMap<String, String> customerData = hz.getMap("customer-info");
        customerData.put("123", "John Doe");
        customerData.put("456", "Alice Smith");
        customerData.put("789", "Bob Johnson");
        customerData.put("111", "Charlie Brown");

        // Create a pipeline
        Pipeline pipeline = buildPipeline();

        // Execute the pipeline
        jet.newJob(pipeline).join();

        // Shutdown
        hz.shutdown(); 
    }

    private static Pipeline buildPipeline() {
        Pipeline pipeline = Pipeline.create();

        // File source (CSV)
        BatchSource<String> source = Sources.filesBuilder(".")
        		.glob("payments.csv")
                .build();

        pipeline.readFrom(source)
                .map(PaymentProcessor::parsePayment)
                .flatMap(PaymentProcessor::debulkTransaction)
                .mapUsingIMap("customer-info",
                        Payment::getCustomerId,
                        (payment, customerName) -> {
                            payment.setCustomerName(customerName.toString());
                            return payment;
                        })
                .filter(PaymentProcessor::isValidTransaction)
                .groupingKey(Payment::getMerchantId)
                .aggregate(AggregateOperations.summingDouble(Payment::getAmount))
                //.writeTo(Sinks.files("../processed_payments"));
               // .writeTo(Sinks.logger());
        		.writeTo(Sinks.fromProcessor("consoleSink",
                ProcessorMetaSupplier.of(() -> new AbstractProcessor() {
                    @Override
                    protected boolean tryProcess(int ordinal, Object item) {
                        System.out.println("Processed Payments: " + item);
                        return true;
                    }
                })));

        return pipeline;
    }

    // Parses a line of CSV into a Payment object
    private static Payment parsePayment(String line) {
        try {
            CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(line));
            List<CSVRecord> records = parser.getRecords();

            if (records.isEmpty()) return null;

            CSVRecord record = records.get(0);

            String transactionId = record.get(0);
            String customerId = record.get(1);
            String merchantId = record.get(2);
            String amountOrBulk = record.get(3);  // This is now correctly read as a full JSON string
            String currency = record.get(4);
            long timestamp = Long.parseLong(record.get(5));

            return new Payment(transactionId, customerId, merchantId, amountOrBulk, currency, timestamp);
        } catch (Exception e) {
            System.err.println("Error parsing payment: " + line);
            e.printStackTrace();
            return null;
        }
    }

    // Expands bulk transactions into individual payments
    private static Traverser<Payment> debulkTransaction(Payment payment) {
        if (payment.isBulk()) {
            List<Payment> payments = payment.expandBulk();
            return Traversers.traverseIterable(payments);
        } else {
            return Traversers.singleton(payment);
        }
    }

    // Filters out fraudulent transactions
    private static boolean isValidTransaction(Payment payment) {
        return payment.getAmount() > 0;
    }
}


