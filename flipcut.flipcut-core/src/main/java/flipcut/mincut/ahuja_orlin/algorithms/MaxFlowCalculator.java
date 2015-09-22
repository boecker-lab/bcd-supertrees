package flipcut.mincut.ahuja_orlin.algorithms;

import flipcut.mincut.ahuja_orlin.graph.Edge;
import flipcut.mincut.ahuja_orlin.graph.FlowGraph;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.queue.TIntQueue;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Classe che implementa l'algoritmo per il calcolo del massimo flusso su grafo.
 * L'algoritmo implementato � noto come "Improved shortest augmenting path" di Ahuja e Orlin.
 *
 * @author Stefano Scerra
 */
public class MaxFlowCalculator {
    /**
     * Classe che gestisce le distance label
     */
    private static class DistanceLabels {
        // mappa nodo -> distance label
        private TIntIntHashMap labels;

        // nodes[i] = numero di nodi del grafo che hanno distance label pari a i
        private int[] nodes;

        /**
         * Costruttore di DistanceLabels
         *
         * @param n numero di nodi del grafo
         */
        public DistanceLabels(int n) {
            labels = new TIntIntHashMap();
            nodes = new int[n + 1];
        }

        /**
         * Restituisce l'etichetta associata al nodo specificato
         *
         * @param n
         * @return
         */
        public int getLabel(int n) {
            return labels.get(n);
        }


        /**
         * Imposta l'etichetta di un nodo.
         * Il valore restituito dal metodo � utilizzato per implementare il criterio
         * di terminazione secondario dell'algoritmo.
         * Quando per qualche i = 1...n (n numero di nodi), il numero di nodi che hanno etichetta i scende a 0,
         * non ci potranno essere altri cammini aumentanti nella rete residuale del grafo e quindi
         * l'algoritmo potr� terminare.
         *
         * @param n
         * @param label
         * @return true se il numero di nodi che hanno il vecchio valore di etichetta di n scende a 0, false altrimenti.
         */
        public boolean setLabel(int n, int label) {
            boolean existsUnassignedLabel = false;
            Integer oldLabel = labels.get(n);
            if (oldLabel != null) {
                nodes[oldLabel]--;
                if (nodes[oldLabel] == 0) existsUnassignedLabel = true;
            }

            labels.put(n, label);
            nodes[label]++;
            return existsUnassignedLabel;
        }
    }

    /**
     * Calcola il massimo flusso inviabile dal nodo source al nodo sink sul grafo g
     *
     * @param g grafo su cui calcolare il massimo flusso
     */
    public static double getMaxFlow(FlowGraph g) {
        if (g.numNodes() == 0) {
            return 0;
        }

        DistanceLabels labels = calcDistanceLabels(g);
        double maximumFlow = 0; // max flow
        int n = g.numNodes();
        List<Edge> backEdges = addBackEdges(g); // aggiungi archi all'indietro per creare la rete residuale
        LinkedList<Edge> path = new LinkedList<Edge>(); // il cammino (aumentante) corrente
        int sourceDist; // distanza della sorgente dal pozzo
        int i = g.getSource(); // nodo corrente

        // Criterio di terminazione: la distance label del nodo sorgente
        // diventa maggiore o uguale al numero di nodi. A quel punto non potr�
        // pi� esistere un cammino aumentante (capacit� residua positiva per ogni arco del path)
        // da source a sink.

        while (i != FlowGraph.NULL && (sourceDist = labels.getLabel(g.getSource())) < n) {
            Edge e = getAdmissibleEdge(g, i, labels);
            if (e != null) {
                i = advance(e, path);
                if (i == g.getSink()) {
                    double delta = augment(g, path);
                    maximumFlow += delta;
                    i = g.getSource();
                    path.clear();
                }
            } else i = retreat(g, labels, i, path);
        }

        removeBackEdges(g, backEdges);

        return maximumFlow;
    }

