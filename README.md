# Hazelcast Payment Processor

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

## Expected Console Output
When you run the application, the processed payments will be displayed in the console, showing individual transactions and aggregated totals.

## Contributing
Feel free to fork the repo, make improvements, and submit pull requests!

## License
This project is licensed under the MIT License.

## Contact
For any questions or issues, reach out via the GitHub repository: [PaymentProcessor](https://github.com/phpavan/PaymentProcessor).
