package cz.mxmx.memoryanalyzer.cache;

public interface InstanceComparisonCacheWithStatistics extends InstanceComparisonCache {

    /**
     * Returns cache hit count
     * @return cache hit count
     */
    long getCacheHitCount();

    /**
     * Returns cache misses count
     * @return cache misses count
     */
    long getCacheMissCount();
}
