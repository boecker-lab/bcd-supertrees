package phylo.tree.algorithm.flipcut.bcdGraph.edge;

import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;

public interface Hyperedge {

    boolean removeSemiuniversals(RoaringBitmap taxaInGraph);

    default boolean isInfinite() {
        assert getWeight() <= CutGraphCutter.getInfinity();
        return getWeight() == CutGraphCutter.getInfinity();
    }

    long getWeight();
    RoaringBitmap ones();
}
