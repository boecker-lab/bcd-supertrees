package phylo.tree.algorithm.flipcut.cli;

import core.cli.Multithreaded;
import core.cli.Progressbar;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.ExplicitBooleanOptionHandler;
import phylo.tree.algorithm.flipcut.AbstractFlipCut;
import phylo.tree.algorithm.flipcut.costComputer.FlipCutWeights;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphTypes;
import phylo.tree.algorithm.gscm.GreedySCMAlgorithm;
import phylo.tree.algorithm.gscm.MultiGreedySCMAlgorithm;
import phylo.tree.algorithm.gscm.RandomizedGreedySCMAlgorithm;
import phylo.tree.algorithm.gscm.SCMAlgorithm;
import phylo.tree.algorithm.gscm.treeMerger.TreeScorers;
import phylo.tree.cli.SupertreeAlgortihmCLI;
import phylo.tree.cli.gscm.SCMCLI;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.logging.Level;

/**
 * Created by fleisch on 20.11.15.
 */
public abstract class BasicBCDCLI<A extends AbstractFlipCut> extends SupertreeAlgortihmCLI<A> implements Multithreaded, Progressbar {
    @Argument(usage = "Path of the file containing the guide tree", index = 1, required = false)
    private Path inputSCM = null;

    public Path getSCMInputFile() {
        if (inputSCM == null)
            return null;
        if (this.inputSCMFile == null) {
            this.inputSCMFile = this.workingPath.resolve(this.inputSCM);
        }
        return this.inputSCMFile;
    }

    private Path inputSCMFile = null;

    @Option(name = "-O", aliases = {"--fullOutput"}, usage = "Output file containing full output", forbids = "-o")
    public void setFullOutput(Path output) {
        setOutput(output);
        fullOutput = true;
    }

    public boolean isFullOutput() {
        return fullOutput;
    }

    private boolean fullOutput = false;


    //##### flip cut parameter #####
    public enum Algorithm {
        BCD, FC
    }

    public enum SuppportedWeights {UNIT_WEIGHT, TREE_WEIGHT, BRANCH_LENGTH, BOOTSTRAP_WEIGHT, LEVEL, BRANCH_AND_LEVEL, BOOTSTRAP_AND_LEVEL}

    protected final EnumMap<SuppportedWeights, FlipCutWeights.Weights> weightMapping = initWheightMapping();

    protected abstract EnumMap<SuppportedWeights, FlipCutWeights.Weights> initWheightMapping();

    //cut graph type
    @Option(name = "-c", aliases = "--cutGraphImplementation", usage = "Choose the graph structure used for the mincut calculation", hidden = true)
    public abstract void setGraphType(CutGraphTypes graphType);

