package phylo.tree.algorithm.flipcut;

import core.algorithm.Algorithm;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import mincut.cutGraphAPI.bipartition.Cut;
import org.slf4j.Logger;
import phylo.tree.algorithm.flipcut.cutter.CutterFactory;
import phylo.tree.algorithm.flipcut.cutter.GraphCutter;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:42
 */
public class FlipCutSingleCut<S, T extends SourceTreeGraph<S>, C extends GraphCutter<S>> extends AbstractFlipCut<S, T, C, CutterFactory<C, S, T>> {
    private static final boolean CALCULATE_SCORE = true;
    private long globalWeight;

    private Queue<Future<Queue<TreeNode>>> results = new ConcurrentLinkedQueue<>();
    private Queue<C> cutterQueue;

    public DebugInfo debugInfo;

    protected Tree supertree = null;

    public FlipCutSingleCut() {
    }

    public FlipCutSingleCut(CutterFactory<C, S, T> type) {
        super(type);
    }

    public FlipCutSingleCut(Logger log, CutterFactory<C, S, T> type) {
        super(log, type);
    }

    public FlipCutSingleCut(Logger log, ExecutorService executorService1, CutterFactory<C, S, T> type) {
        super(log, executorService1, type);
    }

    @Override
    public Tree getResult() {
        return supertree;
    }

    @Override
    public List<Tree> getResults() {
        return Arrays.asList(getResult());
    }

    @Override
    public Algorithm<List<Tree>, Tree> call() {
        calculateST();
        return this;
    }

    ProgressBar progressBar = null;
    AtomicInteger partitions = null;
    private void initProgress(){
        partitions = new AtomicInteger(0);
            java.util.logging.Logger.getLogger("org.jline").setLevel(Level.OFF);        if (printProgress) {
            System.err.println();
            System.out.flush();
            System.err.flush();
            progressBar = new ProgressBar("BCD", initialGraph.numTaxa() - 1, ProgressBarStyle.ASCII);
            printProgress("Start Computation", 0);
        }
    }

    private void calculateST() {
        supertree = null;
        if (initialGraph != null) {
            if (DEBUG) {
                debugInfo = new DebugInfo();
                debugInfo.overallCalculationTime = System.currentTimeMillis();
            }
            if (CALCULATE_SCORE)
                globalWeight = 0;

            Tree supertree = null;
            try {
                //this is the all parralel version
                if (numberOfThreads <= 0) {
                    if (executorService == null) {
                        if (numberOfThreads == 0)
                            numberOfThreads = CORES_AVAILABLE;
                        executorService = Executors.newWorkStealingPool(Math.abs(numberOfThreads));
                    }

                    LOGGER.info("Computing Supertree with " + Math.abs(numberOfThreads) + " threads. Parallelization over MinCuts AND Tree Partitions");
                    initProgress();
                    supertree = computeSTIterativeMultiThreaded();
                    //only max flow calculation is parralel, more efficient
                } else {
                    if (executorService == null) {
                        if (numberOfThreads > 1) {
                            executorService = Executors.newFixedThreadPool(numberOfThreads);
                            LOGGER.info("Computing Supertree with " + Math.abs(numberOfThreads) + " threads. Parallelization over MinCuts");
                        } else {
                            executorService = Executors.newSingleThreadExecutor();
                            LOGGER.info("Computing Supertree with " + Math.abs(numberOfThreads) + " threads.");
                        }
                    }
                    initProgress();
                    supertree = computeSTIterativeSingleThreaded();
                }
            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error("Error during BCD algorithm execution", e);
            }

            if (CALCULATE_SCORE) supertree.setName("" + globalWeight);
            if (DEBUG) {
                debugInfo.weight = globalWeight;
                debugInfo.overallCalculationTime = (System.currentTimeMillis() - debugInfo.overallCalculationTime) / 1000;
            }

            printProgress("Computation DONE!");
            if (progressBar != null) {
                progressBar.close();
                System.err.println();
                System.err.flush();
                System.out.flush();
            }

            this.supertree = supertree;

        } else {
            throw new IllegalArgumentException("No inputTrees found");
        }
    }

    private Tree computeSTIterativeMultiThreaded() throws ExecutionException, InterruptedException {
        Tree supertree = null;

        supertree = new Tree();
        TreeNode root = new TreeNode();
        supertree.addVertex(root);
        supertree.setRoot(root);

        cutterQueue = new ConcurrentLinkedQueue<>();
        results.offer(executorService.submit(new GraphSplitterIterative(initialGraph, root)));
        initialGraph = null;

        //building supertree during waiting for threads
        while (!results.isEmpty()) {
            Future<Queue<TreeNode>> f = results.poll();
            Queue<TreeNode> toAdd = f.get();
            if (toAdd != null && toAdd.size() > 1) {
                TreeNode parent = toAdd.poll();
                for (TreeNode child : toAdd) {
                    supertree.addVertex(child);
                    supertree.addEdge(parent, child);
                }
                printProgress((partitions.addAndGet(toAdd.size() - 1)));
            }
        }
        return supertree;
    }

