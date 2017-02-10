package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.mincut.EdgeColor;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.BasicCut;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.Graph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.KargerStein;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.Vertex;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by fleisch on 15.04.15.
 */
public class KargerSteinCutGraph<V> implements MultiCutGraph<V>, EdgeColorableUndirectedGraph<V> {
    private TIntObjectMap<V> vertexMap = new TIntObjectHashMap<V>();
    private Map<V, Vertex> vertexMapBack = new HashMap<V, Vertex>();
    private Graph g = new Graph();
    private int vertexIndex = 0;

    private TreeMap<Double, TIntSet> calculate() {
        KargerStein cutter = new KargerStein();
        return cutter.getMinCut(g);
    }

    @Override
    public List<BasicCut<V>> calculateMinCuts() {
        TreeMap<Double, TIntSet> cuts = calculate();
        List<BasicCut<V>> basicCuts = new ArrayList<>(cuts.size());

        for (Map.Entry<Double, TIntSet> c : cuts.entrySet()) {
            LinkedHashSet<V> cutset = new LinkedHashSet<V>(c.getValue().size());
            c.getValue().forEach(v -> {
                cutset.add(vertexMap.get(v));
                return true;
            });
            basicCuts.add(new BasicCut<V>(cutset, (long) (c.getKey() * CostComputer.ACCURACY)));
        }
        return basicCuts;
    }

    @Override
    public List<BasicCut<V>> calculateMinCuts(int numberOfCuts) {
        return calculateMinCuts().subList(0, numberOfCuts);
    }

    @Override
    public BasicCut<V> calculateMinCut() throws ExecutionException, InterruptedException {
        return calculateMinCuts(1).get(0);
    }

    @Override
    public void addNode(V vertex) {
        vertexMap.put(vertexIndex, vertex);
        Vertex v = new Vertex(vertexIndex);
        vertexMapBack.put(vertex, v);
        g.addVertex(v);
        vertexIndex++;
    }

    @Override
    public void addEdge(V vertex1, V vertex2, long capacity) {
        addEdge(vertex1, vertex2, capacity, null);
    }

    @Override
    public void addEdge(V vertex1, V vertex2, long capacity, EdgeColor color) {
        if (!vertexMapBack.containsKey(vertex1))
            addNode(vertex1);
        if (!vertexMapBack.containsKey(vertex2))
            addNode(vertex2);
        g.addEdge(vertexMapBack.get(vertex1), vertexMapBack.get(vertex2), capacity, color);
    }

    @Override
    public void clear() {
        vertexMap.clear();
        g = new Graph();
        vertexIndex = 0;
    }


}
