package flipcut.mincut.bipartition;

import java.util.*;

/**
 * Created by fleisch on 15.04.15.
 */
public class BasicCut<V> implements Comparable<BasicCut<V>> {
    public final static BasicCut MAX_CUT_DUMMY = new BasicCut(null,Long.MAX_VALUE);

    protected final LinkedHashSet<V> sinkSet;

    public final V source;
    public final V sink;
    public final long minCutValue;


    public BasicCut(LinkedHashSet<V> sinkSet, V source, V sink, long minCutValue) {
        this.sinkSet = sinkSet;
        this.source = source;
        this.sink = sink;
        this.minCutValue = minCutValue;
    }

    public BasicCut(LinkedHashSet<V> part, long minCutValue) {
        this(part,null,null,minCutValue);
    }

    public LinkedHashSet<V> getSinkSet() {
        return sinkSet;
    }

    @Override
    public int compareTo(BasicCut<V> o) {
        return Long.compare(minCutValue, o.minCutValue);
    }
}
