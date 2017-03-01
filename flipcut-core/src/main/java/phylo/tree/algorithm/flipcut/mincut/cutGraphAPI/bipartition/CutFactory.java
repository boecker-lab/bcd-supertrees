package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 28.02.17.
 */

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface CutFactory<V,C extends Cut<V>> {
    C newCutInstance(LinkedHashSet<V> cutTaxaSource, LinkedHashSet<V> cutTaxaSink, LinkedHashSet<V> cutEdges, long mincutValue);
}
