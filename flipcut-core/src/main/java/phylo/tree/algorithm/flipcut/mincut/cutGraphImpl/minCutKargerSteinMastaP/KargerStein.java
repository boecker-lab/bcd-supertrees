package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com) based on https://gist.github.com/MastaP
 */

import gnu.trove.list.TDoubleList;
import phylo.tree.algorithm.flipcut.mincut.EdgeColor;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.ThreadLocalRandom;

public class KargerStein {
    private Graph best;
    private LinkedHashSet<Graph> cuts;

    public Graph contract(Graph gr, int numOfVerticesLeft) {
        while (gr.vertices.size() > numOfVerticesLeft) {
            Edge edge = drawEdge(gr);
            Iterator<Vertex> it = edge.iterator();
            reorganizeEdges(gr, it.next(), it.next());
        }
        return gr;
    }

    private Graph recursiveContract(Graph gr) {
        final int n = gr.vertices.size();
        if (n <= 6) {
            Graph g1 = contract(gr, 2);
            g1.setCutted(true);
            cuts.add(g1);
            return g1;
        } else {
            final int contractTo = (int) Math.ceil((n / Math.sqrt(2d) + 1d));
            Graph g1 = recursiveContract(contract(gr.clone(), contractTo));
            Graph g2 = recursiveContract(contract(gr.clone(), contractTo));

            if (g1.getSumOfWeights() <= g2.getSumOfWeights())
                return g1;
            else
                return g2;
        }
    }

    private Edge drawEdge(Graph g) {
        int upper = g.getNumOfColors();
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

        EdgeColor picked = g.edgeColorList.get(mid);
        Edge e = (Edge) picked.getRandomElement();
        if (e == null)
            System.out.println("fali");
        return e;
    }

    public LinkedHashSet<Graph> getMinCuts(final Graph gr, final boolean recursive) {
        gr.refreshWeights();
        if (recursive) {
            cuts = new LinkedHashSet<>();
            best = recursiveContract(gr);
        } else {
            best = null;
            int iter = gr.vertices.size() * gr.vertices.size();
            cuts = new LinkedHashSet<>(iter);

            for (int i = 0; i < iter; i++) {
                Graph grc = gr.clone();
                contract(grc, 2);
                grc.setCutted(true);
                cuts.add(grc);
            }
        }
        return cuts;
    }

    public LinkedHashSet<Graph> getMinCuts(final int[][] arr, final boolean recursive) {
        Graph gr = GraphUtils.createGraph(arr);
        return getMinCuts(gr, recursive);
    }

    public Graph getMinCut(final Graph gr) {
        return best;
    }

    public Graph getMinCut(final int[][] arr) {
        getMinCuts(arr, true);
        return best;
    }

    private void reorganizeEdges(Graph gr, Vertex v1, Vertex v2) {
        boolean colorRemoved = false;
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
                EdgeColor color = edge.deleteColor();
                if (color != null && color.numOfEdges() == 0) {//remove color from graph
                    if (gr.removeClolor(color))
                        colorRemoved = true;
                }

                it.remove();

            } else if (v2.edges.contains(edge)) {
                //redirect edges from v2 to v1
                v2.edges.remove(edge);//not needed
                edge.replaceVertex(v2, v1);
                v1.edges.add(edge);
            }
        }
        if (colorRemoved)
            gr.refreshWeights();
    }
}