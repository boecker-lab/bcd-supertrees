package mincut.cutGraphAPI.bipartition;

import java.util.LinkedHashSet;

/**
 * Created by fleisch on 15.04.15.
 */
public class STCut<V> extends AbstractBipartition<V> {
    public final static STCut MAX_CUT_DUMMY = new STCut(new LinkedHashSet(),new LinkedHashSet(), Long.MAX_VALUE);

    public final V source;
    public final V sink;
    public final long minCutValue;



    public STCut(LinkedHashSet<V> sSet,LinkedHashSet<V> tSet, V source, V sink, long minCutValue) {
        super(minCutValue,sSet,tSet);
        this.source = source;
        this.sink = sink;
        this.minCutValue = minCutValue;
    }

    public STCut(LinkedHashSet<V> sSet,LinkedHashSet<V> tSet, long minCutValue) {this(sSet,tSet, null, null, minCutValue);}



    @Override
    public long minCutValue() {
        return minCutValue;
    }
}
