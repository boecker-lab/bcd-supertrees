package mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 28.02.17.
 */

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class AbstractBipartition<V> extends BasicCut<V> {
    protected boolean hashCached = false;
    private int hashCache = 0;

    protected final LinkedHashSet<V> tSet;


    public LinkedHashSet<V> getsSet() {
        return getCutSet();
    }

    public LinkedHashSet<V> gettSet() {
        return tSet;
    }

    public AbstractBipartition(long minCutValue, LinkedHashSet<V> sSet, LinkedHashSet<V> tSet) {
        super(sSet,minCutValue);
        this.tSet = tSet;
    }

    @Override //todo maybe on equals just hashcode for performance reasons
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractBipartition)) return false;

        AbstractBipartition<?> that = (AbstractBipartition<?>) o;

        if (minCutValue != that.minCutValue) return false;
        return tSet.equals(that.tSet) && cutSet.equals(that.cutSet) || tSet.equals(that.cutSet) && cutSet.equals(that.tSet);
    }


    @Override
    public int hashCode() {
        if (!hashCached) {
            int result = (int) (minCutValue ^ (minCutValue >>> 32));
            hashCache = 31 * result + cutSet.hashCode() + tSet.hashCode();
            hashCached = true;
        }
        return hashCache;
    }
}
