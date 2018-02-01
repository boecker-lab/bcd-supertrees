package phylo.tree.algorithm.flipcut.cutter;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import mincut.cutGraphAPI.KargerSteinCutGraph;
import mincut.cutGraphAPI.bipartition.*;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDMultiCutGraph;
import phylo.tree.algorithm.flipcut.cutter.undirectedConversion.ChracterScoreModifier;
import phylo.tree.algorithm.flipcut.cutter.undirectedConversion.KargerGraphCreator;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.TreeSet;
import java.util.concurrent.ExecutorService;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MultiCutGraphCutterUndirectedTranfomationCompressed extends CutGraphCutter<RoaringBitmap> implements MultiCutter<RoaringBitmap, CompressedBCDMultiCutGraph> {
    private final boolean singleSampling;
    private final ChracterScoreModifier modder;
    private final KargerGraphCreator graphCreator;
    private TreeSet<Cut<RoaringBitmap>> mincuts = null;
    private final CompressedBCDMultiCutGraph source;//todo make reusable??


    public MultiCutGraphCutterUndirectedTranfomationCompressed(CompressedBCDMultiCutGraph graphToCut, final ChracterScoreModifier modder, KargerGraphCreator graphCreator, boolean singleSampling) {
        super();
        source = graphToCut;
        this.modder = modder;
        this.graphCreator = graphCreator;
        this.singleSampling = singleSampling;
    }

    public MultiCutGraphCutterUndirectedTranfomationCompressed(CompressedBCDMultiCutGraph graphToCut, ExecutorService executorService, int threads) {
        this(graphToCut, executorService, threads, new ChracterScoreModifier() {
        }, new KargerGraphCreator() {
        }, false);

    }

    public MultiCutGraphCutterUndirectedTranfomationCompressed(CompressedBCDMultiCutGraph graphToCut, ExecutorService executorService, int threads, final ChracterScoreModifier modder, KargerGraphCreator graphCreator, boolean singleSampling) {
        super(executorService, threads);
        source = graphToCut;
        this.modder = modder;
        this.graphCreator = graphCreator;
        this.singleSampling = singleSampling;
    }


    @Override
    public Cut<RoaringBitmap> cut(SourceTreeGraph<RoaringBitmap> source) {
        if (source.equals(this.source))
            return getMinCut();
        return null;
    }

    protected TreeSet<Cut<RoaringBitmap>> calculateMinCuts() {
        TreeSet<Cut<RoaringBitmap>> mincuts = new TreeSet<>();

//        long time = System.currentTimeMillis();
        //search the optimal cat
        CompressedSingleCutter optCutter = new CompressedSingleCutter();
        Cut<RoaringBitmap> optCut = optCutter.cut(source);
        mincuts.add(optCut);
/*
        KargerSteinCutGraph<RoaringBitmap, CutFactory<RoaringBitmap,Cut<RoaringBitmap>>> cutGraph = graphCreator.createGraph(modder, source);

        //sample k-1 random cuts
        final int toGo = source.getK() - 1;
        if (toGo > 0) {
            if (singleSampling) {
                mincuts.addAll(cutGraph.sampleCuts(toGo));
            } else {
                mincuts.addAll(cutGraph.calculateMinCuts(toGo));
            }
        }*/
        return mincuts;
    }

    @Override
    public MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph> getNextCut() {
        /*if (mincuts == null)
            mincuts = calculateMinCuts();
        if (mincuts.isEmpty()) {
            return null;
        }

        Cut<RoaringBitmap> c = mincuts.pollFirst();

        if (c instanceof DefaultMultiCut)
            return (DefaultMultiCut) c;
        else {
            return new HyperMultiCut(source, (HyperCut<FlipCutNodeSimpleWeight>) c);
        }*/
        return null;
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
        private final ChracterScoreModifier modder;
        private final KargerGraphCreator graphCreator;
        private final boolean singleSampling;

        public Factory(ChracterScoreModifier modder, KargerGraphCreator graphCreator, boolean singleSampling) {
            this.modder = modder;
            this.graphCreator = graphCreator;
            this.singleSampling = singleSampling;
        }


        @Override
        public MultiCutGraphCutterUndirectedTranfomationCompressed newInstance(CompressedBCDMultiCutGraph graph) {
            return new MultiCutGraphCutterUndirectedTranfomationCompressed(graph, modder, graphCreator, singleSampling);
        }

        @Override
        public MultiCutGraphCutterUndirectedTranfomationCompressed newInstance(CompressedBCDMultiCutGraph graph, ExecutorService executorService, int threads) {
            return new MultiCutGraphCutterUndirectedTranfomationCompressed(graph, executorService, threads, modder, graphCreator, singleSampling);
        }

        @Override
        public boolean isBCD() {
            return true;
        }
    }
}
