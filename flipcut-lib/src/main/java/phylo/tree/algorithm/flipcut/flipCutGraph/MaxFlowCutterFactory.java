package phylo.tree.algorithm.flipcut.flipCutGraph;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.algorithm.flipcut.cutter.CutterFactory;

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface MaxFlowCutterFactory<C extends CutGraphCutter<LinkedHashSet<N>, T>, N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>> extends CutterFactory<C,LinkedHashSet<N>,T> {
    CutGraphTypes getType();
}
