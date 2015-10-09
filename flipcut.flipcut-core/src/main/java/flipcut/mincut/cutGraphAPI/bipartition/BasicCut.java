package flipcut.mincut.cutGraphAPI.bipartition;

import java.util.LinkedHashSet;

/**
 * Created by fleisch on 15.04.15.
 */
public class BasicCut<V> implements Comparable<BasicCut<V>> {
    public final static BasicCut MAX_CUT_DUMMY = new BasicCut(null, Long.MAX_VALUE);

    public final V source;
    public final V sink;
    public final long minCutValue;

    final LinkedHashSet<V> cutSet;


    public BasicCut(LinkedHashSet<V> cutSet, V source, V sink, long minCutValue) {
        this.cutSet = cutSet;
        this.source = source;
        this.sink = sink;
        this.minCutValue = minCutValue;
    }

    public BasicCut(LinkedHashSet<V> part, long minCutValue) {
        this(part, null, null, minCutValue);
    }

    public LinkedHashSet<V> getCutSet() {
        return cutSet;
    }

    @Override
    public int compareTo(BasicCut<V> o) {
        return Long.compare(minCutValue, o.minCutValue);
    }
}
