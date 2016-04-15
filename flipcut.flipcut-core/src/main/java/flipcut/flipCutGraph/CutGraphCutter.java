package flipcut.flipCutGraph;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 14:15
 */
public abstract class CutGraphCutter<N extends AbstractFlipCutNode<N>,T extends AbstractFlipCutGraph<N>> {


    public enum CutGraphTypes {MAXFLOW_TARJAN_GOLDBERG, MAXFLOW_AHOJI_ORLIN, HYPERGRAPH_MINCUT, HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG, HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN}
    public static final long INFINITY = 1000000;

    public static final boolean MAX_FLIP_NORMALIZATION = false;

    //THE "real" bcd without flip weighting
    public static final boolean IGNORE_MATRIX_ENTRIES = true;
        //only if ignore matrix entries (flips) false
        public static final boolean REAL_CHAR_DELETION = true;
            //if real char deletion false:
            public static final boolean ZEROES = true;

    protected final ExecutorService executorService;
    protected final int threads;

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

    protected CutGraphCutter(CutGraphTypes type) {
        this.type = type;
        this.executorService = null;
        this.threads=1;
    }
    protected CutGraphCutter(CutGraphTypes type, ExecutorService executorService, int threads) {
        this.type = type;
        this.executorService = executorService;
        this.threads =  threads;
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
}
