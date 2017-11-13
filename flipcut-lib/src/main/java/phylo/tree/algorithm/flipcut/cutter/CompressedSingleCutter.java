package phylo.tree.algorithm.flipcut.cutter;

import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.bipartition.CompressedBCDCut;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.STCut;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.Hyperedge;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

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
        final GoldbergTarjanCutGraph cutGraph = new GoldbergTarjanCutGraph();
        cutGraph.setThreads(threats);

        int numMergedTaxe = 0;
        //create cutgraph with merged taxa
        List<RoaringBitmap> guiEdges = new ArrayList<>(source.numGuideEdges());
        if (source.hasGuideEdges()) {
            for (Hyperedge guidEdge : source.guideHyperEdges()) {
                numMergedTaxe += guidEdge.ones.getCardinality();
                guiEdges.add(guidEdge.ones);
            }
        }

        final Set<String> cutgraphTaxa = new HashSet<>(source.numTaxa() - numMergedTaxe + guiEdges.size());

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
                cutGraph.addEdge(edgeIndex, edge, edge.getWeight());
                edgeOnes.forEach((IntConsumer) taxonIndex -> {
                    final String t = source.getTaxon(taxonIndex);
                    cutgraphTaxa.add(t);
                    cutGraph.addEdge(t, edgeIndex, CutGraphCutter.getInfinity());
                    cutGraph.addEdge(edge, t, CutGraphCutter.getInfinity());
                });
            }
        });

        System.out.println("Set up CutGraph in in: " + (double) (System.currentTimeMillis() - tm) / 1000d + "s");

        Iterator<String> taxit = cutgraphTaxa.iterator();
        String s = taxit.next();
        while (taxit.hasNext()) {
            cutGraph.submitSTCutCalculation(s, taxit.next());
        }
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
