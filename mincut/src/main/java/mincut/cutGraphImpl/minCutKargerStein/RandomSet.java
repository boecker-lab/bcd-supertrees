package mincut.cutGraphImpl.minCutKargerStein;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 24.02.17.
 */

import com.google.common.collect.Iterators;

import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class RandomSet<E> extends AbstractSet<E> {

    List<E> dta = new ArrayList<E>();
    Map<E, Integer> idx = new HashMap<E, Integer>();

    public RandomSet() {
    }

    public RandomSet(Collection<E> items) {
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
        Integer id = idx.get(item);
        if (id == null) {
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
