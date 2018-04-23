package mincut.cutGraphAPI.bipartition;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class HashableCut<CutSet> {
    private final Set<CutSet> sets;
    private final double minCutValue;
    private final int hash;

    public HashableCut(CutSet sSet, CutSet tSet, double minCutValue) {
        sets = new LinkedHashSet<>(2);
        sets.add(sSet);
        sets.add(tSet);
        this.minCutValue = minCutValue;
        this.hash = Objects.hash(sets, minCutValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashableCut<?> that = (HashableCut<?>) o;
        return Double.compare(that.minCutValue, minCutValue) == 0 &&
                Objects.equals(sets, that.sets);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public double minCutValue() {
        return minCutValue;
    }

    public CutSet getSset() {
        return sets.iterator().next();
    }

    public CutSet getTset() {
        Iterator<CutSet> it = sets.iterator();
        it.next();
        return it.next();
    }

    @Override
    public String toString() {
        return "Cut(" + sets.toString() + " : " + minCutValue()+")";
    }
}
