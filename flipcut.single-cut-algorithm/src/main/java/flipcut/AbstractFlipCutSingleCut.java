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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:42
 */
public abstract class AbstractFlipCutSingleCut<N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>, C extends CutGraphCutter<N,T>> extends AbstractFlipCut<N,T> {
    private static final boolean CALCULATE_SCORE = true;
//    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public DebugInfo debugInfo;
    private long globalWeight;
    protected C cutter;

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

            if (DEBUG){
                debugInfo = new DebugInfo();
                debugInfo.overallCalculationTime = System.currentTimeMillis();
            }
            if (CALCULATE_SCORE)
                globalWeight = 0;

            System.out.println("Computing Supertree...");


//            Queue<T> graphs = new LinkedList<T>();
//            graphs.offer(initialGraph);

            System.out.println("Starting iterative graph splitting to compute Supertree...");
//            while(graphs.size() > 0){
//                initialGraph = graphs.poll();
//            }

            final Collection<Object> treeStructure = splitGraph(initialGraph);
            System.out.println("...DONE!");
            System.out.println("Building Supertree form partitions...");
            final Tree supertree =  new Tree();
            createSupertree(supertree,treeStructure);


            if (CALCULATE_SCORE)supertree.setName("Tree_" + globalWeight);
            if (DEBUG){
                debugInfo.weight = globalWeight;
                debugInfo.overallCalculationTime = (System.currentTimeMillis() - debugInfo.overallCalculationTime)/1000;
            }
            System.out.println("...DONE!");
            return supertree;
        } else {
            throw new IllegalArgumentException("No inputTrees found");
        }
    }


    private TreeNode createSupertree(final Tree tree, Collection<Object> treeStructure) {
        if (treeStructure.size() > 1 ){
            TreeNode localRoot = new TreeNode();
            tree.addVertex(localRoot);
            if (tree.vertexCount() == 1)
                tree.setRoot(localRoot);
            for (Object o : treeStructure) {
                if (o instanceof TreeNode){
                    TreeNode node = (TreeNode)o;
                    tree.addVertex(node);
                    tree.addEdge(localRoot,node);
                }else{
                    TreeNode child = createSupertree(tree,(Collection<Object>)o);
                    tree.addEdge(localRoot,child);
                }
            }
            return localRoot;
        }else{
            return (TreeNode)treeStructure.iterator().next();
        }
    }

    private Collection<Object> splitGraph(final T currentGraph ){
        // add the node //todo try to do outside
//        supertree.addVertex(currentGraph.treeNode);
//        if(currentGraph.parentNode != null)
//            supertree.addEdge(currentGraph.parentNode, currentGraph.treeNode);

        // init the graph (remove semi universals)
        currentGraph.deleteSemiUniversals();

        // check if we have just one taxon left
        final Collection<Object> childrenNodes =  new HashSet<>();

        if(currentGraph.taxa.size() == 1){
            // the current node becomes the leaf
            currentGraph.treeNode.setLabel((currentGraph.taxa.iterator().next()).name);
            childrenNodes.add(currentGraph.treeNode);
            // check if we have just two taxa left --> cut is trivial
        }else if(currentGraph.taxa.size() == 2){
            //System.out.println("################ TRIVAL CUT ################");
            for (N taxon : currentGraph.taxa) {
                TreeNode t = new TreeNode(taxon.name);
                childrenNodes.add(t);

                //todo try to do outside
//                supertree.addVertex(t);
//                supertree.addEdge(currentGraph.treeNode,t);
            }
        }else{
            // get components
            List<List<N>> components = currentGraph.getComponents();
            if(components.size() == 1){
                if (DEBUG)
                    debugInfo.currentStartTime = System.currentTimeMillis();

                // just one component, we have to cut
                cutter.clear();
                List<T> componentGraphs = cutter.cut(currentGraph);
                //mincut value in graph needed?
                if(CALCULATE_SCORE) globalWeight  += cutter.getMinCutValue(currentGraph);
                if (DEBUG)
                    if (componentGraphs.size() > 2)
                        debugInfo.polytomies.add(componentGraphs.size());

                //Cut graph components
                for (T componentGraph : componentGraphs) {
                    if (currentGraph.SCAFF_TAXA_MERGE){
                        componentGraph.insertScaffPartData(currentGraph,null);
                    }
                    if (currentGraph.GLOBAL_CHARACTER_MERGE)
                        componentGraph.insertCharacterMapping(currentGraph,null);
                    //graphs.offer(componentGraph);
                    childrenNodes.add(splitGraph(componentGraph));
                }

                if (DEBUG)
                    debugInfo.cuttingTimes.add((System.currentTimeMillis() - debugInfo.currentStartTime)/1000);
            }else{
                if (DEBUG)
                    if (components.size() > 2)
                        debugInfo.polytomies.add(components.size());
                if (DEBUG)
                    debugInfo.currentStartTime = System.currentTimeMillis();
                // create the component graphs
                boolean checkEdges = (getCutterType() == CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
                for (List<N> component : components) {
                    T g = createGraph(component, currentGraph.treeNode, checkEdges);
                    //actualize scaffold partition data
                    if (currentGraph.SCAFF_TAXA_MERGE){
                        g.insertScaffPartData(currentGraph,null);
                    }
                    if (currentGraph.GLOBAL_CHARACTER_MERGE)
                        g.insertCharacterMapping(currentGraph,null);
//                    graphs.offer(g);
                    childrenNodes.add(splitGraph(g));
                }
                if (DEBUG)
                    debugInfo.splittingTimes.add((System.currentTimeMillis() - debugInfo.currentStartTime)/1000);
            }
        }
        return childrenNodes;
    }
    private Map<TreeNode,Set<String>> getNodeToChildrenMap(final Tree tree) {
        Map<TreeNode,Set<String>> childrenSets = new HashMap<>();
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
    public void setCutter(CutGraphCutter.CutGraphTypes type){
        cutter = createCutter(type);
    }
    public CutGraphCutter.CutGraphTypes getCutterType(){
        if (cutter == null)
            return null;
        return cutter.getType();
    }


    public class DebugInfo{
        double currentStartTime;
        List<Double> cuttingTimes = new LinkedList<>();
        List<Double> splittingTimes = new LinkedList<>();
        double overallCalculationTime;
        double mergingMapCreateTime = 0d;
        long weight;
        List<Integer> polytomies =  new LinkedList<>();;

        public int getNumberOfCuts(){
            return cuttingTimes.size();
        }
        public int getNumberOfNonCutSplits(){
            return splittingTimes.size();
        }

        public double getCuttingTime(){
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

        public String printDebugInfo(){
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
               cutTime.append(cuttingTime +" ");
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
                cutTime.append(cuttingTime +" ");
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
                cutTime.append(cuttingTime +" ");
            }
            cutTime.append(")");
            debugInfo.append(cutTime.toString());
            debugInfo.append("\n");
            debugInfo.append("\n");
            debugInfo.append("########## END DEBUG INFO ##########");

            return debugInfo.toString();
        }
    }

    /*private class GraphSplitter implements Callable<Collection<Object>>{

        final T currentGraph;
        GraphSplitter(final T currentGraph){
            this.currentGraph = currentGraph;
        }

        @Override
        public Collection<Object> call() throws Exception {
            return null;
        }
    }*/
}
