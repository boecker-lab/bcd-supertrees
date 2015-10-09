package flipcut.mincut.cutGraphImpl.kargerStein;

/**
 * Created by fleisch on 09.10.15.
 */
class Edge {
    int ingoing;
    int outgoing;
    int weight;
    int color;
    double prob;


    Edge(int out, int in, int weight, int color) {
        ingoing = in;
        outgoing = out;
        this.weight = weight;
        this.color = color;
    }
}