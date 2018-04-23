package mincut.cutGraphAPI.bipartition;

import gnu.trove.set.TIntSet;
import mincut.EdgeColor;

import java.util.Set;

public class SimpleHashableCut extends HashableCut<TIntSet> {
    private final Set<EdgeColor> edgeColors;

    public SimpleHashableCut(TIntSet sSet, TIntSet tSet, Set<EdgeColor> edgeColors, double minCutValue) {
        super(sSet, tSet, minCutValue);
        this.edgeColors = edgeColors;
    }

    public Set<EdgeColor> getEdgeColors() {
        return edgeColors;
    }
}
