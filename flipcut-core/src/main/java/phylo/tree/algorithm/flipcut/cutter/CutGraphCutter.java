package phylo.tree.algorithm.flipcut.cutter;

import mincut.cutGraphAPI.bipartition.Cut;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 14:15
 */
public abstract class CutGraphCutter<N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>> implements GraphCutter<N, T> {
    private static final long INFINITY = 1000000;
    public static long getInfinity(){
        return CostComputer.ACCURACY * INFINITY;
    }

    protected final ExecutorService executorService;
    protected final int threads;

    protected T source = null;

    protected CutGraphCutter() {
        this.executorService = null;
        this.threads = 1;
    }

    protected CutGraphCutter(ExecutorService executorService, int threads) {
        this.executorService = executorService;
        this.threads = threads;
    }

    public Cut<N> getMinCut(T source) {
        if (this.source != source) {
            this.source = source;

        }
        return  calculateMinCut();
    }

    /*public LinkedHashSet<N> getMinCutSet(T source) {
        return getMinCut(source).getCutSet();
    }*/

    public long getMinCutValue(T source) {
        return getMinCut(source).minCutValue();
    }

    public void clear() {
        source = null;
    }

    protected abstract Cut<N> calculateMinCut();
}