    private Tree computeSTIterativeSingleThreaded() {
        final C cutter = type.newInstance(initialGraph, executorService, numberOfThreads);
        final Tree supertree = new Tree();

        Queue<T> graphs = new LinkedList<>();
        Queue<TreeNode> treeNodes = new LinkedList<>();

        graphs.offer(initialGraph);
        TreeNode parentNode = new TreeNode();
        treeNodes.offer(parentNode);
        supertree.addVertex(parentNode);
        supertree.setRoot(parentNode);

        int pcount = 1;
        while (graphs.size() > 0) {
            initialGraph = graphs.poll();
            parentNode = treeNodes.poll();


            // init the graph (remove semi universals)
//            printProgress("Deleting Semiuniversals...");
            initialGraph.deleteSemiUniversals();

            // check if we have just one taxon left
            if (initialGraph.numTaxa() == 1) {
                // the current node becomes the leaf
                parentNode.setLabel((String) initialGraph.taxaLabels().iterator().next());
                // check if we have just two taxa left --> cut is trivial
            } else if (initialGraph.numTaxa() == 2) {
                for (Object to : initialGraph.taxaLabels()) {
                    final String taxon = (String) to;
                    TreeNode t = new TreeNode(taxon);
                    supertree.addVertex(t);
                    supertree.addEdge(parentNode, t);
                }
                printProgress(pcount++);
            } else {
                if (DEBUG)
                    debugInfo.currentStartTime = System.currentTimeMillis();

                //partition the current graph
                cutter.clear();

                final int partitionIndex = partitions.get() + 1;
                printProgress("START Part " + partitionIndex + ": " + initialGraph.numTaxa() + " Taxa - " + initialGraph.numCharacter() + " Clades" );

                List<T> componentGraphs = (List<T>) initialGraph.getPartitions(cutter);
                for (T componentGraph : componentGraphs) {
                    // add the node
                    TreeNode treeNode = new TreeNode();
                    supertree.addVertex(treeNode);
                    supertree.addEdge(parentNode, treeNode);
                    //add graph an its treenode at same positions in each list
                    graphs.offer(componentGraph);
                    treeNodes.offer(treeNode);
                }

                printProgress("DONE Part " + partitionIndex + ": "  + initialGraph.numTaxa() + " Taxa - " + initialGraph.numCharacter() + " Clades", partitions.addAndGet(componentGraphs.size() - 1));

                if (CALCULATE_SCORE) {
                    Cut<S> cut = cutter.getMinCut();
                    if (cut != null)
                        globalWeight += cut.minCutValue();
                }
                if (DEBUG)
                    if (componentGraphs.size() > 2)
                        debugInfo.polytomies.add(componentGraphs.size());

                if (DEBUG)
                    debugInfo.cuttingTimes.add((System.currentTimeMillis() - debugInfo.currentStartTime) / 1000);
            }

        }

        return supertree;
    }

    protected void printProgress(String message, long stepto, long limit) {
        if (progressBar != null) {
            progressBar.maxHint(limit);
            progressBar.stepTo(stepto);
            progressBar.setExtraMessage(message);
            LOGGER.debug(message);
        }else{
            LOGGER.info(message);
        }
    }

    protected void printProgress(String message, long stepto) {
        if (progressBar != null) {
            progressBar.stepTo(stepto);
            progressBar.setExtraMessage(message);
            LOGGER.debug(message);
        }else{
            LOGGER.info(message);
        }

    }

    protected void printProgress(String message) {
        if (progressBar != null) {
            progressBar.setExtraMessage(message);
            LOGGER.debug(message);
        }else{
            LOGGER.info(message);
        }
    }

    protected void printProgress(long stepto, long limit) {
        if (progressBar != null) {
            progressBar.maxHint(limit);
            progressBar.stepTo(stepto);
        }
    }

    protected void printProgress(long stepto) {
        if (progressBar != null) {
            progressBar.stepTo(stepto);
        }
    }


    private class GraphSplitterIterative implements Callable<Queue<TreeNode>> {
        final T currentGraph;
        final TreeNode treeNode;

        GraphSplitterIterative(final T currentGraph, final TreeNode treeNode) {
            this.currentGraph = currentGraph;
            this.treeNode = treeNode;
        }

