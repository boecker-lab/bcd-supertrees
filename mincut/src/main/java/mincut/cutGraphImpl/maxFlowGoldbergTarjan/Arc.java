package mincut.cutGraphImpl.maxFlowGoldbergTarjan;

/**
 * Created by fleisch on 09.10.15.
 */

/**
 * Internal edge representation
 */
public class Arc {
    long cap;    /* capacity */
    long resCap;          /* residual capasity */
    Node head;           /* arc head */
    Arc rev;            /* reverse arc */

    public Node getSource() {
        return rev.head;
    }

    public Node getTarget() {
        return head;
    }

    public long getCap() {
        return cap;
    }
}
