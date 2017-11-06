package mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 28.02.17.
 */

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class DefaultBipartition<V> extends AbstractBipartition<V> {
    public DefaultBipartition(long minCutValue, LinkedHashSet<V> sSet, LinkedHashSet<V> tSet) {
        super(minCutValue, sSet, tSet);
    }

    public static class Factory<V> implements CutFactory<LinkedHashSet<V>,DefaultBipartition<V>> {
        @Override
        public DefaultBipartition<V> newCutInstance(LinkedHashSet<V> cutTaxaSource, LinkedHashSet<V> cutTaxaSink, LinkedHashSet<V> cutEdges, long mincutValue) {
            return new DefaultBipartition<V>(mincutValue, cutTaxaSource, cutTaxaSink);
        }

        public DefaultBipartition<V> newCutInstance(LinkedHashSet<V> sSet, LinkedHashSet<V> tSet, long mincutValue) {
            return new DefaultBipartition<V>(mincutValue, sSet, tSet);
        }
    }
}
