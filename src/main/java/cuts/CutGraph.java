/*
 * Epos Phylogeny Framework
 * Copyright (C) 2009.  University of Jena
 *
 * This file is part of Epos.
 *
 * Epos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Epos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Epos.  If not, see <http://www.gnu.org/licenses/>
 */

package cuts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * This is an implementation of the push-relabel method to compute minimum cuts/maximum flows
 * on a directed graph.
 * See :
 * <pre>
  Goldberg and Tarjan, "A New Approach to the Maximum Flow Problem,"
  J. ACM Vol. 35, 921--940, 1988
 </pre>and
 * <pre>
  Cherkassky and Goldberg, "On Implementing Push-Relabel Method for the
  Maximum Flow Problem," Proc. IPCO-4, 157--171, 1995.</pre>
 * <br>
 * The internal code is a translation of the C code at http://www.avglab.com/andrew/soft.html with
 * slight modifications to fit better into an object structure. We need a bit more memory to create the objects,
 * but the runtime is not be effected as we use the same data structures.
 * <p>
 * You can construct the cut graph using arbitrary objects, but note that nodes should not be equal, i.e
 * {@code nodeA.equals(nodeB)} should only be {@code true} if {@code nodeA == nodeB}. Adding the nodes explicitly is optional.
 * The {@link #addEdge(Object, Object, long)} method will insert the nodes if they are not part of the graph.
 * <p>
 * Sample usage:
 * <pre>
<code>
CutGraph g = new CutGraph();

g.addEdge(1, 2, 5);
g.addEdge(2, 3, 5);
g.addEdge(3, 4, 5);
g.addEdge(3, 5, 2);
g.addEdge(4, 2, 5);
g.addEdge(4, 7, 2);
g.addEdge(5, 6, 5);
g.addEdge(6, 6, 5);
g.addEdge(6, 8, 4);
g.addEdge(7, 5, 5);
g.addEdge(7, 8, 1);


System.out.println(hp.getMinCutValue(1, 8));
System.out.println(hp.getMinCut(1, 8));
</code></pre>
 *
 * @param <T> the nodes type
 * @author Thasso Griebel (thasso.griebel@gmail.com)
 */
public class CutGraph<T> {
    /**
     * The internal nodes to simplify graph construction
     */
    Map<Object, N> nodes = new HashMap<Object, N>();
    /**
     * Global edge counter
     */
    private int edges = 0;
    /**
     * The current cut value
     */
    private long cutValue = -1;
    /**
     * All nodes of the component that contains the sink
     */
    private List<T> cut;
    /**
     * The source
     */
    private T source;
    /**
     * The target
     */
    private T target;
    /**
     * The computer
     */
    private CutGraphImpl hipri;

    /**
     * Returns the minimum cut value for a cut between the source and the target node. This
     * computes the cut lazily and just once for a given soruce and target.
     *
     * @param source the source node
     * @param target the target node
     * @return cutValue the cut value for the cut between source and target
     */
    public long getMinCutValue(T source, T target) {
        if (source != this.source || target != this.target || cutValue == -1) {
            this.source = source;
            this.target = target;
            mincut(source, target);
        }
        return cutValue;
    }

    /**
     * Returns the list of nodes in the target component of the graph, including the target node itself.
     * This does lazy computation, if the cut was not computed for given source and sink, it computes the cut.
     *
     * @param source the source
     * @param target the target
     * @return mincut all nodes of the component that contains the target node (incl. the target node)
     */
    public List<T> getMinCut(T source, T target) {
        if (source != this.source || target != this.target || cutValue == -1) {
            this.source = source;
            this.target = target;
            mincut(source, target);
        }
        return cut;
    }

    /**
     * Internal: does the mincut execution
     *
     * @param source the source
     * @param sink the sink
     */
    void mincut(Object source, Object sink) {
        if(hipri == null){
            hipri = new CutGraphImpl(nodes.size(), edges);
            /*
            Remap to internal structure
             */
            for (Map.Entry<Object, N> entry : nodes.entrySet()) {
                N node = entry.getValue();
                Object name = entry.getKey();
                node.node = hipri.createNode(name, node.edges.size() + node.revEdges.size());
            }

            for (N node : nodes.values()) {
                for (E edge : node.edges) {
                    hipri.addEdge(node.node, edge.target.node, edge.cap);
                }
            }
        }

        CutGraphImpl.Node s = nodes.get(source).node;
        CutGraphImpl.Node t = nodes.get(sink).node;

        this.cut = (List<T>) hipri.mincut(s, t, false);
        this.cutValue = hipri.getValue();
    }

    public Map<Object, Object> getNodes() {
        return new HashMap<Object, Object>(nodes);
    }


    /**
     * Adds the given node to the graph. Does nothing if the graph already contains the node. 
     *
     * @param source the source
     */
    public void addNode(Object source) {
        if(hipri != null) throw new RuntimeException("A computation was already started. You can not add new nodes or edges !");
        if (!nodes.containsKey(source)) {
            N node = new N();
            nodes.put(source, node);
        }
    }

    /**
     * Add an edge from source to target with given capacity. This will add the source or target
     * if they are not added to the graph already.
     *
     * @param source   the source
     * @param target   the target
     * @param capacity the capacity
     */
    public void addEdge(Object source, Object target, long capacity) {
        // add node checks if the nodes are already contained
        addNode(source);
        addNode(target);
        
        /*
         * Add edge
         */
        E e = new E(nodes.get(target), capacity);
        nodes.get(source).edges.add(e);
        /*
         * Add reverse edge with capacity 0
         */
        E reverse = new E(nodes.get(source), 0);
        nodes.get(target).revEdges.add(reverse);

        e.reverseEdge = reverse;
        reverse.reverseEdge = e;
        edges +=2;
    }

    public void printGraph(){
        System.out.println("##### Start Printing CutGraph #####");
        Map<N,Object> reverseMap = new HashMap<N, Object>(nodes.size());
        for (Map.Entry<Object,N> node : nodes.entrySet()) {
            reverseMap.put(node.getValue(),node.getKey());
        }
        for (N node : reverseMap.keySet()) {
            for (E edge : node.edges) {
                System.out.println(reverseMap.get(node)+"---"+edge.cap+"--->"+reverseMap.get(edge.target));
            }
        };
        System.out.println("##### Printing CutGraph DONE! #####");
    }

    public Map<Object,List<Object>> getCharacterEdges(Map<Object,Object> reverseMap){
        System.out.println("##### Start Printing CutGraph #####");
        Map<Object,List<Object>> edges = new HashMap<Object, List<Object>>();
        for (Object node : reverseMap.keySet()) {
            for (E edge : ((N)node).edges) {
                List target = new ArrayList(2);
                target.add(reverseMap.get(edge.target));
                target.add(edge.cap);
                edges.put(reverseMap.get(node),target);
            }
        }
        return  edges;
    }

    /**
     * Internal builder representation for Nodes
     */
    private class N {
        private CutGraphImpl.Node node;
        private List<E> edges = new ArrayList<E>();
        private List<E> revEdges = new ArrayList<E>();
    }

    /**
     * Internal representation for edges
     */
    private class E {
        long cap;
        N target;
        E reverseEdge;

        public E(N target, long capacity) {
            this.target = target;
            this.cap = capacity;
        }
    }
    
    
}
