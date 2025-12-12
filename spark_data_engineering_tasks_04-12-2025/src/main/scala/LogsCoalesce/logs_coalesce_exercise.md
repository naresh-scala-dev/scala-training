# Exercise 4: Coalesce - Solution Documentation

## Overview

This exercise demonstrates how to use Spark's `coalesce()` function to optimize write performance when dealing with
filtered datasets that have significantly fewer records than the original.

## Problem Statement

After filtering a large dataset of logs, the resulting dataset contains far fewer records. Writing this filtered dataset
using the original 20 partitions creates 42 small files, reducing I/O performance and creating unnecessary overhead.

## Solution Approach

### Step 1: Create and Filter Large Dataset

- Generated 5 million log records
- Partitioned into 20 initial partitions
- Filtered for ERROR level logs only
- Reduced to 1.25 million records (25% of original)

### Step 2: Compare Write Operations

**Without Coalesce:**

```scala
filteredDF.write.mode("overwrite").parquet(AppConfig.paths.logCoalesce_outputBefore)
```

**With Coalesce:**

```scala
filteredDF.coalesce(4).write.mode("overwrite").parquet(AppConfig.paths.logCoalesce_outputAfter)
```

### Step 3: Performance Metrics

| Metric       | Without Coalesce | With Coalesce | Improvement   |
|--------------|------------------|---------------|---------------|
| Output Files | 42               | 10            | 76% reduction |
| Write Time   | 688 ms           | 430 ms        | 258 ms faster |
| Partitions   | 20               | 4             | 80% reduction |

## Key Learnings

**Coalesce vs Repartition:**

- **Coalesce**: Reduces partitions without full shuffle, ideal when decreasing partition count
- **Repartition**: Triggers a shuffle operation, more expensive, use only when increasing partitions

## Results

File count reduced from 42 to 10
Write performance improved by 37.5%
Optimized partition layout for efficient downstream reads

## Console Output

```
Generating five million log records
Total records generated: 5000000
Initial partitions: 20
Records after filtering: 1250000
Partitions after filtering: 20
File count before coalesce: 42
File count after coalesce: 10
Write time before coalesce: 688
Write time after coalesce: 430
Coalesce reduces partitions without shuffle
Repartition uses shuffle and is more expensive
```

![Screenshot 2025-12-07 at 7.11.21 PM.png](Screenshot%202025-12-07%20at%207.11.21%E2%80%AFPM.png)
![Screenshot 2025-12-07 at 7.11.06 PM.png](Screenshot%202025-12-07%20at%207.11.06%E2%80%AFPM.png)

## Conclusion

The solution successfully demonstrates that using `coalesce()` optimizes write performance and reduces file
fragmentation without the overhead of a full shuffle operation.