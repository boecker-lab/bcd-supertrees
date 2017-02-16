package phylo.tree.algorithm.flipcut.flipCutGraph;

import phylo.tree.algorithm.flipcut.model.MultiCut;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 *         Date: 19.04.13
 *         Time: 15:19
 */
public interface MultiCutter<N extends AbstractFlipCutNode<N>,T extends AbstractFlipCutGraph<N>> extends GraphCutter<N,T> {
    MultiCut getNextCut();
}
