package phylo.tree.algorithm.flipcut.flipCutGraph;


import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;
import phylo.tree.algorithm.flipcut.model.MultiCut;
import phylo.tree.model.TreeNode;

import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 17:59
 */

public class FlipCutGraphMultiSimpleWeight extends FlipCutGraphSimpleWeight {
    private int numTaxaAfterClose = -1;
    private int maxCutNumber;
    private int nextCutIndexToCalculate;
    private Set<MultiCut> splittedCuts; //todo do we rellay need this cleanup to support GC?
    private MultiCut[] cuts;
    private MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> cutter = null;
    private MultiCutterFactory<MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> cutterFactory;


    public FlipCutGraphMultiSimpleWeight(CostComputer costs, int k, MultiCutterFactory<MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> cutterFactory) {
        super(costs, 0); //todo dummy bootstrapthreshold --> implement it
        this.cutterFactory = cutterFactory;
        maxCutNumber = k;
        cuts = new MultiCut[maxCutNumber];
        splittedCuts = new HashSet<>(maxCutNumber);
        nextCutIndexToCalculate = 0;
    }

    protected FlipCutGraphMultiSimpleWeight(LinkedHashSet<FlipCutNodeSimpleWeight> characters, LinkedHashSet<FlipCutNodeSimpleWeight> taxa, TreeNode parentNode, int k, MultiCutterFactory<MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> cutterFactory) {
        super(characters, taxa, parentNode);
        this.cutterFactory = cutterFactory;
        maxCutNumber = k;
        cuts = new MultiCut[maxCutNumber];
        splittedCuts = new HashSet<>(maxCutNumber);
        nextCutIndexToCalculate = 0;
    }

    public FlipCutGraphMultiSimpleWeight(List<FlipCutNodeSimpleWeight> nodes, TreeNode parentNode, int k, MultiCutterFactory<MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> cutterFactory, boolean edgeDeletion) {
        super(nodes, parentNode, edgeDeletion);
        this.cutterFactory = cutterFactory;
        maxCutNumber = k;
        cuts = new MultiCut[maxCutNumber];
        splittedCuts = new HashSet<>(maxCutNumber);
        nextCutIndexToCalculate = 0;
    }


    public Iterator<MultiCut> getCutIterator() {
        return new CutIterator();
    }

    public void setCutSplitted(MultiCut c) {
        splittedCuts.add(c);
    }

    private boolean calculateNextCut() {
        if (nextCutIndexToCalculate < maxCutNumber) {
            if (cutter == null)
                cutter = cutterFactory.newInstance(this);


            MultiCut c = cutter.getNextCut();
            if (c != null) {
                cuts[nextCutIndexToCalculate++] = c;
                if (nextCutIndexToCalculate >= maxCutNumber) {
                    disableCutting();
                }
                return true;
            }
            maxCutNumber = nextCutIndexToCalculate;
        }
        disableCutting();
        return false;
    }

    private void disableCutting() {
//        System.out.println("Disable cutting for this Graph: " + this.toString());
        if (cutter != null) cutter.clear();
        cutter = null;
        characterToDummy = null;
        dummyToCharacters = null;
    }

    @Override
    public List<? extends FlipCutGraphMultiSimpleWeight> split(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes) {
        Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew = copyNodes();
        List<List<LinkedHashSet<FlipCutNodeSimpleWeight>>> graphData = splitToGraphData(new LinkedHashSet<>(sinkNodes), oldToNew);

        List<FlipCutGraphMultiSimpleWeight> graphs = new LinkedList<>();
        for (List<LinkedHashSet<FlipCutNodeSimpleWeight>> data : graphData) {
            FlipCutGraphMultiSimpleWeight graph = new FlipCutGraphMultiSimpleWeight(data.get(0), data.get(1), treeNode, cuts.length, cutterFactory);

            if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE)
                graph.insertScaffPartData(this, oldToNew);

            if (DEBUG)
                checkGraph(graph);

            graphs.add(graph);
        }

