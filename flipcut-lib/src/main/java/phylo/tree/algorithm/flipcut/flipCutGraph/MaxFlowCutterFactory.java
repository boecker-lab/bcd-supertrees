package phylo.tree.algorithm.flipcut.flipCutGraph;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.algorithm.flipcut.cutter.CutterFactory;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface MaxFlowCutterFactory<C extends CutGraphCutter<S, T>, S, T extends SourceTreeGraph> extends CutterFactory<C, S, T> {
    CutGraphTypes getType();

    @Override
    default boolean isBCD() {
        return getType().isBCD();
    }

    @Override
    default boolean isFlipCut() {
        return getType().isFlipCut();
    }
}
