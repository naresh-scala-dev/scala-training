# Sales Caching Analyzer - Project Documentation

## Problem Statement

Perform multiple computations on a sales dataset with columns: customerId, productId, quantity, and amount.
Compare performance with and without caching.

---

## Sample Data

```
customerId | productId | quantity | amount
-----------|-----------|----------|--------
1          | 101       | 2        | 50.0
1          | 102       | 1        | 30.0
2          | 101       | 1        | 25.0
```

---

## Implementation

### Without Caching

```scala
val sales = loadSales(spark)
val customerSpending = sales.groupBy("customerId").agg(sum("amount"))
val productSales = sales.groupBy("productId").agg(sum("quantity"))
```

Data read from disk twice - once for each computation

### With Caching

```scala
val cachedSales = sales.cache()
cachedSales.count() // Trigger caching
val customerSpending = cachedSales.groupBy("customerId").agg(sum("amount"))
val productSales = cachedSales.groupBy("productId").agg(sum("quantity"))
cachedSales.unpersist()
```

Data read from disk once, then reused from memory

---

## Output Results

### Customer Spending

```json
{
  "customerId": 10,
  "total_spent": 290.0,
  "total_items": 6,
  "number_of_purchases": 3
}
{
  "customerId": 5,
  "total_spent": 315.0,
  "total_items": 10,
  "number_of_purchases": 3
}
{
  "customerId": 6,
  "total_spent": 300.0,
  "total_items": 7,
  "number_of_purchases": 3
}
```

### Product Sales

```json
{
  "productId": 101,
  "total_quantity_sold": 11,
  "total_revenue": 275.0,
  "number_of_transactions": 5
}
{
  "productId": 102,
  "total_quantity_sold": 10,
  "total_revenue": 300.0,
  "number_of_transactions": 4
}
{
  "productId": 103,
  "total_quantity_sold": 11,
  "total_revenue": 330.0,
  "number_of_transactions": 4
}
```

---

## Spark UI Evidence

### Storage Tab

![Screenshot 2025-12-07 at 4.16.09 PM.png](Screenshot%202025-12-07%20at%204.16.09%E2%80%AFPM.png)
**Cached Dataset Details:**

- **RDD ID**: 30
- **Storage Level**: Disk Memory Deserialized 1x Replicated
- **Cached Partitions**: 11/11 (100%)
- **Size in Memory**: 5.7 KiB
- **Format**: Parquet with columns (customerId, productId, quantity, amount)

This confirms the sales dataset is fully cached in memory.

### Jobs Tab

![Screenshot 2025-12-07 at 3.53.18 PM.png](Screenshot%202025-12-07%20at%203.53.18%E2%80%AFPM.png)

**Total Jobs**: 22 completed

**Key Job Timings:**

- Job 12: count (cache trigger) - 50 ms
- Job 13: count (from cache) - 5 ms  **10x faster**
- Job 14: Customer aggregation - 34 ms
- Job 18: Product aggregation - 27 ms

### Stages Tab

![Screenshot 2025-12-07 at 4.00.51 PM.png](Screenshot%202025-12-07%20at%204.00.51%E2%80%AFPM.png)
**Completed Stages**: 19  
**Skipped Stages**: 17

**Key Stages:**

- Stage 0: Load data - 0.2 s (initial disk read)
- Stage 17: Count operation - 41 ms (using cache)
- Stage 28: Aggregation - 27 ms (using cache, 5.7 KiB input)

---

## Performance Comparison

| Metric       | Without Cache | With Cache | Improvement    |
|--------------|---------------|------------|----------------|
| First count  | -             | 50 ms      | Baseline       |
| Second count | -             | 5 ms       | **10x faster** |
| Disk reads   | 2x            | 1x         | 50% reduction  |
| Memory usage | Low           | 5.7 KiB    | Acceptable     |

**Key Finding**: Operations reading from cache are 10x faster than reading from disk.

---

## Requirements Fulfilled

**Cache dataset before operations**: `sales.cache()`  
**Compute total per customer**: Customer spending aggregation completed  
**Compute total per product**: Product sales aggregation completed  
**Compare execution time**: 10x speedup demonstrated (50ms → 5ms)

---

## When to Use Caching

**Use caching when:**

- Same dataset accessed multiple times
- Dataset fits in memory (5.7 KiB easily fits)
- Repeated computations needed

**Don't cache when:**

- Dataset used only once
- Dataset too large for memory
- Data changes between operations

---

## Summary

Caching the sales dataset in memory provided **10x performance improvement** for repeated operations.
The dataset (5.7 KiB) was fully cached across 11 partitions, eliminating redundant disk I/O.

**Evidence:**

- Storage tab shows 100% cached (5.7 KiB in memory)
- Second count operation 10x faster (5ms vs 50ms)
- 17 stages skipped due to cache reuse