package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 */

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import phylo.tree.algorithm.flipcut.mincut.Colorable;
import phylo.tree.algorithm.flipcut.mincut.EdgeColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Graph implements Comparable<Graph>, Cloneable {
    private int hashCache = 0;

    final EdgeWeighter weighter;
    final TIntObjectMap<Vertex> vertices = new TIntObjectHashMap<>();

    final List<Edge> edges = new ArrayList<Edge>();
    final HashSet<EdgeColor> edgeColors = new HashSet<>();

    final TDoubleList weights = new TDoubleArrayList();
    private double sumOfWeights = 0;

    public final HashSet<TIntSet> cutSets = new HashSet<>(2);


    public Graph() {
        weighter = new EdgeWeighter() {
        };
    }

    public Graph(EdgeWeighter weighter) {
        this.weighter = weighter;
    }

    public double getSumOfWeights() {
        return sumOfWeights;
    }

    public int getNumOfEdges() {
        return weights.size();
    }

    public void addVertex(Vertex v) {
        vertices.put(v.lbl, v);
    }

    public Vertex getVertex(int lbl) {
        return vertices.get(lbl);
    }

    public boolean addEdge(int lbl1, int lbl2, double weight) {
        Vertex v1 = vertices.get(lbl1);
        Vertex v2 = vertices.get(lbl2);
        return addEdge(v1, v2, weight);
    }

    public boolean addEdge(Vertex v1, Vertex v2, double weight) {
        return addEdge(v1, v2, new EdgeColor(weight));
    }


    public boolean addEdge(Vertex v1, Vertex v2) {
        return addEdge(v1, v2, new EdgeColor(1d));
    }

    public boolean addEdge(Vertex v1, Vertex v2, EdgeColor c) {
        Edge e = new Edge(v1, v2, c);

        if (!vertices.containsKey(v1.lbl))
            addVertex(v1);
        if (!vertices.containsKey(v2.lbl))
            addVertex(v2);

        v1.addEdge(e);
        v2.addEdge(e);
        edges.add(e);
        if (c != null)
            edgeColors.add(c);

        return true;
    }

    @Override
    protected Graph clone() {
        Graph g = new Graph(weighter);
        vertices.forEachEntry((k, v) -> {
            g.vertices.put(k, new Vertex(v.lbl, v.mergedLbls));
            return true;
        });

        for (EdgeColor sourceColor : edgeColors) {
            EdgeColor target = sourceColor.clone();
            for (Colorable c : sourceColor.getEdges()) {
                Edge e = ((Edge) c);

                Iterator<Vertex> it = e.iterator();
                Vertex v1 = g.vertices.get(it.next().lbl);
                Vertex v2 = g.vertices.get(it.next().lbl);

                g.addEdge(v1, v2, target);
            }
        }

        g.weights.addAll(weights);
        g.sumOfWeights = sumOfWeights;

        return g;
    }

    public void refreshWeights() {
        weights.clear();
        sumOfWeights = 0;
        for (Edge edge : edges) {
            weights.add((sumOfWeights += edge.getWeight(weighter)));
        }
    }

    public TIntObjectMap<Vertex> getVertices() {
        return vertices;
    }

    private boolean cutted = false;

    public boolean isCutted() {
        return cutted;
    }

    public void setCutted(boolean cuted) {
        if (cuted != cutted) {
            this.cutted = cuted;
            cutSets.clear();
            if (this.cutted) {
                Iterator<Vertex> it = vertices.valueCollection().iterator();
                cutSets.add(it.next().getMergedLbls());
                cutSets.add(it.next().getMergedLbls());
            }
        }

    }

    public double mincutValue() {
        if (!cutted)
            return Double.NaN;
        return sumOfWeights;
    }

    @Override
    public int compareTo(Graph o) {
        return Double.compare(mincutValue(), o.mincutValue());
    }
//


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Graph)) return false;

        Graph graph = (Graph) o;

        if (Double.compare(graph.sumOfWeights, sumOfWeights) != 0) return false;
        if (cutted != graph.cutted) return false;
        return cutSets.equals(graph.cutSets);

    }

    @Override
    public int hashCode() {
        if (!cutted || hashCache == 0) {
            int result;
            long temp;
            temp = Double.doubleToLongBits(sumOfWeights);
            result = (int) (temp ^ (temp >>> 32));
            result = 31 * result + cutSets.hashCode();
            result = 31 * result + (cutted ? 1 : 0);
            hashCache = result;
        }
        return hashCache;
    }
}
