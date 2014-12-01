package mincut;

import java.util.List;

/**
 * Created by fleisch on 28.11.14.
 */
public abstract class CutGraph<T> {
    /**
     * The current cut value
     */
    protected long cutValue = -1;
    /**
     * All nodes of the component that contains the sink
     */
    protected List<T> cut;
    /**
     * The source
     */
    protected T source;
    /**
     * The target
     */
    protected T target;

    /**
     * Returns the minimum cut value for a cut between the source and the target node. This
     * computes the cut lazily and just once for a given soruce and target.
     *
     * @param source the source node
     * @param target the target node
     * @return cutValue the cut value for the cut between source and target
     */
    public abstract long getMinCutValue(T source, T target);

    /**
     * Returns the list of nodes in the target component of the graph, including the target node itself.
     * This does lazy computation, if the cut was not computed for given source and sink, it computes the cut.
     *
     * @param source the source
     * @param target the target
     * @return mincut all nodes of the component that contains the target node (incl. the target node)
     */
    public abstract List<T> getMinCut(T source, T target);

    /**
     * Adds the given node to the graph. Does nothing if the graph already contains the node.
     *
     * @param source the source
     */
    public abstract void addNode(T source);

    /**
     * Add an edge from source to target with given capacity. This will add the source or target
     * if they are not added to the graph already.
     *
     * @param source   the source
     * @param target   the target
     * @param capacity the capacity
     */
    public abstract void addEdge(T source, T target, long capacity);



}
