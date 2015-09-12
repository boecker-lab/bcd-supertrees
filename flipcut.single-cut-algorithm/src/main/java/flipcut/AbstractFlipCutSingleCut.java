package flipcut;

import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import epos.model.tree.treetools.TreeUtilsBasic;
import flipcut.flipCutGraph.AbstractFlipCutGraph;
import flipcut.flipCutGraph.AbstractFlipCutNode;
import flipcut.flipCutGraph.CutGraphCutter;
import flipcut.flipCutGraph.SingleCutGraphCutter;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:42
 */
public abstract class AbstractFlipCutSingleCut<N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>, C extends CutGraphCutter<N, T>> extends AbstractFlipCut<N, T> {
    private static final boolean CALCULATE_SCORE = true;

    private ExecutorService executor = null;
    private Queue<Future<Queue<TreeNode>>> results = new ConcurrentLinkedQueue<>();

    public DebugInfo debugInfo;
    private long globalWeight;
    private SingleCutGraphCutter.CutGraphTypes type;

    protected AbstractFlipCutSingleCut() {
        super();
    }

    public AbstractFlipCutSingleCut(SingleCutGraphCutter.CutGraphTypes type) {
        super();
        setCutter(type);
    }

    public AbstractFlipCutSingleCut(Logger log, SingleCutGraphCutter.CutGraphTypes type) {
        super(log);
        setCutter(type);
    }


    @Override
    public List<Tree> getSupertrees() {
        return Arrays.asList(getSupertree());
    }

