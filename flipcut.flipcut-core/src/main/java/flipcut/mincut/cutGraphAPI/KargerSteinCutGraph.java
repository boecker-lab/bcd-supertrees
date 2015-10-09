package flipcut.mincut.cutGraphAPI;

import flipcut.mincut.cutGraphAPI.MultiCutGraph;
import flipcut.mincut.cutGraphAPI.bipartition.BasicCut;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by fleisch on 15.04.15.
 */
public class KargerSteinCutGraph<V> implements MultiCutGraph<V> {
    private int nextEdgeColor = 0; // we nee this for uncolored version
    //todo fill me

    @Override
    public List<BasicCut<V>> calculateMinCuts() {
        return null;
    }

    @Override
    public List<BasicCut<V>> calculateMinCuts(int numberOfCuts) {
        return null;
    }

    @Override
    public BasicCut<V> calculateMinCut() throws ExecutionException, InterruptedException {
        return null;
    }

    @Override
    public void addNode(V vertex) {

    }

    @Override
    public void addEdge(V vertex1, V vertex2, long capacity) {

    }

    @Override
    public void clear() {

    }
}
