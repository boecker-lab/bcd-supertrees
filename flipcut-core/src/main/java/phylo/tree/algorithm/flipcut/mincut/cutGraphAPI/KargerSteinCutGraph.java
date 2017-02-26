package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import phylo.tree.algorithm.flipcut.mincut.EdgeColor;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.HyperCut;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.*;

import java.util.*;

/**
 * Created by fleisch on 15.04.15.
 */
public class KargerSteinCutGraph<V> implements MultiCutGraph<V>, EdgeColorableUndirectedGraph<V> {
    private static final boolean RESCURSIVE_KARGER = true;
    private final boolean allowDuplicates = false;
    private TIntObjectMap<V> vertexMap = new TIntObjectHashMap<>();
    private Map<V, Vertex> vertexMapBack = new HashMap<>();
    private BiMap<V, EdgeColor> charactermap = HashBiMap.create();

    private Graph g;
    private int vertexIndex = 0;
    private EdgeWeighter weighter;

    public KargerSteinCutGraph(EdgeWeighter weighter) {
        this.weighter = weighter;
        clear();
    }

    public KargerSteinCutGraph() {
        this(new EdgeWeighter() {
        });
    }


    @Override
    public LinkedList<HyperCut<V>> calculateMinCuts() {
        KargerStein cutter = new KargerStein();
        LinkedHashSet<Graph> cuts = cutter.getMinCuts(g,RESCURSIVE_KARGER);

        LinkedList<HyperCut<V>> basicCuts = buildCuts(cuts);
        Collections.sort(basicCuts);
        return basicCuts;
    }

    private LinkedList<HyperCut<V>> buildCuts(Collection<Graph> sourceCuts){
        LinkedList<HyperCut<V>> basicCuts = new LinkedList<>();
        for (Graph c : sourceCuts) {
            LinkedHashSet<V> cutset = new LinkedHashSet<>();

            Vertex source = c.getVertices().valueCollection().iterator().next();
            source.getMergedLbls().forEach(v -> {
                cutset.add(vertexMap.get(v));
                return true;
            });

            LinkedHashSet<V> cutEdges = new LinkedHashSet<>(source.getEdges().size());
            for (EdgeColor color : c.getEdgeColors()) {
                cutEdges.add(charactermap.inverse().get(color));
            }


            long mincutValue = (long) c.mincutValue();
            if (!charactermap.isEmpty()) {
                mincutValue = (long) cutEdges.stream().mapToDouble(cc -> charactermap.get(cc).getWeight()).sum();
            }
            basicCuts.add(new HyperCut<V>(cutset, cutEdges, mincutValue));
        }
        return basicCuts;
    }

    @Override
    public List<HyperCut<V>> calculateMinCuts(int numberOfCuts) {
        if (numberOfCuts == 0) return null;

        if (numberOfCuts > 1) {
            return calculateMinCuts().subList(0, numberOfCuts);
        } else {
            return buildCuts(Arrays.asList(new KargerStein().sampleCut(g)));
        }
    }

    @Override
    public HyperCut<V> calculateMinCut() {
        Graph cut = new KargerStein().sampleCut(g);
        return buildCuts(Arrays.asList(cut)).get(0);
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
    public void addEdge(V vertex1, V vertex2, long capacity, V hyperedge) {
        if (!vertexMapBack.containsKey(vertex1))
            addNode(vertex1);
        if (!vertexMapBack.containsKey(vertex2))
            addNode(vertex2);
        EdgeColor color = charactermap.get(hyperedge);
        if (color == null) {
            color = new EdgeColor(capacity);
            if (hyperedge != null) //todo proof
                charactermap.put(hyperedge, color);
        }
        g.addEdge(vertexMapBack.get(vertex1), vertexMapBack.get(vertex2), color);
    }

    @Override
    public void clear() {
        vertexMap.clear();
        g = new Graph(/*weighter*/);
        vertexIndex = 0;
        charactermap.clear();
    }


}
