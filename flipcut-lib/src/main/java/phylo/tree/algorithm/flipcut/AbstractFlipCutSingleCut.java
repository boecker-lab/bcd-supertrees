package phylo.tree.algorithm.flipcut;

import core.algorithm.Algorithm;
import core.utils.progressBar.CLIProgressBar;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.algorithm.flipcut.flipCutGraph.MaxFlowCutterFactory;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:42
 */
public abstract class AbstractFlipCutSingleCut<N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>, C extends CutGraphCutter<LinkedHashSet<N>, T>> extends AbstractFlipCut<N, T, C, MaxFlowCutterFactory<C, N, T>> {
    private static final boolean CALCULATE_SCORE = true;
    private long globalWeight;

    private Queue<Future<Queue<TreeNode>>> results = new ConcurrentLinkedQueue<>();
    private Queue<C> cutterQueue;

    public DebugInfo debugInfo;

    protected CLIProgressBar progressBar = null;
    protected int finish;

    protected Tree supertree = null;

    public AbstractFlipCutSingleCut() {
    }

    public AbstractFlipCutSingleCut(MaxFlowCutterFactory<C, N, T> type) {
        super(type);
    }

    public AbstractFlipCutSingleCut(Logger log, MaxFlowCutterFactory<C, N, T> type) {
        super(log, type);
    }

    public AbstractFlipCutSingleCut(Logger log, ExecutorService executorService1, MaxFlowCutterFactory<C, N, T> type) {
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

    private void calculateST() {
        long time = System.currentTimeMillis();
        supertree = null;
        if (initialGraph != null) {
            if (DEBUG) {
                debugInfo = new DebugInfo();
                debugInfo.overallCalculationTime = System.currentTimeMillis();
            }
            if (CALCULATE_SCORE)
                globalWeight = 0;

            System.out.println("Computing Supertree...");

            if (printProgress) {
                progressBar = new CLIProgressBar();
                finish = initialGraph.taxa.size() * 2 + 10;
                progressBar.update(0, finish);
            }


            Tree supertree = null;
            try {
                //this is the all parralel version
                if (numberOfThreads < 0) {
                    if (executorService == null)
                        executorService = Executors.newWorkStealingPool();
                    supertree = computeSTIterativeMultiThreaded();
                    //only max flow calculation is parralel, more efficient
                } else {
                    if (executorService == null) {
                        if (numberOfThreads == 0) {
                            executorService = Executors.newFixedThreadPool(CORES_AVAILABLE);
                        } else if (numberOfThreads > 1) {
                            executorService = Executors.newFixedThreadPool(numberOfThreads);
                        }
                    }
                    supertree = computeSTIterativeSingleThreaded();
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (CALCULATE_SCORE) supertree.setName("" + globalWeight);
            if (DEBUG) {
                debugInfo.weight = globalWeight;
                debugInfo.overallCalculationTime = (System.currentTimeMillis() - debugInfo.overallCalculationTime) / 1000;
            }
            if (printProgress) {
                progressBar.update(finish, finish);
                System.out.println();
            }
            System.out.println("...Supertree construction FINISHED!");
            this.supertree = supertree;
            System.out.println("SupertreeScore: " + supertree.getName());
            System.out.println("calculations time: " + (double) (System.currentTimeMillis() - time) / 1000d + "s");


        } else {
            throw new IllegalArgumentException("No inputTrees found");
        }
    }

    private Tree computeSTIterativeMultiThreaded() throws ExecutionException, InterruptedException {
        LOGGER.info("Computing Supertree during iterative graph splitting (MultiThreaded)");
        Tree supertree = null;

        supertree = new Tree();
        supertree.addVertex(initialGraph.treeNode);
        supertree.setRoot(initialGraph.treeNode);

        cutterQueue = new ConcurrentLinkedQueue<>();
        results.offer(executorService.submit(new GraphSplitterIterative(initialGraph)));
        int pcount = 1;
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
            }
            if (printProgress)
                progressBar.update(pcount++, finish);
        }
        return supertree;
    }

    private Tree computeSTIterativeSingleThreaded() {
        final C cutter = ((MaxFlowCutterFactory<C, N, T>) type).newInstance(initialGraph);
        final Tree supertree = new Tree();

        Queue<T> graphs = new LinkedList<T>();
        graphs.offer(initialGraph);

        LOGGER.info("Computing Supertree during iterative graph splitting (SingleThreaded)");

        int pcount = 1;
        while (graphs.size() > 0) {
            initialGraph = graphs.poll();

            // add the node
            supertree.addVertex(initialGraph.treeNode);
            if (initialGraph.parentNode != null)
                supertree.addEdge(initialGraph.parentNode, initialGraph.treeNode);

            // init the graph (remove semi universals)
            initialGraph.deleteSemiUniversals();

            // check if we have just one taxon left
            if (initialGraph.taxa.size() == 1) {
                // the current node becomes the leaf
                initialGraph.treeNode.setLabel((initialGraph.taxa.iterator().next()).name);
                // check if we have just two taxa left --> cut is trivial
            } else if (initialGraph.taxa.size() == 2) {
                //System.out.println("################ TRIVAL CUT ################");
                for (N taxon : initialGraph.taxa) {
                    TreeNode t = new TreeNode(taxon.name);
                    supertree.addVertex(t);
                    supertree.addEdge(initialGraph.treeNode, t);
                }
            } else {
                // get components
                List<List<N>> components = initialGraph.getComponents();
                if (components.size() == 1) {
                    if (DEBUG)
                        debugInfo.currentStartTime = System.currentTimeMillis();

                    // just one component, we have to cut
                    cutter.clear();

                    List<T> componentGraphs = (List<T>) initialGraph.calculatePartition(cutter);
                    //mincut value in graph needed?
                    if (CALCULATE_SCORE) {
                        long v = cutter.getMinCutValue(initialGraph);
                        globalWeight += v;
                    }
                    if (DEBUG)
                        if (componentGraphs.size() > 2)
                            debugInfo.polytomies.add(componentGraphs.size());

                    //Cut graph components
                    for (T componentGraph : componentGraphs) {
                        if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE) {
                            componentGraph.insertScaffPartData(initialGraph, null);
                        }
                        if (getCutterType().getType().isBCD())
                            componentGraph.insertCharacterMapping(initialGraph);
                        graphs.offer(componentGraph);
                    }

                    if (DEBUG)
                        debugInfo.cuttingTimes.add((System.currentTimeMillis() - debugInfo.currentStartTime) / 1000);
                } else {
                    if (DEBUG)
                        if (components.size() > 2)
                            debugInfo.polytomies.add(components.size());
                    if (DEBUG)
                        debugInfo.currentStartTime = System.currentTimeMillis();
                    // create the component graphs
//                    boolean checkEdges = (getCutterType() == CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
                    for (List<N> component : components) {
                        T g = createGraph(component, initialGraph.treeNode);
                        //actualize scaffold partition data
                        if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE) {
                            g.insertScaffPartData(initialGraph, null);
                        }
                        if (getCutterType().getType().isBCD())
                            g.insertCharacterMapping(initialGraph);
                        graphs.offer(g);
                    }
                    if (DEBUG)
                        debugInfo.splittingTimes.add((System.currentTimeMillis() - debugInfo.currentStartTime) / 1000);
                }
            }
            if (printProgress)
                progressBar.update(pcount++, finish);
        }

