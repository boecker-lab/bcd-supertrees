package phylo.tree.algorithm.flipcut.flipCutGraph;

import java.util.Set;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 30.11.12
 * Time: 11:29
 */
public abstract class AbstractFlipCutNode<N extends AbstractFlipCutNode<N>> {
    protected final static String DUMMY_INDETIFIER = "Dummy";
    /**
     * The nodes name
     */
    public final String name;

    /**
     * The list of outgoing edges 1(1) entries
     */
    public final Set<N> edges;

    /**
     * DFS marker
     */
    protected byte color = 0;
    /**
     * Clone node used for min cut graph
     */
    public N clone; //todo make protected with getter
    protected AbstractFlipCutNode(String name, Set<N> edges) {
        this.name = name;
        this.edges = edges;
    }

    @Override
    public String toString() {
        if (isClone()) {
            if (clone.isDummyCharacter()) {
                return "Clone-" + DUMMY_INDETIFIER + "-Character " + Integer.toHexString(clone.hashCode());
            } else {
                return "Clone-Character " + Integer.toHexString(clone.hashCode());
            }
        } else {
            if (isDummyCharacter()) {
                return DUMMY_INDETIFIER + "-Character " + Integer.toHexString(hashCode());
            } else {
                return name == null ? "Character " + Integer.toHexString(hashCode()) : name;
            }
        }
    }

    public N getClone() {
        return clone;
    }

    public void addEdgeTo(N node) {
        edges.add(node);
    }

    //this is to find redundant characters in a graph and should be high performing
    public abstract boolean characterEquals(N c2);


    public abstract boolean isSemiUniversal();

    public abstract long getEdgeWeight(N node);

    protected abstract N createClone();

    protected abstract N createDummy();

    protected abstract N copy();

    public abstract boolean isTaxon();

    public abstract boolean isClone();

    public abstract boolean isDummyCharacter();

    public abstract boolean isCharacter();

}
