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

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.example.solution.PaymentProcessorSolution;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.map.IMap;
import com.hazelcast.sql.SqlRow;


@SuppressWarnings("unused")
public class Main {
    public static void main(String[] args) throws Exception {
    	Config config = new Config();
    	
    	// Enable pipeline processing
    	config.getJetConfig().setEnabled(true);
    	
        HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
        
        // Load customer enrichment data into IMap
        IMap<String, String> customerData = hz.getMap("customer-info");
        customerData.put("123", "John Doe");
        customerData.put("456", "Alice Smith");
        customerData.put("789", "Bob Johnson");
        customerData.put("111", "Charlie Brown");

        // Define mapping for SQL, eg. to run from Management Center
        String mapping = "CREATE OR REPLACE MAPPING \"customer-info\""
        		+ " TYPE IMap "
        		+ " OPTIONS ('keyFormat'='varchar', 'valueFormat'='varchar')";
        hz.getSql().execute(mapping);
        System.out.println("");
        
        // Demonstrate SQL
        String query = "SELECT * FROM \"customer-info\"";
        System.out.println(query);
        Iterator<SqlRow> sqlRowIterator = hz.getSql().execute(query).iterator();
        while (sqlRowIterator.hasNext()) {
        	System.out.println(sqlRowIterator.next());
        }
        System.out.println("");
        
        // Create a pipeline
        Pipeline pipeline = PaymentProcessorLab.buildPipeline();

        Job job = hz.getJet().newJob(pipeline);
        //Job job = hz.getJet().newJob(PaymentProcessorSolution.buildPipeline(), PaymentProcessorSolution.getJobConfig());
        
        System.out.println("");
        System.out.println(job + " status=" + job.getStatus());
        System.out.println("");

        // Wait for the batch pipeline job to process all input. (Don't do this for streaming pipelines, input is infinite).
        job.join();

        System.out.println("");
        System.out.println(job + " status=" + job.getStatus());
        System.out.println("");

        // Stay running for a while in case you wish to view from Management Center
        System.out.println("-- Sleeping");
        TimeUnit.MINUTES.sleep(5L);
        System.out.println("--");
        hz.shutdown(); 
    }

}

