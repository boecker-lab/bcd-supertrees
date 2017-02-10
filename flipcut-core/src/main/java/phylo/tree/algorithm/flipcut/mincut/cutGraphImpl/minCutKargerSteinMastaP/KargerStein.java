package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com) based on https://gist.github.com/MastaP
 */

import gnu.trove.list.TDoubleList;
import gnu.trove.set.TIntSet;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

public class KargerStein {

    public double contract(Graph gr) {
        while (gr.vertices.size() > 2) {
            Edge edge = drawEdge(gr);
            Iterator<Vertex> it = edge.iterator();
            reorganizeEdges(gr, it.next(), it.next());
        }
        return gr.edges.stream().mapToDouble(e -> e.weight).sum();
    }

    //todo maybe edge drawer class
    private Edge drawEdge(Graph g) {
        int upper = g.getNumOfEdges();
        int downer = 0;

        double r = ThreadLocalRandom.current().nextDouble(0, g.getSumOfWeights());

        TDoubleList values = g.weights;

        int mid;
        while (upper - downer > 1) {
            mid = downer + (upper - downer) / 2;
            double v = values.get(mid);
            if (r > v) {
                downer = mid;
            } else if (r < v) {
                upper = mid;
            } else {
                downer = mid;
                break;
            }
        }

        if (r <= values.get(downer)) {
            mid = downer;
        } else {
            mid = upper;
        }

        return g.edges.get(mid);
    }

    public TreeMap<Double, TIntSet> getMinCut(final Graph gr) {
        TreeMap<Double, TIntSet> results = new TreeMap<>();
        int iter = gr.vertices.size() * gr.vertices.size();
        gr.refreshWeights();
        for (int i = 0; i < iter; i++) {
            Graph grc = gr.clone();
            double currMin = contract(grc);
            results.put(currMin, ((Vertex) grc.vertices.values()[0]).mergedLbls);
        }
        return results;
    }

    public TreeMap<Double, TIntSet> getMinCut(final int[][] arr) {
        Graph gr = GraphUtils.createGraph(arr);
        return getMinCut(gr);
    }

    private void reorganizeEdges(Graph gr, Vertex v1, Vertex v2) {
        //remove old vertex from graph
        Vertex v = gr.vertices.remove(v2.lbl);

        //add merged labels to v1
        v1.mergedLbls.addAll(v2.mergedLbls);

        //redirect edges
        for (Iterator<Edge> it = gr.edges.iterator(); it.hasNext(); ) {
            Edge edge = it.next();
            if (edge.contains(v1, v2)) {
                //remove loops
                v1.edges.remove(edge);
                v2.edges.remove(edge);//not needed
                edge.deleteColor();
                it.remove();
            } else if (v2.edges.contains(edge)) {
                //redirect edges from v2 to v1
                v2.edges.remove(edge);//not needed
                edge.replaceVertex(v2, v1);
                v1.edges.add(edge);
            }
        }
        gr.refreshWeights();
    }
}