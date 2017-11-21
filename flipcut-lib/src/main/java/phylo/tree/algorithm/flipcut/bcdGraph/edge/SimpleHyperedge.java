package phylo.tree.algorithm.flipcut.bcdGraph.edge;

import org.roaringbitmap.RoaringBitmap;

public class SimpleHyperedge extends AbstractHyperedge {
    protected final RoaringBitmap zeroes;

    public SimpleHyperedge(RoaringBitmap ones, RoaringBitmap zeroes, long weight) {
        super(ones);
        this.zeroes = zeroes;
        this.weight = weight;
    }

    public SimpleHyperedge(RoaringBitmap ones, RoaringBitmap zeroes) {
        super(ones);
        this.zeroes = zeroes;
    }

    @Override
    public boolean removeSemiuniversals(RoaringBitmap taxaInGraph) {
        return !RoaringBitmap.intersects(zeroes, taxaInGraph);
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }
}
