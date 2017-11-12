package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import gnu.trove.list.linked.TLongLinkedList;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//todo maybe special guide tree handling

public class Hyperedge {
    public final RoaringBitmap ones;
    private long weight = 0L;
    private final List<RoaringBitmap> zerosS = new LinkedList<>();
    private final TLongList weights = new TLongLinkedList();

    public Hyperedge(RoaringBitmap ones) {
        this.ones = ones;
    }


    //returns true if the whole edge is semiuniversal.
    public boolean removeSemiuniversals(RoaringBitmap taxaInGraph) {
        Iterator<RoaringBitmap> tit = zerosS.iterator();
        TLongIterator wit = weights.iterator();

        while (tit.hasNext()) {
            RoaringBitmap zeros = tit.next();
            long w = wit.next();
            if (RoaringBitmap.intersects(zeros, taxaInGraph)) {
                tit.remove();
                wit.remove();
                if (!isInfinite())
                    weight -= w;
            }
        }
        return zerosS.isEmpty();
    }

    //returns new weight
    public long addZero(RoaringBitmap zeros, long weight) {
        zerosS.add(zeros);
        weights.add(weight);

        if (!isInfinite()) {
            if (weight == CutGraphCutter.getInfinity())
                this.weight = weight;
            else
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
