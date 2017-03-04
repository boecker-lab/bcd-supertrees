package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import phylo.tree.algorithm.flipcut.mincut.EdgeColor;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.AbstractBipartition;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.CutFactory;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.EdgeWeighter;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.Graph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.KargerStein;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.Vertex;

import java.util.*;

/**
 * Created by fleisch on 15.04.15.
 */
public class KargerSteinCutGraph<V, C extends CutFactory<V, ? extends AbstractBipartition<V>>> implements MultiCutGraph<V>, EdgeColorableUndirectedGraph<V> {
    private static final boolean RESCURSIVE_KARGER = true;
    private final boolean allowDuplicates = false;
    private TIntObjectMap<V> vertexMap = new TIntObjectHashMap<>();
    private Map<V, Vertex> vertexMapBack = new HashMap<>();
    private BiMap<V, EdgeColor> charactermap = HashBiMap.create();

    private Graph g;
    private int vertexIndex = 0;
    private EdgeWeighter weighter;

    private final C cutFactory;

    public KargerSteinCutGraph(EdgeWeighter weighter, C cutFactory) {
        this.weighter = weighter;
        this.cutFactory = cutFactory;
        clear();
    }

    public KargerSteinCutGraph(C cutFactory) {
        this(new EdgeWeighter() {
        }, cutFactory);
    }


    @Override
    public List<AbstractBipartition<V>> calculateMinCuts() {
        KargerStein cutter = new KargerStein();
        LinkedHashSet<Graph> cuts = cutter.getMinCuts(g, RESCURSIVE_KARGER);

        ArrayList<AbstractBipartition<V>> basicCuts = new ArrayList<>(cuts.size());
        for (Graph cut : cuts) {
            basicCuts.add(buildCut(cut));
        }
        Collections.sort(basicCuts);
        return basicCuts;
    }

    private AbstractBipartition<V> buildCut(Graph c) {
        Iterator<Vertex> vIt = c.getVertices().valueCollection().iterator();

        //get source taxa set
        Vertex source = vIt.next();
        LinkedHashSet<V> sSet = new LinkedHashSet<>();
        source.getMergedLbls().forEach(v -> {
            sSet.add(vertexMap.get(v));
            return true;
        });

        //get targe taxa set
        Vertex target = vIt.next();
        LinkedHashSet<V> tSet = new LinkedHashSet<>();
        target.getMergedLbls().forEach(v -> {
            tSet.add(vertexMap.get(v));
            return true;
        });

        //get edges
        LinkedHashSet<V> cutEdges = new LinkedHashSet<>(source.getEdges().size());
        for (EdgeColor color : c.getEdgeColors()) {
            cutEdges.add(charactermap.inverse().get(color));
        }
        //get cutsocre from edges
        long mincutValue = (long) c.mincutValue();
        if (!charactermap.isEmpty()) {
            mincutValue = (long) cutEdges.stream().mapToDouble(cc -> charactermap.get(cc).getWeight()).sum();
        }
        return cutFactory.newCutInstance(sSet, tSet, cutEdges, mincutValue);
    }


    @Override
    public List<AbstractBipartition<V>> calculateMinCuts(int numberOfCuts) {
        if (numberOfCuts == 0) return null;

        if (numberOfCuts > 1) {
            List<AbstractBipartition<V>> cuts = calculateMinCuts();
            return cuts.subList(0, Math.min(numberOfCuts, cuts.size()));
        } else {
            return Arrays.asList(calculateMinCut());
        }
    }

    @Override
    public AbstractBipartition<V> calculateMinCut() {
        return buildCut(new KargerStein().getMinCut(g));
    }

    public AbstractBipartition<V> sampleCut() {
        return buildCut(new KargerStein().sampleCut(g));
    }

    public List<AbstractBipartition<V>> sampleCuts(int numberOfCuts) {
        KargerStein algo = new KargerStein();
        Set<Graph> graphs = new HashSet<>(numberOfCuts);

        for (int i = 0; i < numberOfCuts; i++) {
            graphs.add(algo.sampleCut(g));
        }

        Iterator<Graph> it = graphs.iterator();

        List<AbstractBipartition<V>> cuts = new ArrayList<>(graphs.size());
        while (it.hasNext()) {
            cuts.add(buildCut(it.next()));
            it.remove();//dont know if i should do that
        }
        return cuts;
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
        addEdge(vertex1,vertex2,capacity,hyperedge,false);
    }

    public void addEdge(V vertex1, V vertex2, long capacity, V hyperedge,  boolean uncutable) {
        if (!vertexMapBack.containsKey(vertex1))
            addNode(vertex1);
        if (!vertexMapBack.containsKey(vertex2))
            addNode(vertex2);
        EdgeColor color = charactermap.get(hyperedge);
        if (color == null) {
            color = new EdgeColor(capacity,uncutable);
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
