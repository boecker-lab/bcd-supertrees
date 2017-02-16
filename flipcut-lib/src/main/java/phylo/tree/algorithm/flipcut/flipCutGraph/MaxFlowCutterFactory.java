package phylo.tree.algorithm.flipcut.flipCutGraph;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface MaxFlowCutterFactory<C extends CutGraphCutter<N, T>, N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>> extends CutterFactory<C,N,T> {
    SimpleCutGraphCutter.CutGraphTypes getType();
    default boolean isHyperGraphCutter(){
        SimpleCutGraphCutter.CutGraphTypes type = getType();
        return type.equals(SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG) || type.equals(SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN);
    }

}
