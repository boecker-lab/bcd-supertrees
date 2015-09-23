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

package flipcut.mincut.goldberg_tarjan;

import flipcut.mincut.MultiThreadedCutGraph;
import flipcut.mincut.bipartition.BasicCut;
import flipcut.mincut.DirectedCutGraph;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Callable;

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
 *
 * You can construct the cut graph using arbitrary objects, but note that nodes should not be equal, i.e
 * {@code nodeA.equals(nodeB)} should only be {@code true} if {@code nodeA == nodeB}. Adding the nodes explicitly is optional.
 * The {@link #addEdge(Object, Object, long)} method will insert the nodes if they are not part of the graph.
 *
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
public class GoldbergTarjanCutGraph<V> extends MultiThreadedCutGraph<V> implements DirectedCutGraph<V> {
    /**
     * The complete cut with score, source set and sink set
     */
//    protected BasicCut<V> cut;
    protected CutGraphImpl hipri;
    /**
     * The internal nodes to simplify graph construction
     */
    final Map<V, N> nodes = new HashMap<>();
    final Map<CutGraphImpl, Map<N, CutGraphImpl.Node>> algoNodeMaps = new ConcurrentHashMap<>(CORES_AVAILABLE);
    /**
     * Global edge counter
     */
    private int edges = 0;


    /**
     * The computer
     */
    private Queue<Future<BasicCut<V>>> busyHipries;
    private Queue<SS> stToCalculate = new LinkedBlockingQueue<>();


    private CutGraphImpl createHipri() {
        CutGraphImpl hipri = new CutGraphImpl(nodes.size(), edges);
        Map<N, CutGraphImpl.Node> algoNodes = new HashMap<>(nodes.size());
        algoNodeMaps.put(hipri, algoNodes);
            /*
            Remap to internal structure
             */
        for (Map.Entry<V, N> entry : nodes.entrySet()) {
            N node = entry.getValue();
            V vertex = entry.getKey();

            algoNodes.put(node, hipri.createNode(
                    vertex, node.edges.size() + node.revEdges.size()));
        }

        for (N node : nodes.values()) {
            for (E edge : node.edges) {
                hipri.addEdge(algoNodes.get(node), algoNodes.get(edge.target), edge.cap);
            }
        }
        return hipri;
    }

    /**
     * Internal: does the mincut execution
     *
     * @param source the source
     * @param sink   the sink
     */
    @Override
    public BasicCut<V> calculateMinSTCut(V source, V sink) {
        if (hipri == null)
            hipri = createHipri();
        Map<N, CutGraphImpl.Node> map = algoNodeMaps.get(hipri);
        hipri.setSource(map.get(nodes.get(source)));
        hipri.setSink(map.get(nodes.get(sink)));

        LinkedHashSet<V> sinkList = (LinkedHashSet<V>) hipri.calculateMaxSTFlow(false);
        return new BasicCut(sinkList, source, sink, hipri.getValue());
    }

    public void submitSTCutCalculation(V source, V sink) {
        stToCalculate.offer(new SS(source, sink));
    }

    @Override
    public List<BasicCut<V>> calculateMinSTCuts() {
        List<BasicCut<V>> stCuts = new LinkedList<>();
        while (!stToCalculate.isEmpty()) {
            SS st = stToCalculate.poll();
            stCuts.add(
                    calculateMinSTCut(st.source, st.sink));
        }
        return stCuts;
    }

    @Override
    public BasicCut<V> calculateMinCut() throws ExecutionException, InterruptedException {
        if (threads == 1 || CORES_AVAILABLE == 1) {
            return calculatMinCutSingle();
        } else if (threads == 0) {
            return calculatMinCutParallel((CORES_AVAILABLE / 2) - 1); // find a HT solution
        } else {
            return calculatMinCutParallel(threads - 1); //minus 1 because it is daster if 1 thread is left for other things
        }

    }

    private BasicCut<V> calculatMinCutSingle() {
        BasicCut<V> cut = BasicCut.MAX_CUT_DUMMY;
        while (!stToCalculate.isEmpty()) {
            SS st = stToCalculate.poll();
            BasicCut<V> next = calculateMinSTCut(st.source, st.sink);
            if (next.minCutValue < cut.minCutValue)
                cut=next;
        }
        return cut;
    }

    private BasicCut<V> calculatMinCutParallel(int threads) throws ExecutionException, InterruptedException {
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(threads);
        }

        busyHipries = new ArrayBlockingQueue<>(stToCalculate.size());
        BasicCut<V> cut = BasicCut.MAX_CUT_DUMMY;

        int index = 0;
        while (index < threads && !stToCalculate.isEmpty()) {
            SS ss = stToCalculate.poll();
            HipriCallable callable = new HipriCallable();
            callable.setSourceAndSink(ss);
            busyHipries.offer(executorService.submit(callable));
            index++;
        }

        while (!busyHipries.isEmpty()) {
            Future<BasicCut<V>> f = busyHipries.poll();
            BasicCut<V> next = f.get();
            if (next.minCutValue < cut.minCutValue)
                cut = next;
        }
        return cut;
    }

    public Map<Object, Object> getNodes() {
        return new HashMap<Object, Object>(nodes);
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
        E reverse = new E(nodes.get(source), 0);
        nodes.get(target).revEdges.add(reverse);

        e.reverseEdge = reverse;
        reverse.reverseEdge = e;
        edges += 2;
    }

    public void printGraph() {
        System.out.println("##### Start Printing CutGraph #####");
        Map<N, Object> reverseMap = new HashMap<N, Object>(nodes.size());
        for (Map.Entry<V, N> node : nodes.entrySet()) {
            reverseMap.put(node.getValue(), node.getKey());
        }
        for (N node : reverseMap.keySet()) {
            for (E edge : node.edges) {
                System.out.println(reverseMap.get(node) + "---" + edge.cap + "--->" + reverseMap.get(edge.target));
            }
        }
        ;
        System.out.println("##### Printing CutGraph DONE! #####");
    }

    public Map<Object, List<Object>> getCharacterEdges(Map<Object, Object> reverseMap) {
        System.out.println("##### Start Printing CutGraph #####");
        Map<Object, List<Object>> edges = new HashMap<Object, List<Object>>();
        for (Object node : reverseMap.keySet()) {
            for (E edge : ((N) node).edges) {
                List target = new ArrayList(2);
                target.add(reverseMap.get(edge.target));
                target.add(edge.cap);
                edges.put(reverseMap.get(node), target);
            }
        }
        return edges;
    }


    @Override
    public void clear() {
        nodes.clear();
        algoNodeMaps.clear();
        stToCalculate.clear();
        hipri = null;
        edges = 0;
    }

    /**
     * Internal builder representation for Nodes
     */
    private class N {
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

    private class HipriCallable implements Callable<BasicCut<V>> {
        private CutGraphImpl hipri = null;
        private V source;
        private V sink;

        public HipriCallable(CutGraphImpl hipri) {
            this.hipri = hipri;
        }

        public HipriCallable() {
        }

        public void setSource(V source) {
            this.source = source;
        }

        public void setSink(V sink) {
            this.sink = sink;
        }

        public void setSourceAndSink(SS sourceAndSink) {
            this.source = sourceAndSink.source;
            this.sink = sourceAndSink.sink;
        }

        @Override
        public BasicCut<V> call() throws Exception {
            if (hipri == null)
                hipri = createHipri();

            Map<N, CutGraphImpl.Node> m = algoNodeMaps.get(hipri);
            hipri.setSource(m.get(nodes.get(source)));
            hipri.setSink(m.get(nodes.get(sink)));

            LinkedHashSet<V> sinkList = (LinkedHashSet<V>) hipri.calculateMaxSTFlow(false);
            BasicCut<V> cut = new BasicCut(sinkList, source, sink, hipri.getValue());

            SS ss = stToCalculate.poll();
            if (ss != null) {
                HipriCallable nuCallable = new HipriCallable(hipri);
                hipri = null;
                nuCallable.setSourceAndSink(ss);
                busyHipries.offer(executorService.submit(nuCallable));
            }
            return cut;
        }
    }

    private class SS {
        final V source;
        final V sink;

        public SS(V source, V sink) {
            this.source = source;
            this.sink = sink;
        }
    }


}
