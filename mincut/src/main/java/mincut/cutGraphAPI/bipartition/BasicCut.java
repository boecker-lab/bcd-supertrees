package mincut.cutGraphAPI.bipartition;

import java.util.LinkedHashSet;

public class BasicCut<V> implements Cut<LinkedHashSet<V>> {
    protected final LinkedHashSet<V> cutSet;
    protected final long minCutValue;


    public BasicCut(LinkedHashSet<V> cutSet, long mincutValue) {
        this.cutSet = cutSet;
        this.minCutValue = mincutValue;
    }

    @Override
    public long minCutValue() {
        return minCutValue;
    }

    @Override
    public LinkedHashSet<V> getCutSet() {
        return cutSet;
    }
}
