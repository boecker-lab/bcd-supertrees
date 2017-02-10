package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 10.02.17.
 */

import phylo.tree.algorithm.flipcut.mincut.EdgeColor;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface EdgeColorableUndirectedGraph<V> extends CutGraph<V> {
    /**
     * Add an edge from source to sink with given capacity. This will add the source or sink
     * if they are not added to the graph already.
     *
     * @param vertex1  the source
     * @param vertex2  the sink
     * @param capacity the capacity
     * @param color color of the edge
     */
    void addEdge(V vertex1, V vertex2, long capacity, EdgeColor color);
}