    /**
     * Calcola le distance label dei nodi rispetto al nodo pozzo.
     * Ogni etichetta d[i] rappresenta la lunghezza del minimo cammino aumentante
     * tra il nodo i e il nodo pozzo, con d[sink] = 0.
     * L'etichettatura avviene effettuando una visita in ampiezza al contrario
     * del grafo a partire dal nodo pozzo.
     *
     * @param g grafo
     * @return oggetto che contiene distance label
     */
    private static DistanceLabels calcDistanceLabels(FlowGraph g) {
        int n = g.numNodes();
        DistanceLabels labels = new DistanceLabels(n);

        TIntHashSet visited = new TIntHashSet();

        TIntIterator it = g.getNodes().iterator();
        while (it.hasNext()) {
            labels.setLabel(it.next(), n);
        }

        labels.setLabel(g.getSink(), 0);


        TIntQueue queue = new TIntLinkedListQueue();
        queue.offer(g.getSink());

        while (!queue.isEmpty()) {
            int j = queue.poll();
            //System.out.println("Nj: "+j);

            for (Edge e : g.incident(j)) {
                int i = e.getSource();
                if (!visited.contains(i)) {
                    labels.setLabel(i, labels.getLabel(j) + 1);
                    //		System.out.println("d[" + i + "] = " + labels.getLabel(i));
                    visited.add(i);
                    queue.offer(i);
                }
            }
            visited.add(j);

        }

        return labels;
    }

    /**
     * Aggiunge al grafo gli archi all'indietro per ottenere una rete residuale.
     * Per ogni arco (i, j) viene aggiunto un arco (j, i) con capacit� e flusso nulli.
     *
     * @param g grafo
     */
    private static List<Edge> addBackEdges(FlowGraph g) {
        List<Edge> backEdges = new LinkedList<Edge>();
        for (Edge e : g.getEdges()) {
            Edge backEdge = new Edge(e.getDest(), e.getSource(), 0, 0);
            g.addEdge(backEdge);
            backEdges.add(backEdge);
        }
        return backEdges;
    }

    /**
     * Restituisce un arco uscente ammissibile dal nodo i.
     * Un arco (i, j) � definito ammissibile se la sua capacit�
     * residua � positiva e se d[i] = 1 + d[j], dove
     * d[i] = distance label associata al nodo i.
     *
     * @param g Grafo
     * @param i Nodo
     * @param d Distance labels dei nodi
     * @return
     */
    private static Edge getAdmissibleEdge(FlowGraph g, int i, DistanceLabels d) {
        for (Edge e : g.adjacent(i)) {
            if (e.getResidualCap() > 0 && d.getLabel(i) == 1 + d.getLabel(e.getDest())) {
                return e;
            }
        }
        return null;
    }

    /**
     * Aggiunge al cammino l'arco (i, j) e restituisce il nodo j
     *
     * @param e    Arco
     * @param path Cammino
     * @return il nodo successivo da elaborare
     */
    private static int advance(Edge e, LinkedList<Edge> path) {
        path.addLast(e);
        return e.getDest();
    }

    /**
     * Effettua l'incremento di flusso lungo il cammmino aumentante
     * e aggiorna il grafo.
     *
     * @param g    grafo
     * @param path cammino aumentante
     * @return l'incremento di flusso
     */
    private static double augment(FlowGraph g, LinkedList<Edge> path) {
        // delta = incremento di flusso sul cammino
        double delta = Double.MAX_VALUE; // l'ottimismo � fondamentale
        Edge mincutEdge = null;

        // l'incremento � pari alla minima capacit� residuale tra gli archi del cammino
        for (Edge e : path) {
            double residualCap = e.getResidualCap();
            if (residualCap < delta) {
                delta = residualCap;
                mincutEdge = e;
            }
        }

        // aggiorna il grafo
        for (Edge e : path) {
            // incrementa di delta il flusso su (i, j)
            double flow = e.getFlow();
            flow += delta;
            e.setFlow(flow);

            // prendi il back-edge (j, i)
            Edge revEdge = null;
            for (Edge incEdge : g.incident(e.getSource())) {
                if (incEdge.getSource() == e.getDest()) {
                    revEdge = incEdge;
                    break;
                }
            }

            // incrementa di delta la capacit� di (j, i)
            double cap = revEdge.getCap();
            cap += delta;
            revEdge.setCap(cap);
            flow = revEdge.getFlow();
            if (flow > 0) {
                flow -= delta;
                revEdge.setFlow(flow);
            }

        }
        return delta;
    }

