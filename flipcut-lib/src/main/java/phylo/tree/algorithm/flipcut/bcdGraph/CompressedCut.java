package phylo.tree.algorithm.flipcut.bcdGraph;

import mincut.cutGraphAPI.bipartition.Cut;
import org.roaringbitmap.RoaringBitmap;

import java.util.LinkedHashSet;


public class CompressedCut implements Cut<RoaringBitmap> {

    LinkedHashSet<Integer> toDelete;

    @Override
    public long minCutValue() {
        return 0;
    }

    @Override
    public RoaringBitmap getCutSet() {
        return RoaringBitmap.bitmapOf(); //todo finish

    }
}
