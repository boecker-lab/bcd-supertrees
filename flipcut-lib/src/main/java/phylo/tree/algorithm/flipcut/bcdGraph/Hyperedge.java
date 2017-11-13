package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.iterator.TObjectLongIterator;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;

//todo maybe special guide tree handling

public class Hyperedge {
    public final RoaringBitmap ones;
    private long weight = 0L;
    private final TObjectLongMap<RoaringBitmap> zerosS = new TObjectLongHashMap<>();

    public Hyperedge(RoaringBitmap ones) {
        this.ones = ones;
    }


    //returns true if the whole edge is semiuniversal.
    public boolean removeSemiuniversals(RoaringBitmap taxaInGraph) {
        TObjectLongIterator<RoaringBitmap> tit = zerosS.iterator();

        while (tit.hasNext()) {
            tit.advance();
            if (RoaringBitmap.intersects(tit.key(), taxaInGraph)) {
                tit.remove();
                if (!isInfinite())
                    weight -= tit.value();
            }
        }
        return zerosS.isEmpty();
    }

    //returns new weight
    public long addZero(RoaringBitmap zeros, long weight) {
        if (weight == CutGraphCutter.getInfinity() || isInfinite()) {
            zerosS.put(zeros, weight);
            this.weight = weight;
        } else {
            zerosS.adjustOrPutValue(zeros, weight, weight);
            this.weight += weight;
        }

        return this.weight;
    }

    public long getWeight() {
        return weight;
    }

    public boolean isInfinite() {
        assert weight <= CutGraphCutter.getInfinity();
        return weight >= CutGraphCutter.getInfinity();
    }
}
