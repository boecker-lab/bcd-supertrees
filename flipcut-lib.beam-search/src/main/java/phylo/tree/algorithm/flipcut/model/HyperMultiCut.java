package phylo.tree.algorithm.flipcut.model;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 15.02.17.
 */

import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.HyperCut;

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
        minCutValue = sourceCut.minCutValue;
    }

    @Override
    public long minCutValue() {
        return minCutValue;
    }

    @Override
    public List<FlipCutGraphMultiSimpleWeight> getSplittedGraphs() {

        if (splittedGraphs == null) {
            LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes = new LinkedHashSet<>();
            LinkedHashSet<FlipCutNodeSimpleWeight> deletedChars = sourceCut.cutEdges;

            for (FlipCutNodeSimpleWeight taxon : sourceCut.cutTaxaSource) {
                sinkNodes.add(taxon);
                for (FlipCutNodeSimpleWeight character : taxon.edges) {
                    sinkNodes.add(character);
                    sinkNodes.add(character.getClone());
                }
            }
            sinkNodes.removeAll(deletedChars);

            splittedGraphs = (List<FlipCutGraphMultiSimpleWeight>) sourceGraph.split(sinkNodes);
            sourceCut = null;
        }
        return splittedGraphs;
    }
}
