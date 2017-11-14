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

package mincut.cutGraphAPI;

import core.utils.parallel.IterationCallableFactory;
import mincut.cutGraphAPI.bipartition.STCut;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.CutGraphImpl;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.Node;

import java.util.*;

/**
 * This is an implementation of the push-relabel method to compute minimum mincut/maximum flows
 * on a directed graph.
 * See :
 * <pre>
 * Goldberg and Tarjan, "A New Approach to the Maximum Flow Problem,"
 * J. ACM Vol. 35, 921--940, 1988
 * </pre>and
 * <pre>
 * Cherkassky and Goldberg, "On Implementing Push-Relabel Method for the
 * Maximum Flow Problem," Proc. IPCO-4, 157--171, 1995.</pre>
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
 * <code>
 * CutGraph g = new CutGraph();
 *
 * g.addEdge(1, 2, 5);
 * g.addEdge(2, 3, 5);
 * g.addEdge(3, 4, 5);
 * g.addEdge(3, 5, 2);
 * g.addEdge(4, 2, 5);
 * g.addEdge(4, 7, 2);
 * g.addEdge(5, 6, 5);
 * g.addEdge(6, 6, 5);
 * g.addEdge(6, 8, 4);
 * g.addEdge(7, 5, 5);
 * g.addEdge(7, 8, 1);
 *
 *
 * System.out.println(hp.getMinCutValue(1, 8));
 * System.out.println(hp.getMinCut(1, 8));
 * </code></pre>
 *
 * @param <V> the nodes type
 * @author Thasso Griebel (thasso.griebel@gmail.com)
 */
public class GoldbergTarjanCutGraph<V> extends MaxFlowCutGraph<V> implements CutGraph<V> {
    /**
     * Global edge counter
     */
    private int edges = 0;
    /**
     * hipri graph for single threaded execution and mapping
     */
    private CutGraphImpl hipri;
    private Map<N, Node> algoNodes;

    private HipriCallableFactory factory;

    /**
     * The internal nodes to simplify graph construction: not private because of inner class access
     */
    private Map<V, N> nodes = new HashMap<>();


    CutGraphImpl createHipri(final Map<N, Node> algoNodeMapToFill) {
        CutGraphImpl hipri = new CutGraphImpl(nodes.size(), edges);
        //Remap to internal structure
        for (Map.Entry<V, N> entry : nodes.entrySet()) {
            N node = entry.getValue();
            V vertex = entry.getKey();

            algoNodeMapToFill.put(node, hipri.createNode(
                    vertex, node.edges.size() + node.revEdges));
        }

        for (N node : nodes.values()) {
            for (E edge : node.edges) {
                hipri.addEdge(algoNodeMapToFill.get(node), algoNodeMapToFill.get(edge.target), edge.cap);
            }
        }
        return hipri;
    }


    public void addNode(V source) {
        if (hipri != null)
            throw new RuntimeException("A computation was already started. You can not add new nodes or edges !");
        if (!nodes.containsKey(source)) {
            N node = new N();
            nodes.put(source, node);
        }
    }

    public void addEdge(V source, V target, long capacity) {
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
        nodes.get(target).revEdges++;

        /*
         * increase edge counter
         */
        edges += 2;
    }

    @Override
    public void clear() {
        super.clear();
        nodes.clear();
        hipri = null;
        algoNodes = null;
        edges = 0;
    }

    public Map<Object, Object> getNodes() {
        return new HashMap<>(nodes);
    }


    /**
     * Does the mincut execution
     *
     * @param source the source
     * @param sink   the sink
     */
    @Override
    public STCut<V> calculateMinSTCut(V source, V sink) {
        if (hipri == null) {
            algoNodes = new HashMap<>(nodes.size());
            hipri = createHipri(algoNodes);
        }
        return calculateMinSTCut(source, sink, hipri, algoNodes);
    }

    STCut<V> calculateMinSTCut(final V source, final V sink, final CutGraphImpl hipri, Map<N, Node> algoNodeMap) {
        hipri.setSource(algoNodeMap.get(nodes.get(source)));
        hipri.setSink(algoNodeMap.get(nodes.get(sink)));

        List<LinkedHashSet<Object>> cutList = hipri.calculateMaxSTFlowFull(false);
        return new STCut(cutList.get(0), cutList.get(1), source, sink, hipri.getValue());
    }

    private class HipriCallable extends MaxFlowCallable {
        private CutGraphImpl hipri;
        private Map<N, Node> algoNodeMap;

        public HipriCallable(List<MaxFlowCutGraph<V>.SS> jobs) {
            super(jobs);
        }

        @Override
        void initGraph() {
            if (hipri == null) {
                algoNodeMap = new HashMap<>(nodes.size());
                this.hipri = createHipri(algoNodeMap);
            }
        }

        @Override
        public STCut<V> doJob(SS ss) {
            return calculateMinSTCut(ss.source, ss.sink, hipri, algoNodeMap);
        }
    }

    @Override
    HipriCallableFactory getMaxFlowCallableFactory() {
        if (factory == null)
            factory = new HipriCallableFactory();
        return factory;
    }

    private class HipriCallableFactory implements IterationCallableFactory<HipriCallable, SS> {
        @Override
        public HipriCallable newIterationCallable(List<SS> list) {
            return new HipriCallable(list);
        }
    }

    /**
     * Internal builder representation for Nodes
     */
    private class N {
        private List<E> edges = new ArrayList<E>();
        private int revEdges = 0;
    }

    /**
     * Internal representation for edges
     */
    private class E {
        long cap;
        N target;

        public E(N target, long capacity) {
            this.target = target;
            this.cap = capacity;
        }
    }
}
