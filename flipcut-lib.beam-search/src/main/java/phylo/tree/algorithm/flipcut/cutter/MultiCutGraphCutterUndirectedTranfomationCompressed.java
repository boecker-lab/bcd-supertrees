package phylo.tree.algorithm.flipcut.cutter;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import mincut.cutGraphAPI.bipartition.CompressedBCDMultiCut;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.HashableCut;
import mincut.cutGraphAPI.bipartition.MultiCut;
import mincut.cutGraphImpl.minCutKargerStein.CompressedKargerGraph;
import mincut.cutGraphImpl.minCutKargerStein.KargerStein;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDMultiCutGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.Hyperedge;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MultiCutGraphCutterUndirectedTranfomationCompressed extends CutGraphCutter<RoaringBitmap> implements MultiCutter<RoaringBitmap, CompressedBCDMultiCutGraph> {
    private LinkedList<Cut<RoaringBitmap>> mincuts = null;
    private final CompressedBCDMultiCutGraph source;//todo make reusable??
    private final boolean rescursive;

    public MultiCutGraphCutterUndirectedTranfomationCompressed(CompressedBCDMultiCutGraph graphToCut, boolean recursive) {
        super();
        source = graphToCut;
        this.rescursive = recursive;
    }

    public MultiCutGraphCutterUndirectedTranfomationCompressed(CompressedBCDMultiCutGraph graphToCut, boolean recursive, ExecutorService executorService, int threads) {
        super(executorService, threads);
        source = graphToCut;
        this.rescursive = recursive;
    }


    @Override
    public Cut<RoaringBitmap> cut(SourceTreeGraph<RoaringBitmap> source) {
        if (source.equals(this.source))
            return getMinCut();
        return null;
    }

    private LinkedList<Cut<RoaringBitmap>> calculateMinCuts() {
        LinkedList<Cut<RoaringBitmap>> mincuts = null;


        final int toGo = source.getK();
        KargerStein<CompressedKargerGraph,RoaringBitmap> cutter = new KargerStein<>();
        cutter.setMaxCutNumber(toGo);
        CompressedKargerGraph virginGraph = new CompressedKargerGraph(source.getSource());

        //sample k-1 random cuts
        if (toGo > 0) {
            mincuts = cutter.getMinCuts(virginGraph, rescursive).stream().map((it) -> {
                final CompressedBCDMultiCut cut = createCompressedMulticut(it.getSset(), it.getTset(), source);
                assert Double.compare(cut.minCutValue(), it.minCutValue()) == 0 : cut.minCutValue() + " vs " + it.minCutValue();
                return cut;
            }).collect(Collectors.toCollection(LinkedList::new));
        }

        //search the optimal cut
        CompressedSingleCutter optCutter = new CompressedSingleCutter();
        Cut<RoaringBitmap> optCut = optCutter.cut(source.getSource());

        //check if optimal is needed
        if (mincuts == null) {
            mincuts = new LinkedList<>();
            mincuts.add(optCut);
            return mincuts;
        } else if (mincuts.isEmpty() || !mincuts.getFirst().equals(optCut)) {
            mincuts.addFirst(optCut);
        }else{
            System.out.println("Random found optimal cut");
        }

        return mincuts;
    }

    //returns the set of characters, that connect an taxon split
    public static CompressedBCDMultiCut createCompressedMulticut(RoaringBitmap sourceSetTaxa, CompressedBCDMultiCutGraph multiGraph) {
        RoaringBitmap targetSetTaxa = RoaringBitmap.andNot(multiGraph.getSource().taxa, sourceSetTaxa);
        return createCompressedMulticut(sourceSetTaxa, targetSetTaxa, multiGraph);
    }

    //returns the set of characters, that connect an taxon split
    public static CompressedBCDMultiCut createCompressedMulticut(RoaringBitmap sourceSetTaxa, RoaringBitmap targetSetTaxa, CompressedBCDMultiCutGraph multiGraph) {
        final CompressedBCDGraph g = multiGraph.getSource();

        final AtomicLong minCutValue = new AtomicLong(0);
        final RoaringBitmap toDelete = new RoaringBitmap();

        g.characters.forEach((IntConsumer) i -> {
            Hyperedge hyperEdge = g.getEdge(i);

            if (RoaringBitmap.intersects(hyperEdge.ones(), sourceSetTaxa) && RoaringBitmap.intersects(hyperEdge.ones(), targetSetTaxa)) {
                assert !hyperEdge.isInfinite() : "HyperEdge is part of Cut: weight=" + hyperEdge.getWeight() + " taxa: " + hyperEdge.ones();
                toDelete.add(i);
                minCutValue.addAndGet(hyperEdge.getWeight());
            }
        });

        return new CompressedBCDMultiCut(toDelete, minCutValue.longValue(), multiGraph);
    }


    @Override
    public MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph> getNextCut() {
        if (mincuts == null)
            mincuts = calculateMinCuts();
        if (mincuts.isEmpty()) {
            return null;
        }

        Cut<RoaringBitmap> c = mincuts.pollFirst();

        if (c == null)
            return null;

        if (c instanceof CompressedBCDMultiCut)
            return (MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph>) c;
        else {
            return new CompressedBCDMultiCut(c.getCutSet(), c.minCutValue(), source);
        }
    }

    @Override
    public MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph> getMinCut() {
        return getNextCut();
    }

    @Override
    public boolean isBCD() {
        return true;
    }

    static class Factory implements MultiCutterFactory<MultiCutGraphCutterUndirectedTranfomationCompressed, RoaringBitmap, CompressedBCDMultiCutGraph> {
        private final boolean recursive;

        Factory(boolean recursive) {
            this.recursive = recursive;
        }

        @Override
        public MultiCutGraphCutterUndirectedTranfomationCompressed newInstance(CompressedBCDMultiCutGraph graph) {
            return new MultiCutGraphCutterUndirectedTranfomationCompressed(graph, recursive);
        }

        @Override
        public MultiCutGraphCutterUndirectedTranfomationCompressed newInstance(CompressedBCDMultiCutGraph graph, ExecutorService executorService, int threads) {
            return new MultiCutGraphCutterUndirectedTranfomationCompressed(graph, recursive, executorService, threads);
        }

        @Override
        public boolean isBCD() {
            return true;
        }
    }
}
