package phylo.tree.algorithm.flipcut.bcdGraph.edge;

import org.roaringbitmap.RoaringBitmap;

public abstract class AbstractHyperedge implements Hyperedge {
    protected final RoaringBitmap ones;
    protected long weight = 0L;

    protected AbstractHyperedge(RoaringBitmap ones) {
        this.ones = ones;
    }

    @Override
    public long getWeight() {
        return weight;
    }

    @Override
    public RoaringBitmap ones() {
        return ones;
    }
}
