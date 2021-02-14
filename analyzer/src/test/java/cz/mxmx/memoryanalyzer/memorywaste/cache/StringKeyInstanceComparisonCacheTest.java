package cz.mxmx.memoryanalyzer.memorywaste.cache;

import cz.mxmx.memoryanalyzer.cache.InstanceComparisonCache;
import cz.mxmx.memoryanalyzer.cache.StringKeyInstanceComparisonCache;

public class StringKeyInstanceComparisonCacheTest extends InstanceComparisonCacheTest {

    @Override
    public InstanceComparisonCache createCache() {
        return new StringKeyInstanceComparisonCache();
    }
}
