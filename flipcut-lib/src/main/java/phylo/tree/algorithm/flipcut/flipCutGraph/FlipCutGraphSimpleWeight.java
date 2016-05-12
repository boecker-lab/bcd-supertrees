package phylo.tree.algorithm.flipcut.flipCutGraph;

import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 09.01.13
 * Time: 14:58
 */
public class FlipCutGraphSimpleWeight extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight> {

    public FlipCutGraphSimpleWeight(CostComputer costs, int bootstrapThreshold) {
        super(costs, bootstrapThreshold);
    }

    protected FlipCutGraphSimpleWeight(LinkedHashSet<FlipCutNodeSimpleWeight> characters, LinkedHashSet<FlipCutNodeSimpleWeight> taxa, TreeNode parentNode) {
        super(characters, taxa, parentNode);
    }

    public FlipCutGraphSimpleWeight(List<FlipCutNodeSimpleWeight> nodes, TreeNode parentNode, final boolean checkEdges) {
        super(nodes, parentNode, checkEdges);
    }

    @Override
    List<? extends FlipCutGraphSimpleWeight> split(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes) {
        List<List<LinkedHashSet<FlipCutNodeSimpleWeight>>> graphData = splitToGraphData(sinkNodes);
        List<FlipCutGraphSimpleWeight> graphs = new LinkedList<FlipCutGraphSimpleWeight>();
        FlipCutGraphSimpleWeight g1 = new FlipCutGraphSimpleWeight(graphData.get(0).get(0), graphData.get(0).get(1), treeNode);
        FlipCutGraphSimpleWeight g2 = new FlipCutGraphSimpleWeight(graphData.get(1).get(0), graphData.get(1).get(1), treeNode);
        graphs.add(g1);
        graphs.add(g2);

        return graphs;
    }

