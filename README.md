# Hazelcast Simple Payment Processor

## Overview
This project processes payments using **Hazelcast Jet**. It reads payments from a `payments.csv` file, processes them through a Hazelcast Jet pipeline, debulks bulk transactions, aggregates payments by merchant ID, and prints the processed payments to the console.

## Features
- Reads payments from a CSV file.
- Handles bulk payments by debulking them into individual transactions.
- Enriches customer data using Hazelcast **IMap**.
- Aggregates payments based on **Merchant ID**.
- Filters out invalid transactions.
- Writes the processed payments to the **console**.

## Prerequisites
- **Java 8 or later** installed
- **Apache Maven** installed
- **Git** installed

## Cloning the Repository
```sh
 git clone https://github.com/phpavan/PaymentProcessor.git
 cd PaymentProcessor
```

## Building the Project
```sh
mvn clean package
```

## Running the Application
```sh
java -jar target/hazelcast-payment-processor-1.0-SNAPSHOT.jar
```

## Input File Format (`payments.csv`)
The application processes a CSV file with the following format:
```csv
TXN001,123,M001,100,USD,1712551800
TXN002,456,M002,200,EUR,1712551860
TXN006,111,M001,150,USD,1712551980
TXN007,789,M003,"[{\"txn_id\":\"TXN004\",\"amount\":50},{\"txn_id\":\"TXN005\",\"amount\":30}]",USD,1712551920
TXN003,123,M002,100,USD,1712551800
TXN004,456,M003,200,EUR,1712551860
TXN005,111,M001,150,USD,1712551980
```

## Hazelcast Payment Processing Pipeline
The **Hazelcast Jet Pipeline** follows these steps:

1. **Read Payments from CSV**: Reads payment transactions from `payments.csv` using Hazelcast Jet's file source.
2. **Parse Payments**: Converts CSV records into `Payment` objects.
3. **Debulk Transactions**: Identifies bulk payments and expands them into individual transactions.
4. **Enrich with Customer Data**: Uses Hazelcast **IMap** to map customer IDs to their names.
5. **Filter Invalid Transactions**: Removes payments with invalid or zero amounts.
6. **Aggregate Payments by Merchant**: Groups payments by **Merchant ID** and sums the transaction amounts.
7. **Write Output to Console**: Prints the processed payments to the console.

## Hazelcast API Usage
- **`HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);`** – Creates a new Hazelcast instance.
- **`IMap<String, String> customerData = hz.getMap("customer-info");`** – Stores customer data in a distributed map.
- **`pipeline.readFrom(Sources.files("payments.csv"))`** – Reads the CSV file as input.
- **`.map(PaymentProcessor::parsePayment)`** – Converts CSV lines into `Payment` objects.
- **`.flatMap(PaymentProcessor::debulkTransaction)`** – Expands bulk transactions.
- **`.mapUsingIMap("customer-info", Payment::getCustomerId, (payment, customerName) -> {...})`** – Enriches transactions with customer names.
- **`.groupingKey(Payment::getMerchantId).aggregate(AggregateOperations.summingDouble(Payment::getAmount))`** – Aggregates payments by merchant.
- **`.writeTo(Sinks.fromProcessor("consoleSink", ProcessorMetaSupplier.of(...)))`** – Outputs results to the console.

## Expected Console Output
When you run the application, the processed payments will be displayed in the console, showing individual transactions and aggregated totals.

## Contributing
Feel free to fork the repo, make improvements, and submit pull requests!

## License
This project is licensed under the MIT License.

## Contact
For any questions or issues, reach out via the GitHub repository: [PaymentProcessor](https://github.com/phpavan/PaymentProcessor).

