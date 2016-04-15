package flipcut.flipCutGraph;


import flipcut.costComputer.CostComputer;
import flipcut.model.Cut;
import phyloTree.model.tree.TreeNode;

import java.util.*;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 17:59
 */

public class FlipCutGraphMultiSimpleWeight extends FlipCutGraphSimpleWeight {
    public static final boolean VAZIRANI = false;

    private int maxCutNumber;
    private int nextCutIndexToCalculate;
    private final Cut[] cuts;
    private final MultiCutter cutter;

    //todo k needed?
    public FlipCutGraphMultiSimpleWeight(CostComputer costs, int k, CutGraphCutter.CutGraphTypes cutterType) {
        super(costs,0); //todo dummy bootstrapthreshold --> implement it
        if (VAZIRANI) {
            this.cutter = new MultiCutGraphCutter(cutterType,this);
        } else {
            //System.out.println("NO_Vazi");
            this.cutter = new MultiCutGraphCutterGreedy(cutterType,this);
        }
        maxCutNumber = k;
        cuts = new Cut[maxCutNumber];
        nextCutIndexToCalculate = 0;
    }

    protected FlipCutGraphMultiSimpleWeight(LinkedHashSet<FlipCutNodeSimpleWeight> characters, LinkedHashSet<FlipCutNodeSimpleWeight> taxa, TreeNode parentNode, int k, CutGraphCutter.CutGraphTypes cutterType) {
        super(characters, taxa, parentNode);
        if (VAZIRANI) {
            this.cutter = new MultiCutGraphCutter(cutterType,this);
        } else {
            //System.out.println("NO_Vazi");
            this.cutter = new MultiCutGraphCutterGreedy(cutterType,this);
        }
        maxCutNumber = k;
        cuts = new Cut[maxCutNumber];
        nextCutIndexToCalculate = 0;
    }

    public FlipCutGraphMultiSimpleWeight(List<FlipCutNodeSimpleWeight> nodes, TreeNode parentNode, int k, CutGraphCutter.CutGraphTypes cutterType) {
        super(nodes, parentNode, (cutterType == CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG));
        if (VAZIRANI) {
            this.cutter = new MultiCutGraphCutter(cutterType,this);
        } else {
            //System.out.println("NO_Vazi");
            this.cutter = new MultiCutGraphCutterGreedy(cutterType,this);
        }
        maxCutNumber = k;
        cuts = new Cut[maxCutNumber];
        nextCutIndexToCalculate = 0;
    }


    public Iterator<Cut> getCutIterator(){
        return new CutIterator();
    }


    private boolean calculateNextCut() {
        Cut c = cutter.getNextCut();
        if (c != null) {
            cuts[nextCutIndexToCalculate] = c;
            nextCutIndexToCalculate++;
            return true;
        } else {
            maxCutNumber = nextCutIndexToCalculate;
            return false;
        }
    }

    @Override
    public List<? extends FlipCutGraphMultiSimpleWeight> split(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes) {
        Map<FlipCutNodeSimpleWeight,FlipCutNodeSimpleWeight> oldToNew = copyNodes();
        List<List<LinkedHashSet<FlipCutNodeSimpleWeight>>> graphData = splitToGraphData(new LinkedHashSet<>(sinkNodes), oldToNew);
        List<FlipCutGraphMultiSimpleWeight> graphs = new LinkedList<FlipCutGraphMultiSimpleWeight>();
        graphs.add(new FlipCutGraphMultiSimpleWeight(graphData.get(0).get(0),graphData.get(0).get(1),treeNode, cuts.length, cutter.getType()));
        graphs.add(new FlipCutGraphMultiSimpleWeight(graphData.get(1).get(0),graphData.get(1).get(1),treeNode, cuts.length, cutter.getType()));
        if (SCAFF_TAXA_MERGE) {
            for (FlipCutGraphMultiSimpleWeight graph : graphs) {
                graph.insertScaffPartData(this,oldToNew);
            }
        }
        return graphs;
    }

