package phylo.tree.algorithm.flipcut.costComputer;


import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphCutter;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.*;

/**
 * User: Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * 09.07.12 17:57
 */
public class WeightCostComputer extends SimpleCosts {
    public final static Set<FlipCutWeights.Weights> SUPPORTED_COST_TYPES = Collections.unmodifiableSet(new HashSet<FlipCutWeights.Weights>(Arrays.asList(FlipCutWeights.Weights.EDGE_AND_LOGLEVEL, FlipCutWeights.Weights.NODE_LOGLEVEL, FlipCutWeights.Weights.BOOTSTRAP_AND_LOGLEVEL, FlipCutWeights.Weights.BOOTSTRAP_AND_LEVEL, FlipCutWeights.Weights.TREE_WEIGHT, FlipCutWeights.Weights.BOOTSTRAP_AND_TREE_SIZE, FlipCutWeights.Weights.TREE_SIZE, FlipCutWeights.Weights.BOOTSTRAP_VALUES, FlipCutWeights.Weights.BOOTSTRAP_AND_EDGEWEIGHTS, FlipCutWeights.Weights.ClADERATE, FlipCutWeights.Weights.EDGE_AND_ClADERATE, FlipCutWeights.Weights.EDGE_AND_RELATIVE_CLADESIZE, FlipCutWeights.Weights.RELATIVE_CLADESIZE, FlipCutWeights.Weights.EDGE_AND_LEVEL, FlipCutWeights.Weights.EDGE_AND_PATH, FlipCutWeights.Weights.EDGE_ROOT, FlipCutWeights.Weights.EDGE_WEIGHTS, FlipCutWeights.Weights.NODE_LEVEL, FlipCutWeights.Weights.CLADESIZE, FlipCutWeights.Weights.EDGE_AND_ClADESIZE, FlipCutWeights.Weights.GLOBAL_CLADESIZE, FlipCutWeights.Weights.EDGE_AND_GLOBAL_ClADESIZE, FlipCutWeights.Weights.EDGE_AND_LEVEL_BOTTOM_UP, FlipCutWeights.Weights.NODE_LEVEL_BOTTOM_UP)));

    protected final int taxaNumber;

    public WeightCostComputer(List<Tree> trees, FlipCutWeights.Weights weights) {
        super(trees, weights);
        taxaNumber = getTaxaNumber();
    }

    public WeightCostComputer(List<Tree> trees, FlipCutWeights.Weights weights, Tree scaffoldTree) {
        super(trees, weights, scaffoldTree);
        taxaNumber = getTaxaNumber();
    }

    private int getTaxaNumber() {
        Set<String> taxa = new HashSet<String>();
        for (Tree tree : trees) {
            for (TreeNode leaf : tree.getLeaves()) {
                taxa.add(leaf.getLabel());
            }
        }
        return taxa.size();
    }

