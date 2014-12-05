package flipcut;


import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import flipcut.costComputer.CostComputer;
import flipcut.costComputer.FlipCutWeights;
import flipcut.costComputer.UnitCostComputer;
import flipcut.costComputer.WeightCostComputer;
import flipcut.flipCutGraph.CutGraphCutter;
import flipcut.flipCutGraph.FlipCutGraphSimpleWeight;
import flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import flipcut.flipCutGraph.SingleCutGraphCutter;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 18:13
 */
public class FlipCutSingleCutSimpleWeight extends AbstractFlipCutSingleCut<FlipCutNodeSimpleWeight,FlipCutGraphSimpleWeight,SingleCutGraphCutter> {


    protected FlipCutSingleCutSimpleWeight() {
        super();
    }

    public FlipCutSingleCutSimpleWeight(CutGraphCutter.CutGraphTypes type) {
        super(type);
    }

    public FlipCutSingleCutSimpleWeight(Logger log, CutGraphCutter.CutGraphTypes type) {
        super(log, type);
    }



    @Override
    protected FlipCutGraphSimpleWeight createGraph(List<FlipCutNodeSimpleWeight> component, TreeNode treeNode) {
        return new FlipCutGraphSimpleWeight(component,treeNode);
    }

    @Override
    protected SingleCutGraphCutter createCutter(CutGraphCutter.CutGraphTypes type) {
        return new SingleCutGraphCutter(type);
    }

    @Override
    protected FlipCutGraphSimpleWeight createInitGraph(CostComputer costsComputer) {
        return new FlipCutGraphSimpleWeight(costsComputer, bootstrapThreshold);
    }

    //this method contains only simple weightings
    @Override
    protected CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree) {

        if (UnitCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            getLog().info("Using Unit Costs");
            return new UnitCostComputer(inputTrees,scaffoldTree);
        } else if (WeightCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            getLog().info("Using " + weights);
            return new WeightCostComputer(inputTrees, weights, scaffoldTree);
        }

        getLog().warn("No supported weight option found. Setting to standard: "+ FlipCutWeights.Weights.UNIT_COST);
        setWeights(FlipCutWeights.Weights.UNIT_COST);
        return new UnitCostComputer(inputTrees,scaffoldTree);

    }
}
