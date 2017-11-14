package mincut.cutGraphAPI;


/**
 * Created by fleisch on 23.09.15.
 */
public interface CutGraph<V> {

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
