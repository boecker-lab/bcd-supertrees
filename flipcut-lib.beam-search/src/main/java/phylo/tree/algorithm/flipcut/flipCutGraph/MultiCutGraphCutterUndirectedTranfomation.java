package phylo.tree.algorithm.flipcut.flipCutGraph;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.KargerSteinCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.HyperCut;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.NormalizePerColorWeighter;
import phylo.tree.algorithm.flipcut.model.HyperMultiCut;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MultiCutGraphCutterUndirectedTranfomation extends CutGraphCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> implements MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
    private static final Factory FACTORY = new Factory();
    Queue<HyperCut<FlipCutNodeSimpleWeight>> mincuts = null;


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
        KargerSteinCutGraph<FlipCutNodeSimpleWeight> cutGraph = new KargerSteinCutGraph<>(new NormalizePerColorWeighter());
        for (FlipCutNodeSimpleWeight character : source.characters) {
            for (FlipCutNodeSimpleWeight e1 : character.edges) {
                for (FlipCutNodeSimpleWeight e2 : character.edges) {
                    if (e1 != e2) {
                        cutGraph.addEdge(e1, e2, character.edgeWeight, character);
                    }
                }
            }
        }
        mincuts = cutGraph.calculateMinCuts();
    }


    @Override
    public HyperMultiCut getNextCut() {
        if (mincuts == null)
            calculateMinCut();
        if (mincuts.isEmpty()) {
            return null;
        }
        HyperCut<FlipCutNodeSimpleWeight> c = mincuts.poll();
        return new HyperMultiCut(source,c);
    }


    public static Factory getFactory(){
        return FACTORY;
    }

    static class Factory implements MultiCutterFactory<MultiCutGraphCutterUndirectedTranfomation,FlipCutNodeSimpleWeight,FlipCutGraphMultiSimpleWeight>{
        @Override
        public MultiCutGraphCutterUndirectedTranfomation newInstance(FlipCutGraphMultiSimpleWeight graph) {
            return new MultiCutGraphCutterUndirectedTranfomation(graph);
        }

        @Override
        public MultiCutGraphCutterUndirectedTranfomation newInstance(FlipCutGraphMultiSimpleWeight graph, ExecutorService executorService, int threads) {
            return new MultiCutGraphCutterUndirectedTranfomation(graph,executorService,threads);
        }
    }
}
