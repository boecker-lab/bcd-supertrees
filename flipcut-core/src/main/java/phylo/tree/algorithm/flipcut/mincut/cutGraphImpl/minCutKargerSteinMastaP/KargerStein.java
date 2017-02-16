package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com) based on https://gist.github.com/MastaP
 */

import gnu.trove.list.TDoubleList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KargerStein {

    public void contract(Graph gr) {
        while (gr.vertices.size() > 2) {
            Edge edge = drawEdge(gr);
            Iterator<Vertex> it = edge.iterator();
            reorganizeEdges(gr, it.next(), it.next());
        }
        gr.setCutted(true);
    }

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

    public List<Graph> getMinCuts(final Graph gr) {
        int iter = gr.vertices.size() * gr.vertices.size();
        List<Graph> cuts = new ArrayList<>(iter);

        gr.refreshWeights();
        for (int i = 0; i < iter; i++) {
            Graph grc = gr.clone();
            contract(grc);
            cuts.add(grc);
        }
        return cuts;
    }

    public List<Graph> getMinCuts(final int[][] arr) {
        Graph gr = GraphUtils.createGraph(arr);
        return getMinCuts(gr);
    }

    public Graph getMinCut(final Graph gr) {
        List<Graph> cuts = getMinCuts(gr);
        Collections.sort(cuts);
        return cuts.get(0);
    }

    public Graph getMinCut(final int[][] arr) {
        List<Graph> cuts = getMinCuts(arr);
        Collections.sort(cuts);
        return cuts.get(0);
    }

    private void reorganizeEdges(Graph gr, Vertex v1, Vertex v2) {
        //remove old vertex from graph
        gr.vertices.remove(v2.lbl);

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