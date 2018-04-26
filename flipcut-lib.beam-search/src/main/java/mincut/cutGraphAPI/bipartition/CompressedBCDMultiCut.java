package mincut.cutGraphAPI.bipartition;

import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDMultiCutGraph;

import java.util.HashSet;
import java.util.List;

public class CompressedBCDMultiCut extends CompressedBCDCut implements MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph> {
    private static final RoaringBitmap EMPTY_CUTSET = new RoaringBitmap();

    private CompressedBCDMultiCutGraph source;

    private HashSet<RoaringBitmap> split;
    private List<CompressedBCDMultiCutGraph> splittedGraphs;
    private int cachedHash;

    public CompressedBCDMultiCut(CompressedBCDMultiCutGraph source) {
        this(EMPTY_CUTSET, 0L, source);
    }

    public CompressedBCDMultiCut(RoaringBitmap toDelete, long minCutValue, CompressedBCDMultiCutGraph source) {
        super(toDelete, minCutValue);
        this.source = source;
        getSplittedGraphs(); //todo can we do that on demand?
        cachedHash = calcHashCode();
    }

    @Override
    public List<CompressedBCDMultiCutGraph> getSplittedGraphs() {
        if (splittedGraphs == null) {
            splittedGraphs = source.split(getCutSet());
//            toDelete = null;
            split = new HashSet<>(splittedGraphs.size());
            for (CompressedBCDMultiCutGraph graph : splittedGraphs) {
                split.add(graph.getTaxa());
            }
        }

        return splittedGraphs;
    }

    @Override
    public CompressedBCDMultiCutGraph sourceGraph() {
        return source;
    }


    @Override
    public int compareTo(Cut<RoaringBitmap> o) {
        return Long.compare(minCutValue(), o.minCutValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompressedBCDMultiCut that = (CompressedBCDMultiCut) o;

        if (minCutValue != that.minCutValue) return false;
        return split.equals(that.split);
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }
    public int calcHashCode() {
        int result = split.hashCode();
        result = 31 * result + (int) (minCutValue ^ (minCutValue >>> 32));
        return result;
    }
}
