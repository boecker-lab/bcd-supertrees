package phylo.tree.algorithm.flipcut.flipCutGraph;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import mincut.cutGraphAPI.KargerSteinCutGraph;
import mincut.cutGraphAPI.MultiCutGraph;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.FlipCutCutFactory;
import mincut.cutGraphAPI.bipartition.HyperCut;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.algorithm.flipcut.flipCutGraph.undirectedConversion.ChracterScoreModifier;
import phylo.tree.algorithm.flipcut.flipCutGraph.undirectedConversion.KargerGraphCreator;
import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;
import phylo.tree.algorithm.flipcut.model.HyperMultiCut;
import phylo.tree.algorithm.flipcut.model.MultiCut;

import java.util.LinkedHashSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MultiCutGraphCutterUndirectedTranfomation extends CutGraphCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> implements MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> {
    private final boolean singleSampling;
    private final ChracterScoreModifier modder;
    private final KargerGraphCreator graphCreator;
    private TreeSet<Cut<LinkedHashSet<FlipCutNodeSimpleWeight>>> mincuts = null;
    private final FlipCutGraphMultiSimpleWeight source;//todo make reusable??


    public MultiCutGraphCutterUndirectedTranfomation(FlipCutGraphMultiSimpleWeight graphToCut, final ChracterScoreModifier modder, KargerGraphCreator graphCreator, boolean singleSampling) {
        super();
        source = graphToCut;
        this.modder = modder;
        this.graphCreator = graphCreator;
        this.singleSampling = singleSampling;
    }

    public MultiCutGraphCutterUndirectedTranfomation(FlipCutGraphMultiSimpleWeight graphToCut, ExecutorService executorService, int threads) {
        this(graphToCut, executorService, threads, new ChracterScoreModifier() {
        }, new KargerGraphCreator() {
        }, false);

    }

    public MultiCutGraphCutterUndirectedTranfomation(FlipCutGraphMultiSimpleWeight graphToCut, ExecutorService executorService, int threads, final ChracterScoreModifier modder, KargerGraphCreator graphCreator, boolean singleSampling) {
        super(executorService, threads);
        source = graphToCut;
        this.modder = modder;
        this.graphCreator = graphCreator;
        this.singleSampling = singleSampling;
    }

    @Override
    public Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> cut(FlipCutGraphMultiSimpleWeight source) {
        if (source.equals(this.source))
            return getMinCut();
        return null;
    }

    protected TreeSet<Cut<LinkedHashSet<FlipCutNodeSimpleWeight>>> calculateMinCuts() {
        TreeSet<Cut<LinkedHashSet<FlipCutNodeSimpleWeight>>> mincuts = new TreeSet<>();

//        long time = System.currentTimeMillis();
        //search the optimal cat
        SingleCutGraphCutter optCutter = new SingleCutGraphCutter(CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        DefaultMultiCut optCut = new DefaultMultiCut(optCutter.cut(source), source);
        mincuts.add(optCut);

        KargerSteinCutGraph<FlipCutNodeSimpleWeight, FlipCutCutFactory> cutGraph = graphCreator.createGraph(modder, source);

        //sample k-1 random cuts
        final int toGo = source.getK() - 1;
        if (toGo > 0) {
            if (singleSampling) {
                mincuts.addAll(cutGraph.sampleCuts(toGo));
            } else {
                mincuts.addAll(cutGraph.calculateMinCuts(toGo));
            }
        }
        return mincuts;
    }

    @Override
    public MultiCut<LinkedHashSet<FlipCutNodeSimpleWeight>,FlipCutGraphMultiSimpleWeight> getNextCut() {
        if (mincuts == null)
            mincuts = calculateMinCuts();
        if (mincuts.isEmpty()) {
            return null;
        }

        Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> c = mincuts.pollFirst();

        if (c instanceof DefaultMultiCut)
            return (DefaultMultiCut) c;
        else {
            return new HyperMultiCut(source, (HyperCut<FlipCutNodeSimpleWeight>) c);
        }
    }

    @Override
    public MultiCut<LinkedHashSet<FlipCutNodeSimpleWeight>,FlipCutGraphMultiSimpleWeight> getMinCut() {
        return getNextCut();
    }

    @Override
    public boolean isBCD() {
        return true;
    }

    static class Factory implements MultiCutterFactory<MultiCutGraphCutterUndirectedTranfomation, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> {
        private final ChracterScoreModifier modder;
        private final KargerGraphCreator graphCreator;
        private final boolean singleSampling;

        public Factory(ChracterScoreModifier modder, KargerGraphCreator graphCreator, boolean singleSampling) {
            this.modder = modder;
            this.graphCreator = graphCreator;
            this.singleSampling = singleSampling;
        }


        @Override
        public MultiCutGraphCutterUndirectedTranfomation newInstance(FlipCutGraphMultiSimpleWeight graph) {
            return new MultiCutGraphCutterUndirectedTranfomation(graph, modder, graphCreator, singleSampling);
        }

        @Override
        public MultiCutGraphCutterUndirectedTranfomation newInstance(FlipCutGraphMultiSimpleWeight graph, ExecutorService executorService, int threads) {
            return new MultiCutGraphCutterUndirectedTranfomation(graph, executorService, threads, modder, graphCreator, singleSampling);
        }
    }
}
