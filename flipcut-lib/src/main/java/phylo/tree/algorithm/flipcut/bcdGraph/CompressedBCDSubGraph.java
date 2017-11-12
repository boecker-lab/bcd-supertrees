package phylo.tree.algorithm.flipcut.bcdGraph;

import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.util.LinkedList;

public class CompressedBCDSubGraph extends CompressedBCDGraph {
    private final CompressedBCDSourceGraph source;

    public CompressedBCDSubGraph(CompressedBCDSourceGraph source, RoaringBitmap taxa, RoaringBitmap characters, RoaringBitmap activeScaffolds) {
        super(taxa, characters, activeScaffolds);
        this.source = source;
    }

    @Override
    public LinkedList<Hyperedge> getHyperEdgesAsList() {
        LinkedList<Hyperedge> l = new LinkedList<>();
        characters.forEach((IntConsumer) i -> {
            l.add(getSource().sourceMergedHyperEdges[i]);
        });
        return l;
    }

    @Override
    public Iterable<Hyperedge> hyperEdges() {
        return new BitMapIteratable<>(getSource().sourceMergedHyperEdges, characters);
    }

    @Override
    public CompressedBCDSourceGraph getSource() {
        return source;
    }

    @Override
    public Iterable<String> taxaLabels() {
        return new BitMapIteratable<>(getSource().sourceTaxa, taxa);
    }
}
