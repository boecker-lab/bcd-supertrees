package mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 28.02.17.
 */

import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FlipCutCutFactory implements CutFactory<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutHyperCut<FlipCutNodeSimpleWeight>> {

    @Override
    public FlipCutHyperCut<FlipCutNodeSimpleWeight> newCutInstance(LinkedHashSet<FlipCutNodeSimpleWeight> cutTaxaSource, LinkedHashSet<FlipCutNodeSimpleWeight> cutTaxaSink, LinkedHashSet<FlipCutNodeSimpleWeight> cutEdges, long mincutValue) {
        LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes = createNodeSet(cutTaxaSource);
        LinkedHashSet<FlipCutNodeSimpleWeight> sourceNodes = createNodeSet(cutTaxaSink);

        mincutValue = 0;
        for (FlipCutNodeSimpleWeight cutEdge : cutEdges) {
            mincutValue += cutEdge.getEdgeWeight();
            sinkNodes.remove(cutEdge);
            sourceNodes.remove(cutEdge.getClone());
        }
        return new FlipCutHyperCut<>(mincutValue, sourceNodes, sinkNodes);
    }

    private LinkedHashSet<FlipCutNodeSimpleWeight> createNodeSet(LinkedHashSet<FlipCutNodeSimpleWeight> cutTaxa) {
        LinkedHashSet<FlipCutNodeSimpleWeight> nodeSet = new LinkedHashSet<>();
        for (FlipCutNodeSimpleWeight taxon : cutTaxa) {
            nodeSet.add(taxon);
            for (FlipCutNodeSimpleWeight character : taxon.edges) {
                nodeSet.add(character);
                nodeSet.add(character.getClone());
            }
        }
        return nodeSet;
    }
}
