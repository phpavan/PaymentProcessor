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

package com.example.solution;

import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.example.Payment;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;

/**
 * <pre>
 *  *************** SOLUTION ****************
 *  *************** SOLUTION ****************
 *  *************** SOLUTION ****************
 *  *************** SOLUTION ****************
 *  *************** SOLUTION ****************
 * </pre>
 * <p>
 * Cut &amp; paste from here if stuck. Or amend {@link com.example.Main Main}
 * to use this version as is.
 * </p>
 */
public class PaymentProcessorSolution {

	/**
	 * <p>Optional
	 * </p>
	 * <p>Instead of "{@code newJob(pipeline)}" use "{@code newJob(pipeline, jobConfig)}" to
	 * supply configuration specific to that job.
	 * </p>
	 * <p>For example, you can name your jobs and this makes them easier to identify
	 * on Management Center and in the logs.
	 * </p> 
	 */
	public static JobConfig getJobConfig() {
		JobConfig jobConfig = new JobConfig();
		
		jobConfig.setName(PaymentProcessorSolution.class.getSimpleName());
		
		return jobConfig;
	}

	/**
	 * <p>
	 * Solution version of the pipeline.
	 * </p>
	 * <p>
	 * It has implementations of the missing steps from the {@link com.example.PaymentProcessorLab}
	 * example, plus some extra parts to show other capabilities.
	 * </p>
	 * 
	 * @return
	 */
	public static Pipeline buildPipeline() {
        Pipeline pipeline = Pipeline.create();

        /* Use "Sources.filesBuilder()" although this is deprecated, as it permits use of relative
         * paths to files.
         * It is superseded by "FileSources".
         */
        @SuppressWarnings("deprecation")
		BatchSource<String> source = Sources.filesBuilder(".")
			.glob("payments.csv")
			.build();
        
        /* Rather than use Sinks.logger() you can make your own sinks.
         * Here we just print it to the screen, demonstrating the method.
         */
        Sink<Entry<String, Double>> sink1 = SinkBuilder.sinkBuilder("consoleSink1", context -> new PrintWriter(System.out))
        	.receiveFn((PrintWriter printWriter, Entry<String, Double> entry) -> {
        		printWriter.println("* SINK 1 ** Processed Payments: MerchantId: " + entry.getKey() + " Total:" + entry.getValue());
        	})
        	.destroyFn(printWriter -> printWriter.close())
        	.build();
        
        /* An alternative way to build your own sink. Return true if the input has been successfully sunk.
         * If false, it will be re-attempted.
         */
        Sink<Entry<String, Double>> sink2 = Sinks.fromProcessor("consoleSink2",
        	ProcessorMetaSupplier.of(() -> new AbstractProcessor() {
        		@Override
        		protected boolean tryProcess(int ordinal, Object item) {
        			System.out.println("* SINK 2 ** Processed Payments: " + item);
        			return true;
        		}
        }));


        /* Read from the single source
         */
        BatchStage<Entry<String, Double>> aggregated = 
        pipeline
        .readFrom(source)

        /* Method "parsePayment()" uses commons-csv to turn a CSV string into a Payment object
         */
        .map(PaymentProcessorSolution::parsePayment)
                
        /* Method "debulkTransaction()" will output one or more single Payment depending if the input is a single or group Payment
         */
        .flatMap(PaymentProcessorSolution::debulkTransaction)

        /* Use the customer Id from the payment as a lookup key into an IMap.
         * Note AP is ok here, names don't change frequently and when they do atomicity isn't expected
         */
        .mapUsingIMap("customer-info",
        	Payment::getCustomerId,
        	(payment, customerName) -> {
        			payment.setCustomerName(customerName.toString());
        			return payment;
        	})
        	.setName("lookup-customer-info")  // name the step for easier visibility

        /* Discard invalid transactions.
         * Spot the mistake ? We do this after enriching with IMap lookup, when we could do it before.
         */
        .filter(PaymentProcessorSolution::isValidTransaction)
        	.setName("is-valid")

        /* Ensure all payments for the same merchant are handled by the same processor
         */
        .groupingKey(Payment::getMerchantId)
        
        /* Sum payments by key
         */
        .aggregate(AggregateOperations.summingDouble(Payment::getAmount))
        ;

        /* Output to both custom sinks (both get the same items).
         */
        aggregated.writeTo(sink1);
        aggregated.writeTo(sink2);
        ;

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


