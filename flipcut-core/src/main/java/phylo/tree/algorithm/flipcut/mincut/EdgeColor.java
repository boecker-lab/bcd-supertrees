package phylo.tree.algorithm.flipcut.mincut;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 10.02.17.
 */

import phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.RandomSet;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class EdgeColor implements Cloneable {
    public final boolean preMerged; //todo this is ugly. make nice
    private final Object idetifier;
    private final double weight;
    private final RandomSet<Colorable> edges = new RandomSet<>();


    private EdgeColor(Object idetifier, double weight) {
        this(idetifier,weight,false);

    }
    private EdgeColor(Object idetifier, double weight, boolean preMerged) {
        this.idetifier = idetifier;
        this.weight = weight;
        this.preMerged = preMerged;
    }

    public EdgeColor(double weight) {
        this(new Object(), weight);
    }

    public EdgeColor(double weight, boolean preMerged) {
        this(new Object(), weight, preMerged);
    }

    public double getWeight() {
        return weight;
    }

    public Colorable getRandomElement(){
        return edges.peekRandom(ThreadLocalRandom.current());
    }

    public Set<Colorable> getEdges() {
        return edges;
    }

    public int numOfEdges() {
        return edges.size();
    }

    /*public boolean add(Colorable edge) {
        edge.add(this);
        return edges.add(edge);
    }*/

    /*public boolean remove(Colorable edge) {
        if (edges.remove(edge)) {
            edge.removeColor(this);
            return true;
        }
        return false;
    }*/

    public EdgeColor clone() {
        return new EdgeColor(idetifier, weight,preMerged);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EdgeColor)) return false;

        EdgeColor edgeColor = (EdgeColor) o;

        if (Double.compare(edgeColor.weight, weight) != 0) return false;
        return idetifier.equals(edgeColor.idetifier);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = idetifier.hashCode();
        temp = Double.doubleToLongBits(weight);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
