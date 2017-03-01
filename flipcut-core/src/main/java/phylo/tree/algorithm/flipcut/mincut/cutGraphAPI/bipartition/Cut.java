package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 15.02.17.
 */

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface Cut<V> extends Comparable<Cut<V>> {


    long minCutValue();
    LinkedHashSet<V> getCutSet();
    default int compareTo(Cut<V> o) {
        return Long.compare(minCutValue(), o.minCutValue());
    }
}
