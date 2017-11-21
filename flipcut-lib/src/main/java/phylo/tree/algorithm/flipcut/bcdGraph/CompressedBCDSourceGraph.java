package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.map.TIntObjectMap;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.Hyperedge;

import java.util.ArrayList;
import java.util.List;

public class CompressedBCDSourceGraph extends CompressedBCDGraph {
    final String[] sourceTaxa;
    final TIntObjectMap<Hyperedge> sourceMergedHyperEdges;

    final TIntObjectMap<RoaringBitmap> scaffoldCharacterHirarchie;

    private final int firstCloneIndex;


    public CompressedBCDSourceGraph(String[] sourceTaxa, TIntObjectMap<Hyperedge> sourceCharacters, RoaringBitmap activeScaffoldChars, TIntObjectMap<RoaringBitmap> scaffoldCharacterMaping) {
        super(RoaringBitmap.bitmapOf(sourceCharacters.keys()), activeScaffoldChars);
        this.sourceTaxa = sourceTaxa;
        this.sourceMergedHyperEdges = sourceCharacters;
        this.scaffoldCharacterHirarchie = scaffoldCharacterMaping;
        taxa.add(0L, sourceTaxa.length);
//        characters.add(0L, sourceCharacters.length);
        firstCloneIndex = sourceTaxa.length + sourceCharacters.size();
    }

    public int getFirstEdgeCloneIndex() {
        return firstCloneIndex;
    }

    public List<String> getTaxa(RoaringBitmap bits) {
        final List<String> taxa = new ArrayList<>();
        bits.forEach((IntConsumer) i -> taxa.add(sourceTaxa[i]));
        return taxa;
    }


    @Override
    public CompressedBCDSourceGraph getSource() {
        return this;
    }
}