        return graphs;
    }

    // debug method
    private void checkGraph(FlipCutGraphMultiSimpleWeight graph) {
        for (FlipCutNodeSimpleWeight n : graph.characterToDummy.keySet()) {
            FlipCutNodeSimpleWeight node = n.isClone() ? n.clone : n;
            if (graph.characterToDummy.keySet().size() != 2 * graph.characters.size())
                System.out.println("not all chars in map");
            if (!graph.characters.contains(node)) {
                System.out.println("Character not in graph: " + node);
            }
            if (!graph.taxa.containsAll(node.edges)) {
                System.out.println("at least one edge not in graph " + getSortedEdges(node.edges));
            }

            if (!getSortedEdges(node.edges).equals(getSortedEdges(graph.characterToDummy.get(node).edges)))
                System.out.println("dummy edges do not match character edges");
        }

        for (Map.Entry<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> entry : graph.dummyToCharacters.entrySet()) {
            if (!entry.getKey().isClone()) {
                Set<FlipCutNodeSimpleWeight> sets = entry.getValue();
                if (!graph.characters.containsAll(sets)) {
                    System.out.println("at least on char not in graph " + sets);
                }
            }
        }
    }

    //overwritten --> creates cloned nodes for multiple splits
    protected List<List<LinkedHashSet<FlipCutNodeSimpleWeight>>> splitToGraphData(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes, final Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew) {
        LinkedHashSet<FlipCutNodeSimpleWeight> g1Characters = new LinkedHashSet<>();
        LinkedHashSet<FlipCutNodeSimpleWeight> g1Taxa = new LinkedHashSet<>();
        LinkedHashSet<FlipCutNodeSimpleWeight> g2Characters = new LinkedHashSet<>();
        LinkedHashSet<FlipCutNodeSimpleWeight> g2Taxa = new LinkedHashSet<>();

        // check if we have to remove vertices
        List<FlipCutNodeSimpleWeight> preCharactersToRemove = checkRemoveCharacter(sinkNodes);

        List<FlipCutNodeSimpleWeight> charactersToRemove = new ArrayList<>(preCharactersToRemove.size());
        for (FlipCutNodeSimpleWeight toRemove : preCharactersToRemove) {
            FlipCutNodeSimpleWeight toRemoveNew = oldToNew.get(toRemove);
            charactersToRemove.add(toRemoveNew);
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
        removeCharacters(charactersToRemove, g1Characters);

        // remove characters from g2 if we have to remove any
        removeCharacters(charactersToRemove, g2Characters);

        // remove all edges between g2 characters and g1 taxa
        removeEdgesToOtherGraph(g2Characters, g1Taxa);

        // remove edges between g1 characters and g2 taxa
        removeEdgesToOtherGraph(g1Characters, g2Taxa);

        // create the sub graphs
        List<LinkedHashSet<FlipCutNodeSimpleWeight>> g1 = Arrays.asList(g1Characters, g1Taxa);
        List<LinkedHashSet<FlipCutNodeSimpleWeight>> g2 = Arrays.asList(g2Characters, g2Taxa);

        return Arrays.asList(g1, g2);
    }

    public List<FlipCutNodeSimpleWeight> checkRemoveCharacter(Set<FlipCutNodeSimpleWeight> sinkNodes) {
        if (DEBUG) System.out.println("===================");
        List<FlipCutNodeSimpleWeight> charactersToRemove = new LinkedList<FlipCutNodeSimpleWeight>();
        for (FlipCutNodeSimpleWeight node : sinkNodes) {
            if (!node.isTaxon()) {
                // it is character or a character clone
                // check if the other one is also in the set
                if (!sinkNodes.contains(node.clone)) {
                    FlipCutNodeSimpleWeight c = node.isClone() ? node.clone : node;
                    if (DEBUG)
                        System.out.println("--> remove char: " + c.toString() + " with taxa: " + getSortedEdges(c.edges));
                    charactersToRemove.add(c);
                } else if (DEBUG) {//todo debug thing
                    FlipCutNodeSimpleWeight c = node.isClone() ? node.clone : node;
                    System.out.println("--> keep char: " + c.toString() + " with taxa: " + getSortedEdges(c.edges));
                    int count = 0;
                    for (FlipCutNodeSimpleWeight tax : c.edges) {
                        if (!sinkNodes.contains(tax)) {
                            System.out.println("===================this is strange");
                            count++;
                        }
                    }
                    if (count > 0)
                        System.out.println(count + " / " + c.edges.size());
                }
            }
        }
        if (DEBUG) System.out.println("===================");
        return charactersToRemove;
    }

    public List<FlipCutGraphMultiSimpleWeight> buildComponentGraphs(List<List<FlipCutNodeSimpleWeight>> comp) {
        List<FlipCutGraphMultiSimpleWeight> splittedGraphs = new ArrayList<FlipCutGraphMultiSimpleWeight>(comp.size());
        for (List<FlipCutNodeSimpleWeight> component : comp) {
            //clone all nodes
            Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew = new HashMap<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight>(component.size());
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
                        }
                    }
                }
            }

            FlipCutGraphMultiSimpleWeight g;
            if (cutterFactory instanceof MaxFlowCutterFactory) {
                g = new FlipCutGraphMultiSimpleWeight(new ArrayList<>(oldToNew.values()), treeNode, cuts.length, cutterFactory, ((MaxFlowCutterFactory) cutterFactory).getType().isFlipCut());
            } else {
                g = new FlipCutGraphMultiSimpleWeight(new ArrayList<>(oldToNew.values()), treeNode, cuts.length, cutterFactory, false);
            }

            if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE)
                g.insertScaffPartData(this, oldToNew);

            if (DEBUG)
                checkGraph(g);
            splittedGraphs.add(g);

        }
        return splittedGraphs;
    }

    public void close() {
        if (splittedCuts != null && splittedCuts.size() == maxCutNumber) {
            disableCutting();
            cutterFactory = null;
            characters = null;
            scaffoldCharacterMapping = null;
            numTaxaAfterClose = taxa.size();
            taxa = null;
//            cuts = null;
            splittedCuts = null;
        }
    }

    public int getNumTaxa() {
        if (numTaxaAfterClose >= 0)
            return numTaxaAfterClose;
        return taxa.size();
    }

    public boolean containsCuts() {
        return nextCutIndexToCalculate > 0;
    }

    public int getK() {
        return cuts.length;
    }

    class CutIterator implements Iterator<MultiCut> {
        CutIterator() {
            //check if graph is already disconnected
            if (nextCutIndexToCalculate == 0 && maxCutNumber == cuts.length) {
                List<List<FlipCutNodeSimpleWeight>> comp = getComponents();
                if (comp.size() > 1) {//graph is already disconnected we have to build subgraph from parts
                    cuts[nextCutIndexToCalculate] = getCompCut(comp);
                    nextCutIndexToCalculate++;
                    maxCutNumber = nextCutIndexToCalculate;
                }
            }
        }

        int index = 0;

        public boolean hasNext() {
            if (index >= nextCutIndexToCalculate) {
                if (nextCutIndexToCalculate < maxCutNumber) {
                    return calculateNextCut();
                } else {
                    return false;
                }
            }
            return true;
        }

        public MultiCut next() {
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

    private DefaultMultiCut getCompCut(List<List<FlipCutNodeSimpleWeight>> comp) {
        return new DefaultMultiCut(comp, this);
    }
}