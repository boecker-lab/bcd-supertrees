package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 */

import java.util.ArrayList;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
class Edge {

    final List<Vertex> ends = new ArrayList<Vertex>();

    public Edge(Vertex fst, Vertex snd) {
        if (fst == null || snd == null) {
            throw new IllegalArgumentException("Both vertices are required");
        }
        ends.add(fst);
        ends.add(snd);
    }

    public boolean contains(Vertex v1, Vertex v2) {
        return ends.contains(v1) && ends.contains(v2);
    }

    public Vertex getOppositeVertex(Vertex v) {
        if (!ends.contains(v)) {
            throw new IllegalArgumentException("Vertex " + v.lbl);
        }
        return ends.get(1 - ends.indexOf(v));
    }

    public void replaceVertex(Vertex oldV, Vertex newV) {
        if (!ends.contains(oldV)) {
            throw new IllegalArgumentException("Vertex " + oldV.lbl);
        }
        ends.remove(oldV);
        ends.add(newV);
    }
}
