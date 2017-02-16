package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition;

import java.util.LinkedHashSet;

/**
 * Created by fleisch on 15.04.15.
 */
public class STCut<V> implements Cut<V> {
    public final static STCut MAX_CUT_DUMMY = new STCut(null, Long.MAX_VALUE);

    public final V source;
    public final V sink;
    public final long minCutValue;

    final LinkedHashSet<V> cutSet;


    public STCut(LinkedHashSet<V> cutSet, V source, V sink, long minCutValue) {
        this.cutSet = cutSet;
        this.source = source;
        this.sink = sink;
        this.minCutValue = minCutValue;
    }

    public STCut(LinkedHashSet<V> part, long minCutValue) {
        this(part, null, null, minCutValue);
    }

    public LinkedHashSet<V> getCutSet() {
        return cutSet;
    }

    @Override
    public long minCutValue() {
        return minCutValue;
    }
}
