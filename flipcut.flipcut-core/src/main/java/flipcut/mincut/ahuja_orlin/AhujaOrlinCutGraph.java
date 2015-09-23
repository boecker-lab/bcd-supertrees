package flipcut.mincut.ahuja_orlin;

import flipcut.mincut.MultiThreadedCutGraph;
import flipcut.mincut.DirectedCutGraph;
import flipcut.mincut.ahuja_orlin.algorithms.MaxFlowCalculator;
import flipcut.mincut.ahuja_orlin.graph.Edge;
import flipcut.mincut.ahuja_orlin.graph.FlowGraph;
import flipcut.mincut.bipartition.BasicCut;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by fleisch on 21.09.15.
 */
public class AhujaOrlinCutGraph<V> extends MultiThreadedCutGraph<V> implements DirectedCutGraph<V> {
    /**
     * The complete cut with score, source set and sink set
     */
    protected BasicCut<V> cut;
    /**
     * The internal nodes to simplify graph construction
     */
    TObjectIntHashMap<V> nodes = new TObjectIntHashMap<>();

    /**
     * Global node counter
     */
    private int nodeCounter = 0;

    /**
     * The computer
     */
    private MaxFlowCalculator ahuja =  null;
    /**
     * The internal graph
     */
    private FlowGraph ahujaGraph = new FlowGraph();



    @Override
    public BasicCut<V> calculateMinSTCut(V source, V sink) {
        ahujaGraph.setSource(nodes.get(source));
        ahujaGraph.setSink(nodes.get(sink));
        MaxFlowCalculator.getMaxFlow(ahujaGraph);
        //todo claculate mincut form residual network
        return null;
    }

    @Override
    public void submitSTCutCalculation(V source, V sink) {

    }

    @Override
    public List<BasicCut<V>> calculateMinSTCuts() {
        return null;
    }


    @Override
    public BasicCut<V> calculateMinCut() throws ExecutionException, InterruptedException {
        return null;
    }

    @Override
    public void addNode(V vertex) {
        if(ahuja != null) throw new RuntimeException("A computation was already started. You can not add new nodes or edges !");
        if (!nodes.containsKey(vertex)) {
            nodes.put(vertex,++nodeCounter);
            ahujaGraph.addNode(nodeCounter);
        }

    }

    @Override
    public void addEdge(V vertex1, V vertex2, long capacity) {
        addNode(vertex1);
        addNode(vertex2);
        ahujaGraph.addEdge(new Edge(nodes.get(vertex1),nodes.get(vertex2),capacity));
    }

    @Override
    public void clear() {

    }
}
