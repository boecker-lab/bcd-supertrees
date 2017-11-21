package phylo.tree.algorithm.flipcut.bcdGraph.edge;

import gnu.trove.iterator.TObjectLongIterator;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.LoggerFactory;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;

import java.util.Arrays;

//todo maybe special guide tree handling

public class MergedHyperedge extends AbstractHyperedge {

    private final TObjectLongMap<RoaringBitmap> zerosS = new TObjectLongHashMap<>();

    public int umergedNumber() {
        return zerosS.size();
    }

    public MergedHyperedge(RoaringBitmap ones) {
        super(ones);
    }

    //returns true if the whole edge is semiuniversal.
    public boolean removeSemiuniversals(RoaringBitmap taxaInGraph) {
        TObjectLongIterator<RoaringBitmap> tit = zerosS.iterator();
        boolean infiniteRemoval = false;
        while (tit.hasNext()) {
            tit.advance();

            if (!RoaringBitmap.intersects(tit.key(), taxaInGraph)) {
                if (!isInfinite()) {
                    weight -= tit.value();
                } else if (tit.value() == CutGraphCutter.getInfinity()) {
                    infiniteRemoval = true;
                }
                tit.remove();
            }
        }
        if (infiniteRemoval && !zerosS.isEmpty()) {
            weight = Arrays.stream(zerosS.values()).sum();
            LoggerFactory.getLogger(this.getClass()).warn("Re summing hyperedge weight -> This is only possible if a guid tree is used, that does not contain all taxa");
        }
        return zerosS.isEmpty();
    }

    //returns new weight
    public long addZero(RoaringBitmap zeros, long weight) {
        if (weight == CutGraphCutter.getInfinity()) {
            zerosS.put(zeros, weight);
            this.weight = weight;
        } else {
            zerosS.adjustOrPutValue(zeros, weight, weight);
            if (!isInfinite())
                this.weight += weight;
        }

        return this.weight;
    }




}
