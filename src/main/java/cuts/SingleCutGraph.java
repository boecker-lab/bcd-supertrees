package cuts;

import flipCut.flipCutGraph.AbstractFlipCutNode;

import java.util.List;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 30.11.12
 * Time: 16:09
 */
public interface SingleCutGraph<T extends AbstractFlipCutNode<T>> {

    /**
     * Returns the minimum cut value. This
     * computes the cut lazily and just once .
     *
     * @return cutValue the cut value of the minimum cuts
     */
    public long getMinCutValue();

    /**
     * Returns the list of nodes in the target component of the graph.
     * This does lazy computation, if the cut was not computed, it computes the cut.
     *
     * @return mincut all nodes of one component
     */
    public List<T> getMinCut();
}
