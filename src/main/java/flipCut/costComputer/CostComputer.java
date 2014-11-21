package flipCut.costComputer;


import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import flipCut.flipCutGraph.AbstractFlipCutNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User: Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * 09.07.12 17:55
 */

/**
 * node weight styles
 */
public abstract class CostComputer{
    /**
     * The accuracy used to go from double to long
     */
    public final static long ACCURACY = 100000000;
    public final static Set<FlipCutWeights.Weights> SUPPORTED_COST_TYPES = Collections.emptySet();

    protected final List<Tree> trees;
    protected final Tree scaffoldTree;
    protected FlipCutWeights.Weights weights;
    private double scaffoldWeight = 0d;

    //just for normalization for easy combination of different weighting types
    protected double longestBranch;
    protected int maxLevel;
    protected double maxBSValue = -1d;

    public CostComputer(List<Tree> trees, FlipCutWeights.Weights weights) {
        this(trees,weights,null);
    }


    public CostComputer(List<Tree> inputTrees,  FlipCutWeights.Weights weights, Tree scaffoldTree) {
        this.scaffoldTree = scaffoldTree;
        trees = new ArrayList<>(inputTrees);

        //find longest branch... //todo check if this is still needed
        longestBranch = 0;
        for (Tree tree : trees) {
            //finde longest branch and max BSValue
            for (TreeNode node : tree.vertices()) {
                if (node.getParent() != null){
                    //branch stuff
                    double branchLength = node.getDistanceToParent();
                    if (branchLength > longestBranch)
                        longestBranch = branchLength;

                    if (node.isLeaf()){
                        //level stuff
                        int level = node.getLevel();
                        if (level > maxLevel)
                            maxLevel = level;
                    }else {
                        //BS stuff
                        double currentBS = readBSValueFromLabel(node);
                        if (currentBS > maxBSValue){
                            maxBSValue = currentBS;
                        }
                    }

                }
            }
        }

        if (scaffoldTree != null && !trees.contains(scaffoldTree)){
            System.out.println("adding scaffold tree to treelist");
            trees.add(scaffoldTree);
            //scaffoldTree.getRoot().setLabel(Double.toString(getScaffoldWeight()));
        }
        if (maxBSValue < 0d){
            maxBSValue = 100d;
        }
        this.weights = weights;
    }

    //todo maybe not needed because of infinity option
    protected double getScaffoldWeight(){
        //Attention: unchecked
        if (scaffoldWeight == 0d ){
            for (Tree tree : trees) {
                scaffoldWeight += Double.valueOf(tree.getRoot().getLabel());
            }
        }
        return scaffoldWeight;
    }

    protected double readBSValueFromLabel(TreeNode node){
        if (node.getLabel() != null) {
            return Double.valueOf(node.getLabel());
        }else{
            return -2d;
        }
    }

    public abstract long getEdgeWeight(TreeNode node, List<? extends AbstractFlipCutNode> leafes, AbstractFlipCutNode leaf);
    public abstract long getEdgeWeight(TreeNode node, List<TreeNode> leafes, TreeNode leaf);

    public List<Tree> getTrees() {
        return trees;
    }

    public Tree getScaffoldTree() {
        return scaffoldTree;
    }
}