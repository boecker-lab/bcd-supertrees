package flipcut.mincut.ahuja_orlin.graph;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.queue.TIntQueue;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Semplice implementazione di un grafo orientato e pesato per problemi di flusso con liste di adiacenza e incidenza
 *
 * @author Stefano Scerra
 */
public class FlowGraph {
    public static final int NULL = Integer.MIN_VALUE;
    // id nodo -> lista di archi uscenti
    private TIntObjectHashMap<LinkedList<Edge>> adjacencies = new TIntObjectHashMap<LinkedList<Edge>>();
    // id nodo -> lista di archi entranti
    private TIntObjectHashMap<LinkedList<Edge>> incidences = new TIntObjectHashMap<LinkedList<Edge>>();
    // nodo sorgente
    private int source = NULL;
    // nodo pozzo
    private int sink = NULL;
    // value of the maximum flow
    private double maximumFlow = Double.NEGATIVE_INFINITY;

    //Algorithm data structures
    private DistanceLabels labels;
    private final List<Edge> backEdges = new LinkedList<>();


    public void addNode(int n) {
        if (containsNode(n)) throw new IllegalArgumentException("Nodo " + n + " gi� esistente");
        adjacencies.put(n, new LinkedList<Edge>());
        incidences.put(n, new LinkedList<Edge>());
    }


    public void addEdge(Edge e) {
        if (!containsNode(e.source) || !containsNode(e.dest))
            throw new IllegalArgumentException("Impossibile inserire l'arco " + e);
        List<Edge> adjacent = adjacencies.get(e.source);
        List<Edge> incident = incidences.get(e.dest);
        adjacent.add(e);
        incident.add(e);
    }


    public void setSource(int node) {
        source = node;
    }


    public int getSource() {
        return source;
    }


    public void setSink(int node) {
        sink = node;
    }


    public int getSink() {
        return sink;
    }

    public double getMaximumFlow() {
        if (maximumFlow < 0)
            calculateSTFlow();
        return maximumFlow;
    }

    public int numNodes() {
        return adjacencies.size();
    }


    public int numEdges() {
        int numEdges = 0;

        for (List<Edge> adjList : adjacencies.valueCollection()) {
            numEdges += adjList.size();
        }

        return numEdges;
    }

    /**
     * Restituisce gli archi uscenti dal nodo n
     *
     * @param n
     */

    public List<Edge> adjacent(int n) {
        return adjacencies.get(n);
    }

    public boolean containsNode(int n) {
        return adjacencies.containsKey(n);
    }


    public boolean containsEdge(Edge e) {
        List<Edge> adjList = adjacencies.get(e.source);
        return adjList.contains(e);
    }

    public TIntSet getNodes() {
        return adjacencies.keySet();
    }

    /**
     * metodo di convenienza per restituire tutti
     * ma proprio tutti gli archi del grafo
     *
     * @return lista degli archi del grafo
     */

    public List<Edge> getEdges() {
        List<Edge> edges = new LinkedList<Edge>();
        for (List<Edge> adjList : adjacencies.valueCollection()) {
            edges.addAll(adjList);
        }

        return edges;
    }


    public void removeNode(int n) {
        adjacencies.remove(n);
        incidences.remove(n);

        for (List<Edge> adjList : adjacencies.valueCollection()) {
            Iterator<Edge> it = adjList.iterator();
            while (it.hasNext()) {
                Edge e = it.next();
                if (e.dest == n) {
                    it.remove();
                    break; // non � mica un multigrafo...
                }
            }
        }

        for (List<Edge> incList : incidences.valueCollection()) {
            Iterator<Edge> it = incList.iterator();
            while (it.hasNext()) {
                Edge e = it.next();
                if (e.source == n) {
                    it.remove();
                    break; // non � mica un multigrafo...
                }
            }
        }
    }


    public void removeEdge(Edge e) {
        List<Edge> adjList = adjacencies.get(e.source);
        List<Edge> incList = incidences.get(e.dest);
        adjList.remove(e);
        incList.remove(e);
    }


