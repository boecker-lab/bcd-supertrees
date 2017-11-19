package mincut.cutGraphImpl.maxFlowGoldbergTarjan;

/**
 * Created by fleisch on 09.10.15.
 */

/**
 * Internal node representation
 */
public abstract class Node {
    Arc[] arcs;           /* first outgoing arc */
    long excess;           /* excess at the node
                         change to double if needed */
    long d;                /* distance label */
    Node bNext;           /* next node in bucket */
    Node bPrev;           /* previous node in bucket */
    int current;          /* arc pointer */
    int bucketIndex;


    public abstract Object getName();

    private int createdArcs = 0;

    protected Node(int edges) {
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
        return getName().toString();
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

    public static class IntNode extends Node {
        private final int name;

        public IntNode(int edges, int name) {
            super(edges);
            this.name = name;
        }

        public int getIntName() {
            return name;
        }

        @Override
        public Object getName() {
            return getIntName();
        }
    }

    public static class ObjectNode extends Node {
        private final Object name;

        public ObjectNode(int edges, Object name) {
            super(edges);
            this.name = name;
        }

        @Override
        public Object getName() {
            return name;
        }
    }
}