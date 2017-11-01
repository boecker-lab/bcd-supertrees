package phylo.tree.algorithm.flipcut.flipCutGraph;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface MaxFlowCutterFactory<C extends CutGraphCutter<N, T>, N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>> extends CutterFactory<C,N,T> {
    CutGraphTypes getType();
}
