package phylo.tree.algorithm.flipcut.cutter;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import mincut.cutGraphAPI.bipartition.Cut;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface GraphCutter<S, T extends SourceTreeGraph> {
    void clear();

    Cut<S> cut(T source);

    Cut<S> getMinCut();

    default boolean isFlipCut() {
        return !isBCD();
    }

    boolean isBCD();
}
