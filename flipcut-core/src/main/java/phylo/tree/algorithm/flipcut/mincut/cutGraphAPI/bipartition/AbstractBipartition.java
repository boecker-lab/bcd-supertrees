package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 28.02.17.
 */

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class AbstractBipartition<V> implements Cut<V> {
    protected boolean hashCached = false;
    private int hashCache = 0;

    protected final long minCutValue;
    protected final LinkedHashSet<V> sSet;
    protected final LinkedHashSet<V> tSet;


    @Override
    public long minCutValue() {
        return minCutValue;
    }

    @Override
    public LinkedHashSet<V> getCutSet() {
        return getsSet();
    }

    public LinkedHashSet<V> getsSet() {
        return sSet;
    }

    public LinkedHashSet<V> gettSet() {
        return tSet;
    }

    public AbstractBipartition(long minCutValue, LinkedHashSet<V> sSet, LinkedHashSet<V> tSet) {
        this.minCutValue = minCutValue;
        this.sSet = sSet;
        this.tSet = tSet;
    }

    @Override //todo maybe on equals just hashcode for performance reasons
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractBipartition)) return false;

        AbstractBipartition<?> that = (AbstractBipartition<?>) o;

        if (minCutValue != that.minCutValue) return false;
        return tSet.equals(that.tSet) && sSet.equals(that.sSet) || tSet.equals(that.sSet) && sSet.equals(that.tSet);
    }


    @Override
    public int hashCode() {
        if (!hashCached) {
            int result = (int) (minCutValue ^ (minCutValue >>> 32));
            hashCache = 31 * result + sSet.hashCode() + tSet.hashCode();
            hashCached = true;
        }
        return hashCache;
    }


}
