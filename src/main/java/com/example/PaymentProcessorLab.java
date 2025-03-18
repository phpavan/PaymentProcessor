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

import java.io.StringReader;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;

/**
 * 
 * <p>
 * If you get stuck, a solution is provided, {@link com.example.solution.PaymentProcessorSolutionTest}
 * that you can copy from.
 * </p>
 */
public class PaymentProcessorLab {

	/**
	 * <p>Returns a pipeline to execute.
	 * </p>
	 * <p>
	 * This version is a simple sequence of steps. The output of
	 * step 3 is the input of step 4.
	 * </p>
	 * <p>Although the pipeline is a sequence of steps, the input
	 * is processed in parallel. Multiple clones of the pipeline
	 * are run depending how many processors are available.
	 * <p>
	 * <p>More complex pipelines can have branching on output
	 * and joining on input. Step 3 can branching, its output
	 * going to step 4A and to step 4B. Steps 4A and 4B may
	 * do different things, then be rejoined for both to be
	 * the input to step 5.
	 * </p>
	 * <p>It is really a <b>D</b>irected <b>A</b>cyclic <b>G</b>raph. 
	 * </p>
	 *
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static Pipeline buildPipeline() {
		// Create the pipeline that we will attach the stages to.
        Pipeline pipeline = Pipeline.create();

        // File source (CSV)
        BatchSource<String> source = Sources.filesBuilder(".")
        		.glob("payments.csv")
                .build();

        pipeline
        .readFrom(source)
  
        // Add a mapping stage to turn the CSV string into a Payment POJO
        //.map(.....)
                
        // A line on the CSV may represent multiple payments to be handled individually
        //.flatMap(...)

        // Use mapUsingIMap to enrich the payment with the customer's name
        //.mapUsingIMap("customer-info",
        	// function to find the map key in the payment object
        	// ?,
        	// bi-function that takes the input payment & the map value, returning the enriched payment
        	// (?, ?) -> {}

        // Filter out invalid payments.    .....   Spot the mistake ? Check the solution
        //.filter(...)

        // Group payments for the same merchant together, always sent to the same processor (CPU)
        //.groupingKey()
        
        // For the stream of payments, count the amount...
        //.aggregate()

        // Write to the system output
        .writeTo(Sinks.logger());

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


