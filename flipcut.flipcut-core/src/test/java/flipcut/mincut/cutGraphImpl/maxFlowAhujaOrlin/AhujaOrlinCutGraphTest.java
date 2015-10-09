package flipcut.mincut.cutGraphImpl.maxFlowAhujaOrlin;

import flipcut.mincut.cutGraphAPI.AhujaOrlinCutGraph;
import flipcut.mincut.cutGraphAPI.MaxFlowCutGraph;
import flipcut.mincut.cutGraphAPI.bipartition.BasicCut;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by fleisch on 24.09.15.
 */
public class AhujaOrlinCutGraphTest {
    private FlowGraph buildTestGraph() {
        FlowGraph graph = new FlowGraph();

        int na = 0;
        int n1 = 1;
        int n2 = 2;
        int n3 = 3;
        int n4 = 4;
        int n5 = 5;
        int nb = 6;

        graph.addNode(na);
        graph.setSource(na);

        graph.addNode(n1);
        graph.addNode(n2);
        graph.addNode(n3);
        graph.addNode(n4);
        graph.addNode(n5);

        graph.addNode(nb);
        graph.setSink(nb);

        graph.addEdge(na, n1, 3);
        graph.addEdge(na, n3, 5);
        graph.addEdge(na, n2, 3);
        graph.addEdge(n1, n3, 4);
        graph.addEdge(n1, n4, 3);
        graph.addEdge(n3, n4, 2);
        graph.addEdge(n3, nb, 1);
        graph.addEdge(n3, n5, 4);
        graph.addEdge(n2, n3, 2);
        graph.addEdge(n2, n5, 2);
        graph.addEdge(n4, nb, 4);
        graph.addEdge(n5, nb, 4);

        return graph;
    }


    @Test
    public void simpleSampleFlowGraphTest() {
        // costruisci il grafo di test
        FlowGraph g = buildTestGraph();

        // calcola il massimo flusso inviabile tra sorgente e pozzo su g
        double f = g.getMaximumFlow();
//        TIntHashSet sourceSet = g.calculateSTCut();
//        System.out.println(sourceSet.toString());
//        System.out.println("max flow on g = " + f);
        // stampa la distribuzione di flusso di g (la lista degli archi con relativi flussi e capacit�)
        System.out.println("flow distribution on g = " + g.getEdges());
        // calcola il massimo flusso inviabile tra sorgente e pozzo utilizzando il sottografo
        // indotto dai nodi 1, 3 e 4
        Set<Integer> s134 = new HashSet<Integer>();
        s134.add(1);
        s134.add(3);
        s134.add(4);
        FlowGraph g134 = buildTestGraph().getSubGraph(s134);
        double f134 = g134.getMaximumFlow();
        System.out.println("max flow on g[1, 3, 4] = " + f134);
        System.out.println("flow distribution on g[1, 3, 4] = " + g134.getEdges());

        // calcola il massimo flusso inviabile tra sorgente e pozzo utilizzando il sottografo
        // indotto dai nodi 2 e 5
        Set<Integer> s25 = new HashSet<Integer>();
        s25.add(2);
        s25.add(5);
        FlowGraph g25 = buildTestGraph().getSubGraph(s25);
        double f25 = g25.getMaximumFlow();
        System.out.println("max flow on g[2, 5] = " + f25);
        System.out.println("flow distribution on g[2, 5] = " + g25.getEdges());

        // usa la tua fantasia
        Set<Integer> s2 = new HashSet<Integer>();
        s2.add(2);
        FlowGraph g2 = buildTestGraph().getSubGraph(s2);
        double f2 = g2.getMaximumFlow();
        System.out.println("max flow on g[2] = " + f2);
        System.out.println("flow distribution on g[2] = " + g2.getEdges());
    }

    @Test
    public void testExample() {
        MaxFlowCutGraph hp = new AhujaOrlinCutGraph<>();
        hp.addNode(1);
        hp.addNode(2);
        hp.addNode(3);
        hp.addNode(4);
        hp.addNode(5);
        hp.addNode(6);
        hp.addNode(7);
        hp.addNode(8);


        hp.addEdge(1, 2, 5);
        hp.addEdge(2, 3, 5);
        hp.addEdge(3, 4, 5);
        hp.addEdge(3, 5, 2);
        hp.addEdge(4, 2, 5);
        hp.addEdge(4, 7, 2);
        hp.addEdge(5, 6, 5);
        hp.addEdge(6, 6, 5);
        hp.addEdge(6, 8, 4);
        hp.addEdge(7, 5, 5);
        hp.addEdge(7, 8, 1);

        BasicCut cut = hp.calculateMinSTCut(1, 8);
        System.out.println(cut.minCutValue);
        System.out.println(cut.getCutSet());


        assertEquals(4, cut.minCutValue);
        assertEquals(4, cut.getCutSet().size());
        assertFalse(cut.getCutSet().contains(5));
        assertFalse(cut.getCutSet().contains(6));
        assertFalse(cut.getCutSet().contains(7));
        assertFalse(cut.getCutSet().contains(8));

        /*
        p max 8 11
        n 1 s
        n 8 t
        a 1 2 5
        a 2 3 5
        a 3 4 5
        a 3 5 2
        a 4 2 5
        a 4 7 2
        a 5 6 5
        a 6 7 5
        a 6 8 4
        a 7 5 5
        a 7 8 1
        */

    }

}
