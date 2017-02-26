package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 10.02.17.
 */

import phylo.tree.algorithm.flipcut.mincut.EdgeColor;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface EdgeWeighter {

    default double weightEdge(final Edge e){
        return e.colors != null && !e.colors.isEmpty() ? e.colors.stream().mapToDouble(EdgeColor::getWeight).sum(): 1d;
    };
}
