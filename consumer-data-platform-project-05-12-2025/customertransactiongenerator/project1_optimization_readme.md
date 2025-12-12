# PROJECT 1: Customer Transaction Generator

## Overview

A high-performance Scala application using Apache Spark to generate synthetic customer, product, and transaction data and load it into MySQL database with minimal latency and maximum throughput.

**Key Optimizations:**
- Single Spark session and single database connection (no multiple connections)
- Batch processing with configurable batch sizes
- Parallel data generation and loading
- Connection pooling and resource management
- Comprehensive logging and metrics

---

## Architecture Improvements

### Before (Problem)
```
Application.main()
├─ Generate Customers → Write to MySQL (Connection 1)
├─ Generate Products → Write to MySQL (Connection 2)
└─ Generate Transactions → Write to MySQL (Connection 3)
```
**Issue**: 3 separate JDBC connections created = 3x network overhead

### After (Solution)
```
Application.main()
├─ Create Single Spark Session
├─ Create Single DataWriter (reuses connection pool)
├─ Generate Customers → Write (Connection pooled)
├─ Generate Products → Write (Connection pooled)
├─ Generate Transactions → Write (Connection pooled)
└─ Verify Data
```
**Benefit**: Connection pooling = efficient resource utilization

---

## Key Components

### 1. SparkSessionFactory.scala (NEW)
```scala
object SparkSessionFactory {
  - Singleton pattern for Spark session
  - Creates session once, reuses for all operations
  - Proper cleanup on shutdown
  - Performance configs applied at creation
}
```

**Benefits:**
- No session recreation overhead
- Consistent performance tuning across all operations
- Proper resource cleanup

### 2. DataWriter.scala (NEW)
```scala
class DataWriter(spark: SparkSession, appConfig: AppConfig) {
  - Single connection pool for all table writes
  - Repartitioning based on performance config
  - Automatic throughput calculation
  - Table verification post-write
}
```

**Benefits:**
- Reuses connection pool across all writes
- Configurable batching
- Built-in verification

### 3. Generators (Updated)
- Added logging at 500/50/10000 record intervals
- Duration tracking for each generation phase
- Performance metrics on completion

### 4. DataValidator.scala (Enhanced)
- Validates all three datasets
- Counts invalid records with logging
- Shows invalid records (first 10 only)
- Clear success/failure messages

### 5. Application.scala (Refactored)
```scala
object Application {
  - Single Spark session creation
  - Single DataWriter instance
  - Sequential but optimized writes
  - Proper error handling and cleanup
  - Detailed step-by-step logging
}
```

---

## Performance Tuning Configuration

### application.conf

```conf
performance {
  # JDBC & MySQL Tuning
  bulk-batch-size = 8000           # Rows per JDBC batch
  mysql-batch-insert-size = 5000   # MySQL insert batch
  mysql-fetch-size = 1000          # Result set fetch size
  mysql-connection-timeout = 30000 # Connection timeout ms
  
  # Spark Tuning
  partitions = 8                   # RDD/DataFrame partitions
  shuffle-partitions = 8           # Shuffle operation partitions
  executor-cores = 4               # Cores per executor
  executor-memory = "2g"           # Executor heap
  driver-memory = "2g"             # Driver heap
}
```

### MySQL Connection URL Optimizations

```
rewriteBatchedStatements=true     # Combine multiple INSERTs
allowMultiQueries=true            # Allow batch queries
connectionTimeout=30000           # 30 second timeout
serverTimezone=UTC                # Timezone specification
```

### Spark Configurations Applied

```
spark.sql.shuffle.partitions         # Control shuffle parallelism
spark.executor.cores                 # Core allocation
spark.executor.memory                # Memory allocation
spark.shuffle.compress               # Enable compression
spark.io.compression.codec           # Snappy compression
spark.sql.adaptive.enabled           # Adaptive query execution
spark.sql.adaptive.coalescePartitions # Partition coalescing
spark.sql.adaptive.skewJoin.enabled  # Skew join handling
```

---

## Execution Flow

