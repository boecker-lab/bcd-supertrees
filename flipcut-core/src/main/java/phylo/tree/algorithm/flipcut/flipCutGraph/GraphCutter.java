package phylo.tree.algorithm.flipcut.flipCutGraph;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface GraphCutter<N extends AbstractFlipCutNode<N>,T extends AbstractFlipCutGraph<N>>  {
    LinkedHashSet<N> getMinCutSet(T source);
    long getMinCutValue(T source);
    void clear();
    List<T> cut(T source);
}
