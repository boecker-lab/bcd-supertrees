package phylo.tree.algorithm.flipcut.cutter;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
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
        final TIntObjectMap<TIntList> charMapping = new TIntObjectHashMap<>();
        final TIntObjectMap<Node.IntNode> cutgraphTaxa = new TIntObjectHashMap<>(source.numTaxa());

        final CutGraphImpl hipri = createHipri(
                source, createGuideEdges(source), charMapping, cutgraphTaxa
        );
        CompressedGoldbergTarjanCutGraph cutGraph = new CompressedGoldbergTarjanCutGraph(hipri);
        TIntObjectIterator<Node.IntNode> taxit = cutgraphTaxa.iterator();
        taxit.advance();
        Node s = taxit.value();
        while (taxit.hasNext()) {
            taxit.advance();
            cutGraph.submitSTCutCalculation(s, taxit.value());
        }


        cutGraph.setThreads(threats);
        cutGraph.setExecutorService(executorService);

        //we do not have to map merged taxa back, hence we need only the hyperedges we have to delete
        try {
            STCut cut = cutGraph.calculateMinCut();
            RoaringBitmap toDelete = new RoaringBitmap();

            LinkedHashSet[] css = new LinkedHashSet[]{cut.getsSet(), cut.gettSet()};
            for (LinkedHashSet cs : css) {
                for (Object o : cs) {
                    //search for indeces
                    final int index = (int) o;
                    //find character indeces -> larger indeces are taxa ;-)
                    if (source.isCharacter(index)) {
                        if (!cs.contains(source.getCloneIndex(index))) {
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

    public static List<RoaringBitmap> createGuideEdges(CompressedBCDGraph source) {
        //create cutgraph with merged taxa
        List<RoaringBitmap> guiEdges = new ArrayList<>(source.numGuideEdges());
        if (source.hasGuideEdges()) {
            for (Hyperedge guidEdge : source.guideHyperEdges()) {
                guiEdges.add(guidEdge.ones);
            }
        }
        return guiEdges;
    }

    public static CutGraphImpl createHipri(CompressedBCDGraph source, List<RoaringBitmap> guiEdges, TIntObjectMap<TIntList> charMapping, TIntObjectMap<Node.IntNode> cutgraphTaxa) {
        final TIntIntMap nodeToEdges = new TIntIntHashMap(source.numTaxa() + 2 * source.numCharacter());
        final Map<RoaringBitmap, TIntList> hyperEdgeMerging = new HashMap<>();
        final AtomicInteger numEdges = new AtomicInteger(0);

        // add edge to cutgraph (maybe with merged taxa)
        source.characters.forEach((IntConsumer) edgeIndex -> {
            RoaringBitmap edgeOnes = source.getEdge(edgeIndex).ones;

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
                    final int edgeCloneIndex = source.getCloneIndex(edgeIndex);
                    hyperEdgeMerging.put(edgeOnes, merged);

                    nodeToEdges.adjustOrPutValue(edgeIndex, 1, 1);
                    nodeToEdges.adjustOrPutValue(edgeCloneIndex, 1, 1);

                    numEdges.getAndAdd(2);

                    edgeOnes.forEach((IntConsumer) t -> {

                        nodeToEdges.adjustOrPutValue(edgeIndex, 1, 1);
                        nodeToEdges.adjustOrPutValue(edgeCloneIndex, 1, 1);
                        nodeToEdges.adjustOrPutValue(t, 2, 2);

                        numEdges.getAndAdd(4);
                    });
                }
                merged.add(edgeIndex);
            }
        });


        final CutGraphImpl hipri = new CutGraphImpl(nodeToEdges.size(), numEdges.get());

        hyperEdgeMerging.forEach((edgeOnes, mergedIndeces) -> {
            final int edgeIndex = mergedIndeces.get(0);
//
            final int edgeCloneIndex = source.getCloneIndex(edgeIndex);

            Node.IntNode out = hipri.createNode(edgeIndex, nodeToEdges.get(edgeCloneIndex));
            Node.IntNode in = hipri.createNode(edgeCloneIndex, nodeToEdges.get(edgeCloneIndex));

            long weight = 0;
            if (mergedIndeces.size() > 1) {
                TIntIterator it = mergedIndeces.iterator();
                while (it.hasNext()) {
                    weight += source.getEdge(it.next()).getWeight();
                }
                charMapping.put(edgeIndex, mergedIndeces);
            } else {
                weight = source.getEdge(edgeIndex).getWeight();
            }

            hipri.addEdge(out, in, weight);

            edgeOnes.forEach((IntConsumer) taxonIndex -> {
                Node.IntNode t = cutgraphTaxa.get(taxonIndex);
                if (t == null) {
                    t = hipri.createNode(taxonIndex, nodeToEdges.get(taxonIndex));
                    cutgraphTaxa.put(taxonIndex, t);
                }
                hipri.addEdge(t, out, CutGraphCutter.getInfinity());
                hipri.addEdge(in, t, CutGraphCutter.getInfinity());
            });
        });


        return hipri;
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
