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

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:42
 */
public abstract class AbstractFlipCutSingleCut<N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>, C extends CutGraphCutter<N,T>> extends AbstractFlipCut<N,T> {
    private static final boolean CALCULATE_SCORE = true;

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
        if (currentGraph != null) {

            if (DEBUG){
                debugInfo = new DebugInfo();
                debugInfo.overallCalculationTime = System.currentTimeMillis();
            }
            if (CALCULATE_SCORE)
                globalWeight = 0;

            getLog().info("Computing FS Supertree");
            //build static node merge map for initial graph
            if (cutter.mergeCharacters && cutter.staticCharacterMap){
                if (DEBUG)
                    debugInfo.currentStartTime = System.currentTimeMillis();

                cutter.buildInitialCharacterMergingMap(currentGraph);

                if (DEBUG)
                    debugInfo.mergingMapCreateTime = (System.currentTimeMillis() - debugInfo.currentStartTime)/1000 ;
            }
            Tree supertree = new Tree();

            Queue<T> graphs = new LinkedList<T>();
            graphs.offer(currentGraph);

            //remove identical Nodes. That means complete identical Matrix columns of the Nodes that are from trees with same taxon set
            //maybe useful for MrBayes input trees, but maybe this becomes nearly useless because of undisputed sibling reduction
            if (removeDuplets){
                getLog().info("Merging identical characters");
                int removed = currentGraph.mergeRetundantCharacters();
                /*if (DEBUG)*/ System.out.println(removed + " characters removed!");
                getLog().info(removed + " characters removed!");
            }

            getLog().info("Starting iterative graph splitting to compute Supertree");
            while(graphs.size() > 0){
                currentGraph = graphs.poll();

                // add the node
                supertree.addVertex(currentGraph.treeNode);
                if(currentGraph.parentNode != null)
                    supertree.addEdge(currentGraph.parentNode, currentGraph.treeNode);

                // init the graph (remove semi universals)
                if (cutter.mergeCharacters && cutter.staticCharacterMap){
                    //todo maybe do this only before cutting
                    List<N> toRemove = currentGraph.deleteSemiUniversals();
                    for (N node : toRemove) {
                        cutter.removeNodeFromMergeSet(node);
                    }
                }else{
                    currentGraph.deleteSemiUniversals();
                }


                // check if we have just one taxon left
                if(currentGraph.taxa.size() == 1){
                    // the current node becomes the leaf
                    currentGraph.treeNode.setLabel((currentGraph.taxa.get(0)).name);
                // check if we have just two taxa left --> cut is trivial
                }else if(currentGraph.taxa.size() == 2){
                    //System.out.println("################ TRIVAL CUT ################");
                    for (N taxon : currentGraph.taxa) {
                        TreeNode t = new TreeNode(taxon.name);
                        supertree.addVertex(t);
                        supertree.addEdge(currentGraph.treeNode,t);
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
                        if(CALCULATE_SCORE) globalWeight = globalWeight + cutter.getMinCutValue(currentGraph);
                        if (DEBUG)
                            if (componentGraphs.size() > 2)
                                debugInfo.polytomies.add(componentGraphs.size());

                        //Cut graph components
                        for (T componentGraph : componentGraphs) {
                            if (currentGraph.SCAFF_TAXA_MERGE){
                                componentGraph.insertScaffPartData(currentGraph,null);
                            }
                            graphs.offer(componentGraph);
                        }

                        if (DEBUG)
                            debugInfo.cuttingTimes.add((System.currentTimeMillis() - debugInfo.currentStartTime)/1000);
                    }else{
//                        if (components.size() > 2){
//                            //todo do recursive pairwise extended semi universal search
//                        }
                        if (DEBUG)
                            if (components.size() > 2)
                                debugInfo.polytomies.add(components.size());
                        if (DEBUG)
                            debugInfo.currentStartTime = System.currentTimeMillis();
                        // create the component graphs
                        for (List<N> component : components) {
                            T g = createGraph(component, currentGraph.treeNode);
                            //actualize scaffold partition data
                            if (currentGraph.SCAFF_TAXA_MERGE){
                                g.insertScaffPartData(currentGraph,null);
                            }
                            graphs.offer(g);
                        }
                        if (DEBUG)
                            debugInfo.splittingTimes.add((System.currentTimeMillis() - debugInfo.currentStartTime)/1000);
                    }
                }
            }
            if (CALCULATE_SCORE)supertree.setName("Tree_" + globalWeight);
            if (DEBUG){
                debugInfo.weight = globalWeight;
                debugInfo.overallCalculationTime = (System.currentTimeMillis() - debugInfo.overallCalculationTime)/1000;
            }

            /*if (PP){
                System.out.println("starting PP to delete unsupported clades");
                pp(supertree);

            }*/

            return supertree;
        } else {
            throw new IllegalArgumentException("No inputTrees found");
        }
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

    protected abstract T createGraph(List<N> component, TreeNode treeNode);
    protected abstract C createCutter(CutGraphCutter.CutGraphTypes type);
    public void setCutter(CutGraphCutter.CutGraphTypes type){
        cutter = createCutter(type);
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
}
