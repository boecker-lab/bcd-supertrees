package phylo.tree.algorithm.flipcut.costComputer;

/**
 * User: Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * 10.07.12 11:20
 */
public interface FlipCutWeights {
    static enum Weights{
        /**
         * Use the incoming edge weights
         */
        EDGE_WEIGHTS,
        /**
         * Use the nodes level
         */
        NODE_LEVEL,
        /**
         * Use the nodes level
         */
        NODE_LOGLEVEL,
        /**
         * Use the nodes level form leaf to node
         */
        NODE_LEVEL_BOTTOM_UP,
        /**
         * Use the number of leaves under the node
         */
        CLADESIZE,
        /**
         * Use the incoming edge weight weighted by the nodes level
         */
        EDGE_AND_LEVEL,
        /**
         * Use the incoming edge weight weighted by the nodes level form leaf to node
         */
        EDGE_AND_LOGLEVEL,
        /**
         * Use the incoming edge weight weighted by the nodes level form leaf to node
         */
        EDGE_AND_LEVEL_BOTTOM_UP,
        /**
         * Use the incoming edge weight weighted by the number of leaves under the node
         */
        EDGE_AND_ClADESIZE,
        /**
         * Use the incoming edge weight weighted by the sum of the edge weight on the path to the root
         */
        EDGE_AND_PATH,
        /**
         * Use the pathlength to the root
         */
        EDGE_ROOT,
        /**
         * Uni costs
         */
        UNIT_COST,
        /**
         * strange SAC costs from bwd
         */
        STRANGE,
        /**
         * Uses Distances from one Vertex to any other of the clade, relative to the number of taxa
         */
        UNWEIGHTED_DISTANCES,
        /**
         * Uses Distances from one Vertex to any other of the clade
         */
        WEIGHTED_DISTANCES,
        /**
         * Uses Distances from one Vertex to any other of the clade, relative to the number of taxa, weighted by the EDGE_AND_CLADE score
         */
        EDGE_AND_CLADE_PLUS_DISTANCES,
        /**
         * Uses Distances from one Vertex to any other of the clade, relative to the number of taxa, weighted by the EDGE_AND_CLADE score
         */
        FLIPS_PLUS_EDGE_AND_CLADE_PLUS_DISTANCES,
        /**
         * Uses only bootstrap Values of the corresponding chararacter as multiplier
         */
        BOOTSTRAP_VALUES,
        /**
         * Uses weight of the incoming edge of the corresponding chraracter and it's bootstrap Value as multiplier
         */
        BOOTSTRAP_AND_EDGEWEIGHTS,
        /**
         * Uses weight of the incoming edge of the corresponding chraracter and it's bootstrap Value as multiplier
         */
        BOOTSTRAP_AND_LEVEL,
        /**
         * Uses weight of the incoming edge of the corresponding chraracter and it's bootstrap Value as multiplier
         */
        BOOTSTRAP_AND_LOGLEVEL,

        TREE_WEIGHT,

        EDGE_AND_GLOBAL_ClADESIZE, GLOBAL_CLADESIZE, EDGE_AND_GLOBAL_CLADE_PLUS_DISTANCES, EDGE_PLUS_DISTANCES, EDGE_AND_RELATIVE_CLADE_PLUS_DISTANCES, RELATIVE_CLADESIZE, EDGE_AND_RELATIVE_CLADESIZE, EDGE_AND_ClADERATE, EDGE_AND_CLADERATE_PLUS_DISTANCES, ClADERATE, TREE_SIZE,

        BOOTSTRAP_AND_TREE_SIZE,

        RELATIVE_DISTANCES_AND_BOOTSTRAP
    }
}
