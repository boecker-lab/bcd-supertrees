package phylo.tree.algorithm.flipcut.bcdGraph;

import mincut.cutGraphAPI.bipartition.Cut;
import org.roaringbitmap.RoaringBitmap;


public class CompressedCut implements Cut<RoaringBitmap> {

    final RoaringBitmap toDelete;
    final long minCutValue;

    public CompressedCut(RoaringBitmap toDelete, long minCutValue) {
        this.toDelete = toDelete;
        this.minCutValue = minCutValue;
    }

    @Override
    public long minCutValue() {
        return minCutValue;
    }

    @Override
    public RoaringBitmap getCutSet() {
        return toDelete;

    }
}
