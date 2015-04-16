package flipcut.mincut;

import flipcut.mincut.bipartition.BasicCut;

import java.util.List;

/**
 * Created by fleisch on 15.04.15.
 */
public interface MultiCutGraph<V> extends CutGraph<V> {
    /**
     * Returns the weights of all calculated cuts of this graph. This
     * computes the cuts lazily.
     *
     * @return values of the all calulated cuts
     */
    default long[] getMinCutValues(){
        List<BasicCut<V>> mins = getMinCuts();
        long[] cutValues = new long[mins.size()];
        for (int i = 0; i < cutValues.length; i++) {
            cutValues[i] = mins.get(i).minCutValue;
        }
        return cutValues;
    }

    /**
     * Resturns a list of Cuts calculated lazily
     *
     * @return mincuts: all cut calculate by this algorithm
     */
    public List<BasicCut<V>> getMinCuts();
}
