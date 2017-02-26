package phylo.tree.algorithm.flipcut.flipCutGraph;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.KargerSteinCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.Cut;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.HyperCut;
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
        LinkedHashSet<FlipCutNodeSimpleWeight> optCutSet = optCutter.getMinCut(source);
        long mincutValue = optCutter.getMinCutValue(source);
        DefaultMultiCut optCut = new DefaultMultiCut(optCutSet, mincutValue, source);
//        System.out.println("Optimal Cut needed " + (System.currentTimeMillis() - time) / 1000d + "s");

        mincuts.add(optCut);


        KargerSteinCutGraph<FlipCutNodeSimpleWeight> cutGraph = new KargerSteinCutGraph<>(/*new NormalizePerColorWeighter()*/);
        for (FlipCutNodeSimpleWeight character : source.characters) {
            for (FlipCutNodeSimpleWeight e1 : character.edges) {
                for (FlipCutNodeSimpleWeight e2 : character.edges) {
                    if (e1 != e2) {
                        cutGraph.addEdge(e1, e2, character.edgeWeight, character);
                    }
                }
            }
        }

        //sample k-1 random cuts
        for (int i = 0; i < source.maxCutNumber - 1; i++) {
//            time = System.currentTimeMillis();
//            System.out.println();
            mincuts.add(cutGraph.calculateMinCut());
//            System.out.println("On Random Cut needed " + (System.currentTimeMillis() - time) / 1000d + "s");
        }
//        mincuts.addAll(cutGraph.calculateMinCuts(source.maxCutNumber-1));
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
