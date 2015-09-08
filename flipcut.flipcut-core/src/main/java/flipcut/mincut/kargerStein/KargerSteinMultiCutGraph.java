package flipcut.mincut.kargerStein;

import flipcut.mincut.AbstractMultiCutGraph;
import flipcut.mincut.MultiCutGraph;
import flipcut.mincut.UndirectedCutGraph;
import flipcut.mincut.bipartition.BasicCut;

import java.util.List;

/**
 * Created by fleisch on 15.04.15.
 */
public class KargerSteinMultiCutGraph<V> extends AbstractMultiCutGraph<V> implements MultiCutGraph<V>,UndirectedCutGraph<V> {
    private int nextEdgeColor = 0; // we nee this for uncolored version

    @Override
    public List<BasicCut<V>> getMinCuts() {
        return null;
    }

    @Override
    public BasicCut getMinCut() {
        return null;
    }

    @Override
    public void addNode(V vertex) {
    }

    @Override
    public void addEdge(V vertex1, V vertex2, long capacity) {
        addEdge(vertex1,vertex2,capacity,--nextEdgeColor);
    }

    public void addEdge(V vertex1, V vertex2, long capacity, int color) {
        //todo @Martin this should add vertices automatically if they are not already in the graph
    }

    @Override
    public void calculateMinCuts() {

    }
}
