package flipcut.mincut.cutGraphAPI;

import flipcut.mincut.cutGraphAPI.bipartition.BasicCut;
import flipcut.mincut.cutGraphImpl.maxFlowAhujaOrlin.FlowGraph;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import utils.parallel.IterationCallableFactory;

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
    public BasicCut<V> calculateMinSTCut(V source, V sink) {
        return calculateMinSTCut(source, sink, ahujaGraph);
    }

    BasicCut<V> calculateMinSTCut(final V source, final V sink, final FlowGraph ahujaGraph) {
        ahujaGraph.setSource(vertexToNode.get(source));
        ahujaGraph.setSink(vertexToNode.get(sink));

        LinkedHashSet<V> sourceList = creatCut(ahujaGraph);
        BasicCut<V> cut = new BasicCut(sourceList, source, sink, (long) ahujaGraph.getMaximumFlow());
        return cut;
    }

    private LinkedHashSet<V> creatCut(FlowGraph flowGraph) {
        TIntHashSet soourceSet = flowGraph.calculateSTCut();
        TIntIterator ti = soourceSet.iterator();
        LinkedHashSet<V> set = new LinkedHashSet<>(soourceSet.size());

        while (ti.hasNext()) {
            set.add(nodeToVertex.get(ti.next()));
        }
        return set;
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
        public BasicCut<V> doJob(SS ss) {
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
