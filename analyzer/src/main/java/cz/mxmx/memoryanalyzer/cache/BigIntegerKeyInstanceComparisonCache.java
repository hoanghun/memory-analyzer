package cz.mxmx.memoryanalyzer.cache;

import cz.mxmx.memoryanalyzer.model.InstanceDump;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BigIntegerKeyInstanceComparisonCache implements InstanceComparisonCache {
    private final Map<BigInteger, Boolean> cache = new HashMap<>();

    @Override
    public Optional<Boolean> instancesEqual(InstanceDump a, InstanceDump b) {
        BigInteger key = createKey(a, b);
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public void cacheComparisonResult(InstanceDump a, InstanceDump b, boolean comparisonResult) {
        BigInteger key = createKey(a, b);
        cache.put(key, comparisonResult);
    }

    private BigInteger createKey(InstanceDump a, InstanceDump b) {
        InstanceDump first = a.getInstanceId() < b.getInstanceId() ? a : b;
        InstanceDump second = first == a ? b : a;

        BigInteger bigA = BigInteger.valueOf(first.getInstanceId());
        BigInteger bigB = BigInteger.valueOf(second.getInstanceId());

        return bigA.multiply(bigA).add(bigA).add(bigB);
    }
}
