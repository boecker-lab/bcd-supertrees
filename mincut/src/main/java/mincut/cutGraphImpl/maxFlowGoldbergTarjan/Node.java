package mincut.cutGraphImpl.maxFlowGoldbergTarjan;

/**
 * Created by fleisch on 09.10.15.
 */

/**
 * Internal node representation
 */
public class Node {
    Arc[] arcs;           /* first outgoing arc */
    long excess;           /* excess at the node
                         change to double if needed */
    long d;                /* distance label */
    Node bNext;           /* next node in bucket */
    Node bPrev;           /* previous node in bucket */
    int current;          /* arc pointer */
    int bucketIndex;
    public final Object name;

    private int createdArcs = 0;

    Node(int edges, Object name) {
        this.name = name;
        this.arcs = new Arc[edges];
    }

    public Arc[] getArcs() {
        return arcs;
    }

    Arc current() {
        return arcs[current];
    }

    @Override
    public String toString() {
        return name.toString();
    }

    void addArc(Node head, long cap) {
        Arc a = new Arc();
        a.head = head;
        a.cap = cap;
        a.resCap = cap;
        addArc(a);


        Arc rev = new Arc();
        rev.head = this;
        rev.rev = a;
        a.rev = rev;
        head.addArc(rev);
    }

    private void addArc(Arc arc) {
        arcs[createdArcs++] = arc;
    }
}