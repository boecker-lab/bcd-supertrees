package phylo.tree.algorithm.flipcut.cutter;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;

import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface CutterFactory< C extends GraphCutter<S,T>,S,T extends SourceTreeGraph> {
    C newInstance(T graph);
    C newInstance(T graph,ExecutorService executorService, int threads);
}
