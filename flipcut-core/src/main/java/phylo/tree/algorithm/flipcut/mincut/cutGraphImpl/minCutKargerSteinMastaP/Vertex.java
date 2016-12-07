package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 */

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
class Vertex {

    final int lbl;
    final TIntSet mergedLbls;
    final Set<Edge> edges = new HashSet<Edge>();

    public Vertex(int lbl) {
        this.lbl = lbl;
        mergedLbls = new TIntHashSet();
        mergedLbls.add(lbl);
    }

    public void addEdge(Edge edge) {
        edges.add(edge);
    }

    public Edge getEdgeTo(Vertex v2) {
        for (Edge edge : edges) {
            if (edge.contains(this, v2))
                return edge;
        }
        return null;
    }

}
