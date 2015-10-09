package flipcut.mincut.cutGraphImpl.goldberg_tarjan;

/**
 * Created by fleisch on 09.10.15.
 */

/**
 * Internal edge representation
 */
class Arc {
    long cap;    /* capacity */
    long resCap;          /* residual capasity */
    Node head;           /* arc head */
    Arc rev;            /* reverse arc */
}
