package phylo.tree.algorithm.flipcut.costComputer;

import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

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
        if (scaffoldTree != null && t.equals(scaffoldTree)) {
            return CutGraphCutter.getInfinity();
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