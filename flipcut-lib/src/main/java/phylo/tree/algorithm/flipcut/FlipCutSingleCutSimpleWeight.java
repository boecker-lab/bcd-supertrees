package phylo.tree.algorithm.flipcut;


import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.costComputer.FlipCutWeights;
import phylo.tree.algorithm.flipcut.costComputer.UnitCostComputer;
import phylo.tree.algorithm.flipcut.costComputer.WeightCostComputer;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.SingleCutGraphCutter;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 18:13
 */
public class FlipCutSingleCutSimpleWeight extends AbstractFlipCutSingleCut<FlipCutNodeSimpleWeight, FlipCutGraphSimpleWeight, SingleCutGraphCutter> {

    public FlipCutSingleCutSimpleWeight() {}

    public FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.Factory type) {
        super(type);
    }

    public FlipCutSingleCutSimpleWeight(Logger log, SingleCutGraphCutter.Factory type) {
        super(log, type);
    }

    public FlipCutSingleCutSimpleWeight(Logger log, ExecutorService executorService1, SingleCutGraphCutter.Factory type) {
        super(log, executorService1, type);
    }

    @Override
    protected String name() {
        return getClass().getSimpleName();
    }

    @Override
    protected FlipCutGraphSimpleWeight createGraph(List<FlipCutNodeSimpleWeight> component, TreeNode treeNode) {
        return new FlipCutGraphSimpleWeight(component, treeNode, type.getType().isFlipCut());
    }

    @Override
    protected FlipCutGraphSimpleWeight createInitGraph(CostComputer costsComputer) {
        return new FlipCutGraphSimpleWeight(costsComputer, bootstrapThreshold);
    }

    //this method contains only simple weightings
    @Override
    protected CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree) {

        if (UnitCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            LOGGER.info("Using Unit Costs");
            return new UnitCostComputer(inputTrees, scaffoldTree);
        } else if (WeightCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            LOGGER.info("Using " + weights);
            return new WeightCostComputer(inputTrees, weights, scaffoldTree);
        }

        LOGGER.warning("No supported weight option found. Setting to standard: " + FlipCutWeights.Weights.UNIT_COST);
        setWeights(FlipCutWeights.Weights.UNIT_COST);
        return new UnitCostComputer(inputTrees, scaffoldTree);

    }
}
