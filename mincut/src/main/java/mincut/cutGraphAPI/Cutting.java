package mincut.cutGraphAPI;

import mincut.cutGraphAPI.bipartition.Cut;

import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;

public interface Cutting<V> {
    /**
     * Returns the minimum cut.
     * This does lazy computation, if the cut was not computed, it computes the cut.
     *
     * @return mincut all connected components of the input graph
     */
    Cut<LinkedHashSet<V>> calculateMinCut() throws ExecutionException, InterruptedException;
}
