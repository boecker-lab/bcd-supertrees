package flipcut.costComputer;

import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import flipcut.flipCutGraph.AbstractFlipCutNode;
import flipcut.flipCutGraph.CutGraphCutter;

import java.util.*;

/**
 * User: Markus Fleischauer (markus.fleischauerquni-jena.de)
 * 09.07.12 17:56
 */
public class UnitCostComputer extends SimpleCosts {
    public final static Set<FlipCutWeights.Weights> SUPPORTED_COST_TYPES = Collections.unmodifiableSet(new HashSet<FlipCutWeights.Weights>(Arrays.asList(FlipCutWeights.Weights.UNIT_COST)));
    //todo make tree weighting possible
    public UnitCostComputer(List<Tree> trees, Tree scaff) {
        super(trees,null,scaff);
    }

    @Override
    public long getEdgeWeight(TreeNode node) {
        Tree t = (Tree)node.getGraph();
        if (scaffoldTree != null && trees.equals(scaffoldTree)) {
            return ACCURACY * CutGraphCutter.INFINITY;
        } else {
            return 1;
        }
    }

    @Override
    public long getEdgeWeight(TreeNode node, List<? extends AbstractFlipCutNode> leafes, AbstractFlipCutNode leaf) {
        return getEdgeWeight(node);
    }

    @Override
    public long getEdgeWeight(TreeNode node, List<TreeNode> leafes, TreeNode leaf) {
        return getEdgeWeight(node);
    }
}