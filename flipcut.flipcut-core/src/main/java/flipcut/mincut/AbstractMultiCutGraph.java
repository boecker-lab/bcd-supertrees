package flipcut.mincut;

import flipcut.mincut.bipartition.BasicCut;

import java.util.List;

/**
 * Created by fleisch on 07.05.15.
 */
public abstract class AbstractMultiCutGraph<V> extends AbstractSingelCutGraph<V> implements MultiCutGraph<V>{
    @Override
    public long[] getMinCutValues(){
        List<BasicCut<V>> mins = getMinCuts();
        long[] cutValues = new long[mins.size()];
        for (int i = 0; i < cutValues.length; i++) {
            cutValues[i] = mins.get(i).minCutValue;
        }
        return cutValues;
    }
}