```
1. Load Configuration (from application.conf)
   └─ All values externalized, no hardcoding

2. Create Spark Session (Single instance)
   └─ Performance configs applied upfront
   └─ Log level set to WARN for Spark internals

3. Create DataWriter (Single instance)
   └─ Connection pool initialized
   └─ Properties configured

4. Generate Data (Sequential)
   ├─ Generate Customers (5000 records)
   │  └─ Logs at every 500 records
   │  └─ Total duration logged
   │
   ├─ Generate Products (500 records)
   │  └─ Logs at every 50 records
   │  └─ Total duration logged
   │
   └─ Generate Transactions (200K-500K records)
      └─ Logs at every 10,000 records
      └─ Total duration logged

5. Validate Data
   ├─ Validate Customers
   ├─ Validate Products
   └─ Validate Transactions
   └─ Abort if validation fails (strict-mode)

6. Load to MySQL (Sequential)
   ├─ Customers (reuses connection pool)
   │  └─ Throughput calculation
   │  └─ Record count verification
   │
   ├─ Products (reuses connection pool)
   │  └─ Throughput calculation
   │  └─ Record count verification
   │
   └─ Transactions (reuses connection pool)
      └─ Throughput calculation
      └─ Record count verification

7. Verify Database
   ├─ Count customers in DB
   ├─ Count products in DB
   └─ Count transactions in DB

8. Cleanup & Exit
   └─ Spark session stopped
   └─ All resources released
```

---

## How Single Connection Works

### Connection Pooling

```scala
// DataWriter creates one Properties object with credentials
val properties = new Properties()
properties.setProperty("user", username)
properties.setProperty("password", password)
properties.setProperty("batchsize", "5000")

// Each write call reuses pool
dataframe.write.jdbc(url, "customers", properties)  // Connection 1
dataframe.write.jdbc(url, "products", properties)   // Reuses Pool
dataframe.write.jdbc(url, "transactions", properties) // Reuses Pool
```

### MySQL JDBC Connection Pool

```
First Write:
  - Creates connection pool
  - Initializes 10 connections (pool-size)
  
Subsequent Writes:
  - Returns connection from pool
  - No new connections created
  - Connections returned to pool after write
  
Result:
  - Minimal overhead
  - Efficient resource usage
  - Faster overall execution
```

---

## Logging Format

All logs include timestamp, level, class, and message:

```
2024-01-15 10:30:45.123 INFO  config.ConfigLoader - Loading application configuration from application.conf
2024-01-15 10:30:45.234 DEBUG config.ConfigLoader - MySQL config loaded host retail-mysql... database ecommerce
2024-01-15 10:30:46.100 INFO  database.SparkSessionFactory - Creating new Spark session with performance tuning
2024-01-15 10:30:47.500 INFO  generator.CustomerDataGenerator - Generating customers count 5000
2024-01-15 10:30:47.650 DEBUG generator.CustomerDataGenerator - Generated customer index 500
2024-01-15 10:30:48.123 INFO  generator.CustomerDataGenerator - Customer generation completed count 5000 duration milliseconds 1023
2024-01-15 10:30:48.200 INFO  validation.DataValidator - Validating customers count 5000
2024-01-15 10:30:48.250 INFO  validation.DataValidator - Customer validation passed all records are valid
2024-01-15 10:30:48.300 INFO  database.DataWriter - Starting write to MySQL table customers
2024-01-15 10:30:48.350 DEBUG database.DataWriter - DataFrame row count for table customers is 5000
2024-01-15 10:30:52.100 INFO  database.DataWriter - Successfully written to MySQL table customers rows 5000 duration milliseconds 3800 throughput records per second 1315
```

---

## Performance Metrics

### Expected Execution Times (5000 customers, 500 products, 200K transactions)

```
Customer Generation:        2-3 seconds
Product Generation:         1 second
Transaction Generation:     30-45 seconds
Data Validation:            2-3 seconds
Customer Write:             3-5 seconds (throughput: 1000-1500 rec/sec)
Product Write:              1-2 seconds (throughput: 250-500 rec/sec)
Transaction Write:          45-60 seconds (throughput: 3300-4400 rec/sec)
Database Verification:      5-10 seconds
Total Duration:             95-130 seconds (~2 minutes)
```

### Throughput Calculation

```
Throughput = Total Records / (Duration in milliseconds / 1000)

Example:
  5000 customers in 3800ms
  = 5000 / 3.8 = 1315 records per second
```

---

## Configuration Examples

### For Development (Fast Generation)

```conf
generation {
  customer-count = 1000
  product-count = 100
  transaction-count = 10000
}
```

### For Testing (Medium Load)

```conf
generation {
  customer-count = 5000
  product-count = 500
  transaction-count = 200000
}
```

### For Production (Large Load)

```conf
generation {
  customer-count = 100000
  product-count = 5000
  transaction-count = 10000000
}

performance {
  partitions = 16
  executor-cores = 8
  executor-memory = "4g"
  driver-memory = "4g"
}
```

---

## Database Verification

### Post-Load Checks

```sql
SELECT COUNT(*) FROM customers;     -- Should be 5000
SELECT COUNT(*) FROM products;      -- Should be 500
SELECT COUNT(*) FROM transactions;  -- Should be 200000
```

### Sample Queries

