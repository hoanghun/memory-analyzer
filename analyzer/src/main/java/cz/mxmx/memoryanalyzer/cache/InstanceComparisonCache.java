package cz.mxmx.memoryanalyzer.cache;

import cz.mxmx.memoryanalyzer.model.InstanceDump;

import java.util.Optional;

public interface InstanceComparisonCache {

    /**
     * Returns comparison stored in the cache wrapped in Optional, if comparison has not been cached
     * between two instances, returns Optional.Empty.
     * @param a instance
     * @param b instance
     * @return Optional cached comparison result between two instance a and b
     */
    Optional<Boolean> instancesEqual(InstanceDump a, InstanceDump b);

    /**
     * Stores comparison between two instances into the cache.
     * @param a instance
     * @param b instance
     * @param comparisonResult comparison result of these two instances
     */
    void cacheComparisonResult(InstanceDump a, InstanceDump b, boolean comparisonResult);

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
