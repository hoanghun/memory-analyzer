package cz.mxmx.memoryanalyzer.memorywaste.cache;

import cz.mxmx.memoryanalyzer.cache.BigIntegerKeyInstanceComparisonCache;
import cz.mxmx.memoryanalyzer.cache.InstanceComparisonCache;

public class BigIntegerKeyInstanceComparisonCacheTest extends InstanceComparisonCacheTest {

    @Override
    public InstanceComparisonCache createCache() {
        return new BigIntegerKeyInstanceComparisonCache();
    }
}
