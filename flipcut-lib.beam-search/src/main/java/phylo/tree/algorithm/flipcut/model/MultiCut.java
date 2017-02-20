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

import java.util.List;
import java.util.Map;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class MultiCut<V extends AbstractFlipCutNode<V>, G extends AbstractFlipCutGraph<V>> implements Cut<V> {
    protected final G sourceGraph;
    protected List<FlipCutGraphMultiSimpleWeight> splittedGraphs;

    public MultiCut(G sourceGraph) {
        this.sourceGraph = sourceGraph;
    }

    public abstract List<G> getSplittedGraphs();
}