        @Override
        public Queue<TreeNode> call() throws Exception {

            // init the graph (remove semi universals)
            currentGraph.deleteSemiUniversals();

            // check if we have just one taxon left
            if (currentGraph.numTaxa() == 1) {
                // the current node becomes the leaf
                treeNode.setLabel((String) currentGraph.taxaLabels().iterator().next());
                return null;
            } else {
                final int c = currentGraph.numCharacter();
                final int t = currentGraph.numTaxa();
                final int partitionIndex = partitions.get() + 1;
                printProgress("START Part " + partitionIndex + ": " + t + " Taxa - " + c + " Clades" );

                final Queue<TreeNode> nodes = new LinkedList<>();
                nodes.offer(treeNode);

                //partition the current graph
                List<T> componentGraphs = null;
                {
                    C cutter = cutterQueue.poll();
                    if (cutter == null)
                        cutter = type.newInstance(currentGraph, executorService, Math.abs(numberOfThreads));
                    cutter.clear();

                    componentGraphs = (List<T>) currentGraph.getPartitions(cutter);
                    if (CALCULATE_SCORE) {
                        Cut<S> cut = cutter.getMinCut();
                        if (cut != null)
                            globalWeight += cut.minCutValue();
                    }

                    cutter.clear();
                    cutterQueue.offer(cutter);
                }

                for (T componentGraph : componentGraphs) {
                    TreeNode componentTreeNode = new TreeNode();
                    nodes.offer(componentTreeNode);
                    results.offer(executorService.submit(new GraphSplitterIterative(componentGraph, componentTreeNode)));
                }

                printProgress("DONE Part " + partitionIndex + ": " + t + " Taxa - " + c + " Clades" );

                return nodes;
            }
        }
    }

    public class DebugInfo {
        double currentStartTime;
        List<Double> cuttingTimes = new LinkedList<>();
        List<Double> splittingTimes = new LinkedList<>();
        double overallCalculationTime;
        double mergingMapCreateTime = 0d;
        long weight;
        List<Integer> polytomies = new LinkedList<>();
        ;

        public int getNumberOfCuts() {
            return cuttingTimes.size();
        }

        public int getNumberOfNonCutSplits() {
            return splittingTimes.size();
        }

        public double getCuttingTime() {
            double allTime = 0;
            for (Double cuttingTime : cuttingTimes) {
                allTime += cuttingTime;
            }
            return allTime;
        }

        public double getCurrentStartTime() {
            return currentStartTime;
        }

        public List<Double> getCuttingTimes() {
            return cuttingTimes;
        }

        public List<Double> getSplittingTimes() {
            return splittingTimes;
        }

        public double getOverallCalculationTime() {
            return overallCalculationTime;
        }

        public long getWeight() {
            return weight;
        }

        public List<Integer> getPolytomies() {
            return polytomies;
        }

        public int getNumOfPolytomies() {
            return polytomies.size();
        }

        public String printDebugInfo() {
            StringBuffer debugInfo = new StringBuffer();
            debugInfo.append("########## BEGIN DEBUG INFO ##########");
            debugInfo.append("\n");
            debugInfo.append("\n");
            debugInfo.append("FlipCut Score: " + weight);
            debugInfo.append("\n");
            debugInfo.append("Calculation Time: " + overallCalculationTime + "s");
            debugInfo.append("\n");
            debugInfo.append("MergeMap Time: " + mergingMapCreateTime + "s");
            debugInfo.append("\n");
            debugInfo.append("\n");
            debugInfo.append("Number of MinCut Operations " + getNumberOfCuts());
            debugInfo.append("\n");
            debugInfo.append("MinCut Times: ");
            debugInfo.append("\n");
            StringBuffer cutTime = new StringBuffer();
            cutTime.append("( ");
            double allTime = 0;
            for (Double cuttingTime : cuttingTimes) {
                allTime += cuttingTime;
                cutTime.append(cuttingTime + " ");
            }
            cutTime.append(")");
            debugInfo.append(cutTime.toString());
            debugInfo.append("\n");
            debugInfo.append("Overall MinCut Time: " + allTime);
            debugInfo.append("\n");
            debugInfo.append("\n");
            debugInfo.append("Number of NON MinCut Split Operation " + getNumberOfNonCutSplits());
            debugInfo.append("\n");
            debugInfo.append("MinCut Times: ");
            debugInfo.append("\n");
            cutTime = new StringBuffer();
            cutTime.append("( ");
            allTime = 0;
            for (Double cuttingTime : splittingTimes) {
                allTime += cuttingTime;
                cutTime.append(cuttingTime + " ");
            }
            cutTime.append(")");
            debugInfo.append(cutTime.toString());
            debugInfo.append("\n");
            debugInfo.append("Overall NON MinCut Split Time: " + allTime);
            debugInfo.append("\n");
            debugInfo.append("number of polytomie splits: " + getNumOfPolytomies());
            debugInfo.append("\n");
            debugInfo.append("Polytomies: ");
            debugInfo.append("\n");
            cutTime = new StringBuffer();
            cutTime.append("( ");
            for (Integer cuttingTime : polytomies) {
                cutTime.append(cuttingTime + " ");
            }
            cutTime.append(")");
            debugInfo.append(cutTime.toString());
            debugInfo.append("\n");
            debugInfo.append("\n");
            debugInfo.append("########## END DEBUG INFO ##########");

            return debugInfo.toString();
        }
    }


    @Override
    protected String name() {
        return getClass().getSimpleName();
    }
}
