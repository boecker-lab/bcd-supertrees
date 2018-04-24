package mincut.cutGraphImpl.minCutKargerStein;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 24.02.17.
 */

//todo use trove for indexing to save time and memory

import com.google.common.collect.Iterators;
import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class RandomSet<E> extends AbstractSet<E> {

    private final List<E> dta;
    private final TObjectIntMap<E> idx;

    public RandomSet() {
        dta = new ArrayList<E>();
        idx = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
    }

    public RandomSet(int cap) {
        dta = new ArrayList<E>(cap);
        idx = new TObjectIntHashMap<>(cap, Constants.DEFAULT_LOAD_FACTOR, -1);
    }

    public RandomSet(Collection<E> items) {
        this(items.size());
        for (E item : items) {
            idx.put(item, dta.size());
            dta.add(item);
        }
    }

    @Override
    public boolean add(E item) {
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
     * @param id
     */
    public E removeAt(int id) {
        if (id >= dta.size()) {
            return null;
        }
        E res = dta.get(id);
        idx.remove(res);
        E last = dta.remove(dta.size() - 1);
        // skip filling the hole if last is removed
        if (id < dta.size()) {
            idx.put(last, id);
            dta.set(id, last);
        }
        return res;
    }

    @Override
    public boolean remove(Object item) {
        @SuppressWarnings(value = "element-type-mismatch")
        int id = idx.get(item);
        if (id == idx.getNoEntryValue()) {
            return false;
        }
        removeAt(id);
        return true;
    }

    public E get(int i) {
        return dta.get(i);
    }

    public E pollRandom(Random rnd) {
        if (dta.isEmpty()) {
            return null;
        }
        int id = rnd.nextInt(dta.size());
        return removeAt(id);
    }

    public E peekRandom(Random rnd) {
        if (dta.isEmpty()) {
            return null;
        }
        int id = rnd.nextInt(dta.size());
        return get(id);
    }

    @Override
    public int size() {
        return dta.size();
    }

    @Override
    public Iterator<E> iterator() {
        return Iterators.unmodifiableIterator(dta.iterator());
    }
}
