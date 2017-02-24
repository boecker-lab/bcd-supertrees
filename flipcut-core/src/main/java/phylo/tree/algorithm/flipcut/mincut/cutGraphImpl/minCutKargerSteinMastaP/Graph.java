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

import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Graph implements Comparable<Graph>, Cloneable {
    private int hashCache = 0;

    //    final EdgeWeighter weighter;
    final TIntObjectMap<Vertex> vertices = new TIntObjectHashMap<>();

    final List<Edge> edges = new ArrayList<>();
    final Set<EdgeColor> edgeColors = new HashSet<>();

    List<EdgeColor> edgeColorList;
    TDoubleList weights;
    private double sumOfWeights = 0;

    public final HashSet<TIntSet> cutSets = new HashSet<>(2);


    public Graph() {
//        weighter = new EdgeWeighter() {
//        };
    }

//    public Graph(EdgeWeighter weighter) {
//        this.weighter = weighter;
//    }

    public double getSumOfWeights() {
        return sumOfWeights;
    }

    public int getNumOfColors() {
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
        Graph g = new Graph(/*weighter*/);
        vertices.forEachEntry((k, v) -> {
            g.vertices.put(k, new Vertex(v.lbl, v.mergedLbls));
            return true;
        });

        Iterable<EdgeColor> es;
        final boolean weightsSet = edgeColorList != null;
        if (weightsSet) {
            es = edgeColorList;
            g.edgeColorList = new ArrayList<>(edgeColorList.size());
        } else {
            es = edgeColors;
        }

        for (EdgeColor sourceColor : es) {
            EdgeColor target = sourceColor.clone();
            if (weightsSet) g.edgeColorList.add(target);
            for (Colorable c : sourceColor.getEdges()) {
                Edge e = ((Edge) c);

                Iterator<Vertex> it = e.iterator();
                Vertex v1 = g.vertices.get(it.next().lbl);
                Vertex v2 = g.vertices.get(it.next().lbl);

                g.addEdge(v1, v2, target);
            }
        }

        if (weightsSet) {
            g.weights = new TDoubleArrayList(weights);
            g.sumOfWeights = sumOfWeights;
        }

        return g;
    }

    public void refreshWeights() {
        weights = new TDoubleArrayList(edgeColors.size());
        edgeColorList = new ArrayList<>(edgeColors.size());
        sumOfWeights = 0;
        for (EdgeColor edge : edgeColors) {
            edgeColorList.add(edge);
            weights.add((sumOfWeights += edge.getWeight()));
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

    public boolean removeClolor(EdgeColor color) {
        if (edgeColors.remove(color)) {
            weights = null;
            edgeColorList = null;
            sumOfWeights = 0;
            return true;
        }
        return false;
    }
}
