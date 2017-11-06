package mincut.cutGraphAPI;


import mincut.cutGraphAPI.bipartition.Cut;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by fleisch on 15.04.15.
 */
public interface MultiCutGraph<V> extends CutGraph<V> {
    /**
     * Resturns a list of Cuts calculated lazily
     *
     * @return mincuts: all cut calculate by this algorithm
     */
    List<? extends Cut<LinkedHashSet<V>>> calculateMinCuts();

    List<? extends Cut<LinkedHashSet<V>>> calculateMinCuts(int numberOfCuts);
}