    public Tree getSupertree() {
        if (initialGraph != null) {


            if (DEBUG) {
                debugInfo = new DebugInfo();
                debugInfo.overallCalculationTime = System.currentTimeMillis();
            }
            if (CALCULATE_SCORE)
                globalWeight = 0;

            System.out.println("Computing Supertree...");

            Tree supertree = null;
            try {
                if (numberOfThreads < 1) {
                    executor = Executors.newCachedThreadPool();
                    supertree = computeIterativeMultiThreaded();
                } else if (numberOfThreads == 1) {
                    supertree = computeSTreeSingleThreaded();
                } else {
                    executor = Executors.newFixedThreadPool(numberOfThreads);
                    supertree = computeIterativeMultiThreaded();
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (executor != null)
                executor.shutdown();
            if (CALCULATE_SCORE) supertree.setName("Tree_" + globalWeight);
            if (DEBUG) {
                debugInfo.weight = globalWeight;
                debugInfo.overallCalculationTime = (System.currentTimeMillis() - debugInfo.overallCalculationTime) / 1000;
            }
            System.out.println("...Supertree construction FINISHED!");
            return supertree;

        } else {
            throw new IllegalArgumentException("No inputTrees found");
        }
    }


    private Tree computeSTreeMultiThreadedStreams() {
        System.out.println("testing streams...");
        final Tree supertree = new Tree();

        List<T> graphs = Collections.synchronizedList(new LinkedList<>());
        graphs.add(initialGraph);
        supertree.addVertex(initialGraph.treeNode);
        supertree.setRoot(initialGraph.treeNode);

        getLog().info("Computing Supertree during iterative graph splitting (MultiThreaded)");

        while (graphs.size() > 0) {
            Stream<T> graphStream = graphs.parallelStream();

            final List<T> tmpGraphs = Collections.synchronizedList(new LinkedList<T>());
            graphStream.forEach(currentGraph -> {
                Collection<T> nuGraphs = splitGraph(currentGraph);
                if (nuGraphs != null) {
                    for (T graph : nuGraphs) {
                        // add the node
                        supertree.addVertex(graph.treeNode);
                        if (graph.parentNode != null)
                            supertree.addEdge(graph.parentNode, graph.treeNode);
                        tmpGraphs.add(graph);
                    }
                }
            });

            graphs = tmpGraphs;
        }

        return supertree;
    }

    private Collection<T> splitGraph(T initialGraph) {
        C cutter = createCutter(type);

        // init the graph (remove semi universals)
        initialGraph.deleteSemiUniversals();

        // check if we have just one taxon left
        if (initialGraph.taxa.size() == 1) {
            // the current node becomes the leaf
            initialGraph.treeNode.setLabel((initialGraph.taxa.iterator().next()).name);
            return null;
        } else {
            Set<T> outputGraphs = new HashSet<>(5);
            // get components
            List<List<N>> components = initialGraph.getComponents();
            if (components.size() == 1) {
                // just one component, we have to cut
                cutter.clear();
                List<T> componentGraphs = cutter.cut(initialGraph);
                //mincut value in graph needed?
                if (CALCULATE_SCORE) globalWeight += cutter.getMinCutValue(initialGraph);

                //Cut graph components
                for (T componentGraph : componentGraphs) {
                    if (initialGraph.SCAFF_TAXA_MERGE) {
                        componentGraph.insertScaffPartData(initialGraph, null);
                    }
                    if (initialGraph.GLOBAL_CHARACTER_MERGE)
                        componentGraph.insertCharacterMapping(initialGraph, null);
                    outputGraphs.add(componentGraph);
                }


            } else {
                // create the component graphs
                boolean checkEdges = (getCutterType() == CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
                for (List<N> component : components) {
                    T g = createGraph(component, initialGraph.treeNode, checkEdges);
                    //actualize scaffold partition data
                    if (initialGraph.SCAFF_TAXA_MERGE) {
                        g.insertScaffPartData(initialGraph, null);
                    }
                    if (initialGraph.GLOBAL_CHARACTER_MERGE)
                        g.insertCharacterMapping(initialGraph, null);
                    outputGraphs.add(g);
                    ;
                }
            }
            return outputGraphs;
        }
    }


    private Tree computeIterativeMultiThreaded() throws ExecutionException, InterruptedException {
        getLog().info("Computing Supertree during iterative graph splitting (SingleThreaded)");
        Tree supertree = null;

        int cores = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(cores);
//        executor = Executors.newCachedThreadPool();
//        executor = Executors.newWorkStealingPool();


        supertree = new Tree();
        supertree.addVertex(initialGraph.treeNode);
        supertree.setRoot(initialGraph.treeNode);

        results.offer(executor.submit(new GraphSplitterIterative(initialGraph)));

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
        }

        executor.shutdown();
        return supertree;
    }

    private Tree computeSTreeSingleThreaded() {
        final C cutter = createCutter(type);
        final Tree supertree = new Tree();

        Queue<T> graphs = new LinkedList<T>();
        graphs.offer(initialGraph);

        getLog().info("Computing Supertree during iterative graph splitting (SingleThreaded)");

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
                    List<T> componentGraphs = cutter.cut(initialGraph);
                    //mincut value in graph needed?
                    if (CALCULATE_SCORE) globalWeight += cutter.getMinCutValue(initialGraph);
                    if (DEBUG)
                        if (componentGraphs.size() > 2)
                            debugInfo.polytomies.add(componentGraphs.size());

                    //Cut graph components
                    for (T componentGraph : componentGraphs) {
                        if (initialGraph.SCAFF_TAXA_MERGE) {
                            componentGraph.insertScaffPartData(initialGraph, null);
                        }
                        if (initialGraph.GLOBAL_CHARACTER_MERGE)
                            componentGraph.insertCharacterMapping(initialGraph, null);
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
                    boolean checkEdges = (getCutterType() == CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
                    for (List<N> component : components) {
                        T g = createGraph(component, initialGraph.treeNode, checkEdges);
                        //actualize scaffold partition data
                        if (initialGraph.SCAFF_TAXA_MERGE) {
                            g.insertScaffPartData(initialGraph, null);
                        }
                        if (initialGraph.GLOBAL_CHARACTER_MERGE)
                            g.insertCharacterMapping(initialGraph, null);
                        graphs.offer(g);
                    }
                    if (DEBUG)
                        debugInfo.splittingTimes.add((System.currentTimeMillis() - debugInfo.currentStartTime) / 1000);
                }
            }
        }

        return supertree;
    }

    private Tree computeSTreeMultiThreaded() {
        Tree supertree = null;
        try {
            System.out.println("Starting iterative graph splitting (MultiThreaded)...");
            GraphSplitterRecursive rootTask = new GraphSplitterRecursive(initialGraph);
            final Collection<Object> treeStructure = executor.submit(rootTask).get();
            System.out.println("...DONE!");
            System.out.println("Building Supertree from partitions...");
            supertree = new Tree();
            createSupertree(supertree, treeStructure);
            System.out.println("...DONE!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return supertree;
    }


    private TreeNode createSupertree(final Tree tree, Collection<Object> treeStructure) {
        if (treeStructure.size() > 1) {
            TreeNode localRoot = new TreeNode();
            tree.addVertex(localRoot);
            if (tree.vertexCount() == 1)
                tree.setRoot(localRoot);
            for (Object o : treeStructure) {
                if (o instanceof TreeNode) {
                    TreeNode node = (TreeNode) o;
                    tree.addVertex(node);
                    tree.addEdge(localRoot, node);
                } else {
                    TreeNode child = createSupertree(tree, (Collection<Object>) o);
                    tree.addEdge(localRoot, child);
                }
            }
            return localRoot;
        } else {
            return (TreeNode) treeStructure.iterator().next();
        }
    }

    private Map<TreeNode, Set<String>> getNodeToChildrenMap(final Tree tree) {
        Map<TreeNode, Set<String>> childrenSets = new HashMap<>();
        for (TreeNode node : tree.vertices()) {
            if (node.isInnerNode()) {
                if (node != tree.getRoot()) {
                    childrenSets.put(node, TreeUtilsBasic.getLeafLabels(node));
                }
            }
        }
        return childrenSets;
    }

    protected abstract T createGraph(List<N> component, TreeNode treeNode, final boolean checkEdges);

    protected abstract C createCutter(CutGraphCutter.CutGraphTypes type);

    public void setCutter(CutGraphCutter.CutGraphTypes type) {
        this.type = type;
    }

    public CutGraphCutter.CutGraphTypes getCutterType() {
        return type;
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

    private class GraphSplitterRecursive implements Callable<Collection<Object>> {
        final T currentGraph;

        GraphSplitterRecursive(final T currentGraph) {
            this.currentGraph = currentGraph;
        }

        @Override
        public Collection<Object> call() throws Exception {
            // init the graph (remove semi universals)
            currentGraph.deleteSemiUniversals();
            // check if we have just one taxon left
            final Collection<Object> childrenNodes = new HashSet<>();


            if (currentGraph.taxa.size() == 1) {
                // the current node becomes the leaf
                currentGraph.treeNode.setLabel((currentGraph.taxa.iterator().next()).name);
                childrenNodes.add(currentGraph.treeNode);
                // check if we have just two taxa left --> cut is trivial
            } else if (currentGraph.taxa.size() == 2) {
                for (N taxon : currentGraph.taxa) {
                    TreeNode t = new TreeNode(taxon.name);
                    childrenNodes.add(t);
                }
            } else {
                final List<GraphSplitterRecursive> toSubmit = new LinkedList<>();
                // get components
                List<List<N>> components = currentGraph.getComponents();
                if (components.size() == 1) {
                    // just one component, we have to cut
//                    cutter.clear();
                    C cutter = createCutter(getCutterType());
                    List<T> componentGraphs = cutter.cut(currentGraph);
                    //mincut value in graph needed?
                    if (CALCULATE_SCORE) globalWeight += cutter.getMinCutValue(currentGraph);

                    //Cut graph components
                    for (T componentGraph : componentGraphs) {
                        if (currentGraph.SCAFF_TAXA_MERGE) {
                            componentGraph.insertScaffPartData(currentGraph, null);
                        }
                        if (currentGraph.GLOBAL_CHARACTER_MERGE)
                            componentGraph.insertCharacterMapping(currentGraph, null);
                        //graphs.offer(componentGraph);
                        toSubmit.add(new GraphSplitterRecursive(componentGraph));
                    }
                } else {

                    // create the component graphs
                    boolean checkEdges = (getCutterType() == CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
                    for (List<N> component : components) {
                        T g = createGraph(component, currentGraph.treeNode, checkEdges);
                        //actualize scaffold partition data
                        if (currentGraph.SCAFF_TAXA_MERGE) {
                            g.insertScaffPartData(currentGraph, null);
                        }
                        if (currentGraph.GLOBAL_CHARACTER_MERGE)
                            g.insertCharacterMapping(currentGraph, null);

                        toSubmit.add(new GraphSplitterRecursive(g));
                    }
                }

                //submitting subtasks
                final List<Future<Collection<Object>>> splitTasks = new LinkedList<>();
                for (GraphSplitterRecursive graphSplitter : toSubmit) {
                    splitTasks.add(executor.submit(graphSplitter));
                }
                //waiting until sub tasks are finished
                for (Future<Collection<Object>> splitTask : splitTasks) {
                    childrenNodes.add(splitTask.get());
                }
            }

            return childrenNodes;
        }
    }


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
                C cutter = createCutter(type);//todo get cutter from cutter pool

                Queue<TreeNode> nodes = new LinkedList<>();
                nodes.offer(currentGraph.treeNode);
                List<GraphSplitterIterative> outputGraphs = new LinkedList<>();
                // get components
                List<List<N>> components = currentGraph.getComponents();
                if (components.size() == 1) {
                    // just one component, we have to cut
                    cutter.clear();
                    List<T> componentGraphs = cutter.cut(currentGraph);
                    //mincut value in graph needed?
                    if (CALCULATE_SCORE) globalWeight += cutter.getMinCutValue(currentGraph);

                    //Cut graph components
                    for (T componentGraph : componentGraphs) {
                        if (currentGraph.SCAFF_TAXA_MERGE) {
                            componentGraph.insertScaffPartData(currentGraph, null);
                        }
                        if (currentGraph.GLOBAL_CHARACTER_MERGE)
                            componentGraph.insertCharacterMapping(currentGraph, null);
                        nodes.add(componentGraph.treeNode);
                        results.offer(executor.submit(new GraphSplitterIterative(componentGraph)));
                    }
                } else {
                    // create the component graphs
                    boolean checkEdges = (getCutterType() == CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
                    for (List<N> component : components) {
                        T g = createGraph(component, currentGraph.treeNode, checkEdges);
                        //actualize scaffold partition data
                        if (currentGraph.SCAFF_TAXA_MERGE) {
                            g.insertScaffPartData(currentGraph, null);
                        }
                        if (currentGraph.GLOBAL_CHARACTER_MERGE)
                            g.insertCharacterMapping(currentGraph, null);
                        nodes.add(g.treeNode);
                        results.offer(executor.submit(new GraphSplitterIterative(g)));
                    }
                }
                return nodes;
            }
        }
    }
}