```sql
-- Verify customer data
SELECT * FROM customers LIMIT 5;

-- Verify product categories
SELECT DISTINCT category FROM products;

-- Verify transaction amounts
SELECT MIN(amount), MAX(amount), AVG(amount) FROM transactions;

-- Verify date ranges
SELECT MIN(txn_timestamp), MAX(txn_timestamp) FROM transactions;
```

---

## Error Handling

### Configuration Errors
```
Log: Failed to load MySQL configuration
Action: Check application.conf syntax and values
```

### Connection Errors
```
Log: Failed to connect to MySQL database
Action: Verify MySQL is running and credentials are correct
```

### Validation Errors
```
Log: Data validation failed, aborting execution
Action: Check invalid records in logs, fix generator or disable strict-mode
```

### Memory Errors
```
Log: OutOfMemoryError in Spark
Action: Increase executor-memory and driver-memory in config
```

---

## Advantages of This Approach

1. **Single Spark Session**
   - No session recreation overhead
   - Consistent performance tuning
   - Proper cleanup

2. **Connection Pooling**
   - No connection creation overhead per table
   - Efficient resource usage
   - Automatic connection reuse

3. **Configurable Performance**
   - All tuning parameters external to code
   - Easy to adjust for different scales
   - No code recompilation needed

4. **Comprehensive Logging**
   - Track every step of execution
   - Performance metrics included
   - Debug information available

5. **Validation Before Load**
   - Catch errors early
   - Fail fast if data invalid
   - Prevents corrupted DB inserts

6. **Verification After Load**
   - Confirm all data written
   - Throughput calculation
   - Final count check

---

## Running the Application

### Build
```bash
sbt clean compile package
```

### Run
```bash
sbt run
```
### Performance Summary
Application logs final metrics:
- Total customers generated
- Total products generated
- Total transactions generated
- Duration for each phase
- Throughput for each load
- Final database counts

### Database Size Check
```sql
SELECT 
  table_name,
  ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.tables
WHERE table_schema = 'ecommerce';
```

---

## Troubleshooting

### Slow Performance

**Check 1: Partition Size**
```
If partitions too small → increase performance.partitions
If partitions too large → decrease performance.partitions
```

**Check 2: Batch Size**
```
If throughput < 1000 rec/sec → increase bulk-batch-size
If memory issues → decrease bulk-batch-size
```

**Check 3: MySQL Configuration**
```
Check max_allowed_packet setting
Check connection limit
Check slow query log
```

### Out of Memory

**Solution:**
```
executor-memory = "4g"
driver-memory = "2g"
```

### Connection Timeout

**Solution:**
```
mysql-connection-timeout = 60000  # Increase to 60 seconds
```

---

## Next Steps

After successful completion:

1. **Verify Data Quality**
   - Check row counts
   - Verify data relationships
   - Check date ranges

2. **Move to Project 2**
   - Pipeline A: Customer Profiles
   - Use generated customer data

3. **Run Pipelines**
   - Pipeline B: Daily Summaries
   - Pipeline C: Event Archival

4. **Deploy API**
   - Play Framework API Service
   - Query generated data

---

## Files Included

```
├── src/main/scala/
│   ├── Application.scala              # Main entry point
│   ├── config/
│   │   ├── AppConfig.scala           # Configuration case classes
│   │   └── ConfigLoader.scala        # Configuration loader
│   ├── database/
│   │   ├── SparkSessionFactory.scala # Single Spark session
│   │   └── DataWriter.scala          # Single data writer
│   ├── generator/
│   │   ├── CustomerDataGenerator.scala
│   │   ├── ProductDataGenerator.scala
│   │   └── TransactionDataGenerator.scala
│   ├── models/
│   │   ├── Customer.scala
│   │   ├── Product.scala
│   │   └── Transaction.scala
│   ├── utils/
│   │   └── DateUtils.scala
│   └── validation/
│       └── DataValidator.scala
├── src/main/resources/
│   ├── application.conf              # Configuration file
│   └── logback.xml                   # Logging configuration
├── build.sbt                          # Build configuration
└── README.md                          # This file
```

---

## Performance Tuning Summary

```
Key Tunings Applied:
✓ Single Spark session (no recreation)
✓ Single DataWriter (connection pooling)
✓ Batch inserts (bulk-batch-size = 8000)
✓ Partition parallelism (partitions = 8)
✓ Snappy compression (io.compression.codec)
✓ Adaptive query execution enabled
✓ Shuffle partition optimization
✓ Skew join handling
✓ MySQL rewriteBatchedStatements enabled
✓ MySQL allowMultiQueries enabled

Result:
Expected Throughput: 3000-4000 records/second
Expected Total Time: 2 minutes for 500K transactions
```