    //overwritten --> creates cloned nodes for multiple splits
//    @Override
    protected List<List<LinkedHashSet<FlipCutNodeSimpleWeight>>> splitToGraphData(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes, final Map<FlipCutNodeSimpleWeight,FlipCutNodeSimpleWeight> oldToNew) {


        LinkedHashSet<FlipCutNodeSimpleWeight> g1Characters = new LinkedHashSet<>();
        LinkedHashSet<FlipCutNodeSimpleWeight> g1Taxa = new LinkedHashSet<>();
        LinkedHashSet<FlipCutNodeSimpleWeight> g2Characters = new LinkedHashSet<>();
        LinkedHashSet<FlipCutNodeSimpleWeight> g2Taxa = new LinkedHashSet<>();

        // check if we have to remove vertices
        List<FlipCutNodeSimpleWeight> preCharactersToRemove = checkRemoveCharacter(sinkNodes);
        List<FlipCutNodeSimpleWeight> charactersToRemove = new ArrayList<FlipCutNodeSimpleWeight>(preCharactersToRemove.size());
        for (FlipCutNodeSimpleWeight toRemove : preCharactersToRemove) {
            charactersToRemove.add(oldToNew.get(toRemove));
        }

        //remove all clone nodes
        Iterator<FlipCutNodeSimpleWeight> iterator = sinkNodes.iterator();
        while (iterator.hasNext()) {
            FlipCutNodeSimpleWeight n = iterator.next();
            if (n.isClone()) {
                iterator.remove();
            }
        }

        //fill g1 lists
        for (FlipCutNodeSimpleWeight node : sinkNodes) {
            if (node.isTaxon()) {
                g1Taxa.add(oldToNew.get(node));
            } else {
                g1Characters.add(oldToNew.get(node));
            }
        }


        // fill g2 lists
        for (FlipCutNodeSimpleWeight character : characters) {
            g2Characters.add(oldToNew.get(character));
        }
        g2Characters.removeAll(g1Characters);

        for (FlipCutNodeSimpleWeight taxon : taxa) {
            g2Taxa.add(oldToNew.get(taxon));
        }
        g2Taxa.removeAll(g1Taxa);


        //todo maybe use iterator and delete removed from toRemove list
        // remove characters from g1 if we have to remove any
        removeCharacters(charactersToRemove,g1Characters);

        // remove characters from g2 if we have to remove any
        removeCharacters(charactersToRemove,g2Characters);

        // remove all edges between g2 characters and g1 taxa
        removeEdgesToOtherGraph(g2Characters, g1Taxa);

        // remove edges between g1 characters and g2 taxa
        removeEdgesToOtherGraph(g1Characters, g2Taxa);

        // create the sub graphs
        List<LinkedHashSet<FlipCutNodeSimpleWeight>> g1 = Arrays.asList(g1Characters, g1Taxa);
        List<LinkedHashSet<FlipCutNodeSimpleWeight>> g2 = Arrays.asList(g2Characters, g2Taxa);

        return Arrays.asList(g1, g2);
    }

    public List<FlipCutNodeSimpleWeight> checkRemoveCharacter(Set<FlipCutNodeSimpleWeight> sinkNodes){
        List<FlipCutNodeSimpleWeight> charactersToRemove = new LinkedList<FlipCutNodeSimpleWeight>();
        for (FlipCutNodeSimpleWeight node : sinkNodes) {
            if (!node.isTaxon()) {
                // it is character or a character clone
                // check if the other one is also in the set
                if (!sinkNodes.contains(node.clone)) {
                    FlipCutNodeSimpleWeight c = node.isClone() ? node.clone : node;
                    charactersToRemove.add(c);
                }
            }
        }
        return charactersToRemove;
    }

    public CutGraphCutter.CutGraphTypes getCutterType() {
        return cutter.getType();
    }

    public List<FlipCutGraphMultiSimpleWeight> buildComponentGraphs(List<List<FlipCutNodeSimpleWeight>> comp) {
        List<FlipCutGraphMultiSimpleWeight> splittedGraphs = new ArrayList<FlipCutGraphMultiSimpleWeight>(comp.size());
        for (List<FlipCutNodeSimpleWeight> component : comp) {
            //clone all nodes
            Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew = new HashMap<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight>(characters.size() + taxa.size());

            for (FlipCutNodeSimpleWeight node : component) {
                oldToNew.put(node, node.copy());
            }

            for (FlipCutNodeSimpleWeight node : component) {
                if (!node.isTaxon()) {
                    FlipCutNodeSimpleWeight nClone = oldToNew.get(node);
                    for (FlipCutNodeSimpleWeight taxon : node.edges) {
                        FlipCutNodeSimpleWeight tClone = oldToNew.get(taxon);
                        nClone.addEdgeTo(tClone);
                        tClone.addEdgeTo(nClone);
                    }
                    for (FlipCutNodeSimpleWeight taxon : node.imaginaryEdges) {
                        FlipCutNodeSimpleWeight tClone = oldToNew.get(taxon);
                        if (tClone != null) {
                            nClone.addImaginaryEdgeTo(tClone);
                        }else{
                            //todo check!!!
                            //nClone.characterWeight -= nClone.edgeWeight;
                        }
                    }
                }
            }
            FlipCutGraphMultiSimpleWeight g = new FlipCutGraphMultiSimpleWeight(new ArrayList<FlipCutNodeSimpleWeight>(oldToNew.values()),treeNode,cuts.length,getCutterType());
            if (SCAFF_TAXA_MERGE )
                g.insertScaffPartData(this,oldToNew);
            splittedGraphs.add(g);

        }
        return splittedGraphs;
    }

    public boolean containsCuts() {
        return nextCutIndexToCalculate > 0;
    }

    private Cut getCompCut(List<List<FlipCutNodeSimpleWeight>> comp){
        return new Cut(comp,this);
    }

    class CutIterator implements Iterator<Cut>{
        CutIterator() {
            //check if graph is already disconnected
            if (nextCutIndexToCalculate == 0 && maxCutNumber == cuts.length){
                List<List<FlipCutNodeSimpleWeight>> comp = getComponents();
                if (comp.size() > 1){//graph is already disconnected we have to build subgraph from parts
                    cuts[nextCutIndexToCalculate] = getCompCut(comp);
                    nextCutIndexToCalculate++;
                    maxCutNumber = nextCutIndexToCalculate;
                }
            }
        }

        int index = 0;
        public boolean hasNext() {
            if (index >= nextCutIndexToCalculate){
                if (nextCutIndexToCalculate < maxCutNumber) {
                    return calculateNextCut();
                } else {
                    return false;
                }
            }
            return true;
        }

        public Cut next() {
            if (cuts[index] == null)
                calculateNextCut();
            return cuts[index++];

        }

        /*ATTENTION this method is not supported!!!*/
        public void remove() {
            try {
                throw new NoSuchMethodException("Remove operation is not Supported for this Iterator");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }



}