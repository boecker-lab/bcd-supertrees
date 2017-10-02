package phylo.tree.algorithm.flipcut.flipCutGraph;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import phylo.tree.algorithm.flipcut.flipCutGraph.blacklists.GreedyBlackList;
import phylo.tree.algorithm.flipcut.flipCutGraph.blacklists.RandomizedBlackList;
import phylo.tree.algorithm.flipcut.flipCutGraph.blacklists.RecursiveBlackList;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MultiCutGrapCutterFactories {
    public enum MultiCutterType {VAZIRANI, GREEDY,GREEDY_RAND, GREEDY_RECURSIVE, MC}


    private MultiCutGrapCutterFactories() {
    }


    public static MultiCutterFactory newInstance() {
        return newInstance(MultiCutterType.GREEDY, SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
    }

    public static MultiCutterFactory newInstance(MultiCutterType multiCutterType) {
        return newInstance(multiCutterType, SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
    }

    public static MultiCutterFactory newInstance(SimpleCutGraphCutter.CutGraphTypes simpleCutterType) {
        return newInstance(MultiCutterType.GREEDY, simpleCutterType);
    }

    public static MultiCutterFactory newInstance(MultiCutterType multiCutterType, SimpleCutGraphCutter.CutGraphTypes simpleCutterType) {
        switch (multiCutterType) {
            case VAZIRANI:
                return new MultiCutGraphCutter.Factory(simpleCutterType);
            case GREEDY:
                return new MultiCutGraphCutterGreedy.Factory(simpleCutterType,new GreedyBlackList());
            case GREEDY_RAND:
                return new MultiCutGraphCutterGreedy.Factory(simpleCutterType, new RandomizedBlackList());
            case GREEDY_RECURSIVE:
                return new MultiCutGraphCutterGreedy.Factory(simpleCutterType, new RecursiveBlackList());
            case MC:
                return MultiCutGraphCutterUndirectedTranfomation.getFactory();
            default:
                return new MultiCutGraphCutterGreedy.Factory(simpleCutterType, new GreedyBlackList());
        }
    }


}
