package phylo.tree.algorithm.flipcut.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 30.03.17.
 */

import phylo.tree.algorithm.flipcut.costComputer.*;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;
import phylo.tree.treetools.TreetoolUtils;
import phylo.tree.treetools.WeightedTreePartitions;

import java.util.*;

import static phylo.tree.model.TreeUtils.getLeafLabels;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Utils extends TreetoolUtils{


    public static Tree addCladewiseSplitFit(List<Tree> sourceTrees, FlipCutWeights.Weights weighting, Tree tree) {
        addCladewiseSplitFit(sourceTrees, weighting, Collections.singletonList(tree));
        return tree;
    }

    public static void addCladewiseSplitFit(List<Tree> sourceTrees, FlipCutWeights.Weights weighting, Collection<Tree> trees) {
        final SimpleCosts comp;
        if (weighting == FlipCutWeights.Weights.UNIT_COST) {
            comp =  new UnitCostComputer(sourceTrees,null);
        } else {
            comp = new WeightCostComputer(sourceTrees,weighting);
        }

        List<WeightedTreePartitions> biparts = new LinkedList<>();
        for (Tree sourceTree : sourceTrees) {
            final TreeNode r = sourceTree.getRoot();
            Set<String> sourceLeafes = getLeafLabels(r);
            for (TreeNode treeNode : sourceTree.getRoot().depthFirstIterator()) {
                if (treeNode != r && treeNode.isInnerNode()) {
                    biparts.add(new WeightedTreePartitions(getLeafLabels(treeNode), sourceLeafes, comp.getEdgeWeight(treeNode)));
                }
            }
        }

        for (Tree tree : trees) {
            addCladewiseSplitFit(biparts, tree);
        }
    }
}