    @Override
    protected List<LinkedHashSet<FlipCutNodeSimpleWeight>> createGraphData(CostComputer costs, int bootstrapThreshold) {
        System.out.println("Creating intitial FC graph...");
        List<Tree> inputTrees = new ArrayList<>(costs.getTrees());
        Tree scaff = costs.getScaffoldTree();
        if (SCAFF_TAXA_MERGE) {
            final int size;
            if (scaff != null)
                size = scaff.vertexCount() - scaff.getNumTaxa();
            else
                size = 0;
            charToTreeNode = new ConcurrentHashMap<>(size);
            treeNodeToChar = new ConcurrentHashMap<>(size);
            activePartitions = Collections.newSetFromMap(new ConcurrentHashMap<>(size));
        }

        if (GLOBAL_CHARACTER_MERGE) {
            characterToDummy = new ConcurrentHashMap<>();//todo size estimation?
            dummyToCharacters = new ConcurrentHashMap<>();//todo size estimation?
        }

        Map<String, FlipCutNodeSimpleWeight> taxa = new HashMap<String, FlipCutNodeSimpleWeight>();
        Map<List<Set<FlipCutNodeSimpleWeight>>, FlipCutNodeSimpleWeight> characters = new HashMap<>();
        Map<Set<FlipCutNodeSimpleWeight>, FlipCutNodeSimpleWeight> edgeSetToDummy = new HashMap<>();

        //create taxon list
        System.out.println("create taxon list");
        for (Tree tree : inputTrees) {
            for (TreeNode taxon : tree.getLeaves()) {
                if (!taxa.containsKey(taxon.getLabel())) {
                    FlipCutNodeSimpleWeight n = new FlipCutNodeSimpleWeight(taxon.getLabel());
                    taxa.put(n.name, n);
                }
            }
        }

        //create character list
        System.out.println("create chracter list");
//        int trees = 0;
        int chars = 0;
        int bsIngnoredChars = 0;
        for (Tree tree : inputTrees) {
//            trees++;
//            System.out.println("processing tree number: " + trees);
            Map<String, FlipCutNodeSimpleWeight> leaves = new HashMap<String, FlipCutNodeSimpleWeight>();
            for (TreeNode treeNode : tree.getLeaves()) {
                leaves.put(treeNode.getLabel(), taxa.get(treeNode.getLabel()));
            }

            for (TreeNode character : tree.vertices()) {
                //skip leaves
                if (character.isLeaf()) continue;
                // also skip root
                if (character == tree.getRoot()) continue;


                // skip character with small bootstrap value
                if (character.getLabel() != null) {
                    try {
                        double bootstrap = Double.valueOf(character.getLabel());
                        if (bootstrap < bootstrapThreshold) {
                            bsIngnoredChars++;
                            continue;
                        }
                    } catch (NumberFormatException e) {
                    }
                }

                Map<String, FlipCutNodeSimpleWeight> chracterLeaves = new HashMap<String, FlipCutNodeSimpleWeight>();
                for (TreeNode treeNode : character.getLeaves()) {
                    chracterLeaves.put(treeNode.getLabel(), taxa.get(treeNode.getLabel()));
                }

                FlipCutNodeSimpleWeight c = new FlipCutNodeSimpleWeight(null, new HashSet<FlipCutNodeSimpleWeight>(chracterLeaves.size()), new HashSet<FlipCutNodeSimpleWeight>(leaves.size() - chracterLeaves.size()));
                c.edgeWeight = costs.getEdgeWeight(character, null, (TreeNode) null);

                chars++;
                for (FlipCutNodeSimpleWeight taxon : leaves.values()) {
                    //add leaves and set to "1"
                    if (chracterLeaves.containsKey(taxon.name)) {
                        c.addEdgeTo(taxon);
                        // add reverse edge
//                        taxon.addEdgeTo(c); --> //we have to wait with this until we know if character will be part of the graph --> see below
                        //now set all nodes of this tree to "0"
                    } else {
                        c.addImaginaryEdgeTo(taxon);
                    }
                }

                // add to characters list
                List<Set<FlipCutNodeSimpleWeight>> characterIndentifier = new ArrayList<>(2);
                characterIndentifier.add(c.edges);
                characterIndentifier.add(c.imaginaryEdges);

                //identical character merge
                FlipCutNodeSimpleWeight characerInList = characters.get(characterIndentifier);
                if (characerInList == null) {
                    characters.put(characterIndentifier, c);
                    //know we know that character gets into graph, so we can add reverse edges to the taxa
                    for (FlipCutNodeSimpleWeight taxon : c.edges) {
                        taxon.addEdgeTo(c);
                    }

                    //global character merge for chars with same edgeset
                    if (GLOBAL_CHARACTER_MERGE) {
                        FlipCutNodeSimpleWeight dummy = edgeSetToDummy.get(c.edges);
                        if (dummy == null) {
                            dummy = new FlipCutNodeSimpleWeight(c.edges);
                            edgeSetToDummy.put(dummy.edges, dummy);
                            dummyToCharacters.put(dummy, Collections.newSetFromMap(new ConcurrentHashMap()));
                            dummyToCharacters.put(dummy.clone, Collections.newSetFromMap(new ConcurrentHashMap()));
                        }
                        addCharacterToDummyMapping(c, dummy);
                    }

                    characerInList = c;
                } else {
                    characerInList.edgeWeight += c.edgeWeight;
                    if (GLOBAL_CHARACTER_MERGE)
                        characterToDummy.get(characerInList).edgeWeight += c.edgeWeight;
                }

                //insert scaffold characters to mapping if activated
                if (SCAFF_TAXA_MERGE) {
                    if (scaff != null && tree.equals(scaff)) {
                        addTreeNodeCharGuideTreeMapping(character, characerInList);
                        //create set of active partitions
                        if (character.getParent().equals(scaff.getRoot()))
                            activePartitions.add(characerInList);
                    }
                }
            }
        }
        if (DEBUG)
            if (charToTreeNode != null)
                System.out.println("Scaffold node number: " + charToTreeNode.size());


        List<LinkedHashSet<FlipCutNodeSimpleWeight>> out = new ArrayList<>(2);
        out.add(new LinkedHashSet<>(characters.values()));
        out.add(new LinkedHashSet<>(taxa.values()));

        System.out.println(bsIngnoredChars + " characters were ignored because of a bootstrap value less than " + bootstrapThreshold);
        System.out.println(out.get(0).size() + " of " + chars + " added to initial graph");
        if (GLOBAL_CHARACTER_MERGE)
            System.out.println(characterToDummy.size() / 2 + " can be merged to " + dummyToCharacters.size() / 2 + " during mincut phases");
        System.out.println("...Done!");


        return out;
    }

