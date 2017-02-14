package phylo.tree.algorithm.flipcut.flipCutGraph;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import phylo.tree.algorithm.flipcut.mincut.EdgeColor;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.KargerSteinCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.BasicCut;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.NormalizePerColorWeighter;
import phylo.tree.algorithm.flipcut.model.Cut;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MultiCutGraphCutterUndirectedTranfomation extends CutGraphCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> implements MultiCutter {
    Queue<BasicCut<FlipCutNodeSimpleWeight>> mincuts = null;

    public MultiCutGraphCutterUndirectedTranfomation(CutGraphTypes type) {
        super(type);
    }

    public MultiCutGraphCutterUndirectedTranfomation(CutGraphTypes type, ExecutorService executorService, int threads) {
        super(type, executorService, threads);
    }

    @Override
    public List<FlipCutGraphMultiSimpleWeight> cut(FlipCutGraphMultiSimpleWeight source) {
        return getNextCut().getSplittedGraphs();
    }

    @Override
    protected void calculateMinCut() {
        KargerSteinCutGraph<FlipCutNodeSimpleWeight> cutGraph = new KargerSteinCutGraph<>(new NormalizePerColorWeighter());
        for (FlipCutNodeSimpleWeight character : source.characters) {
            EdgeColor color = new EdgeColor();
            color.setWeight(character.edgeWeight);
            for (FlipCutNodeSimpleWeight e1 : character.edges) {
                for (FlipCutNodeSimpleWeight e2 : character.edges) {
                    if (e1 != e2) {
                        cutGraph.addEdge(e1, e2, 1, color);
                    }
                }
            }
        }
        mincuts = cutGraph.calculateMinCuts();
    }


    @Override
    public Cut getNextCut() {
        if (mincuts == null)
            calculateMinCut();
        if (mincuts.isEmpty()) {
            return null;
        }
        BasicCut<FlipCutNodeSimpleWeight> c = mincuts.poll();
        mincut = c.getCutSet();
        mincutValue = c.minCutValue; //todo map cut score back, just if we have a weighting that does not mactch with the original one
        return new Cut(mincut, mincutValue, source);
    }
}
