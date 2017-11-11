package phylo.tree.algorithm.flipcut;


import phylo.tree.algorithm.flipcut.cutter.GraphCutter;

import java.util.List;

public interface SourceTreeGraph<C> {


    /**
     * Remove semi universal characters
     */
    void deleteSemiUniversals();

    /**
     * Splits this graph into two disconnected graphs, one consisting of the given set
     * of nodes, the other graph consists of all vertices not contained in the given
     * set of nodes.
     *
     * @return graphs list of two graphs created
     */
    List<? extends SourceTreeGraph<C>> getPartitions(GraphCutter<C> c);


    Iterable<String> taxaLabels();

    int numTaxa();
    int numCharacter();

//    boolean hastActiveScaffoldPartitions();
}
