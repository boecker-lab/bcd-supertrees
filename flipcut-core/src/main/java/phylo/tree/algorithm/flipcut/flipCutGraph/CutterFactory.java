package phylo.tree.algorithm.flipcut.flipCutGraph;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface CutterFactory< C extends GraphCutter<N,T>,N extends AbstractFlipCutNode<N>,T extends AbstractFlipCutGraph<N>> {
    C newInstance(T graph);
    C newInstance(T graph,ExecutorService executorService, int threads);
}
