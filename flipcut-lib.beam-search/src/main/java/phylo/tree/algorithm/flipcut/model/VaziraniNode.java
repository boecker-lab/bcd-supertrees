package phylo.tree.algorithm.flipcut.model;

import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;

import java.util.Collection;

/**
 * User: Markus Fleischauer (markus.fleischauerquni-jena.de)
 * 09.07.12 17:14
 */
public class VaziraniNode<T extends AbstractFlipCutNode<T>> implements Comparable<VaziraniNode>{
    public final Collection<T> cut;
    public final long cutWeight;
    public final int k;

    public VaziraniNode(Collection<T> cut, long cutWeight, int k) {
        this.cut = cut;
        this.cutWeight = cutWeight;
        this.k = k;

    }
    public VaziraniNode(VaziraniNode n){
        this(n.cut,n.cutWeight,n.k);
    }

    public int compareTo(VaziraniNode o) {
        return (cutWeight < o.cutWeight) ? -1 : ((cutWeight == o.cutWeight) ? 0 : 1);
    }
}