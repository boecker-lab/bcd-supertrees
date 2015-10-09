package flipcut.mincut.cutGraphAPI.bipartition;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by fleisch on 16.04.15.
 */
public class MutliCut<V> extends BasicCut<V> {
    private int hashCode = 0;
    private final LinkedHashSet<V> sourceSet;


    public MutliCut(LinkedHashSet<V> sourceSet, LinkedHashSet<V> sinkList, V source, V sink, long minCutValue) {
        super(sinkList, source, sink, minCutValue);
        this.sourceSet = sourceSet;
    }

    public MutliCut(LinkedHashSet<V> p1, LinkedHashSet<V> p2, long minCutValue) {
        this(p1, p2, null, null, minCutValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicCut)) return false;

        MutliCut cut = (MutliCut) o;
        if (!(hashCode == o.hashCode()))
            return false;

        if (!(
                (sourceSet.equals(cut.sourceSet) && cutSet.equals(cut.cutSet)) ||
                        (sourceSet.equals(cut.cutSet) && cutSet.equals(cut.sourceSet))
        ))
            return false;

        return true;
    }

    @Override
    public int hashCode() { //this should be a efficient hashcode, so equals perfomance is not that important if using hashmaps
        if (hashCode == 0) {
            int sourceSize = sourceSet.size();
            int sinkSize = cutSet.size();

            if (sourceSize < sinkSize) {
                hashCode = 31 * sourceSize * sourceSet.hashCode() + 67 * sinkSize * cutSet.hashCode();
            } else if (sourceSize > sinkSize) {
                hashCode = 31 * sinkSize * cutSet.hashCode() + 67 * sourceSize * sourceSet.hashCode();
            } else {
                hashCode = sinkSize + cutSet.hashCode() + sourceSize + sourceSet.hashCode();
            }
        }
        return hashCode;
    }

    public Set<V> getSourceSet() {
        return sourceSet;
    }

}
