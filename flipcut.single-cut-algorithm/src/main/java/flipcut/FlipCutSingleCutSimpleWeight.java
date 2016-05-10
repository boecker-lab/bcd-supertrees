package flipcut;


import flipcut.costComputer.CostComputer;
import flipcut.costComputer.FlipCutWeights;
import flipcut.costComputer.UnitCostComputer;
import flipcut.costComputer.WeightCostComputer;
import flipcut.flipCutGraph.CutGraphCutter;
import flipcut.flipCutGraph.FlipCutGraphSimpleWeight;
import flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import flipcut.flipCutGraph.SingleCutGraphCutter;
import phylo.tree.model.tree.Tree;
import phylo.tree.model.tree.TreeNode;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 18:13
 */
public class FlipCutSingleCutSimpleWeight extends AbstractFlipCutSingleCut<FlipCutNodeSimpleWeight,FlipCutGraphSimpleWeight,SingleCutGraphCutter> {

    public FlipCutSingleCutSimpleWeight() {
        super();
    }

    @Override
    protected String name() {
        return getClass().getSimpleName();
    }

    public FlipCutSingleCutSimpleWeight(CutGraphCutter.CutGraphTypes type) {
        super(type);
    }

    public FlipCutSingleCutSimpleWeight(Logger log, CutGraphCutter.CutGraphTypes type) {
        super(log, type);
    }

    public FlipCutSingleCutSimpleWeight(Logger log, ExecutorService executorService1, CutGraphCutter.CutGraphTypes type) {
        super(log, executorService1, type);
    }

    @Override
    protected FlipCutGraphSimpleWeight createGraph(List<FlipCutNodeSimpleWeight> component, TreeNode treeNode, final boolean checkEdges) {
        return new FlipCutGraphSimpleWeight(component,treeNode,checkEdges);
    }

    @Override
    protected SingleCutGraphCutter createCutter() {
        if (executorService == null) {
            return new SingleCutGraphCutter(type);
        }else{
            if (numberOfThreads > 0) {
                return new SingleCutGraphCutter(type,executorService,numberOfThreads);
            } else {
                return new SingleCutGraphCutter(type,executorService,CORES_AVAILABLE);
            }
        }
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
            return new UnitCostComputer(inputTrees,scaffoldTree);
        } else if (WeightCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            LOGGER.info("Using " + weights);
            return new WeightCostComputer(inputTrees, weights, scaffoldTree);
        }

        LOGGER.warning("No supported weight option found. Setting to standard: "+ FlipCutWeights.Weights.UNIT_COST);
        setWeights(FlipCutWeights.Weights.UNIT_COST);
        return new UnitCostComputer(inputTrees,scaffoldTree);

    }
}
