package mincut.cutGraphAPI;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 10.02.17.
 */

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
     * @param weight  weight of the hyperedge
     * @param character  the hyper edge where source an sink are part of
     *
     */
    void addEdge(V vertex1, V vertex2, long weight, V character);
}
