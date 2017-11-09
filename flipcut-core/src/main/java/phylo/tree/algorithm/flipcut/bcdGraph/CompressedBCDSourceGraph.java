package phylo.tree.algorithm.flipcut.bcdGraph;

import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CompressedBCDSourceGraph extends CompressedBCDGraph {
    final String[] sourceTaxa;
    final RoaringBitmap[] sourceHyperEdges;
    final RoaringBitmap[] sourceImaginaryHyperEdges;
    RoaringBitmap scaffoldCharacters;

    final long[] chracterWeights;

    public CompressedBCDSourceGraph(String[] sourceTaxa, RoaringBitmap[] sourceCharacters, RoaringBitmap[] sourceCharacterImaginaryEdgeSets, long[] chracterWeights) {
        super(new RoaringBitmap(), new RoaringBitmap());
        this.sourceTaxa = sourceTaxa;
        this.sourceHyperEdges = sourceCharacters;
        this.sourceImaginaryHyperEdges = sourceCharacterImaginaryEdgeSets;
        this.chracterWeights = chracterWeights;
    }


    public String getTaxon(int pos) {
        return sourceTaxa[pos];
    }

    public List<String> getTaxa(RoaringBitmap bits) {
        final List<String> taxa = new ArrayList<>();
        bits.forEach((IntConsumer) i -> taxa.add(sourceTaxa[i]));
        return taxa;
    }

    public RoaringBitmap getImaginaryHyperEdge(int character) {
        return sourceImaginaryHyperEdges[character];
    }

    public RoaringBitmap getHyperEdge(int character) {
        return sourceHyperEdges[character];
    }

    public long getCharacterWeight(int character) {
        return chracterWeights[character];
    }

    @Override
    public int numCharacter() {
        return sourceHyperEdges.length;
    }

    @Override
    public CompressedBCDSourceGraph getSource() {
        return this;
    }

    @Override
    public Iterable<RoaringBitmap> hyperEdges() {
        return Arrays.asList(sourceHyperEdges);
    }

    @Override
    public Iterable<RoaringBitmap> imaginaryHyperEdges() {
        return Arrays.asList(sourceImaginaryHyperEdges);
    }

    @Override
    public LinkedList<RoaringBitmap> getHyperEdgesAsList() {
        return new LinkedList<>(Arrays.asList(sourceHyperEdges));
    }

    @Override
    public Iterable<String> taxaLabels() {
        return Arrays.asList(sourceTaxa);
    }
}
