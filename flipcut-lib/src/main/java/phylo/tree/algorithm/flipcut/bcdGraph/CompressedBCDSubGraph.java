package phylo.tree.algorithm.flipcut.bcdGraph;

import org.roaringbitmap.RoaringBitmap;

public class CompressedBCDSubGraph extends CompressedBCDGraph {
    private final CompressedBCDSourceGraph source;

    public CompressedBCDSubGraph(CompressedBCDSourceGraph source, RoaringBitmap taxa, RoaringBitmap characters, RoaringBitmap activeScaffolds) {
        super(taxa, characters, activeScaffolds);
        this.source = source;
    }

    @Override
    public CompressedBCDSourceGraph getSource() {
        return source;
    }
}
