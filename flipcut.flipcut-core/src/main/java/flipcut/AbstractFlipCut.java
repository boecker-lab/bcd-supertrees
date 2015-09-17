package flipcut;//import epos.model.tree.Tree;

import epos.model.algo.SupertreeAlgorithm;
import epos.model.tree.Tree;
import flipcut.costComputer.CostComputer;
import flipcut.costComputer.FlipCutWeights;
import flipcut.flipCutGraph.AbstractFlipCutGraph;
import flipcut.flipCutGraph.AbstractFlipCutNode;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;


/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:10
 */
public abstract class AbstractFlipCut<N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>> extends SupertreeAlgorithm {
    protected static final boolean DEBUG = false;
    /**
     * number of thread that should be used 0 -> automatic
     */
    protected int numberOfThreads = 0;

    /**
     * number of thread that should be used 0 -> automatic
     */
    protected boolean printProgress = false;

    /**
     * Use edge weights
     */
    protected FlipCutWeights.Weights weights = FlipCutWeights.Weights.UNIT_COST;
    /**
     * Verbose logs
     */
    protected boolean verbose = false;
    /**
     * Set minimum bootstrap value of a clade to be part of the analysis ()
     */
    protected int bootstrapThreshold = 0;
    /**
     * The Graph actual working on
     */
    protected T initialGraph;

    /**
     * Create new instace with logger
     */
    protected AbstractFlipCut() {
        super();
    }
    /**
     * Create new instace with logger
     *
     * @param log the logger
     */
    protected AbstractFlipCut(Logger log) {
        super(log);
    }

    /**
     * Create new instace with logger and global Executor service
     *
     * @param log the logger
     */
    protected AbstractFlipCut(Logger log, ExecutorService executorService1) {
        super(log,executorService1);
    }

    /**
     * Activate verbose log output
     *
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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

    public void setBootstrapThreshold(int bootstrapThreshold) {
        this.bootstrapThreshold = bootstrapThreshold;
    }

    public int getBootstrapThreshold() {
        return bootstrapThreshold;
    }

    public void setPrintProgress(boolean printProgress) {
        this.printProgress = printProgress;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    protected abstract T createInitGraph(CostComputer costsComputer);

    protected abstract CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree);
}
