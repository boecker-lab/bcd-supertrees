package mincut.cutGraphAPI.bipartition;

public class VaziraniCut<S> implements Cut<S> {
    public final int k;
    public final S cutSet;
    public long minCutValue;


    public VaziraniCut(S cutSet, long minCutValue, int k) {
        this.k = k;
        this.cutSet = cutSet;
        this.minCutValue = minCutValue;
    }

    @Override
    public long minCutValue() {
        return minCutValue;
    }

    @Override
    public S getCutSet() {
        return cutSet;
    }

    public int k() {
        return k;
    }//todo remove
}
