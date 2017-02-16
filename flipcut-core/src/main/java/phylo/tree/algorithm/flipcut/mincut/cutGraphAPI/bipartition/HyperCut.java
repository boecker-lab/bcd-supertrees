package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 15.02.17.
 */

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class HyperCut<V> implements Cut<V> {
    public final LinkedHashSet<V> cutTaxaSource;
    public final LinkedHashSet<V> cutEdges;
    public final long minCutValue;

    public HyperCut(LinkedHashSet<V> cutTaxaSource, LinkedHashSet<V> cutEdges, long mincutValue) {
        this.cutEdges = cutEdges;
        this.cutTaxaSource = cutTaxaSource;
        this.minCutValue = mincutValue;
    }

    @Override
    public long minCutValue() {
        return minCutValue;
    }
}
