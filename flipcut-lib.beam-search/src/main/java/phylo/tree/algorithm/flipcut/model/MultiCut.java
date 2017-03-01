package phylo.tree.algorithm.flipcut.model;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 15.02.17.
 */

import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.Cut;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class MultiCut<V extends AbstractFlipCutNode<V>, G extends AbstractFlipCutGraph<V>> implements Cut<V> {
    protected int hashCache;
    private boolean hash = false;

    protected final G sourceGraph;
    protected List<FlipCutGraphMultiSimpleWeight> splittedGraphs;


    public MultiCut(G sourceGraph) {
        this.sourceGraph = sourceGraph;
    }

    public abstract List<G> getSplittedGraphs();


    protected abstract List<List<FlipCutNodeSimpleWeight>> comp();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultMultiCut)) return false;

        DefaultMultiCut that = (DefaultMultiCut) o;

        if (minCutValue() != that.minCutValue()) return false;
        if (getCutSet() != null ? !getCutSet().equals(that.getCutSet()) : that.getCutSet() != null) return false;
        return comp() != null ? comp().equals(that.comp()) : that.comp() == null;

    }

    @Override
    public int hashCode() {
        if (!hash)
            calculateHash();
        return hashCache;
    }

    protected void calculateHash(){
        hashCache = getCutSet() != null ? getCutSet().hashCode() : 0;
        hashCache = 31 * hashCache + (comp() != null ? comp().hashCode() : 0);
        hashCache = 31 * hashCache + (int) (minCutValue() ^ (minCutValue() >>> 32));
        hash = true;
    }
}