    protected List<List<LinkedHashSet<FlipCutNodeSimpleWeight>>> splitToGraphData(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes) {
        LinkedHashSet<FlipCutNodeSimpleWeight> g1Characters = new LinkedHashSet(characters.size());
        LinkedHashSet<FlipCutNodeSimpleWeight> g1Taxa = new LinkedHashSet(taxa.size());
        LinkedHashSet<FlipCutNodeSimpleWeight> g2Characters;
        LinkedHashSet<FlipCutNodeSimpleWeight> g2Taxa;
        LinkedHashSet<FlipCutNodeSimpleWeight> charactersToRemove = new LinkedHashSet(characters.size());

        // check if we have to remove vertices
        for (FlipCutNodeSimpleWeight node : sinkNodes) {
            if (!node.isTaxon()) {
                // it is character or a character clone
                // check if the other one is also in the set
                if (!sinkNodes.contains(node.clone)) {
                    FlipCutNodeSimpleWeight c = node.isClone() ? node.clone : node;
                    charactersToRemove.add(c);
                    if (DEBUG) System.out.println("remove character " + c);
                }
            }
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
                g1Taxa.add(node);
            } else {
                g1Characters.add(node);
            }
        }

        // fill g2 lists
        g2Characters = new LinkedHashSet<>(characters);
        g2Characters.removeAll(g1Characters);
        g2Taxa = new LinkedHashSet<>(taxa);
        g2Taxa.removeAll(g1Taxa);

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

    //helper method for split
    protected void removeEdgesToOtherGraph(Collection<FlipCutNodeSimpleWeight> aCharacters, Collection<FlipCutNodeSimpleWeight> bTaxa) {
        for (FlipCutNodeSimpleWeight aCharacter : aCharacters) {
            for (FlipCutNodeSimpleWeight bTaxon : bTaxa) {
                //update zero edge counter
                if (aCharacter.imaginaryEdges.remove(bTaxon)) { // < 0
                    //remove edge to other side
                } else if (aCharacter.edges.remove(bTaxon)) { // > 0
                    // remove reverse edge
                    System.out.println("!!!!!!!!!!!!! should not the case for bcd !!!!!!!!!!!!!");
                    bTaxon.edges.remove(aCharacter);
                }
            }
        }
    }

    @Override
    protected Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> copyNodes() {
        //clone all nodes
        Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew = new HashMap<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight>(characters.size() + taxa.size());

        for (FlipCutNodeSimpleWeight character : characters) {
            oldToNew.put(character, character.copy());
        }
        for (FlipCutNodeSimpleWeight taxon : taxa) {
            oldToNew.put(taxon, taxon.copy());
        }

        for (FlipCutNodeSimpleWeight character : characters) {
            FlipCutNodeSimpleWeight cClone = oldToNew.get(character);
            for (FlipCutNodeSimpleWeight taxon : character.edges) {
                FlipCutNodeSimpleWeight tClone = oldToNew.get(taxon);
                cClone.addEdgeTo(tClone);
                tClone.addEdgeTo(cClone);
            }
            for (FlipCutNodeSimpleWeight taxon : character.imaginaryEdges) {
                FlipCutNodeSimpleWeight tClone = oldToNew.get(taxon);
                cClone.addImaginaryEdgeTo(tClone);
            }
        }

        return oldToNew;
    }

    @Override
    protected boolean checkEdges(final boolean edgeDeletion) {
        boolean deleted = false;
        // check edges from characters
        for (FlipCutNodeSimpleWeight character : characters) {
            if (edgeDeletion)
                deleted = deleted || character.edges.retainAll(taxa);
            character.imaginaryEdges.retainAll(taxa);
        }
        if (edgeDeletion) {
            // check reverse edges from taxa
            for (FlipCutNodeSimpleWeight taxon : taxa) {
                deleted = deleted || taxon.edges.retainAll(characters);
            }
        }
        return deleted;

    }


    //########## methods for edge identical character mappin ##########

    @Override
    public void insertCharacterMapping(AbstractFlipCutGraph<FlipCutNodeSimpleWeight> source, Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> oldToNew) {
        characterToDummy = source.characterToDummy;
        dummyToCharacters = source.dummyToCharacters;
    }

    @Override
    public void addCharacterToDummyMapping(FlipCutNodeSimpleWeight character, FlipCutNodeSimpleWeight dummy) {
        characterToDummy.put(character, dummy);
        characterToDummy.put(character.clone, dummy.clone);
        dummyToCharacters.get(dummy).add(character);
        dummyToCharacters.get(dummy.clone).add(character.clone);

        dummy.edgeWeight += character.edgeWeight;
    }

    @Override
    public void removeCharacterFromDummyMapping(FlipCutNodeSimpleWeight character) {
        FlipCutNodeSimpleWeight dummy = characterToDummy.remove(character);
        characterToDummy.remove(character.clone);
        dummyToCharacters.get(dummy).remove(character);
        dummyToCharacters.get(dummy.clone).remove(character.clone);

        dummy.edgeWeight -= character.edgeWeight;
    }
    //########## methods for edge identical character mappin END ##########
}
