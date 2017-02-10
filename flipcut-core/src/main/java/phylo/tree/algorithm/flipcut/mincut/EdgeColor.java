package phylo.tree.algorithm.flipcut.mincut;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 10.02.17.
 */

import java.util.HashSet;
import java.util.Set;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class EdgeColor {
    private double weight;
    private final Set<Colorable> edges = new HashSet<>();


    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Set<Colorable> getEdges() {
        return edges;
    }

    public int numOfEdges(){
        return edges.size();
    }

    public boolean add(Colorable edge) {
        edge.setColor(this);
        return edges.add(edge);
    }

    public boolean remove(Colorable edge) {
        if (edges.remove(edge)) {
            edge.setColor(null);
            return true;
        }
        return false;
    }
}
