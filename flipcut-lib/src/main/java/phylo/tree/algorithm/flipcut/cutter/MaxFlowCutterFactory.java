package phylo.tree.algorithm.flipcut.cutter;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphTypes;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface MaxFlowCutterFactory<C extends GraphCutter<S>, S, T extends SourceTreeGraph<S>> extends CutterFactory<C, S, T> {
    CutGraphTypes getType();

    @Override
    default boolean isBCD() {
        return getType().isBCD();
    }

    @Override
    default boolean isFlipCut() {
        return getType().isFlipCut();
    }

    static MaxFlowCutterFactory newInstance() {
        return newInstance(CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
    }

    static MaxFlowCutterFactory newInstance(CutGraphTypes simpleCutterType) {
        switch (simpleCutterType) {
            default:
                return new SingleCutGraphCutter.Factory(simpleCutterType);
        }
    }
}
