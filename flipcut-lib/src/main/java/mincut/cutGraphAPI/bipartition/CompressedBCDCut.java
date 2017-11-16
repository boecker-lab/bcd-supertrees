package mincut.cutGraphAPI.bipartition;

import org.roaringbitmap.RoaringBitmap;


public class CompressedBCDCut implements Cut<RoaringBitmap> {

    protected RoaringBitmap toDelete;
    protected long minCutValue;

    public CompressedBCDCut(RoaringBitmap toDelete, long minCutValue) {
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
