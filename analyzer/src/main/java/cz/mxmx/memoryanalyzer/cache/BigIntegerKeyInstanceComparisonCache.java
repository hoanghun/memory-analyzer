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
        InstanceDump first = a.getInstanceId() < b.getInstanceId() ? a : b;
        InstanceDump second = first == a ? b : a;

        BigInteger key = applySzudziksFunction(first.getInstanceId(), second.getInstanceId());
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public void cacheComparisonResult(InstanceDump a, InstanceDump b, boolean comparisonResult) {
        InstanceDump first = a.getInstanceId() < b.getInstanceId() ? a : b;
        InstanceDump second = first == a ? b : a;

        BigInteger key = applySzudziksFunction(first.getInstanceId(), second.getInstanceId());
        cache.put(key, comparisonResult);
    }

    private BigInteger applySzudziksFunction(long a, long b) {
        BigInteger bigA = BigInteger.valueOf(a);
        BigInteger bigB = BigInteger.valueOf(b);
        BigInteger result;
        if (a >= b) {
            result = bigA.multiply(bigA).add(bigA).add(bigB);
        } else {
            result = bigA.add(bigB).multiply(bigB);
        }

        return result;
    }
}
