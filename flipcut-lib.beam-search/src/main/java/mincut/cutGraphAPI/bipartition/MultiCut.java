package mincut.cutGraphAPI.bipartition;

import phylo.tree.algorithm.flipcut.SourceTreeGraphMultiCut;

import java.util.List;

public interface MultiCut<S, G extends SourceTreeGraphMultiCut<S, G>> extends Cut<S> {

    List<G> getSplittedGraphs();
    G sourceGraph();

}
