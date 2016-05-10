package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI;

import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.BasicCut;

import java.util.concurrent.ExecutionException;

/**
 * Created by fleisch on 23.09.15.
 */
public interface CutGraph<V> {

    /**
     * Returns the minimum cut.
     * This does lazy computation, if the cut was not computed, it computes the cut.
     *
     * @return mincut all connected components of the input graph
     */
    BasicCut<V> calculateMinCut() throws ExecutionException, InterruptedException;

    /**
     * Adds the given node to the graph. Does nothing if the graph already contains the node.
     *
     * @param vertex the source
     */
    void addNode(V vertex);

    /**
     * Add an edge from source to sink with given capacity. This will add the source or sink
     * if they are not added to the graph already.
     *
     * @param vertex1  the source
     * @param vertex2  the sink
     * @param capacity the capacity
     */
    void addEdge(V vertex1, V vertex2, long capacity);
    void clear();
}
