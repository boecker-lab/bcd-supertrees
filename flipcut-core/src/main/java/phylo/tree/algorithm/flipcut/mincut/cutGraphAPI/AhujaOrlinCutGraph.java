package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI;

import core.utils.parallel.IterationCallableFactory;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.STCut;
import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.maxFlowAhujaOrlin.FlowGraph;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by fleisch on 21.09.15.
 */
public class AhujaOrlinCutGraph<V> extends MaxFlowCutGraph<V> {
    /**
     * The internal nodes to simplify graph construction
     */
    private TObjectIntHashMap<V> vertexToNode = new TObjectIntHashMap<>();
    TIntObjectHashMap<V> nodeToVertex = new TIntObjectHashMap<>();

    private AhujaOrlinCallableFactory factory;

    /**
     * The internal graph
     */
    private final FlowGraph ahujaGraph = new FlowGraph();
    private int nodeCounter = 0;


    @Override
    public void addNode(V vertex) {
        if (!vertexToNode.containsKey(vertex)) {
            vertexToNode.put(vertex, ++nodeCounter);
            nodeToVertex.put(nodeCounter, vertex);
            ahujaGraph.addNode(nodeCounter);
        }

    }

    @Override
    public void addEdge(V vertex1, V vertex2, long capacity) {
        addNode(vertex1);
        addNode(vertex2);
        ahujaGraph.addEdge(vertexToNode.get(vertex1), vertexToNode.get(vertex2), capacity);
    }


    @Override
    public void clear() {
        super.clear();
        vertexToNode.clear();
        nodeToVertex.clear();
        ahujaGraph.clear();
        nodeCounter = 0;
    }

    /**
     * Internal: does the mincut execution
     *
     * @param source the source
     * @param sink   the sink
     */
    @Override
    public STCut<V> calculateMinSTCut(V source, V sink) {
        return calculateMinSTCut(source, sink, ahujaGraph);
    }

    STCut<V> calculateMinSTCut(final V source, final V sink, final FlowGraph ahujaGraph) {
        ahujaGraph.setSource(vertexToNode.get(source));
        ahujaGraph.setSink(vertexToNode.get(sink));

        List<LinkedHashSet<V>> sourceList = creatCut(ahujaGraph);
        STCut<V> cut = new STCut(sourceList.get(0), sourceList.get(1), source, sink, (long) ahujaGraph.getMaximumFlow());
        return cut;
    }

    private List<LinkedHashSet<V>> creatCut(FlowGraph flowGraph) {
        //create source set
        TIntHashSet sourceSet = flowGraph.calculateSTCut();
        TIntIterator sIt = sourceSet.iterator();
        LinkedHashSet<V> sset = new LinkedHashSet<>(sourceSet.size());
        while (sIt.hasNext()) {
            sset.add(nodeToVertex.get(sIt.next()));
        }

        //create target set
        TIntHashSet targetSet = flowGraph.getTSet(sourceSet);
        TIntIterator tIt = targetSet.iterator();
        LinkedHashSet<V> tset = new LinkedHashSet<>(targetSet.size());
        while (tIt.hasNext()) {
            tset.add(nodeToVertex.get(tIt.next()));
        }

        return Arrays.asList(sset, tset);
    }

    private class AhujaOrlinCallable extends MaxFlowCallable {
        private FlowGraph flowGraph = null;

        AhujaOrlinCallable(List<SS> jobs) {
            super(jobs);
        }


        @Override
        void initGraph() {
            if (flowGraph == null)
                flowGraph = ahujaGraph.clone();
        }

        @Override
        public STCut<V> doJob(SS ss) {
            return calculateMinSTCut(ss.source, ss.sink, flowGraph);
        }
    }

    @Override
    AhujaOrlinCallableFactory getMaxFlowCallableFactory() {
        if (factory == null)
            factory = new AhujaOrlinCallableFactory();
        return factory;
    }

    private class AhujaOrlinCallableFactory implements IterationCallableFactory<AhujaOrlinCallable, SS> {
        @Override
        public AhujaOrlinCallable newIterationCallable(List<SS> list) {
            return new AhujaOrlinCallable(list);
        }
    }
}
