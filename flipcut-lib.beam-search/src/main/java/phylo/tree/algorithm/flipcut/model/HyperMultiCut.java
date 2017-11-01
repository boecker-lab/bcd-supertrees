package phylo.tree.algorithm.flipcut.model;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 15.02.17.
 */

import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import mincut.cutGraphAPI.bipartition.HyperCut;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class HyperMultiCut extends MultiCut<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {

    private HyperCut<FlipCutNodeSimpleWeight> sourceCut;
    private final long minCutValue;

    public HyperMultiCut(FlipCutGraphMultiSimpleWeight sourceGraph, HyperCut<FlipCutNodeSimpleWeight> sourceCut) {
        super(sourceGraph);
        this.sourceCut = sourceCut;
        minCutValue = sourceCut.minCutValue();
        calculateHash();
    }

    @Override
    public long minCutValue() {
        return minCutValue;
    }

    @Override
    public LinkedHashSet<FlipCutNodeSimpleWeight> getCutSet() {
        return sourceCut.getCutSet();
    }

    @Override
    public List<FlipCutGraphMultiSimpleWeight> getSplittedGraphs() {
        if (splittedGraphs == null) {

            splittedGraphs = (List<FlipCutGraphMultiSimpleWeight>) sourceGraph.split(getCutSet());
            sourceCut = null;
        }
        sourceGraph.setCutSplitted(this);
        sourceGraph.close();
        return splittedGraphs;
    }

    @Override
    protected List<List<FlipCutNodeSimpleWeight>> comp() {
        return null;
    }


}
