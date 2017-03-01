package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 28.02.17.
 */

import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FlipCutCutFactory<V extends AbstractFlipCutNode<V>> implements CutFactory<V,FlipCutHyperCut<V>> {

    @Override
    public FlipCutHyperCut<V> newCutInstance(LinkedHashSet<V> cutTaxaSource, LinkedHashSet<V> cutTaxaSink, LinkedHashSet<V> cutEdges, long mincutValue) {
        LinkedHashSet<V> sinkNodes = createNodeSet(cutTaxaSource);
        LinkedHashSet<V> sourceNodes = createNodeSet(cutTaxaSink);

        for (V cutEdge : cutEdges) {
            sinkNodes.remove(cutEdge);
            sourceNodes.remove(cutEdge.getClone());
        }


        return new FlipCutHyperCut<>(mincutValue, sourceNodes, sinkNodes);
    }

    private LinkedHashSet<V> createNodeSet(LinkedHashSet<V> cutTaxa) {
        LinkedHashSet<V> nodeSet = new LinkedHashSet<>();
        for (V taxon : cutTaxa) {
            nodeSet.add(taxon);
            for (V character : taxon.edges) {
                nodeSet.add(character);
                nodeSet.add(character.getClone());
            }
        }
        return nodeSet;
    }
}
