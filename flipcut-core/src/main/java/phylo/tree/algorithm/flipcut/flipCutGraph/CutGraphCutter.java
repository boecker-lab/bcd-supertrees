package phylo.tree.algorithm.flipcut.flipCutGraph;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 14:15
 */
public abstract class CutGraphCutter<N extends AbstractFlipCutNode<N>,T extends AbstractFlipCutGraph<N>> implements GraphCutter<N,T>{
    public static final long INFINITY = 1000000;

    protected final ExecutorService executorService;
    protected final int threads;

    protected Map<N, N> nodeToDummy;
    protected Map<N, Set<N>> dummyToMerged;

    protected T source = null;
    protected List<N> cutGraphTaxa = null;

    protected LinkedHashSet<N> mincut;
    protected long mincutValue;
    protected List<T> split = null;

    protected CutGraphCutter() {
        this.executorService = null;
        this.threads=1;
    }
    protected CutGraphCutter(ExecutorService executorService, int threads) {
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

    protected abstract void calculateMinCut();
}
