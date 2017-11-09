package phylo.tree.algorithm.flipcut.flipCutGraph;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.slf4j.LoggerFactory;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.cutter.GraphCutter;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 09.01.13
 * Time: 14:58
 */
public class FlipCutGraphSimpleWeight extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight> {

    public FlipCutGraphSimpleWeight(CostComputer costs, int bootstrapThreshold) {
        super(costs, bootstrapThreshold);
    }

    protected FlipCutGraphSimpleWeight(LinkedHashSet<FlipCutNodeSimpleWeight> characters, LinkedHashSet<FlipCutNodeSimpleWeight> taxa) {
        super(characters, taxa);
    }

    public FlipCutGraphSimpleWeight(List<FlipCutNodeSimpleWeight> nodes) {
        super(nodes);
    }

    @Override
    public List<? extends FlipCutGraphSimpleWeight> split(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes) {
        List<List<LinkedHashSet<FlipCutNodeSimpleWeight>>> graphDatas = splitToGraphData(sinkNodes);
        List<FlipCutGraphSimpleWeight> graphs = new LinkedList<FlipCutGraphSimpleWeight>();

        for (List<LinkedHashSet<FlipCutNodeSimpleWeight>> graphData : graphDatas) {
            FlipCutGraphSimpleWeight g = new FlipCutGraphSimpleWeight(graphData.get(0), graphData.get(1));
            graphs.add(g);
        }

        return graphs;
    }

    @Override
    protected List<LinkedHashSet<FlipCutNodeSimpleWeight>> createGraphData(CostComputer costs, int bootstrapThreshold) {
        List<Tree> inputTrees = new ArrayList<>(costs.getTrees());

        Map<String, FlipCutNodeSimpleWeight> taxa = new HashMap<>();

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
        int chars = 0;
        int bsIngnoredChars = 0;

        // identical character merge
        Map<List<Set<FlipCutNodeSimpleWeight>>, FlipCutNodeSimpleWeight> charactersMap = new HashMap<>();

        //init scaffold merging
        Tree scaff = costs.getScaffoldTree();
        final int size;
        if (scaff != null)
            size = scaff.vertexCount() - scaff.getNumTaxa();
        else
            size = 0;
        scaffoldCharacterMapping = Maps.synchronizedBiMap(HashBiMap.create(size));
        activePartitions = Collections.newSetFromMap(new ConcurrentHashMap<>(size));
        //init scaffold merging end


        for (Tree tree : inputTrees) {
            Map<String, FlipCutNodeSimpleWeight> leaves = new HashMap<>();
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
                        LoggerFactory.getLogger(this.getClass()).warn("Could not parse BS Value of tree vertex.");
                    }
                }

                Map<String, FlipCutNodeSimpleWeight> characterLeaves = new HashMap<String, FlipCutNodeSimpleWeight>();
                for (TreeNode treeNode : character.getLeaves()) {
                    characterLeaves.put(treeNode.getLabel(), taxa.get(treeNode.getLabel()));
                }

                FlipCutNodeSimpleWeight c = new FlipCutNodeSimpleWeight(null, new HashSet<FlipCutNodeSimpleWeight>(characterLeaves.size()), new HashSet<FlipCutNodeSimpleWeight>(leaves.size() - characterLeaves.size()));
                c.edgeWeight = costs.getEdgeWeight(character, null, (TreeNode) null);

                chars++;
                for (FlipCutNodeSimpleWeight taxon : leaves.values()) {
                    //add leaves and set to "1"
                    if (characterLeaves.containsKey(taxon.name)) {
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
                FlipCutNodeSimpleWeight characerInList = charactersMap.get(characterIndentifier);
                if (characerInList == null) {
                    charactersMap.put(characterIndentifier, c);
                    //know we know that character gets into graph, so we can add reverse edges to the taxa
                    for (FlipCutNodeSimpleWeight taxon : c.edges) {
                        taxon.addEdgeTo(c);
                    }
                    characerInList = c;
                } else {
                    characerInList.edgeWeight += c.edgeWeight;
                }

                //########## scaffold merging
                //insert scaffold characters to mapping if activated
                if (scaff != null && tree.equals(scaff)) {
                    addTreeNodeCharGuideTreeMapping(character, characerInList);
                    //create set of active partitions -> characters from scaffold tree that are currently the top level
                    if (character.getParent().equals(scaff.getRoot()))
                        activePartitions.add(characerInList);
                }
                //########## scaffold merging
            }
        }
        if (DEBUG)
            if (scaffoldCharacterMapping != null)
                System.out.println("Scaffold node number: " + scaffoldCharacterMapping.size());


