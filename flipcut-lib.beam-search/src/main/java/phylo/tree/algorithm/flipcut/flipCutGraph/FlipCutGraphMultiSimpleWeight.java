package phylo.tree.algorithm.flipcut.flipCutGraph;


import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;
import phylo.tree.algorithm.flipcut.model.MultiCut;
import phylo.tree.model.TreeNode;

import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 *         Date: 17.01.13
 *         Time: 17:59
 */

public class FlipCutGraphMultiSimpleWeight extends FlipCutGraphSimpleWeight {
    private int maxCutNumber;
    private int nextCutIndexToCalculate;
    private final MultiCut[] cuts;
    private final MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> cutter;
    private final MultiCutterFactory<MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight>, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> cutterFactory;

    //todo k needed?
    public FlipCutGraphMultiSimpleWeight(CostComputer costs, int k, MultiCutterFactory<MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight>, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> cutterFactory) {
        super(costs, 0); //todo dummy bootstrapthreshold --> implement it
        this.cutter = cutterFactory.newInstance(this);
        this.cutterFactory = cutterFactory;
        maxCutNumber = k;
        cuts = new MultiCut[maxCutNumber];
        nextCutIndexToCalculate = 0;
    }

    protected FlipCutGraphMultiSimpleWeight(LinkedHashSet<FlipCutNodeSimpleWeight> characters, LinkedHashSet<FlipCutNodeSimpleWeight> taxa, TreeNode parentNode, int k, MultiCutterFactory<MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight>, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> cutterFactory) {
        super(characters, taxa, parentNode);
        this.cutter = cutterFactory.newInstance(this);
        this.cutterFactory = cutterFactory;
        maxCutNumber = k;
        cuts = new MultiCut[maxCutNumber];
        nextCutIndexToCalculate = 0;
    }

    public FlipCutGraphMultiSimpleWeight(List<FlipCutNodeSimpleWeight> nodes, TreeNode parentNode, int k, MultiCutterFactory<MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight>, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> cutterFactory, boolean edgeDeletion) {
        super(nodes, parentNode, edgeDeletion);
        this.cutter = cutterFactory.newInstance(this);
        this.cutterFactory = cutterFactory;
        maxCutNumber = k;
        cuts = new MultiCut[maxCutNumber];
        nextCutIndexToCalculate = 0;
    }


    public Iterator<MultiCut> getCutIterator() {
        return new CutIterator();
    }


    private boolean calculateNextCut() {
        MultiCut c = cutter.getNextCut();
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
        Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew = copyNodes();
        List<List<LinkedHashSet<FlipCutNodeSimpleWeight>>> graphData = splitToGraphData(new LinkedHashSet<>(sinkNodes), oldToNew);

        List<FlipCutGraphMultiSimpleWeight> graphs = new LinkedList<>();
        for (List<LinkedHashSet<FlipCutNodeSimpleWeight>> data : graphData) {
            FlipCutGraphMultiSimpleWeight graph = new FlipCutGraphMultiSimpleWeight(data.get(0), data.get(1), treeNode, cuts.length, cutterFactory);

            if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE)
                graph.insertScaffPartData(this, oldToNew);

            if (AbstractFlipCutGraph.GLOBAL_CHARACTER_MERGE)
                graph.insertCharacterMapping(this, oldToNew);

            graphs.add(graph);

        }

