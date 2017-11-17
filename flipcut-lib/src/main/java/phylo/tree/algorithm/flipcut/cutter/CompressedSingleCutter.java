package phylo.tree.algorithm.flipcut.cutter;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import mincut.cutGraphAPI.CompressedGoldbergTarjanCutGraph;
import mincut.cutGraphAPI.bipartition.CompressedBCDCut;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.STCut;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.CutGraphImpl;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.Node;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.Hyperedge;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class CompressedSingleCutter implements GraphCutter<RoaringBitmap> {

    private CompressedBCDCut cachedCut = null;
    private final int threats;
    private final ExecutorService executorService;


    public CompressedSingleCutter() {
        this.threats = 1;
        this.executorService = null;
    }

    public CompressedSingleCutter(int threats, ExecutorService executorService) {
        this.threats = threats;
        this.executorService = executorService;
    }

    @Override
    public void clear() {
        cachedCut = null;
    }

    @Override
    public Cut<RoaringBitmap> cut(SourceTreeGraph<RoaringBitmap> source) {
        return cut((CompressedBCDGraph) source);
    }

    public Cut<RoaringBitmap> cut(final CompressedBCDGraph source) {
        TIntObjectMap<TIntList> charMapping = new TIntObjectHashMap<>();
        final CompressedGoldbergTarjanCutGraph cutGraph = createCutGraph(source, charMapping);
        cutGraph.setThreads(threats);
        cutGraph.setExecutorService(executorService);

        //we do not have to map merged taxa back, hence we need only the hyperedges we have to delete
        try {
            STCut cut = cutGraph.calculateMinCut();
            RoaringBitmap toDelete = new RoaringBitmap();

            LinkedHashSet[] css = new LinkedHashSet[]{cut.getsSet(), cut.gettSet()};
            for (LinkedHashSet cs : css) {
                for (Object o : cs) {
                    if (o instanceof Integer) {
                        final int index = (int) o;
                        if (!cs.contains(source.getEdge(index))) {
                            TIntList indeces = charMapping.get(index);
                            if (indeces == null) {
                                toDelete.add(index);
                            } else {
                                indeces.forEach(i -> {
                                    toDelete.add(i);
                                    return true;
                                });
                            }
                        }
                    }
                }
            }

            cachedCut = new CompressedBCDCut(toDelete, cut.minCutValue());
            return cachedCut;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace(); //todo logging
            return null;
        }
    }

    public static CompressedGoldbergTarjanCutGraph createCutGraph(CompressedBCDGraph source, TIntObjectMap<TIntList> charMapping) {
        //create cutgraph with merged taxa
        List<RoaringBitmap> guiEdges = new ArrayList<>(source.numGuideEdges());
        if (source.hasGuideEdges()) {
            for (Hyperedge guidEdge : source.guideHyperEdges()) {
                guiEdges.add(guidEdge.ones);
            }
        }

        final TObjectIntMap<Object> nodeToEdges = new TObjectIntHashMap<>(source.numTaxa() + 2 * source.numCharacter());
        final AtomicInteger numEdges = new AtomicInteger(0);
        final Map<RoaringBitmap, TIntList> hyperEdgeMerging = new HashMap<>();

        // add edge to cutgraph (maybe with merged taxa)
        source.characters.forEach((IntConsumer) edgeIndex -> {
            Hyperedge edge = source.getEdge(edgeIndex);
            RoaringBitmap edgeOnes = edge.ones;

            // do taxa merging if needed
            for (RoaringBitmap guideOnes : guiEdges) {
                if (RoaringBitmap.intersects(edgeOnes, guideOnes)) {
                    RoaringBitmap common = RoaringBitmap.and(guideOnes, edgeOnes);
                    common.xor(edgeOnes);
                    edgeOnes = common;
                    edgeOnes.add(guideOnes.first());
                }
            }

            if (edgeOnes.getCardinality() > 1) {
                //merging hyperedges, that are identical because of guide tree merging
                TIntList merged = hyperEdgeMerging.get(edgeOnes);
                if (merged == null) {
                    merged = new TIntLinkedList();
                    hyperEdgeMerging.put(edgeOnes, merged);

                    nodeToEdges.adjustOrPutValue(edgeIndex, 1, 1);
                    nodeToEdges.adjustOrPutValue(edge, 1, 1);

                    numEdges.getAndAdd(2);

                    edgeOnes.forEach((IntConsumer) taxonIndex -> {
                        final String t = source.getTaxon(taxonIndex);

                        nodeToEdges.adjustOrPutValue(edgeIndex, 1, 1);
                        nodeToEdges.adjustOrPutValue(edge, 1, 1);
                        nodeToEdges.adjustOrPutValue(t, 2, 2);

                        numEdges.getAndAdd(4);
                    });
                }
                merged.add(edgeIndex);
            }
        });


        Map<String, Node> cutgraphTaxa = new HashMap<>(source.numTaxa());
        final CutGraphImpl hipri = new CutGraphImpl(nodeToEdges.size(), numEdges.get());

        hyperEdgeMerging.forEach((edgeOnes, mergedIndeces) -> {
            int edgeIndex = mergedIndeces.get(0);
            Hyperedge edge = source.getEdge(edgeIndex);
            Node out = hipri.createNode(edgeIndex, nodeToEdges.get(edge));
            Node in = hipri.createNode(edge, nodeToEdges.get(edge));

            long weight = 0;
            if (mergedIndeces.size() > 1) {
                TIntIterator it = mergedIndeces.iterator();
                while (it.hasNext()) {
                    weight += source.getEdge(it.next()).getWeight();
                }
                charMapping.put(edgeIndex, mergedIndeces);
            } else {
                weight = edge.getWeight();
            }

            hipri.addEdge(out, in, weight);

            edgeOnes.forEach((IntConsumer) taxonIndex -> {
                final String taxon = source.getTaxon(taxonIndex);
                Node t = cutgraphTaxa.get(taxon);
                if (t == null) {
                    t = hipri.createNode(taxon, nodeToEdges.get(taxon));
                    cutgraphTaxa.put(taxon, t);
                }
                hipri.addEdge(t, out, CutGraphCutter.getInfinity());
                hipri.addEdge(in, t, CutGraphCutter.getInfinity());
            });
        });

        CompressedGoldbergTarjanCutGraph cutGraph = new CompressedGoldbergTarjanCutGraph(hipri);
        Iterator<Node> taxit = cutgraphTaxa.values().iterator();
        Node s = taxit.next();
        while (taxit.hasNext()) {
            cutGraph.submitSTCutCalculation(s, taxit.next());
        }

        return cutGraph;
    }


    @Override
    public Cut<RoaringBitmap> getMinCut() {
        return cachedCut;
    }

    @Override
    public boolean isBCD() {
        return true;
    }

    public static class CompressedSingleCutterFactory implements CutterFactory<CompressedSingleCutter, RoaringBitmap, CompressedBCDGraph> {

        @Override
        public CompressedSingleCutter newInstance(CompressedBCDGraph graph) {
            return new CompressedSingleCutter();
        }

        @Override
        public CompressedSingleCutter newInstance(CompressedBCDGraph graph, ExecutorService executorService,
                                                  int threads) {
            return new CompressedSingleCutter(threads, executorService);
        }

        @Override
        public boolean isBCD() {
            return true;
        }
    }
}
