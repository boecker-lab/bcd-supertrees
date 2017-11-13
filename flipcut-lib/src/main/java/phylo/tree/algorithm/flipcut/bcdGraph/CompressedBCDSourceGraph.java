package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.map.TIntObjectMap;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;
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