    public void clear() {
        adjacencies.clear();
        incidences.clear();
        maximumFlow = Double.NEGATIVE_INFINITY;
        backEdges.clear();
        labels = null;
        source = NULL;
        sink = NULL;
    }

    /**
     * Restituisce gli archi entranti nel nodo n
     *
     * @param n
     */

    public List<Edge> incident(int n) {
        return incidences.get(n);
    }


    public FlowGraph clone() {
        FlowGraph clonedGraph = new FlowGraph();
        TIntIterator it = getNodes().iterator();
        while (it.hasNext()) { // inizializza le strutture dati relative ai nodi nel nuovo grafo
            int n = it.next();
            clonedGraph.adjacencies.put(n, new LinkedList<Edge>());
            clonedGraph.incidences.put(n, new LinkedList<Edge>());

            if (n == source) {
                clonedGraph.setSource(n);
            } else if (n == sink) {
                clonedGraph.setSink(n);
            }
        }


        it = getNodes().iterator();
        while (it.hasNext()) {
            int n = it.next();
            LinkedList<Edge> clonedAdjList = clonedGraph.adjacencies.get(n);
            // riempi le liste di adiacenza e di incidenza
            for (Edge e : adjacent(n)) {
                Edge clonedEdge = new Edge(e.source, e.dest, e.cap, e.flow);
                clonedAdjList.add(clonedEdge);
                clonedGraph.incidences.get(e.dest)
                        .add(clonedEdge);
            }
        }
        return clonedGraph;
    }

    /**
     * Restituisce il sottografo indotto dall'insieme di nodi s
     *
     * @param s insieme di id di nodi
     * @return un nuovo sottografo
     */

    public FlowGraph getSubGraph(Collection<Integer> s) {
        FlowGraph subGraph = new FlowGraph();

        for (int n : s) {
            subGraph.addNode(n);
        }
        if (!subGraph.containsNode(source))
            subGraph.addNode(source);
        subGraph.setSource(source);
        if (!subGraph.containsNode(sink))
            subGraph.addNode(sink);
        subGraph.setSink(sink);

        TIntIterator it = subGraph.getNodes().iterator();
        while (it.hasNext()) {
            int n = it.next();
            LinkedList<Edge> clonedAdjList = subGraph.adjacencies.get(n);
            // riempi le liste di adiacenza e di incidenza
            for (Edge e : adjacent(n)) {
                if (subGraph.containsNode(e.dest)) {
                    Edge clonedEdge = new Edge(e.source, e.dest, e.cap, e.flow);
                    clonedAdjList.add(clonedEdge);
                    subGraph.incidences.get(e.dest)
                            .add(clonedEdge);
                }
            }
        }

        return subGraph;
    }


    public String toString() {
        return "Adiacenze: " + adjacencies.toString() + "\nIncidenze: " + incidences.toString();
    }


    //Algorithmic Part

    /**
     * Calcola il massimo flusso inviabile dal nodo source al nodo sink sul grafo g
     */
    public void calculateSTFlow() {
        maximumFlow = 0; // max flow
        if (numNodes() == 0) {
            return;
        }

        labels = calcDistanceLabels();
        int n = numNodes();
        addBackEdges(); // aggiungi archi all'indietro per creare la rete residuale
        LinkedList<Edge> path = new LinkedList<Edge>(); // il cammino (aumentante) corrente
        int i = getSource(); // nodo corrente

        // Criterio di terminazione: la distance label del nodo sorgente
        // diventa maggiore o uguale al numero di nodi. A quel punto non potr�
        // pi� esistere un cammino aumentante (capacit� residua positiva per ogni arco del path)
        // da source a sink.

        while (i != FlowGraph.NULL && labels.getLabel(getSource()) < n) {
            Edge e = getAdmissibleEdge(i, labels);
            if (e != null) {
                i = advance(e, path);
                if (i == getSink()) {
                    double delta = augment(path);
                    maximumFlow += delta;
                    i = getSource();
                    path.clear();
                }
            } else i = retreat(labels, i, path);
        }
    }