    @Override
    public long getEdgeWeight(TreeNode node) {
        //incoming edge weight
        double edgeWeight;
        double nodeLevel = 1;
        Tree tree = (Tree) node.getGraph();
        if (scaffoldTree != null && tree.equals(scaffoldTree)) {
            return CutGraphCutter.INFINITY * ACCURACY;
        } else {
            //###############################
            //##### most common weights #####
            //###############################
            if (weights == FlipCutWeights.Weights.EDGE_WEIGHTS) {
                edgeWeight = calcEdgeWeight(node);
            } else if (weights == FlipCutWeights.Weights.EDGE_AND_LEVEL) {
                edgeWeight = calcEdgeWeight(node);
                nodeLevel = calcNodeLevel(node);
            } else if (weights == FlipCutWeights.Weights.EDGE_AND_LOGLEVEL) {
                edgeWeight = calcEdgeWeight(node);
                nodeLevel = Math.log(calcNodeLevel(node));
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_VALUES) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = calcBSValueFromLabelNorm(node);
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_LN) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = calcLNBoots(node);
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_LOG2) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = calcLOG2Boots(node);
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_LOG) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = calcLOGBoots(node);
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_LOG_PROPS) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = logBoots(calcBSValueFromLabel(node), 0.02, 0.98);
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_VALUES_LOG_75_01) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = logisticFunction(.1, 75d, parseBSValueFromLabel(node));
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_VALUES_LOG_66_01) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = logisticFunction(.1, (2d / 3d * 100d), parseBSValueFromLabel(node));
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_VALUES_LOG_50_01) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = logisticFunction(.1, 50d, parseBSValueFromLabel(node));
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_AND_LEVEL) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = calcBSValueFromLabelNorm(node);
                nodeLevel = calcNodeLevel(node);
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_AND_LOGLEVEL) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = calcBSValueFromLabelNorm(node);
                nodeLevel = Math.log(calcNodeLevel(node));
            } else if (weights == FlipCutWeights.Weights.NODE_LEVEL) {
                edgeWeight = calcNodeLevel(node);
            } else if (weights == FlipCutWeights.Weights.NODE_LOGLEVEL) {
                edgeWeight = Math.log(calcNodeLevel(node));


                //shitty working stuff
            } else if (weights == FlipCutWeights.Weights.BOOTSTRAP_AND_EDGEWEIGHTS) {
                //ATTENTION: labels unchecked
                //get bootstrap value from label
                edgeWeight = (100d * calcBSValueFromLabelNorm(node)) + calcEdgeWeight(node);

                //UNIT_COST with treeweighting
            } else if (weights == FlipCutWeights.Weights.TREE_WEIGHT) {
                //weighting with tree weight is done below
                edgeWeight = 1;
                //###########################################
                //##### some other experimental weights #####
                //###########################################
            } else {
                edgeWeight = node.getEdgeToParent().getWeight();
                TreeNode root = tree.getRoot();
                /*if (weights == FlipCutWeights.Weights.EDGE_AND_LEVEL) {
                    edgeWeight = edgeWeight * node.getLevel();
                } else*/
                if (weights == FlipCutWeights.Weights.EDGE_AND_ClADESIZE) {
                    edgeWeight = edgeWeight * (root.getLeaves().length - node.getLeaves().length);
                } else if (weights == FlipCutWeights.Weights.EDGE_AND_ClADERATE) {
                    edgeWeight = edgeWeight * (1 - (node.getLeaves().length / root.getLeaves().length));
                } else if (weights == FlipCutWeights.Weights.ClADERATE) {
                    edgeWeight = (1 - (node.getLeaves().length / root.getLeaves().length));
                } else if (weights == FlipCutWeights.Weights.EDGE_AND_GLOBAL_ClADESIZE) {
                    edgeWeight = edgeWeight * (taxaNumber - node.getLeaves().length);
                } else if (weights == FlipCutWeights.Weights.CLADESIZE) {
                    edgeWeight = root.getLeaves().length - node.getLeaves().length;
                } else if (weights == FlipCutWeights.Weights.GLOBAL_CLADESIZE) {
                    edgeWeight = taxaNumber - node.getLeaves().length;

                } else if (weights == FlipCutWeights.Weights.RELATIVE_CLADESIZE) {
                    int leafesNumber = root.getLeaves().length;
                    edgeWeight = (double) (leafesNumber - node.getLeaves().length) / leafesNumber;
                } else if (weights == FlipCutWeights.Weights.EDGE_AND_RELATIVE_CLADESIZE) {
                    int leafesNumber = root.getLeaves().length;
                    edgeWeight = edgeWeight * ((double) (leafesNumber - node.getLeaves().length) / leafesNumber);
                    //Iterative Weights
                } else if (weights == FlipCutWeights.Weights.EDGE_AND_PATH) {
                    double pathLength = 0;
                    TreeNode n = node;
                    while (n.getParent() != null) {
                        pathLength += (n.getDistanceToParent());
                        n = n.getParent();
                    }
                    edgeWeight = edgeWeight * (pathLength);
                } else if (weights == FlipCutWeights.Weights.EDGE_ROOT) {
                    edgeWeight = 0;
                    TreeNode n = node;
                    while (n.getParent() != null) {
                        edgeWeight += (n.getDistanceToParent());
                        n = n.getParent();
                    }
                }
            }


            // rounding and weighting by tree weight
            double treeWeight = parseTreeWeightFromLabel(tree);

            long w;
            if (weights == FlipCutWeights.Weights.TREE_WEIGHT) {
                w = Math.round(Math.max(ZERO, treeWeight) * ACCURACY);
            } else {
                w = Math.round(ACCURACY * Math.max(ZERO, edgeWeight) * Math.max(ZERO, nodeLevel) * Math.max(ZERO, treeWeight));
            }
            return w;
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

    private double calcBSValueFromLabelNorm(TreeNode node) {
        return parseBSValueFromLabel(node) / maxBSValue;
    }

    private double calcBSValueFromLabel(TreeNode node) {
        return parseBSValueFromLabel(node) / 100d;
    }

    private double calcLNBoots(TreeNode node) {
        return Math.log1p(parseBSValueFromLabel(node));
    }

    private double calcLOG2Boots(TreeNode node) {
        return calcLNBoots(node) / Math.log(2);
    }

    private double calcLOGBoots(TreeNode node) {
        return calcLNBoots(node) / Math.log(10);
    }

    private double logBoots(final double bs, final double min, final double max) {
        double p = Math.max(min, Math.min(max, bs));
        return -Math.log(1 - p);
    }

    private double logisticFunction(double k, double x0, double x) {
        return 1 / (1 + (Math.exp((-k * (x - x0)))));
    }

    private double calcNodeLevel(TreeNode node) {
        return (double) node.getLevel() / (double) maxLevel;
    }

    private double calcEdgeWeight(TreeNode node) {
        return (node.getDistanceToParent() / longestBranch);
    }
}
