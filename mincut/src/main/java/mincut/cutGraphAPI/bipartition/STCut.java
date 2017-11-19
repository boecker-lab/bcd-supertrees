package mincut.cutGraphAPI.bipartition;

import java.util.LinkedHashSet;

/**
 * Created by fleisch on 15.04.15.
 */
public class STCut<V> extends AbstractBipartition<V> implements STCutInterface<V, LinkedHashSet<V>> {
    public final static STCut MAX_CUT_DUMMY = new STCut(new LinkedHashSet(), new LinkedHashSet(), Long.MAX_VALUE);

    public final V source;
    public final V sink;


    public STCut(LinkedHashSet<V> sSet, LinkedHashSet<V> tSet, V source, V sink, long minCutValue) {
        super(minCutValue, sSet, tSet);
        this.source = source;
        this.sink = sink;
    }

    public STCut(LinkedHashSet<V> sSet, LinkedHashSet<V> tSet, long minCutValue) {
        this(sSet, tSet, null, null, minCutValue);
    }

    @Override
    public V source() {
        return source;
    }

    @Override
    public V target() {
        return target();
    }
}
