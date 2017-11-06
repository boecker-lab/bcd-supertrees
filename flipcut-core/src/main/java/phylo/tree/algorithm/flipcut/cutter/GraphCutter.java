package phylo.tree.algorithm.flipcut.cutter;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import mincut.cutGraphAPI.bipartition.Cut;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface GraphCutter<N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>> {
    void clear();
    Cut<N> cut(T source);
}
