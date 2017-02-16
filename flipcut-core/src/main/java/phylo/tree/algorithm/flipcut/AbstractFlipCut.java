package phylo.tree.algorithm.flipcut;

import phylo.tree.algorithm.SupertreeAlgorithm;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.costComputer.FlipCutWeights;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutterFactory;
import phylo.tree.algorithm.flipcut.flipCutGraph.GraphCutter;
import phylo.tree.model.Tree;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;


/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 *         Date: 29.11.12
 *         Time: 14:10
 */
public abstract class AbstractFlipCut<N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>, C extends GraphCutter<N, T>, F extends CutterFactory<C, N, T>> extends SupertreeAlgorithm {
    protected static final boolean DEBUG = true;
    public static final int CORES_AVAILABLE = Runtime.getRuntime().availableProcessors();
    /**
     * number of thread that should be used 0 {@literal ->} automatic {@literal <} 0 massive parralel
     */
    protected int numberOfThreads = 0;

    /**
     * number of thread that should be used 0 {@literal ->} automatic
     */
    protected boolean printProgress = false;


    protected F type;
    /**
     * Use edge weights
     */
    protected FlipCutWeights.Weights weights = FlipCutWeights.Weights.UNIT_COST;
    /**
     * Set minimum bootstrap value of a clade to be part of the analysis
     */
    protected int bootstrapThreshold = 0;
    /**
     * The Graph actual working on
     */
    protected T initialGraph;


    protected AbstractFlipCut() {
        super();
    }

    /**
     * Create new instace with logger
     */
    protected AbstractFlipCut(F type) {
        super();
        this.type = type;
    }

    /**
     * Create new instace with logger
     *
     * @param log the logger
     */
    protected AbstractFlipCut(Logger log, F type) {
        super(log);
        this.type = type;
    }

    /**
     * Create new instace with logger and global Executor service
     *
     * @param log the logger
     */
    protected AbstractFlipCut(Logger log, ExecutorService executorService1, F type) {
        super(log, executorService1);
        this.type = type;
    }

    public void setPrintProgress(boolean printProgress) {
        this.printProgress = printProgress;
    }

    public boolean isPrintProgress() {
        return printProgress;
    }


    /**
     * Sets the edge weights function
     *
     * @param weights the weighting scheme
     */
    public void setWeights(FlipCutWeights.Weights weights) {
        this.weights = weights;
    }


    @Override
    public void setInput(List<Tree> inputTrees) {
        setInput(inputTrees, null);
    }

    public void setInput(List<Tree> inputTrees, Tree scaffoldTree) {
        final CostComputer costs = initCosts(inputTrees, scaffoldTree);
        initialGraph = createInitGraph(costs);
    }

    public void setCutter(F type) {
        this.type = type;
    }

    public F getCutterType() {
        return type;
    }

    public void setBootstrapThreshold(int bootstrapThreshold) {
        this.bootstrapThreshold = bootstrapThreshold;
    }

    public int getBootstrapThreshold() {
        return bootstrapThreshold;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    protected abstract T createInitGraph(CostComputer costsComputer);

    protected abstract CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree);


}
