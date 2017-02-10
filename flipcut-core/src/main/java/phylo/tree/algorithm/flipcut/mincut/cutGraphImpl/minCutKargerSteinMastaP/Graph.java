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
import phylo.tree.algorithm.flipcut.mincut.EdgeColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Graph implements Cloneable{

    final EdgeWeighter weigter;
    final TIntObjectMap<Vertex> vertices = new TIntObjectHashMap<>();

    final List<Edge> edges = new ArrayList<Edge>();
    final HashSet<EdgeColor> edgeColors = new HashSet<>();

    final TDoubleList weights = new TDoubleArrayList();
    private double sumOfWeights = 0;


    public Graph() {
        weigter = new EdgeWeighter() {};
    }
    public Graph(EdgeWeighter weigter) {
        this.weigter = weigter;
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
        return addEdge(v1, v2, weight, null);
    }

    public boolean addEdge(Vertex v1, Vertex v2, EdgeColor c) {
        return addEdge(v1, v2, 1d, c);
    }

    public boolean addEdge(Vertex v1, Vertex v2) {
        return addEdge(v1, v2, 1d, null);
    }

    public boolean addEdge(Vertex v1, Vertex v2, double weight, EdgeColor c) {
        Edge e = new Edge(v1, v2, weight, c);

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
        Graph g = new Graph();
        vertices.forEachEntry((k, v) -> {
            g.vertices.put(k, new Vertex(v.lbl));
            return true;
        });
        edges.stream().forEach(e -> {
            Iterator<Vertex> it = e.iterator();
            g.addEdge(g.vertices.get(it.next().lbl), g.vertices.get(it.next().lbl), e.weight);
        });
        g.edgeColors.addAll(edgeColors);
        g.weights.addAll(weights);
        g.sumOfWeights = sumOfWeights;

        return g;
    }

    public void refreshWeights() {
        weights.clear();
        sumOfWeights = 0;
        for (Edge edge : edges) {
            weigter.weightEdge(this,edge);
            weights.add((sumOfWeights += edge.weight));
        }
    }
}
