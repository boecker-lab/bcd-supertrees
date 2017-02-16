package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 10.02.17.
 */

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class NormalizePerColorWeighter implements EdgeWeighter {
    @Override
    public double weightEdge(Edge e) {
        if (e.color != null)
            return e.color.getWeight() / e.color.numOfEdges();
        return 1d;
    }
}
