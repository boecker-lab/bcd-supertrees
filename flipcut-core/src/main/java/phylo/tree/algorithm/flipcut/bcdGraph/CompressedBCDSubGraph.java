package phylo.tree.algorithm.flipcut.bcdGraph;

import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.util.LinkedList;

public class CompressedBCDSubGraph extends CompressedBCDGraph {
    private final CompressedBCDSourceGraph source;

    public CompressedBCDSubGraph(CompressedBCDSourceGraph source, RoaringBitmap taxa, RoaringBitmap characters) {
        super(taxa, characters);
        this.source = source;
    }

    @Override
    public LinkedList<RoaringBitmap> getHyperEdgesAsList() {
        LinkedList<RoaringBitmap> l = new LinkedList<>();
        characters.forEach((IntConsumer) i -> {
            l.add(getSource().sourceHyperEdges[i]);
        });
        return l;
    }

    @Override
    public Iterable<RoaringBitmap> hyperEdges() {
        return new BitMapIteratable<>(getSource().sourceHyperEdges, characters);
    }

    @Override
    public Iterable<RoaringBitmap> imaginaryHyperEdges() {
        return new BitMapIteratable<>(getSource().sourceImaginaryHyperEdges, characters);
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
