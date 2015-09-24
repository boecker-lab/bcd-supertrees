package flipcut.mincut.ahuja_orlin;

import flipcut.mincut.MaxFlowCutGraph;
import flipcut.mincut.ahuja_orlin.graph.Edge;
import flipcut.mincut.ahuja_orlin.graph.FlowGraph;
import flipcut.mincut.bipartition.BasicCut;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.LinkedHashSet;
import java.util.concurrent.Future;

/**
 * Created by fleisch on 21.09.15.
 */
public class AhujaOrlinCutGraph<V> extends MaxFlowCutGraph<V> {
    /**
     * The complete cut with score, source set and sink set
     */
//    protected BasicCut<V> cut;
    /**
     * The internal nodes to simplify graph construction
     */
    TObjectIntHashMap<V> vertexToNode = new TObjectIntHashMap<>();
    TIntObjectHashMap<V> nodeToVertex = new TIntObjectHashMap<>();

    /**
     * The internal graph
     */
    private final FlowGraph ahujaGraph = new FlowGraph();

    private int nodeCounter = 0;



    @Override
    public void addNode(V vertex) {
        if (!vertexToNode.containsKey(vertex)) {
            vertexToNode.put(vertex,++nodeCounter);
            nodeToVertex.put(nodeCounter,vertex);
            ahujaGraph.addNode(nodeCounter);
        }

    }

    @Override
    public void addEdge(V vertex1, V vertex2, long capacity) {
        addNode(vertex1);
        addNode(vertex2);
        ahujaGraph.addEdge(new Edge(vertexToNode.get(vertex1),vertexToNode.get(vertex2),capacity));
    }


    @Override
    public void clear() {
        super.clear();
        vertexToNode.clear();
        nodeToVertex.clear();
        ahujaGraph.clear();
        nodeCounter=0;
    }

    /**
     * Internal: does the mincut execution
     *
     * @param source the source
     * @param sink   the sink
     */
    @Override
    public BasicCut<V> calculateMinSTCut(V source, V sink) {
        ahujaGraph.setSource(vertexToNode.get(source));
        ahujaGraph.setSink(vertexToNode.get(sink));

        LinkedHashSet<V> sourceList = creatCut(ahujaGraph);
        BasicCut<V> cut = new BasicCut(sourceList, source, sink, (long)ahujaGraph.getMaximumFlow());
        return cut;
    }

    @Override
    protected MaxFlowCallable createCallable() {
        return new AhujaOrlinCallable();
    }

    private class AhujaOrlinCallable extends MaxFlowCallable {
        private FlowGraph flowGraph = null;

        public AhujaOrlinCallable(FlowGraph flowGraph) {
            this.flowGraph = flowGraph;
        }

        public AhujaOrlinCallable() {}

        @Override
        public BasicCut<V> call() throws Exception {
            if (flowGraph == null)
                flowGraph = ahujaGraph.clone();

            flowGraph.setSource(vertexToNode.get(source));
            flowGraph.setSink(vertexToNode.get(sink));

            BasicCut<V> cut = new BasicCut<V>(creatCut(flowGraph),source,sink,(long)flowGraph.getMaximumFlow());

            SS ss = stToCalculate.poll();
            if (ss != null) {
                AhujaOrlinCallable nuCallable = new AhujaOrlinCallable(flowGraph);
                flowGraph = null;
                nuCallable.setSourceAndSink(ss);
                busyMaxFlow.offer(executorService.submit(nuCallable));
            }
            return cut;
        }
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
}
