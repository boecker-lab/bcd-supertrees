package phylo.tree.algorithm.flipcut.flipCutGraph;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 26.02.17.
 */

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SimpleCutterFactories {
    private SimpleCutterFactories() {
    }

    public static MaxFlowCutterFactory newInstance() {
        return newInstance(SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
    }

    public static MaxFlowCutterFactory newInstance(SimpleCutGraphCutter.CutGraphTypes simpleCutterType) {
        switch (simpleCutterType) {
            default:
                return new SingleCutGraphCutter.Factory(simpleCutterType);
        }
    }
}
