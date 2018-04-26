package phylo.tree.algorithm.flipcut.cutter;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import mincut.cutGraphAPI.bipartition.CompressedBCDCut;
import mincut.cutGraphAPI.bipartition.CompressedBCDMultiCut;
import mincut.cutGraphAPI.bipartition.Cut;
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
    private Cut<RoaringBitmap> mincut = null;
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

        KargerStein<CompressedKargerGraph, RoaringBitmap> cutter = new KargerStein<>();
        cutter.setMaxCutNumber(source.getK());
        CompressedKargerGraph virginGraph = new CompressedKargerGraph(source.getSource());

        //sample k random cuts
        mincuts = cutter.getMinCuts(virginGraph, rescursive).stream().map((it) -> {
            final CompressedBCDMultiCut cut = createCompressedMulticut(it.getSset(), it.getTset(), source);
            assert Double.compare(cut.minCutValue(), it.minCutValue()) == 0 : cut.minCutValue() + " vs " + it.minCutValue();
            return cut;
        }).collect(Collectors.toCollection(LinkedList::new));

        //check if optimal is found again needed
        if (mincuts == null) {
            mincuts = new LinkedList<>();
        } else {
            if (!mincuts.isEmpty()) {
                Iterator<Cut<RoaringBitmap>> it = mincuts.iterator();
                while (it.hasNext()) {
                    Cut<RoaringBitmap> cut = it.next();
                    if (cut.minCutValue() == mincut.minCutValue()) {
                        if (compare((CompressedBCDMultiCut) cut, (CompressedBCDCut) mincut)) {
                            it.remove();
                        }
                    } else {
                        break;
                    }
                }
                while (mincuts.size() >= source.getK())
                    mincuts.pollLast();
            }
        }
        return mincuts;
    }

    private boolean compare(CompressedBCDMultiCut randomCut, CompressedBCDCut mincut) {
        if (randomCut.minCutValue() != mincut.minCutValue())
            return false;
        return randomCut.getCutSet().equals(mincut.getCutSet());
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
        if (mincut == null) {
            CompressedSingleCutter optCutter = new CompressedSingleCutter();
            mincut = optCutter.cut(source.getSource());
            return new CompressedBCDMultiCut(mincut.getCutSet(), mincut.minCutValue(), source);
        }

        if (mincuts == null)
            mincuts = calculateMinCuts();

        if (mincuts.isEmpty())
            return null;

        Cut<RoaringBitmap> c = mincuts.pollFirst();

        return (MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph>) c;
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
