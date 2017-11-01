package phylo.tree.algorithm.flipcut.flipCutGraph;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 09.01.13
 * Time: 14:15
 */
public class FlipCutNodeSimpleWeight extends AbstractFlipCutNode<FlipCutNodeSimpleWeight> {
    /**
     * The list of outgoing edges -1(0) entries
     */
    public final Set<FlipCutNodeSimpleWeight> imaginaryEdges;
    /**
     * The edges weight
     */
    protected long edgeWeight;

    //main constructor
    protected FlipCutNodeSimpleWeight(String name, Set<FlipCutNodeSimpleWeight> edges, Set<FlipCutNodeSimpleWeight> imaginaryEdges) {
        super(name, edges);
        this.imaginaryEdges = imaginaryEdges;
        if (!isTaxon() && !isClone()) {
            createClone();
        }
    }

    //Taxon constructor
    public FlipCutNodeSimpleWeight(String name) {
        this(name, new HashSet<FlipCutNodeSimpleWeight>(), null);
    }

    //CharacterConstructor
    public FlipCutNodeSimpleWeight() {
        this(null, new HashSet<FlipCutNodeSimpleWeight>(), new HashSet<FlipCutNodeSimpleWeight>());
    }

    //DummyConstructor
    FlipCutNodeSimpleWeight(Set<FlipCutNodeSimpleWeight> edges) {
        this(null, edges, null);
    }

    @Override
    public long getEdgeWeight(FlipCutNodeSimpleWeight node) {
        return getEdgeWeight();
    }

    public long getEdgeWeight() {
        return edgeWeight;
    }

    @Override
    protected FlipCutNodeSimpleWeight createClone() {
        FlipCutNodeSimpleWeight clone = new FlipCutNodeSimpleWeight(name, null, null);
        clone.clone = this;
        this.clone = clone;

        return clone;
    }

    @Override
    protected FlipCutNodeSimpleWeight createDummy() {
        return new FlipCutNodeSimpleWeight(edges);
    }

    @Override
    public boolean isClone() {
        return (edges == null && imaginaryEdges == null);
    }

    @Override
    public boolean isTaxon() {
        return (imaginaryEdges == null && name != null);
    }

    @Override
    public boolean isCharacter() {
        return (imaginaryEdges != null);
    }

    @Override
    public boolean isDummyCharacter() {
        return (edges != null && imaginaryEdges == null && name == null);
    }


    public void addImaginaryEdgeTo(FlipCutNodeSimpleWeight node) {
        imaginaryEdges.add(node);
    }

    //this is to find redundant characters in a graph
    //unchecked, save use only on an initial graph
    @Override
    public boolean characterEquals(FlipCutNodeSimpleWeight c2) {
        if (isTaxon() || c2.isTaxon()) return false;
        return edges.equals(c2.edges) && imaginaryEdges.equals(c2.imaginaryEdges);
    }

    @Override
    protected FlipCutNodeSimpleWeight copy() {
        FlipCutNodeSimpleWeight copy;
        if (isTaxon()) {
            copy = new FlipCutNodeSimpleWeight(name);
        } else {
            copy = new FlipCutNodeSimpleWeight();
        }
        copy.edgeWeight = edgeWeight;
        copy.color = color;

        return copy;
    }

    @Override //unchecked
    public boolean isSemiUniversal() {
        return imaginaryEdges.isEmpty();
    }

    public boolean compareCharLazy(FlipCutNodeSimpleWeight c2) {
        if (isTaxon() || c2.isTaxon()) return false;
        return edges.equals(c2.edges);
    }
}
