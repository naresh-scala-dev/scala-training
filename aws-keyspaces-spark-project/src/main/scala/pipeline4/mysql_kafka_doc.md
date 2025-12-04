# Pipeline 4: MySQL to Kafka Streaming

## Overview
This pipeline uses Spark Structured Streaming to continuously poll a MySQL table for new orders, convert them to Avro format, and publish to a Kafka topic in real-time.

## Source System
**Database:** MySQL

**Table:** new_orders

**Schema:**
- order_id: integer (Primary Key)
- customer_id: integer
- amount: double
- created_at: timestamp

## Target System
**Message Broker:** Apache Kafka

**Topic:** orders_avro_topic

**Format:** Avro

**Schema:** OrderRecord

## Transformation Logic

### Incremental Load Strategy
The pipeline tracks the last processed order_id and only reads records with higher order_id values in subsequent iterations.

### Avro Conversion
Each row is converted to Avro format using the defined schema before publishing to Kafka.

### Polling Mechanism
The pipeline uses a rate source as a heartbeat to trigger batch processing every 5 seconds.

## Data Flow

1. Rate source triggers batch processing every 5 seconds
2. Query MySQL for new orders where order_id > last processed order_id
3. Convert timestamp to string format
4. Serialize records to Avro format
5. Publish Avro messages to Kafka topic
6. Update offset tracker with maximum order_id from current batch

## Input Table (MySQL)

### SQL Schema

```sql
CREATE TABLE new_orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    amount DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Sample Data

```sql
INSERT INTO new_orders (customer_id, amount) VALUES
(1, 150.50),
(2, 220.00),
(3, 89.99);
```

## Output Format (Kafka)

### Avro Schema

**File:** orders.avsc

```json
{
  "type": "record",
  "name": "OrderRecord",
  "namespace": "com.retail",
  "fields": [
    { "name": "order_id", "type": "int" },
    { "name": "customer_id", "type": "int" },
    { "name": "amount", "type": "double" },
    { "name": "created_at", "type": "string" }
  ]
}
```


## Expected Output

### Scenario Example

**Initial State:**
- MySQL table has 3 orders (order_id: 1, 2, 3)
- Last offset: 0

**Batch 1 (t=0s):**
- Reads orders 1, 2, 3
- Sends 3 Avro messages to Kafka
- Updates offset to 3

**Batch 2 (t=5s):**
- No new orders found (order_id > 3)
- No messages sent
- Offset remains 3

**New Order Inserted:**
- order_id=4, customer_id=5, amount=99.99 added to MySQL

**Batch 3 (t=10s):**
- Reads order 4
- Sends 1 Avro message to Kafka
- Updates offset to 4
![img_2.png](img_2.png)

### Kafka Message Format

Each message in the Kafka topic contains an Avro-serialized binary payload representing:

```json
{
  "order_id": 4,
  "customer_id": 5,
  "amount": 99.99,
  "created_at": "2025-12-04 13:03:36"
}
```

## Configuration Requirements

### MySQL Connection
- JDBC URL with host, port, and database name
- Username and password for authentication
- MySQL JDBC driver
- Table name for polling

### Kafka Connection
- Bootstrap servers address
- Topic name for publishing messages
- Kafka clients library

### Spark Settings
- Streaming trigger interval (5 seconds)
- Checkpoint location for fault tolerance
- Log level configuration

## Execution Steps

1. Create new_orders table in MySQL with sample data
2. Create Kafka topic orders_avro_topic
3. Configure MySQL and Kafka connection parameters
4. Package the Spark application with dependencies
5. Submit the streaming job
6. Monitor console output for batch processing logs
7. Verify messages in Kafka topic using consumer

## Key Learnings

### Structured Streaming
Spark Structured Streaming provides:
- Continuous processing of data streams
- Fault tolerance with checkpointing
- Exactly-once semantics
- Integration with various sources and sinks

### Incremental Processing
Using order_id as an increment column enables:
- Efficient change data capture
- Avoiding duplicate processing
- Minimal database load
- Stateful offset tracking

### Avro Format
Avro provides:
- Schema evolution support
- Compact binary serialization
- Language-agnostic data exchange
- Schema registry integration capability

### Micro-batch Processing
The trigger interval determines:
- Latency vs throughput trade-off
- Database query frequency
- Resource utilization
- Near real-time data availability

### Offset Management
Manual offset tracking demonstrates:
- Stateful stream processing
- Recovery from failures
- Preventing duplicate processing
- Custom checkpoint logic

### Real-time Pipeline Pattern
This pattern is common for:
- Change data capture from databases
- Event-driven architectures
- Real-time analytics pipelines
- Microservices integration

### Performance Considerations
- Polling interval affects database load and latency
- Batch size impacts Kafka throughput
- Order_id index improves query performance
- Network bandwidth between components
- Kafka partition strategy for scalability