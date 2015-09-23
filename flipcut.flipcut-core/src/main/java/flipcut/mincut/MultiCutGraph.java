package flipcut.mincut;

import flipcut.mincut.bipartition.BasicCut;

import java.util.List;

/**
 * Created by fleisch on 15.04.15.
 */
public interface MultiCutGraph<V> extends CutGraph<V>{
    /**
     * Resturns a list of Cuts calculated lazily
     *
     * @return mincuts: all cut calculate by this algorithm
     */
    List<BasicCut<V>> calculateMinCuts();
    List<BasicCut<V>> calculateMinCuts(int numberOfCuts);
}
