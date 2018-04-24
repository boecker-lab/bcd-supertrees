package mincut.cutGraphImpl.minCutKargerStein;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 24.02.17.
 */


import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.util.Objects;
import java.util.Random;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class RandomIntSet implements Cloneable {

    private final TIntArrayList dta;
    private final TIntIntMap idx;

    public RandomIntSet() {
        dta = new TIntArrayList();
        idx = new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
    }

    public RandomIntSet(int initCapacity) {
        dta = new TIntArrayList(initCapacity);
        idx = new TIntIntHashMap(initCapacity, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
    }

    public RandomIntSet(int... items) {
        this(items.length);
        for (int item : items) {
            idx.put(item, dta.size());
            dta.add(item);
        }
    }

    public RandomIntSet(RoaringBitmap items) {
        this(items.getCardinality());
        items.forEach(new IntConsumer() {
            @Override
            public void accept(int item) {
                idx.put(item, dta.size());
                dta.add(item);
            }
        });
    }

    private RandomIntSet(TIntArrayList dta, TIntIntMap idx) {
        this.dta = new TIntArrayList(dta);
        this.idx = new TIntIntHashMap(idx);
    }

    public boolean add(int item) {
        if (idx.containsKey(item)) {
            return false;
        }
        idx.put(item, dta.size());
        dta.add(item);
        return true;
    }

    /**
     * Override element at position <code>id</code> with last element.
     *
     * @param index index of element to remove
     */
    public int removeAt(int index) {
        if (index >= dta.size()) {
            throw new IllegalArgumentException("Illegal index");
//            return idx.getNoEntryValue();
        }
        /*if (index == dta.size() -1){
            idx.remove(dta.get(index));
            dta.removeAt()
        }else {

        }*/

        int res = dta.get(index);
        idx.remove(res);
        int last = dta.get(dta.size() - 1);
        dta.removeAt(dta.size() - 1);
        // skip filling the hole if last is removed
        if (index < dta.size()) {
            idx.put(last, index);
            dta.set(index, last);
        }
        return res;
    }

    public boolean remove(int item) {
        @SuppressWarnings(value = "element-type-mismatch")
        int index = idx.get(item);
        if (index == idx.getNoEntryValue()) {
            return false;
        }
        removeAt(index);
        return true;
    }

    public int get(int index) {
        return dta.get(index);
    }

    public int pollRandom(Random rnd) {
        if (dta.isEmpty()) {
            throw new IllegalArgumentException("Set is empty");
//            return idx.getNoEntryValue();
        }
        int id = rnd.nextInt(dta.size());
        return removeAt(id);
    }

    public int peekRandom(Random rnd) {
        if (dta.isEmpty()) {
            throw new IllegalArgumentException("Set is empty");
//            return idx.getNoEntryValue();
        }
        int id = rnd.nextInt(dta.size());
        return get(id);
    }

    public int size() {
        return dta.size();
    }

    public TIntIterator iterator() {
        return dta.iterator();
    }


    public boolean contains(int item) {
        return idx.containsKey(item);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RandomIntSet that = (RandomIntSet) o;
        return Objects.equals(idx.keySet(), that.idx.keySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(idx.keySet());
    }

    public RandomIntSet clone() {
        return new RandomIntSet(dta, idx);
    }

    public TIntArrayList valuesCopy() {
        return new TIntArrayList(dta);
    }


}