    /**
     * Retrocede il costruendo cammino aumentante al nodo precedente,
     * aggiornando la distance label del nodo corrente.
     *
     * @param g      Grafo
     * @param labels Distance labels
     * @param i      Nodo corrente
     * @param path   Cammino
     * @return nodo predecessore di i o null se non ci sono pi� cammini aumentanti
     */
    private static int retreat(FlowGraph g, DistanceLabels labels, int i, LinkedList<Edge> path) {
        // Aggiorna l'etichetta del nodo i
        // dMin contiene la pi� piccola distance label associata ai nodi
        // adiacenti a i mediante archi con capacit� residua positiva
        int dMin = g.numNodes() - 1;

        for (Edge e : g.adjacent(i)) {
            if (e.getResidualCap() > 0) {
                int j = e.getDest();
                int dj = labels.getLabel(j);
                if (dj < dMin) dMin = dj;
            }
        }

        boolean flag = labels.setLabel(i, 1 + dMin); // d[i] = 1 + dMin

        int predecessor;
        if (!flag) {
            // Back-tracking sul cammino: elimina l'ultimo arco e restituisci il nodo predecessore
            if (i != g.getSource()) {
                Edge e = path.removeLast();     // indietro tutta!
                predecessor = e.getSource();
            } else predecessor = g.getSource();
        } else predecessor = FlowGraph.NULL;
        // se il numero di nodi ai quali � associata l'etichetta che aveva il nodo i scende a 0,
        // non ci possono pi� essere cammini aumentanti e l'algoritmo pu� terminare: restituisci null

        return predecessor;
    }

    private static void removeBackEdges(FlowGraph g, List<Edge> backEdges) {
        for (Edge e : backEdges) {
            g.removeEdge(e);
        }
    }

    /**
     * Esempio di utilizzo dell'algoritmo
     */
    public static void main(String[] args) {
        // costruisci il grafo di test
        FlowGraph g = buildTestGraph();

        // calcola il massimo flusso inviabile tra sorgente e pozzo su g
        double f = MaxFlowCalculator.getMaxFlow(g);
        System.out.println("max flow on g = " + f);
        // stampa la distribuzione di flusso di g (la lista degli archi con relativi flussi e capacit�)
        System.out.println("flow distribution on g = " + g.getEdges());
        // calcola il massimo flusso inviabile tra sorgente e pozzo utilizzando il sottografo
        // indotto dai nodi 1, 3 e 4
        Set<Integer> s134 = new HashSet<Integer>();
        s134.add(1);
        s134.add(3);
        s134.add(4);
        FlowGraph g134 = buildTestGraph().getSubGraph(s134);
        double f134 = MaxFlowCalculator.getMaxFlow(g134);
        System.out.println("max flow on g[1, 3, 4] = " + f134);
        System.out.println("flow distribution on g[1, 3, 4] = " + g134.getEdges());

        // calcola il massimo flusso inviabile tra sorgente e pozzo utilizzando il sottografo
        // indotto dai nodi 2 e 5
        Set<Integer> s25 = new HashSet<Integer>();
        s25.add(2);
        s25.add(5);
        FlowGraph g25 = buildTestGraph().getSubGraph(s25);
        double f25 = MaxFlowCalculator.getMaxFlow(g25);
        System.out.println("max flow on g[2, 5] = " + f25);
        System.out.println("flow distribution on g[2, 5] = " + g25.getEdges());

        // usa la tua fantasia
        Set<Integer> s2 = new HashSet<Integer>();
        s2.add(2);
        FlowGraph g2 = buildTestGraph().getSubGraph(s2);
        double f2 = MaxFlowCalculator.getMaxFlow(g2);
        System.out.println("max flow on g[2] = " + f2);
        System.out.println("flow distribution on g[2] = " + g2.getEdges());
    }

    private static FlowGraph buildTestGraph() {
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

        graph.addEdge(new Edge(na, n1, 3));
        graph.addEdge(new Edge(na, n3, 5));
        graph.addEdge(new Edge(na, n2, 3));
        graph.addEdge(new Edge(n1, n3, 4));
        graph.addEdge(new Edge(n1, n4, 3));
        graph.addEdge(new Edge(n3, n4, 2));
        graph.addEdge(new Edge(n3, nb, 1));
        graph.addEdge(new Edge(n3, n5, 4));
        graph.addEdge(new Edge(n2, n3, 2));
        graph.addEdge(new Edge(n2, n5, 2));
        graph.addEdge(new Edge(n4, nb, 4));
        graph.addEdge(new Edge(n5, nb, 4));

        return graph;
    }
}