        List<LinkedHashSet<FlipCutNodeSimpleWeight>> out = new ArrayList<>(2);
        out.add(new LinkedHashSet<>(charactersMap.values()));
        out.add(new LinkedHashSet<>(taxa.values()));

        System.out.println(bsIngnoredChars + " characters were ignored because of a bootstrap value less than " + bootstrapThreshold);
        System.out.println(out.get(0).size() + " of " + chars + " added to initial graph");
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
//                    System.out.println("!!!!!!!!!!!!! IMAGINARY-EDGE remove !!!!!!!!!!!!!");
                } else if (aCharacter.edges.contains(bTaxon)) { // > 0
                    System.out.println("!!!!!!!!!!!!!  EDGE remove -> should not the case for bcd: taxon " + bTaxon.name + " !!!!!!!!!!!!!");
                    System.out.println("--> char: " + aCharacter.toString() + " with taxa: " + getSortedEdges(aCharacter.edges));
                    aCharacter.edges.remove(bTaxon);
                    // remove reverse edge
                    bTaxon.edges.remove(aCharacter);
                }
            }
        }
    }

    // just for debug
    public static List<String> getSortedEdges(Collection<FlipCutNodeSimpleWeight> in) {
        List<String> out = new ArrayList<>(in.size());
        for (FlipCutNodeSimpleWeight flipCutNodeSimpleWeight : in) {
            out.add(flipCutNodeSimpleWeight.name);
        }

        Collections.sort(out);
        return out;
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

    @Override
    public List<? extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight>> getPartitions(GraphCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, AbstractFlipCutGraph<FlipCutNodeSimpleWeight>> c) {
        List<? extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight>> graphs = calculatePartitions(c);
        for (AbstractFlipCutGraph<FlipCutNodeSimpleWeight> graph : graphs) {
            // checks an removes edges to taxa that are not in this component!
            if (graph.checkEdges(c.isFlipCut()))
                System.out.println("INFO: Edges between graphs deleted! - Not possible for BCD");
            if (SCAFF_TAXA_MERGE)
                graph.insertScaffPartData(this,null);
            if (c.isBCD())
                graph.insertCharacterMapping(this);
        }
        return graphs;
    }

    //########## methods for edge identical character mapping ##########
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
        if (dummy != null) {
            characterToDummy.remove(character.clone);

            Set<FlipCutNodeSimpleWeight> chars = dummyToCharacters.get(dummy);
            chars.remove(character);
            if (chars.size() <= 1) {
                dummyToCharacters.remove(dummy);
                dummyToCharacters.remove(dummy.clone);
                for (FlipCutNodeSimpleWeight aChar : chars) {
                    characterToDummy.remove(aChar);
                    characterToDummy.remove(aChar.clone);
                }
            } else {
                dummyToCharacters.get(dummy.clone).remove(character.clone);
                dummy.edgeWeight -= character.edgeWeight;
            }
        }
    }
    //########## methods for edge identical character mappin END ##########


    /*@Override
    public List<? extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight>> calculatePartition(GraphCutter c) {
        List<? extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight>> graphs = super.calculatePartition(c);
        for (AbstractFlipCutGraph<FlipCutNodeSimpleWeight> graph : graphs) {
            if (SCAFF_TAXA_MERGE) graph.insertScaffPartData(this, null);
            graph.insertCharacterMapping(this);
        }
        return graphs;
    }*/


    @Override
    protected FlipCutGraphSimpleWeight newInstance(List<FlipCutNodeSimpleWeight> component) {
        return new FlipCutGraphSimpleWeight(component);
    }
}
