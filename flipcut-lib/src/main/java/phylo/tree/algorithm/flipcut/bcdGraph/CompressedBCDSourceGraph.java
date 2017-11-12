package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.map.TIntObjectMap;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CompressedBCDSourceGraph extends CompressedBCDGraph {
    final String[] sourceTaxa;

    final Hyperedge[] sourceMergedHyperEdges;




    final TIntObjectMap<RoaringBitmap> scaffoldCharacterHirarchie;


    public CompressedBCDSourceGraph(String[] sourceTaxa, Hyperedge[] sourceCharacters, RoaringBitmap activeScaffoldChars, TIntObjectMap<RoaringBitmap> scaffoldCharacterMaping) {
        super(activeScaffoldChars);
        this.sourceTaxa = sourceTaxa;
        this.sourceMergedHyperEdges = sourceCharacters;
        this.scaffoldCharacterHirarchie = scaffoldCharacterMaping;
        taxa.add(0L, sourceTaxa.length);
        characters.add(0L, sourceCharacters.length);
    }


    public String getTaxon(int pos) {
        return sourceTaxa[pos];
    }

    public List<String> getTaxa(RoaringBitmap bits) {
        final List<String> taxa = new ArrayList<>();
        bits.forEach((IntConsumer) i -> taxa.add(sourceTaxa[i]));
        return taxa;
    }


    public Hyperedge getHyperEdge(int character) {
        return sourceMergedHyperEdges[character];
    }

//    public long getCharacterWeight(int character) {
//        return getHyperEdge(character).getWeight();
//    }

    @Override
    public int numCharacter() {
        return sourceMergedHyperEdges.length;
    }

    @Override
    public CompressedBCDSourceGraph getSource() {
        return this;
    }

    @Override
    public Iterable<Hyperedge> hyperEdges() {
        return Arrays.asList(sourceMergedHyperEdges);
    }

    @Override
    public LinkedList<Hyperedge> getHyperEdgesAsList() {
        return new LinkedList<>(Arrays.asList(sourceMergedHyperEdges));
    }

    @Override
    public Iterable<String> taxaLabels() {
        return Arrays.asList(sourceTaxa);
    }
}
