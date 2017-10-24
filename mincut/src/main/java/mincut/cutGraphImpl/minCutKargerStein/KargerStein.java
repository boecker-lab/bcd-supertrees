package mincut.cutGraphImpl.minCutKargerStein;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com) based on https://gist.github.com/MastaP
 */

import gnu.trove.list.TDoubleList;
import mincut.EdgeColor;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.ThreadLocalRandom;

public class KargerStein {
    public static final double SQRT2 = Math.sqrt(2d);
    private Graph best;
    private LinkedHashSet<Graph> cuts;

    public Graph contract(Graph gr, int numOfVerticesLeft) {
        while (gr.vertices.size() > numOfVerticesLeft) {
//            long t =  System.currentTimeMillis();
            Edge edge = drawEdge(gr);
//            System.out.println("drew edge in: " + (System.currentTimeMillis() - t)/1000d + "s");
            Iterator<Vertex> it = edge.iterator();
//            t =  System.currentTimeMillis();
            reorganizeEdges(gr, it.next(), it.next());
//            System.out.println("reorganize edges in edge in: " + (System.currentTimeMillis() - t)/1000d + "s");
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
            final int contractTo = (int) Math.ceil((((double) n) / SQRT2) + 1d);

            Graph g1 = recursiveContract(contract(gr.clone(), contractTo));
            Graph g2 = recursiveContract(contract(gr.clone(), contractTo));

            return (g1.getSumOfWeights() < g2.getSumOfWeights()) ? g1 : g2;
        }
    }

    private Edge drawEdge(Graph g) {
        EdgeColor picked;
        if (g.preMergedColors.isEmpty()) {
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

            picked = g.edgeColorList.get(mid);

        } else {
            picked = g.preMergedColors.iterator().next();
        }

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
            final int n = gr.vertices.size();
            int iter = (int) Math.ceil(n * n * (Math.log(n) / Math.log(2)));
            cuts = new LinkedHashSet<>(iter);

            for (int i = 0; i < iter; i++) {
                Graph grc = gr.clone();
                contract(grc, 2);
                grc.setCutted(true);
                cuts.add(grc);
                if (best == null || grc.mincutValue() < best.mincutValue())
                    best = grc;
            }
        }
        return cuts;
    }

    public LinkedHashSet<Graph> getMinCuts(final int[][] arr, final boolean recursive) {
        Graph gr = GraphUtils.createGraph(arr);
        return getMinCuts(gr, recursive);
    }

    public Graph getMinCut(final Graph gr, final boolean recursive) {
        getMinCuts(gr, recursive);
        return best;
    }

    public Graph getMinCut(final int[][] arr, final boolean recursive) {
        getMinCuts(arr, recursive);
        return best;
    }


    public Graph sampleCut(Graph gr) {
        if (!gr.hasFreshEdges())
            gr.refreshWeights();
        Graph grc = gr.clone();
        contract(grc, 2);
        grc.setCutted(true);
        return grc;
    }

    private void reorganizeEdges(Graph gr, Vertex v1, Vertex v2) {
        boolean colorRemoved = false;
        //remove old vertex from graph
        gr.vertices.remove(v2.lbl);

        //add merged labels to v1
        v1.mergedLbls.addAll(v2.mergedLbls);

        //redirect edges
        for (Iterator<Edge> edgeIt = gr.edges.values().iterator(); edgeIt.hasNext(); ) {
            Edge edge = edgeIt.next();
            if (edge.contains(v1, v2)) {
                //remove loops
                v1.edges.remove(edge);
                v2.edges.remove(edge);//not needed
                for (Iterator<EdgeColor> colorit = edge.colorIterator(); colorit.hasNext(); ) {
                    EdgeColor color = colorit.next();
                    colorit.remove();

                    if (color.numOfEdges() == 0) { //remove colors from graph
                        if (gr.removeClolor(color))
                            colorRemoved = true;
                    }
                }
                edgeIt.remove();
            } else if (v2.edges.contains(edge)) {
                //redirect edges from v2 to v1
                v2.edges.remove(edge);//not needed
                Vertex v = edge.getOppositeVertex(v2);
                Edge toAdd = gr.getEdge(v, v1);
                if (toAdd != null) {
                    for (Iterator<EdgeColor> colorit = edge.colorIterator(); colorit.hasNext(); ) {
                        toAdd.add(colorit.next());
                    }
                    edge.clearColors();
                    edgeIt.remove();
                } else {
                    edge.replaceVertex(v2, v1);
                    v1.edges.add(edge);
                }
            }
        }
        if (colorRemoved)
            gr.refreshWeights();
    }
}