    //FlipCut or BCD
    @Option(name = "-a", aliases = "--algorithm", usage = "The algorithm that is used to estimate the Supertree.\n BCD = BadCharacterDeletion supertrees\n FC = FlipCut supertrees.", hidden = true)
    public void setAlgorithm(Algorithm algo) {
        if (algo == Algorithm.FC)
            setGraphType(CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
        else
            setGraphType(CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
    }

    //Weighting
    @Option(name = "-w", aliases = "--weighting", usage = "Weighting strategy", forbids = "-W")
    public void setWeights(SuppportedWeights weighting) {
        if (weighting != null) {
            FlipCutWeights.Weights tmp = weightMapping.get(weighting);
            if (tmp != null) {
                setWeights(tmp);
            }
        }
    }

    @Option(name = "-W", aliases = "--internalWeighting", usage = "Weighting with internal code including nonofficial weightings", forbids = "-w", hidden = true)
    public void setWeights(FlipCutWeights.Weights weights) {
        this.weights = weights;
    }//default value is UW

    public FlipCutWeights.Weights getWeights() {
        return weights;
    }

    private FlipCutWeights.Weights weights = FlipCutWeights.Weights.UNIT_COST;

    @Option(name = "-b", aliases = "--bootstrapThreshold", usage = "Minimal bootstrap value of a tree-node to be considered during the supertree calculation")
    private int bootstrapThreshold = 0;

    public int getBootstrapThreshold() {
        return bootstrapThreshold;
    }


    //annotations inherited from superclass
    private int numberOfThreads = 0;

    @Override
    public void setNumberOfThreads(int i) {
        numberOfThreads = i;
    }

    @Override
    public int getNumberOfThreads() {
        return numberOfThreads;
    }


    //##### GSCM Stuff #####
    @Option(name = "-s", aliases = "--scm", handler = ExplicitBooleanOptionHandler.class, usage = "Use SCM-tree as guide tree", forbids = {"-S", "-SR"})
    public boolean useSCM = true;  //default value has to be true because of -S

    @Option(name = "-S", aliases = "--scmScoring", usage = "Enable the usage of a GSCM-tree as guide tree and specify the scorings that will be used to construct this GSCM-tree", forbids = "-s", hidden = true, handler = SCMCLI.ScorerTypeArrayOptionHandler.class)
    public TreeScorers.ScorerType[] scorerTypes = new TreeScorers.ScorerType[]{/*TreeScorers.ScorerType.COLLISION_SUBTREES, TreeScorers.ScorerType.UNIQUE_CLADE_RATE,*/ TreeScorers.ScorerType.UNIQUE_CLADES_LOST};



    @Option(name = "-Sr", aliases = {"--scmRandomized"}, usage = "Enables randomization (standard iterations are numberOfTrees^2 per scoring)", forbids = {"-s", "-SR"}, hidden = true)
    private void setRandomized(boolean r) {
        if (r) {
            randomIterations = 0;
        } else {
            randomIterations = -1;
        }
    }

    @Option(name = "-SR", aliases = {"--scmRandomIterations"}, usage = "Enables randomization and specifies the number of iterations per scoring", forbids = {"-s", "-Sr"}, hidden = true)
    private void setRandomIterations(int iter) {
        if (iter < 0) {
            randomIterations = -1;
        } else {
            randomIterations = iter;
        }
    }

    private int randomIterations = -1;


    //pre and post procesing stuff
    @Option(name = "-n", aliases = "--ucr", usage = "Run unsupported clade reduction post-processing", hidden = true)
    public boolean unsupportedCladeReduction = false;

    @Option(name = "-u", aliases = "--uds", usage = "Run undisputed sibling pre-processing", hidden = true)
    public boolean removeUndisputedSiblings = false;

    @Option(name = "-r", aliases = "--noRooting", usage = "Do not optimize fake roots of unrooted input trees based on Guide tree", hidden = true)
    public boolean noRootingOptimization = false;

    @Option(name = "-i", aliases = "--insufficient", usage = "Skip if input trees have insufficient taxa overlap", hidden = true)
    public boolean skipInsufficientOverlapInstances = false; //todo warn if taxa have insufficient taxa overlap taxa overlap check

    @Option(name = "-j", aliases = "--supportValues", usage = "Calculate Split Fit for every clade of the supertree(s) ")
    public boolean supportValues = false;

    private boolean disableProgressBar = false;
    @Override
    public boolean isProgressBar() {
        return !disableProgressBar;
    }

    @Override
    public void setProgressBar(boolean progressBar) {
        disableProgressBar = progressBar;
    }

    public SCMAlgorithm getSCMInstance() {
        SCMAlgorithm algo = null;
        if (randomIterations < 0) {
            //non random
            if (scorerTypes.length > 1) {
                //multi
                algo = new MultiGreedySCMAlgorithm(TreeScorers.getScorerArray(isMultiThreaded(), scorerTypes));
            } else {
                //standard
                algo = new GreedySCMAlgorithm(TreeScorers.getScorer(isMultiThreaded(), scorerTypes[0]));
            }
        } else {
            //randomized
            algo = new RandomizedGreedySCMAlgorithm(randomIterations, TreeScorers.getScorerArray(isMultiThreaded(), scorerTypes));
        }
        algo.setThreads(numberOfThreads);
        algo.setPrintProgress(isProgressBar());
        return algo;
    }

    public abstract AbstractFlipCut createAlgorithmInstance();

    @Override
    public void setLogLevel(LogLevelEnum level) {
        Level l = Level.parse(level.name());
        if (isProgressBar() && l.intValue() < Level.INFO.intValue()) {
            logLevel = l;
            LOG_FILE_OUT.setLevel(l);
        } else {
            super.setLogLevel(level);
        }
    }
}
