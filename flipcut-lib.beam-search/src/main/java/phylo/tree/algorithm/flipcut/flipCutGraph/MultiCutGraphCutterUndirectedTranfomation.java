package phylo.tree.algorithm.flipcut.flipCutGraph;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import mincut.cutGraphAPI.bipartition.FlipCutCutFactory;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import mincut.cutGraphAPI.KargerSteinCutGraph;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.HyperCut;
import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;
import phylo.tree.algorithm.flipcut.model.HyperMultiCut;
import phylo.tree.algorithm.flipcut.model.MultiCut;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MultiCutGraphCutterUndirectedTranfomation extends CutGraphCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> implements MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
    private static final boolean MINIMAL_SAMPLING = true;
    private static final Factory FACTORY = new Factory();
    private TreeSet<Cut<FlipCutNodeSimpleWeight>> mincuts = null;

    public MultiCutGraphCutterUndirectedTranfomation(FlipCutGraphMultiSimpleWeight graphToCut) {
        super();
        source = graphToCut;
    }

    public MultiCutGraphCutterUndirectedTranfomation(FlipCutGraphMultiSimpleWeight graphToCut, ExecutorService executorService, int threads) {
        super(executorService, threads);
        source = graphToCut;
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
        DefaultMultiCut optCut = new DefaultMultiCut(optCutter.getMinCut(source),  source);
        mincuts.add(optCut);

        KargerSteinCutGraph<FlipCutNodeSimpleWeight, FlipCutCutFactory<FlipCutNodeSimpleWeight>> cutGraph = new KargerSteinCutGraph<>(new FlipCutCutFactory<FlipCutNodeSimpleWeight>());
        for (FlipCutNodeSimpleWeight character : source.characters) {
            for (FlipCutNodeSimpleWeight e1 : character.edges) {
                for (FlipCutNodeSimpleWeight e2 : character.edges) {
                    if (e1 != e2) {
                        boolean guide = character.edgeWeight == CutGraphCutter.getInfinity();
                        cutGraph.addEdge(e1, e2, character.edgeWeight, character, (guide));
                    }
                }
            }
        }

        //sample k-1 random cuts
        final int toGo = source.getK() - 1;
        if (toGo > 0) {
            if (MINIMAL_SAMPLING) {
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


    public static Factory getFactory() {
        return FACTORY;
    }

    static class Factory implements MultiCutterFactory<MultiCutGraphCutterUndirectedTranfomation, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
        @Override
        public MultiCutGraphCutterUndirectedTranfomation newInstance(FlipCutGraphMultiSimpleWeight graph) {
            return new MultiCutGraphCutterUndirectedTranfomation(graph);
        }

        @Override
        public MultiCutGraphCutterUndirectedTranfomation newInstance(FlipCutGraphMultiSimpleWeight graph, ExecutorService executorService, int threads) {
            return new MultiCutGraphCutterUndirectedTranfomation(graph, executorService, threads);
        }
    }
}
