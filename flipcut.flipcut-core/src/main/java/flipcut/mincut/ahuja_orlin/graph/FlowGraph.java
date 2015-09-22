package flipcut.mincut.ahuja_orlin.graph;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Semplice implementazione di un grafo orientato e pesato per problemi di flusso con liste di adiacenza e incidenza 
 * @author Stefano Scerra
 *
 */
public class FlowGraph
{
	// id nodo -> lista di archi uscenti
	private TIntObjectHashMap<LinkedList<Edge>> adjacencies = new TIntObjectHashMap<LinkedList<Edge>>();
	// id nodo -> lista di archi entranti
	private TIntObjectHashMap<LinkedList<Edge>> incidences = new TIntObjectHashMap<LinkedList<Edge>>();
	// id nodo -> nodo
//	private Map<Integer, Node> nodes = new HashMap<Integer, Node>();
	// nodo sorgente
	private int source = NULL;
	// nodo pozzo
	private int sink = NULL;

    public static final int NULL = Integer.MIN_VALUE;
	
	
	public void addNode(int n)
	{
		if(containsNode(n)) throw new IllegalArgumentException("Nodo " + n + " gi� esistente");
		adjacencies.put(n, new LinkedList<Edge>());
		incidences.put(n, new LinkedList<Edge>());
	}
	
	
	public void addEdge(Edge e)
	{
		if(!containsNode(e.source) || !containsNode(e.dest))
			throw new IllegalArgumentException("Impossibile inserire l'arco " + e);
		List<Edge> adjacent = adjacencies.get(e.source);
		List<Edge> incident = incidences.get(e.dest);
		adjacent.add(e);
		incident.add(e);
	}	
	
	
	public void setSource(int node)
	{
		source = node;
	}

	
	public int getSource()
	{
		return source;
	}

	
	public void setSink(int node)
	{
		sink = node;		
	}

	
	public int getSink()
	{
		return sink;
	}

	
	public int numNodes()
	{
		return adjacencies.size();
	}

	
	public int numEdges()
	{
		int numEdges = 0;
		
		for(List<Edge> adjList : adjacencies.valueCollection())
		{
			numEdges += adjList.size();
		}
		
		return numEdges;
	}	
	
	/**
	 *  Restituisce gli archi uscenti dal nodo n
	 * @param n
	 */
	
	public List<Edge> adjacent(int n)
	{
		return adjacencies.get(n);
	}
	
	public boolean containsNode(int n)
	{
		return adjacencies.containsKey(n);
	}
	
	
	public boolean containsEdge(Edge e)
	{
		List<Edge> adjList = adjacencies.get(e.source);
		return adjList.contains(e);
	}

	public TIntSet getNodes() //todo trove
	{
		return adjacencies.keySet();
	}

	/**
	 * metodo di convenienza per restituire tutti
	 * ma proprio tutti gli archi del grafo
	 * @return lista degli archi del grafo
	 */
	
	public List<Edge> getEdges()
	{
		List<Edge> edges = new LinkedList<Edge>();
		for(List<Edge> adjList: adjacencies.valueCollection())
		{
			edges.addAll(adjList);
		}
		
		return edges;
	}
	
	
	public void removeNode(int n)
	{
		adjacencies.remove(n);
		incidences.remove(n);
		
		for(List<Edge> adjList : adjacencies.valueCollection())
		{
			Iterator<Edge> it = adjList.iterator();
			while(it.hasNext())
			{
				Edge e = it.next();
				if(e.dest == n)
				{
					it.remove();
					break; // non � mica un multigrafo...
				}
			}
		}
		
		for(List<Edge> incList : incidences.valueCollection())
		{
			Iterator<Edge> it = incList.iterator();
			while(it.hasNext())
			{
				Edge e = it.next();
				if(e.source == n)
				{
					it.remove();
					break; // non � mica un multigrafo...
				}
			}
		}
	}
	
	
	public void removeEdge(Edge e)
	{
		List<Edge> adjList = adjacencies.get(e.source);
		List<Edge> incList = incidences.get(e.dest);
		adjList.remove(e);
		incList.remove(e);
	}
	
	
	public void clear()
	{
		adjacencies.clear();
		incidences.clear();
	}
	
	/**
	 *  Restituisce gli archi entranti nel nodo n
	 * @param n
	 */
	
	public List<Edge> incident(int n)
	{
		return incidences.get(n);
	}
	
	
	public Object clone()
	{
		FlowGraph clonedGraph = new FlowGraph();
        TIntIterator it = getNodes().iterator();
        while(it.hasNext())
		{ // inizializza le strutture dati relative ai nodi nel nuovo grafo
			int n = it.next();
            clonedGraph.adjacencies.put(n, new LinkedList<Edge>());
			clonedGraph.incidences.put(n, new LinkedList<Edge>());

			if(n == source)
			{
				clonedGraph.setSource(n);
			}
			else if(n == sink)
			{
				clonedGraph.setSink(n);
			}
		}


        it = getNodes().iterator();
        while(it.hasNext())
        {
            int n = it.next();
			LinkedList<Edge> clonedAdjList = clonedGraph.adjacencies.get(n);
			// riempi le liste di adiacenza e di incidenza
			for(Edge e : adjacent(n))
			{
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
	 * @param s insieme di id di nodi
	 * @return un nuovo sottografo
	 */
	
	public FlowGraph getSubGraph(Collection<Integer> s)
	{
		FlowGraph subGraph = new FlowGraph();
		
		for(int n : s)
		{
			subGraph.addNode(n);
		}
        if (!subGraph.containsNode(source))
            subGraph.addNode(source);
        subGraph.setSource(source);
        if (!subGraph.containsNode(sink))
            subGraph.addNode(sink);
        subGraph.setSink(sink);

        TIntIterator it = subGraph.getNodes().iterator();
        while(it.hasNext())
        {
            int n = it.next();
			LinkedList<Edge> clonedAdjList = subGraph.adjacencies.get(n);
			// riempi le liste di adiacenza e di incidenza
			for(Edge e : adjacent(n))
			{
				if(subGraph.containsNode(e.dest))
				{
					Edge clonedEdge = new Edge(e.source, e.dest, e.cap, e.flow);
					clonedAdjList.add(clonedEdge);
					subGraph.incidences.get(e.dest)
                            .add(clonedEdge);
				}
			}
		}

		return subGraph;
	}

	
	public String toString()
	{
		return "Adiacenze: " + adjacencies.toString() + "\nIncidenze: " + incidences.toString();
	}
	
}
