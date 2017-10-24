package phylo.tree.algorithm.flipcut.flipCutGraph;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import mincut.cutGraphAPI.KargerSteinCutGraph;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.FlipCutCutFactory;
import mincut.cutGraphAPI.bipartition.HyperCut;
import phylo.tree.algorithm.flipcut.flipCutGraph.undirectedConversion.ChracterScoreModifier;
import phylo.tree.algorithm.flipcut.flipCutGraph.undirectedConversion.KargerGraphCreator;
import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;
import phylo.tree.algorithm.flipcut.model.HyperMultiCut;
import phylo.tree.algorithm.flipcut.model.MultiCut;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MultiCutGraphCutterUndirectedTranfomation extends CutGraphCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> implements MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
    private final boolean singleSampling;
    private final ChracterScoreModifier modder;
    private final KargerGraphCreator graphCreator;

    private TreeSet<Cut<FlipCutNodeSimpleWeight>> mincuts = null;

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
    public List<FlipCutGraphMultiSimpleWeight> cut(FlipCutGraphMultiSimpleWeight source) {
        return getNextCut().getSplittedGraphs();
    }

    @Override
    protected void calculateMinCut() {
        mincuts = new TreeSet<>();

//        long time = System.currentTimeMillis();
        //search the optimal cat
        SingleCutGraphCutter optCutter = new SingleCutGraphCutter(SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        DefaultMultiCut optCut = new DefaultMultiCut(optCutter.getMinCut(source), source);
        mincuts.add(optCut);

        KargerSteinCutGraph<FlipCutNodeSimpleWeight, FlipCutCutFactory<FlipCutNodeSimpleWeight>> cutGraph = graphCreator.createGraph(modder, source);

        //sample k-1 random cuts
        final int toGo = source.getK() - 1;
        if (toGo > 0) {
            if (singleSampling) {
                mincuts.addAll(cutGraph.sampleCuts(toGo));
            } else {
                mincuts.addAll(cutGraph.calculateMinCuts(toGo));
            }
        }
    }

    @Override
    public MultiCut getNextCut() {
        if (mincuts == null)
            calculateMinCut();
        if (mincuts.isEmpty()) {
            return null;
        }

        Cut<FlipCutNodeSimpleWeight> c = mincuts.pollFirst();

        if (c instanceof DefaultMultiCut)
            return (MultiCut) c;
        else {
            return new HyperMultiCut(source, (HyperCut<FlipCutNodeSimpleWeight>) c);
        }
    }


    static class Factory implements MultiCutterFactory<MultiCutGraphCutterUndirectedTranfomation, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
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
