package flipcut.flipCutGraph;

import java.util.*;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 14:15
 */
public abstract class CutGraphCutter<N extends AbstractFlipCutNode<N>,T extends AbstractFlipCutGraph<N>> {
    public static enum CutGraphTypes {MAXFLOW_TARJAN_GOLDBERG, HYPERGRAPH_MINCUT, HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW}
    public static final long INFINITY = 1000000;

    public final boolean mergeCharacters = true;

    public static final boolean MAX_FLIP_NORMALIZATION = false;

    //THE "real" bcd without flip weighting
    public static final boolean IGNORE_MATRIX_ENTRIES = true;
        //only if ignore matrix entries (flips) false
        public static final boolean REAL_CHAR_DELETION = true;
            //if real char deletion false:
            public static final boolean ZEROES = true;

    protected Map<N, N> nodeToDummy;
    protected Map<N, Set<N>> dummyToMerged;

    protected final CutGraphTypes type;
    public CutGraphTypes getType() {
        return type;
    }

    protected T source = null;
    protected List<N> cutGraphTaxa = null;

    protected LinkedHashSet<N> mincut;
    protected long mincutValue;
    protected List<T> split = null;

    public CutGraphCutter(CutGraphTypes type) {
        this.type = type;
    }

    public LinkedHashSet<N> getMinCut(T source) {
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
        nodeToDummy = null;
        dummyToMerged = null;

    }

    public abstract List<T> cut(T source);
    protected abstract void calculateMinCut();
    protected abstract int buildCharacterMergingMap(T source,final boolean globalMap);
}
