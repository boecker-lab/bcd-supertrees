package phylo.tree.algorithm.flipcut.cutter;

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
    private int threats = 1;


    public CompressedSingleCutter(int threats) {
        this.threats = threats;
    }

    public int getThreats() {
        return threats;
    }

    public void setThreats(int threats) {
        this.threats = threats;
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
        long tm = System.currentTimeMillis();
        final CompressedGoldbergTarjanCutGraph cutGraph = createCutGraph(source);
        cutGraph.setThreads(threats);
        System.out.println("Set up CutGraph in in: " + (double) (System.currentTimeMillis() - tm) / 1000d + "s");


        //we do not have to map merged taxa back, hence we need only the hyperedges we have to delete
        try {
            STCut cut = cutGraph.calculateMinCut();
            RoaringBitmap toDelete = new RoaringBitmap();

            LinkedHashSet[] css = new LinkedHashSet[]{cut.getsSet(), cut.gettSet()};
            for (LinkedHashSet cs : css) {
                for (Object o : cs) {
                    if (o instanceof Integer) {
                        int index = (int) o;
                        if (!cs.contains(source.getEdge(index))) {
                            toDelete.add(index);
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

    private CompressedGoldbergTarjanCutGraph createCutGraph(CompressedBCDGraph source) {
        int numMergedTaxe = 0;
        //create cutgraph with merged taxa
        List<RoaringBitmap> guiEdges = new ArrayList<>(source.numGuideEdges());
        if (source.hasGuideEdges()) {
            for (Hyperedge guidEdge : source.guideHyperEdges()) {
                numMergedTaxe += guidEdge.ones.getCardinality();
                guiEdges.add(guidEdge.ones);
            }
        }


        final TObjectIntMap<Object> nodeToEdges = new TObjectIntHashMap<>(source.numTaxa() + 2 * source.numCharacter());
        final TIntObjectMap<List<String>> hyperEdges = new TIntObjectHashMap<>(source.numCharacter());

        final AtomicInteger numEdges = new AtomicInteger(0);


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
                nodeToEdges.adjustOrPutValue(edgeIndex, 1, 1);
                nodeToEdges.adjustOrPutValue(edge, 1, 1);
                numEdges.getAndAdd(2);

                edgeOnes.forEach((IntConsumer) taxonIndex -> {
                    final String t = source.getTaxon(taxonIndex);
                    List<String> l = hyperEdges.get(edgeIndex);
                    if (l == null) {
                        l = new LinkedList<>();
                        hyperEdges.put(edgeIndex, l);
                    }
                    l.add(t);

                    nodeToEdges.adjustOrPutValue(t, 1, 1);
                    nodeToEdges.adjustOrPutValue(edgeIndex, 1, 1);

                    nodeToEdges.adjustOrPutValue(edge, 1, 1);
                    nodeToEdges.adjustOrPutValue(t, 1, 1);

                    numEdges.getAndAdd(4);
//
                });
            }
        });

        Map<String, Node> cutgraphTaxa = new HashMap<>(source.numTaxa());
        final CutGraphImpl hipri = new CutGraphImpl(nodeToEdges.size(), numEdges.get());
        for (int edgeIndex : hyperEdges.keys()) {
            Hyperedge edge = source.getEdge(edgeIndex);
            Node out = hipri.createNode(edgeIndex, nodeToEdges.get(edge));
            Node in = hipri.createNode(edge, nodeToEdges.get(edge));
            hipri.addEdge(out, in, edge.getWeight());

            for (String taxon : hyperEdges.get(edgeIndex)) {
                Node t = cutgraphTaxa.get(taxon);
                if (t == null){
                    t = hipri.createNode(taxon, nodeToEdges.get(taxon));
                    cutgraphTaxa.put(taxon,t);
                }
                hipri.addEdge(t, out, CutGraphCutter.getInfinity());
                hipri.addEdge(in, t, CutGraphCutter.getInfinity());
            }
        }

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
            return new CompressedSingleCutter(1);
        }

        @Override
        public CompressedSingleCutter newInstance(CompressedBCDGraph graph, ExecutorService executorService,
                                                  int threads) {
            return new CompressedSingleCutter(threads);
        }

        @Override
        public boolean isBCD() {
            return true;
        }
    }
}
