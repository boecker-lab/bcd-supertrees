package mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 28.02.17.
 */

import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FlipCutHyperCut<V extends AbstractFlipCutNode<V>> extends HyperCut<V> {

    public FlipCutHyperCut(long minCutValue, LinkedHashSet<V> sSet, LinkedHashSet<V> tSet) {
        super(minCutValue, sSet, tSet);
    }
}
