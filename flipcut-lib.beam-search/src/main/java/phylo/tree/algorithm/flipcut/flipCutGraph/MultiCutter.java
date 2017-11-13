package phylo.tree.algorithm.flipcut.flipCutGraph;

import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.cutter.GraphCutter;
import mincut.cutGraphAPI.bipartition.MultiCut;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 19.04.13
 * Time: 15:19
 */
public interface MultiCutter<S, T extends SourceTreeGraph<S>> extends GraphCutter<S> {
    MultiCut<S,T> getNextCut();
}
