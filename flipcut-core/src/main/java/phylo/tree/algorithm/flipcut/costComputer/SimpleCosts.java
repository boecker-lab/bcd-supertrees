package phylo.tree.algorithm.flipcut.costComputer;




import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 14.01.13
 * Time: 17:47
 */
public abstract class SimpleCosts extends CostComputer {

    public SimpleCosts(List<Tree> trees, FlipCutWeights.Weights weights) {
        super(trees, weights);
    }

    public SimpleCosts(List<Tree> trees, FlipCutWeights.Weights weights, Tree scaffoldTree) {
        super(trees, weights, scaffoldTree);
    }

    public abstract long getEdgeWeight(TreeNode node);

    //this method contains only simple weightings
    public static CostComputer newCostComputer(List<Tree> inputTrees, FlipCutWeights.Weights weights) {
        return newCostComputer(inputTrees,null,weights);
    }
    public static CostComputer newCostComputer(List<Tree> inputTrees, Tree scaffoldTree, FlipCutWeights.Weights weights) {

        if (UnitCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            LOGGER.info("Using Unit Costs");
            return new UnitCostComputer(inputTrees, scaffoldTree);
        } else if (WeightCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            LOGGER.info("Using " + weights);
            return new WeightCostComputer(inputTrees, weights, scaffoldTree);
        }

        LOGGER.warn("No supported weight option found. Setting to standard: " + FlipCutWeights.Weights.UNIT_COST);
        return new UnitCostComputer(inputTrees, scaffoldTree);

    }
}
