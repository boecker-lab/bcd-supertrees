package phylo.tree.algorithm.flipcut.model;

import mincut.cutGraphAPI.bipartition.BasicCut;
import mincut.cutGraphAPI.bipartition.STCut;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * User: Markus Fleischauer (markus.fleischauerquni-jena.de)
 * 09.07.12 17:14
 */
public class VaziraniCut<T extends AbstractFlipCutNode<T>> extends BasicCut<T> {
    public final int k;

    public VaziraniCut(STCut<T> cut, Collection<T> sSet, int k) {
        super(new LinkedHashSet<>(cut.getCutSet()), cut.minCutValue);
        cutSet.addAll(sSet);
        this.k = k;
    }

    public VaziraniCut(LinkedHashSet<T> cutSet, long minCutValue, int k) {
        super(cutSet, minCutValue);
        this.k = k;
    }
}