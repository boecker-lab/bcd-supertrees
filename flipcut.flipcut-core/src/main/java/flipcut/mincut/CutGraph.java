package flipcut.mincut;

import flipcut.mincut.bipartition.BasicCut;

/**
 * Created by fleisch on 14.04.15.
 */
public interface CutGraph<V> {

    /**
     * Returns the weight of the minimum of this graph. This
     * computes the cut lazily and just once for a given soruce and sink.
     *
     * @return cutValue the cut value for the cut between source and sink
     */
    long getMinCutValue();
    /*default long getMinCutValue(){
        if (getMinCut() == null)
            return -1;
        return getMinCut().minCutValue;
    }*/

    /**
     * Returns the minimum cut.
     * This does lazy computation, if the cut was not computed, it computes the cut.
     *
     * @return mincut all connected components of the input graph
     */
    public BasicCut getMinCut();
    /**
     * Adds the given node to the graph. Does nothing if the graph already contains the node.
     *
     * @param vertex the source
     */
    public void addNode(V vertex);

    /**
     * Add an edge from source to sink with given capacity. This will add the source or sink
     * if they are not added to the graph already.
     *
     * @param vertex1   the source
     * @param vertex2   the sink
     * @param capacity the capacity
     */
    public void addEdge(V vertex1, V vertex2, long capacity);
}
