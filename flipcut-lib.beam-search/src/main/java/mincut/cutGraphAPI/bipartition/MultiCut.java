package mincut.cutGraphAPI.bipartition;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 15.02.17.
 */

import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class MultiCut<S, G extends SourceTreeGraph> implements Cut<S> {
    protected int hashCache;
    private boolean hash = false;

    protected final G sourceGraph;
    protected List<FlipCutGraphMultiSimpleWeight> splittedGraphs;

    public G sourceGraph() {
        return sourceGraph;
    }

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

    protected void calculateHash() {
        hashCache = getCutSet() != null ? getCutSet().hashCode() : 0;
        hashCache = 31 * hashCache + (comp() != null ? comp().hashCode() : 0);
        hashCache = 31 * hashCache + (int) (minCutValue() ^ (minCutValue() >>> 32));
        hash = true;
    }
}