    public TIntHashSet calculateSTCut() {
        calculateSTFlow();
        final TIntHashSet sourceSet = new TIntHashSet(numNodes());
        sourceSet.add(source);
        dfs(sourceSet, adjacencies.get(source));
        return sourceSet;
    }

    private void dfs(final TIntHashSet sourceSet, LinkedList<Edge> edges) {
        for (Edge edge : edges) {
            int target = edge.dest;
            if (edge.getResidualCap() != 0 && !sourceSet.contains(target)) {
                sourceSet.add(target);
                dfs(sourceSet, adjacencies.get(target));
            }
        }
    }


    /**
     * Calcola le distance label dei nodi rispetto al nodo pozzo.
     * Ogni etichetta d[i] rappresenta la lunghezza del minimo cammino aumentante
     * tra il nodo i e il nodo pozzo, con d[sink] = 0.
     * L'etichettatura avviene effettuando una visita in ampiezza al contrario
     * del grafo a partire dal nodo pozzo.
     *
     * @return oggetto che contiene distance label
     */
    private DistanceLabels calcDistanceLabels() {
        int n = numNodes();
        DistanceLabels labels = new DistanceLabels(n);

        TIntHashSet visited = new TIntHashSet();

        TIntIterator it = getNodes().iterator();
        while (it.hasNext()) {
            labels.setLabel(it.next(), n);
        }

        labels.setLabel(getSink(), 0);


        TIntQueue queue = new TIntLinkedListQueue();
        queue.offer(getSink());

        while (!queue.isEmpty()) {
            int j = queue.poll();

            for (Edge e : incident(j)) {
                int i = e.getSource();
                if (!visited.contains(i)) {
                    labels.setLabel(i, labels.getLabel(j) + 1);
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
     */
    private List<Edge> addBackEdges() {
        removeBackEdges();
        for (Edge e : getEdges()) {
            Edge backEdge = new Edge(e.getDest(), e.getSource(), 0, 0);
            addEdge(backEdge);
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
     * @param i Nodo
     * @param d Distance labels dei nodi
     * @return
     */
    private Edge getAdmissibleEdge(int i, DistanceLabels d) {
        for (Edge e : adjacent(i)) {
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
    private int advance(Edge e, LinkedList<Edge> path) {
        path.addLast(e);
        return e.getDest();
    }

    /**
     * Effettua l'incremento di flusso lungo il cammmino aumentante
     * e aggiorna il grafo.
     *
     * @param path cammino aumentante
     * @return l'incremento di flusso
     */
    private double augment(LinkedList<Edge> path) {
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
            for (Edge incEdge : incident(e.getSource())) {
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
     * @param labels Distance labels
     * @param i      Nodo corrente
     * @param path   Cammino
     * @return nodo predecessore di i o null se non ci sono pi� cammini aumentanti
     */
    private int retreat(DistanceLabels labels, int i, LinkedList<Edge> path) {
        // Aggiorna l'etichetta del nodo i
        // dMin contiene la pi� piccola distance label associata ai nodi
        // adiacenti a i mediante archi con capacit� residua positiva
        int dMin = numNodes() - 1;

        for (Edge e : adjacent(i)) {
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
            if (i != getSource()) {
                Edge e = path.removeLast();     // indietro tutta!
                predecessor = e.getSource();
            } else predecessor = getSource();
        } else predecessor = FlowGraph.NULL;
        // se il numero di nodi ai quali � associata l'etichetta che aveva il nodo i scende a 0,
        // non ci possono pi� essere cammini aumentanti e l'algoritmo pu� terminare: restituisci null

        return predecessor;
    }

    public void removeBackEdges() {
        for (Edge e : backEdges) {
            removeEdge(e);
        }
        backEdges.clear();
    }


    /**
     * Classe che gestisce le distance label
     */
    private class DistanceLabels {
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

}