        return graphs;
    }

    //overwritten --> creates cloned nodes for multiple splits
    protected List<List<LinkedHashSet<FlipCutNodeSimpleWeight>>> splitToGraphData(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes, final Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew) {
        LinkedHashSet<FlipCutNodeSimpleWeight> g1Characters = new LinkedHashSet<>();
        LinkedHashSet<FlipCutNodeSimpleWeight> g1Taxa = new LinkedHashSet<>();
        LinkedHashSet<FlipCutNodeSimpleWeight> g2Characters = new LinkedHashSet<>();
        LinkedHashSet<FlipCutNodeSimpleWeight> g2Taxa = new LinkedHashSet<>();

        // check if we have to remove vertices
        List<FlipCutNodeSimpleWeight> preCharactersToRemove = checkRemoveCharacter(sinkNodes);
        List<FlipCutNodeSimpleWeight> charactersToRemove = new ArrayList<FlipCutNodeSimpleWeight>(preCharactersToRemove.size());
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

    public List<FlipCutGraphMultiSimpleWeight> buildComponentGraphs(List<List<FlipCutNodeSimpleWeight>> comp) {
        List<FlipCutGraphMultiSimpleWeight> splittedGraphs = new ArrayList<FlipCutGraphMultiSimpleWeight>(comp.size());
        for (List<FlipCutNodeSimpleWeight> component : comp) {
            //clone all nodes
            Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew = new HashMap<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight>(component.size());
            for (FlipCutNodeSimpleWeight node : component) {
                oldToNew.put(node, node.copy());
            }

            Set<FlipCutNodeSimpleWeight> characters = new HashSet<>();
            for (FlipCutNodeSimpleWeight node : component) {
                if (!node.isTaxon()) {
                    characters.add(node);
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

            Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> characterToDummy = new HashMap<>();
            Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToCharacters = new HashMap<>();

            FlipCutGraphMultiSimpleWeight g;
            if (cutterFactory instanceof MaxFlowCutterFactory) {
                g = new FlipCutGraphMultiSimpleWeight(new ArrayList<>(oldToNew.values()), treeNode, cuts.length, cutterFactory, !((MaxFlowCutterFactory) cutterFactory).isHyperGraphCutter());
            } else {
                g = new FlipCutGraphMultiSimpleWeight(new ArrayList<>(oldToNew.values()), treeNode, cuts.length, cutterFactory, false);
            }

            if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE)
                g.insertScaffPartData(this, oldToNew);

            if (AbstractFlipCutGraph.GLOBAL_CHARACTER_MERGE)
                g.insertCharacterMapping(this, oldToNew);


            splittedGraphs.add(g);

        }
        return splittedGraphs;
    }

    //gets mapping and characters of the new compount to duplicate merging maps;
    private void cloneCharacterMaps(final AbstractFlipCutGraph<FlipCutNodeSimpleWeight> sourceGraph, final Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew) {
        for (FlipCutNodeSimpleWeight sourceChar : sourceGraph.characters) {
            FlipCutNodeSimpleWeight sourceDummy = sourceGraph.characterToDummy.get(sourceChar);
            if (sourceDummy != null) {
                FlipCutNodeSimpleWeight targetChar = oldToNew.get(sourceChar);
                if (targetChar != null && characters.contains(targetChar) && !characterToDummy.containsKey(targetChar)) {
                    Set<FlipCutNodeSimpleWeight> sourceSet = sourceGraph.dummyToCharacters.get(sourceDummy);
                    FlipCutNodeSimpleWeight targetDummy = new FlipCutNodeSimpleWeight(new HashSet<>(oldToNew.get(sourceChar).edges));

                    Set<FlipCutNodeSimpleWeight> targetSet = new HashSet<>();
                    Set<FlipCutNodeSimpleWeight> targetCloneSet = new HashSet<>();

                    dummyToCharacters.put(targetDummy, targetSet);
                    dummyToCharacters.put(targetDummy.clone, targetCloneSet);
                    for (FlipCutNodeSimpleWeight source : sourceSet) {
                        FlipCutNodeSimpleWeight target = oldToNew.get(source);
                        if (target != null && characters.contains(target)) {
                            characterToDummy.put(target, targetDummy);
                            characterToDummy.put(target.clone, targetDummy.clone);

                            targetDummy.edgeWeight += target.edgeWeight;

                            targetSet.add(target);
                            targetCloneSet.add(target.clone);
                        }
                    }
                }
            }
        }
    }

    public boolean containsCuts() {
        return nextCutIndexToCalculate > 0;
    }

    private DefaultMultiCut getCompCut(List<List<FlipCutNodeSimpleWeight>> comp) {
        return new DefaultMultiCut(comp, this);
    }

    @Override
    public void insertCharacterMapping(AbstractFlipCutGraph<FlipCutNodeSimpleWeight> source, Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew) {
        dummyToCharacters = new HashMap<>(characters.size());
        characterToDummy = new HashMap<>(characters.size());
        cloneCharacterMaps(source, oldToNew);
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


}