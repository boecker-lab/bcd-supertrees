package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 */

import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
class Graph {

    final Map<Integer, Vertex> vertices = new TreeMap<Integer, Vertex>(new Comparator<Integer>() {
        //for pretty printing
        @Override
        public int compare(Integer arg0, Integer arg1) {
            return arg0.compareTo(arg1);
        }
    });

    final List<Edge> edges = new ArrayList<Edge>();

    public void addVertex(Vertex v) {
        vertices.put(v.lbl, v);
    }

    public Vertex getVertex(int lbl) {
        Vertex v;
        if ((v = vertices.get(lbl)) == null) {
            v = new Vertex(lbl);
            addVertex(v);
        }
        return v;
    }
}
