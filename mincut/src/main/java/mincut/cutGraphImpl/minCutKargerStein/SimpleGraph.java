package mincut.cutGraphImpl.minCutKargerStein;
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
import mincut.Colorable;
import mincut.EdgeColor;
import mincut.cutGraphAPI.bipartition.SimpleHashableCut;

import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SimpleGraph implements KargerGraph<SimpleGraph, TIntSet> {
    private int hashCache = 0;

    final TIntObjectMap<Vertex> vertices = new TIntObjectHashMap<>();

    final Map<VertexPair, Edge> edges = new HashMap<>();
    final Set<EdgeColor> edgeColors = new HashSet<>();
    final Set<EdgeColor> preMergedColors = new HashSet<>();

    List<EdgeColor> edgeColorList;
    TDoubleList weights;
    private double sumOfWeights = 0;

    private SimpleHashableCut cutSets = null;

    public double getSumOfWeights() {
        return sumOfWeights;
    }

    @Override
    public int getNumberOfVertices() {
        return vertices.size();
    }

    private int getNumOfColors() {
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

    public Edge getEdge(Vertex v1, Vertex v2) {
        return edges.get(new VertexPair(v1, v2));
    }

    public boolean addEdge(Vertex v1, Vertex v2, EdgeColor c) {
        if (!vertices.containsKey(v1.lbl))
            addVertex(v1);
        if (!vertices.containsKey(v2.lbl))
            addVertex(v2);

        Edge e = getEdge(v1, v2);
        if (e == null) {
            e = new Edge(v1, v2, c);

            v1.addEdge(e);
            v2.addEdge(e);

            edges.put(new VertexPair(v1, v2), e);

        } else {
            e.add(c);
        }
        if (c.preMerged) {
            preMergedColors.add(c);
        }
        return edgeColors.add(c);
    }

    @Override
    public SimpleGraph clone() {
        SimpleGraph g = new SimpleGraph(/*weighter*/);
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

        for (int l : vertices.keys()) {
            //todo debug
            if (g.getVertex(l) == null)
                System.out.println("What");
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

    @Override
    public SimpleHashableCut asCut() {
        if (cutSets == null && isCutted()) {
            Iterator<Vertex> it = vertices.valueCollection().iterator();

            cutSets = new SimpleHashableCut(it.next().getMergedLbls(), it.next().getMergedLbls(), getEdgeColors(), getSumOfWeights());
        }

        if (cutSets == null)
            throw new NoResultException("Cutsets are still empty!");


        return cutSets;


    }

    private void refreshWeights() {
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

//    private boolean cutted = false;

    public boolean isCutted() {
        return vertices.size() == 2;
    }


    public double mincutValue() {
        if (!isCutted())
            return Double.NaN;
        return sumOfWeights;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleGraph)) return false;

        SimpleGraph graph = (SimpleGraph) o;

        if (Double.compare(graph.sumOfWeights, sumOfWeights) != 0) return false;
        if (isCutted() != graph.isCutted()) return false;
        return cutSets == null || cutSets.equals(graph.cutSets);

    }

    @Override
    public int hashCode() {
        if (!isCutted() || hashCache == 0) {
            int result;
            long temp;
            temp = Double.doubleToLongBits(sumOfWeights);
            result = (int) (temp ^ (temp >>> 32));
            if (cutSets != null)
                result = 31 * result + cutSets.hashCode();
            result = 31 * result + (isCutted() ? 1 : 0);
            hashCache = result;
        }
        return hashCache;
    }

    private boolean removeClolor(EdgeColor color) {
        if (edgeColors.remove(color)) {
            preMergedColors.remove(color);
            weights = null;
            edgeColorList = null;
            sumOfWeights = 0;
            return true;
        }
        return false;
    }

    public void contract(final Random random) {
        if (edgeColorList == null || weights == null)
            refreshWeights();
//            long t =  System.currentTimeMillis();
        Edge edge = drawEdge(random);
//            System.out.println("drew edge in: " + (System.currentTimeMillis() - t)/1000d + "s");
        Iterator<Vertex> it = edge.iterator();
//            t =  System.currentTimeMillis();
        reorganizeEdges(it.next(), it.next());
//            System.out.println("reorganize edges in edge in: " + (System.currentTimeMillis() - t)/1000d + "s");
    }

    private void reorganizeEdges(Vertex v1, Vertex v2) {
        boolean colorRemoved = false;
        //remove old vertex from graph
        vertices.remove(v2.lbl);

        //add merged labels to v1
        v1.mergedLbls.addAll(v2.mergedLbls);

        //redirect edges
        for (Iterator<Edge> edgeIt = edges.values().iterator(); edgeIt.hasNext(); ) {
            Edge edge = edgeIt.next();
            if (edge.contains(v1, v2)) {
                //remove loops
                v1.edges.remove(edge);
                v2.edges.remove(edge);//not needed
                for (Iterator<EdgeColor> colorit = edge.colorIterator(); colorit.hasNext(); ) {
                    EdgeColor color = colorit.next();
                    colorit.remove();

                    if (color.numOfEdges() == 0) { //remove colors from graph
                        if (removeClolor(color))
                            colorRemoved = true;
                    }
                }
                edgeIt.remove();
            } else if (v2.edges.contains(edge)) {
                //redirect edges from v2 to v1
                v2.edges.remove(edge);//not needed
                Vertex v = edge.getOppositeVertex(v2);
                Edge toAdd = getEdge(v, v1);
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
            refreshWeights();
    }

    private Edge drawEdge(final Random random) {
        EdgeColor picked;
        if (preMergedColors.isEmpty()) {
            int upper = getNumOfColors();
            int downer = 0;

            double r = random.nextDouble() * getSumOfWeights();

            TDoubleList values = weights;

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

            picked = edgeColorList.get(mid);

        } else {
            picked = preMergedColors.iterator().next();
        }

        Edge e = (Edge) picked.getRandomElement();
        if (e == null)
            System.out.println("fali");
        return e;
    }

    public Set<EdgeColor> getEdgeColors() {
        return edgeColors;
    }
}
