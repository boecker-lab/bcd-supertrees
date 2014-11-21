package flipCut;


import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import flipCut.costComputer.CostComputer;
import flipCut.costComputer.FlipCutWeights;
import flipCut.costComputer.UnitCostComputer;
import flipCut.costComputer.WeightCostComputer;
import flipCut.flipCutGraph.CutGraphCutter;
import flipCut.flipCutGraph.FlipCutGraphSimpleWeight;
import flipCut.flipCutGraph.FlipCutNodeSimpleWeight;
import flipCut.flipCutGraph.SingleCutGraphCutter;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 18:13
 */
public class FlipCutSingleCutSimpleWeight extends AbstractFlipCutSingleCut<FlipCutNodeSimpleWeight,FlipCutGraphSimpleWeight,SingleCutGraphCutter> {



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
    protected SingleCutGraphCutter getCutter(CutGraphCutter.CutGraphTypes type) {
        return new SingleCutGraphCutter(type);
    }

    @Override
    protected FlipCutGraphSimpleWeight createInitGraph(CostComputer costsComputer, double bootstrapThreshold) {
        return new FlipCutGraphSimpleWeight(costsComputer, bootstrapThreshold);
    }

    //this method contains only simple weightings
    @Override
    protected CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree) {
        CostComputer costs = null;
        if (UnitCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            getLog().info("Using Unit Costs");
            costs = new UnitCostComputer(inputTrees,scaffoldTree);
        } else if (WeightCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            costs = new WeightCostComputer(inputTrees, weights, scaffoldTree);
            getLog().info("Using " + weights);
        }else{
            getLog().warn("No supported weight option found. Setting to standard: "+ FlipCutWeights.Weights.EDGE_AND_LEVEL);
            setWeights(FlipCutWeights.Weights.EDGE_AND_LEVEL);
            initCosts(inputTrees, scaffoldTree);
        }
        return costs;
    }
}