        return supertree;
    }

    protected abstract T createGraph(List<N> component, TreeNode treeNode);

    private class GraphSplitterIterative implements Callable<Queue<TreeNode>> {
        final T currentGraph;

        GraphSplitterIterative(final T currentGraph) {
            this.currentGraph = currentGraph;
        }

        @Override
        public Queue<TreeNode> call() throws Exception {

            // init the graph (remove semi universals)
            currentGraph.deleteSemiUniversals();

            // check if we have just one taxon left
            if (currentGraph.taxa.size() == 1) {
                // the current node becomes the leaf
                currentGraph.treeNode.setLabel((currentGraph.taxa.iterator().next()).name);
                return null;
            } else {


                Queue<TreeNode> nodes = new LinkedList<>();
                nodes.offer(currentGraph.treeNode);
                List<GraphSplitterIterative> outputGraphs = new LinkedList<>();
                // get components
                List<List<N>> components = currentGraph.getComponents();
                if (components.size() == 1) {
                    // just one component, we have to cut
                    C cutter = cutterQueue.poll();
                    if (cutter == null)
                        cutter = type.newInstance(currentGraph);

                    List<T> componentGraphs = (List<T>) currentGraph.calculatePartition(cutter);
                    //mincut value in graph needed?
                    if (CALCULATE_SCORE) globalWeight += cutter.getMinCutValue(currentGraph);

                    //Cut graph components
                    for (T componentGraph : componentGraphs) {
                        if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE) {
                            componentGraph.insertScaffPartData(currentGraph, null);
                        }
                        if (getCutterType().getType().isBCD())
                            componentGraph.insertCharacterMapping(currentGraph);
                        nodes.add(componentGraph.treeNode);
                        results.offer(executorService.submit(new GraphSplitterIterative(componentGraph)));
                    }
                    cutter.clear();
                    cutterQueue.offer(cutter);
                } else {
                    // create the component graphs
                    for (List<N> component : components) {
                        T g = createGraph(component, currentGraph.treeNode);
                        //actualize scaffold partition data
                        if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE) {
                            g.insertScaffPartData(currentGraph, null);
                        }
                        if (getCutterType().getType().isBCD())
                            g.insertCharacterMapping(currentGraph);
                        nodes.add(g.treeNode);
                        results.offer(executorService.submit(new GraphSplitterIterative(g)));
                    }
                }
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


}
