package phylo.tree.algorithm.flipcut.flipCutGraph;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.cutter.CutterFactory;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface MultiCutterFactory<C extends MultiCutter<S, T>, S, T extends SourceTreeGraph> extends CutterFactory<C, S, T> {

}
