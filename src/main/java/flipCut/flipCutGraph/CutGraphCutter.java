package flipCut.flipCutGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 14:15
 */
public abstract class CutGraphCutter<N extends AbstractFlipCutNode<N>,T extends AbstractFlipCutGraph<N>> {
    public static enum CutGraphTypes {MAXFLOW_TARJAN_GOLDBERG, HYPERGRAPH_MINCUT, HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW}
    public static final long INFINITY = 1000000;

    public final boolean mergeCharacters = false;
    public final boolean staticCharacterMap = false;

    public static final boolean MAX_FLIP_NORMALIZATION = false;

    //THE "real" bcd without flip weighting
    public static final boolean IGNORE_MATRIX_ENTRIES = true;
        //only if ignore matrix entries (flips) false
        public static final boolean REAL_CHAR_DELETION = true;
            //if real char deletion false:
            public static final boolean ZEROES = true;

    protected HashMap<N, N> nodeToDummy;
    protected HashMap<N, Set<N>> dummyToMerged;

    protected HashMap<N, N> TaxaToDummy;
    protected HashMap<N, Set<N>> dummyToMergedTaxa;

    protected final CutGraphTypes type;

    protected T source = null;
    protected List<N> cutGraphTaxa = null;

    protected List<N> mincut;
    protected long mincutValue;
    protected List<T> split = null;

    public CutGraphCutter(CutGraphTypes type) {
        this.type = type;
    }

    public List<N> getMinCut(T source) {
        if (this.source != source) {
            this.source = source;
            calculateMinCut();
        }
        return mincut;
    }

    public long getMinCutValue(T source) {
        if (this.source != source) {
            this.source = source;
            calculateMinCut();
        }
        return mincutValue;
    }

    public void clear(){
        source = null;
        mincut = null;
        mincutValue = 0;
        split = null;
        if (!staticCharacterMap) {
            nodeToDummy = null;
            dummyToMerged = null;
        }
    }

    public abstract List<T> cut(T source);
    protected abstract void calculateMinCut();
    protected abstract int buildCharacterMergingMap(T source);
//    protected abstract int buildScaffoldTaxaMergingMap(Set<Set<N>> mergedTaxa);

    public int buildInitialCharacterMergingMap(T initGraph){
        return buildCharacterMergingMap(initGraph);
    }


    public abstract void removeNodeFromMergeSet(N toRemove);

}
