package cz.mxmx.memoryanalyzer.memorywaste.cache;

import cz.mxmx.memoryanalyzer.cache.DefaultInstanceComparisonCache;
import cz.mxmx.memoryanalyzer.cache.InstanceComparisonCache;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class InstanceComparisonCacheTest {

    @Test
    public void basicCacheTest() {
        long id = 0;
        InstanceComparisonCache cache = new DefaultInstanceComparisonCache();
        InstanceDump first = new InstanceDump(id++, null);
        InstanceDump second = new InstanceDump(id, null);

        cache.cacheComparisonResult(first, second, true);
        Assert.assertEquals(cache.instancesEqual(first, second), Optional.of(true));
    }

    @Test
    public void basicCacheTestWithEmpty() {
        long id = 0;
        InstanceComparisonCache cache = new DefaultInstanceComparisonCache();
        InstanceDump first = new InstanceDump(id++, null);
        InstanceDump second = new InstanceDump(id, null);

        Assert.assertEquals(cache.instancesEqual(first, second), Optional.empty());
    }

    @Test
    public void basicCacheTestWithSwappedOrder() {
        long id = 0;
        InstanceComparisonCache cache = new DefaultInstanceComparisonCache();
        InstanceDump first = new InstanceDump(id++, null);
        InstanceDump second = new InstanceDump(id, null);

        cache.cacheComparisonResult(first, second, true);
        Assert.assertEquals(cache.instancesEqual(second, first), Optional.of(true));
    }

    @Test
    public void complexCacheTest() {
        long id = 0;
        InstanceComparisonCache cache = new DefaultInstanceComparisonCache();
        InstanceDump first = new InstanceDump(id++, null);
        InstanceDump second = new InstanceDump(id++, null);
        InstanceDump third = new InstanceDump(id++, null);
        InstanceDump fourth = new InstanceDump(id++, null);
        InstanceDump fifth = new InstanceDump(id, null);

        cache.cacheComparisonResult(first, second, true);
        cache.cacheComparisonResult(first, third, true);
        cache.cacheComparisonResult(second, third, true);

        cache.cacheComparisonResult(fourth, first, false);
        cache.cacheComparisonResult(third, fourth, false);
        cache.cacheComparisonResult(fifth, fourth, true);

        Assert.assertEquals(Optional.of(true), cache.instancesEqual(fourth, fifth));
        Assert.assertEquals(Optional.of(false), cache.instancesEqual(first, fourth));
        Assert.assertEquals(Optional.empty(), cache.instancesEqual(fourth, second));
        Assert.assertEquals(Optional.of(false), cache.instancesEqual(fourth, third));

        Assert.assertEquals(Optional.of(true), cache.instancesEqual(first, third));
        Assert.assertEquals(Optional.of(true), cache.instancesEqual(first, second));
        Assert.assertEquals(Optional.of(true), cache.instancesEqual(third, second));
    }
}
