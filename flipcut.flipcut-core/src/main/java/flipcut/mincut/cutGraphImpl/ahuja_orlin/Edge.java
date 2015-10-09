package flipcut.mincut.cutGraphImpl.ahuja_orlin;

/**
 * Classe che rappresenta un arco del grafo
 * @author Stefano Scerra
 *
 */
class Edge
{
	final int source; // nodo sorgente
	final int dest; // nodo destinazione
	double cap = 0.0d; // capacit� dell'arco
	double flow = 0.0d; // flusso su quest'arco
	
	/**
	 * Inizializza un nuovo arco tra i due nodi indicati e con una data capacit�
	 * @param source
	 * @param dest
	 * @param cap
	 */
	public Edge(int source, int dest, double cap)
	{
		this.source = source;
		this.dest = dest;
		this.cap = cap;
	}
	
	/**
	 * Inizializza un nuovo arco tra i nodi indicati con una data capacit� ed un dato valore di flusso
	 * @param source
	 * @param dest
	 * @param cap
	 * @param flow
	 */
	public Edge(int source, int dest, double cap, double flow)
	{
		this.source = source;
		this.dest = dest;
		this.cap = cap;
		this.flow = flow;
	}
	
	/**
	 *  Costruttore per copia
	 * @param e
	 */
	public Edge(Edge e)
	{
		source = e.source;
		dest = e.dest;
		cap = e.cap;
		flow = e.flow;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o == null) return false;
		if(!(o instanceof Edge)) return false;
		Edge e = (Edge)o;
		return e.source == source && e.dest == dest && e.flow == flow && e.cap == cap;
	}

	@Override
	public String toString()
	{
		return "(" + source + ", " + dest + ") [" + flow + " / " + cap + "]";
	}	
	
	/**
	 * Restituisce il flusso dell'arco
	 * @return il flusso
	 */
	public double getFlow()
	{
		return flow;
	}

	/**
	 * Imposta il flusso dell'arco
	 * @param flow
	 */
	public void setFlow(double flow)
	{
		if(flow > cap) throw new IllegalArgumentException("Impossibile assegnare un flusso maggiore "
				+ "della capacit� dell'arco");
		this.flow = flow;
	}

	/**
	 * Restituisce il nodo da cui l'arco esce
	 * @return il nodo sorgente
	 */
	public int getSource()
	{
		return source;
	}

	/**
	 * Restituisce il nodo in cui l'arco entra
	 * @return nodo destinazione
	 */
	public int getDest()
	{
		return dest;
	}

	/**
	 * Restituisce la capacita' dell'arco
	 * @return la capacita'
	 */
	public double getCap()
	{
		return cap;
	}
	
	/**
	 * Imposta una capacita' per l'arco
	 * @param cap
	 */
	public void setCap(double cap)
	{
		this.cap = cap;
	}
	
	/**
	 * Restituisce la capacita' residua dell'arco (capacita' - flusso)
	 * @return la capacita' residua
	 */
	public double getResidualCap()
	{
		return cap - flow;
	}

}