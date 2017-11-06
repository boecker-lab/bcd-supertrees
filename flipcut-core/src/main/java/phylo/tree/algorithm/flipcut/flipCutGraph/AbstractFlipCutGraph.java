package phylo.tree.algorithm.flipcut.flipCutGraph;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import mincut.cutGraphAPI.bipartition.Cut;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.cutter.GraphCutter;
import phylo.tree.model.TreeNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:36
 */
public abstract class AbstractFlipCutGraph<N extends AbstractFlipCutNode<N>> implements SourceTreeGraph {

    /**
     * Turn on/off debug mode
     */
    protected static final boolean DEBUG = false;
    /**
     * Turn on/off guide tree based taxa merging
     */
    public static final boolean SCAFF_TAXA_MERGE = true;
    /**
     * Mapping for guide tree based taxa merging
     */
    protected BiMap<N, TreeNode> scaffoldCharacterMapping = null;
    /**
     * Active partitions for guide tree based taxa merging
     */
    protected Set<N> activePartitions = new HashSet<>();

    /**
     * Mapping for edge  based character merging (Global character Map)
     */
    protected Map<N, N> characterToDummy = null;
    protected Map<N, Set<N>> dummyToCharacters = null;
    /**
     * The character vertex set
     */
    public LinkedHashSet<N> characters;
    /**
     * The taxa vertex set
     */
    public LinkedHashSet<N> taxa;
    /**
     * Marker for DFS
     */
    protected static final byte WHITE = 0;
    /**
     * Marker for DFS
     */
    protected static final byte GREY = 1;
    /**
     * cashes micut value from cut() for global scoring
     */
    protected long minCutValue = Long.MAX_VALUE;
    /**
     * parentNode in the Supertree
     */
    public final TreeNode parentNode;
    /**
     * node in the Supertree
     */
    public final TreeNode treeNode;


    protected AbstractFlipCutGraph(CostComputer costs, int bootstrapThreshold) {
        System.out.println("Creating graph representation of input trees...");

        List<LinkedHashSet<N>> data = createGraphData(costs, bootstrapThreshold);
        this.characters = data.get(0);
        this.taxa = data.get(1);
        parentNode = null;
        treeNode = new TreeNode();

        System.out.println("...Done!");
    }


    /**
     * Create a new graph with a list of characters and taxa and a tree node. The parent node
     * is optional and can be null if this is the root graph. No checks for edges that
     * connect the given vertex set to nodes no in this set are done!
     *
     * @param characters the character
     * @param taxa       the taxa
     */
    protected AbstractFlipCutGraph(LinkedHashSet<N> characters, LinkedHashSet<N> taxa, TreeNode parentNode) {
        this.characters = characters;
        this.taxa = taxa;
        this.parentNode = parentNode;
        treeNode = new TreeNode();
    }


    /**
     * Takes a list  of nodes and splits them into characters and taxa. Then we check for
     * edges that connect the vertices to nodes not contained in this graphs vertex set and remove those edges.
     *
     * @param nodes the nodes
     */
    public AbstractFlipCutGraph(List<N> nodes, TreeNode parentNode, final boolean edgeDeletion) {
        characters = new LinkedHashSet<>(nodes.size());
        taxa = new LinkedHashSet<>(nodes.size());
        for (N node : nodes) {
            if (node.isTaxon()) {
                taxa.add(node);
            } else {
                characters.add(node);
            }
        }

        // checks an removes edges to taxa that are not in this component!
        if (checkEdges(edgeDeletion))
            System.out.println("INFO: Edges between graphs deleted! - Not possible for BCD");

        this.parentNode = parentNode;
        treeNode = new TreeNode();
    }

    protected abstract List<LinkedHashSet<N>> createGraphData(CostComputer costs, int bootstrapThreshold);

    protected void removeAdjacentEdges(N characterToRemove) {
        // remove edges to taxa
        if (characterToRemove == null || characterToRemove.edges == null)
            System.out.println("fail");
        for (N taxon : characterToRemove.edges) {
            if (!taxon.edges.remove(characterToRemove))
                System.out.println("WTF");
        }
        //check that no active scaffold character gets removded
        //JUST for DEBUGGING
        if (DEBUG) {
            if (scaffoldCharacterMapping.containsKey(characterToRemove)) {
                System.out.println("ERROR: Illegal SCAFFOLD character deletion!!! " + characterToRemove.toString());
            }
        }
    }

    protected void removeCharacters(Collection<N> toRemove, Collection<N> characters) {
        //remove chracters and edges to them from graph
        for (N remove : toRemove) {
            if (characters.remove(remove)) {
                removeAdjacentEdges(remove);
            }
        }
    }

    /**
     * Remove semi universal characters
     */
    @Override
    public void deleteSemiUniversals() {
        if (characterToDummy == null)
            createCharacterMapping();
        Iterator<N> it = characters.iterator();
        while (it.hasNext()) {
            N character = it.next();
            if (character.isSemiUniversal()) {
                //remove deleted partitions an insert child partitions

                removeCharacterFromDummyMapping(character);

                if (SCAFF_TAXA_MERGE && !activePartitions.isEmpty()) {
                    TreeNode node = scaffoldCharacterMapping.get(character);
                    if (node != null) {
                        Set<N> toInsert = new HashSet(node.childCount());
                        for (TreeNode child : node.getChildren()) {
                            if (child.isInnerNode()) {
                                N n = scaffoldCharacterMapping.inverse().get(child);
                                toInsert.add(n);
                            }
                        }
                        activePartitions.remove(character);
                        activePartitions.addAll(toInsert);
                        removeTreeNodeCharGuideTreeMapping(character);
                    }
                }

                it.remove();
                removeAdjacentEdges(character);
                if (DEBUG)
                    System.out.println("Removing semi universal char " + character.toString() + " semiUniversal");
            }
        }
    }


    /**
     * Splits this graph into two disconnected graphs, one consisting of the given set
     * of nodes, the other graph consists of all vertices not contained in the given
     * set of nodes.
     *
     * @param sinkNodes the set of nodes for
     * @return graphs list of two graphs created
     */
    public abstract List<? extends AbstractFlipCutGraph<N>> split(LinkedHashSet<N> sinkNodes);


    /**
     * Does a DFS search for connected components and returns a list of components
     *
     * @return components list of components
     */
    public List<List<N>> getComponents() {
        List<List<N>> components = new ArrayList<List<N>>(2);
        List<N> currentComponent;
        for (N node : characters) {
            node.color = WHITE;
        }
        for (N node : taxa) {
            node.color = WHITE;
        }

        for (N n : taxa) {
            if (n.color == WHITE) {
                currentComponent = new ArrayList<N>();
                components.add(currentComponent);
                dfs(n, currentComponent);
            }
        }
        return components;
    }

    /**
     * DFS iteration
     *
     * @param v         the current node v
     * @param component the current component
     */
    protected void dfs(N v, List<N> component) {
        v.color = GREY;
        component.add(v);
        for (N edge : v.edges) {
            if (edge != null && edge.color == WHITE) {
                dfs(edge, component);
            }
        }
    }

    protected abstract Map<N, N> copyNodes();


    public long getMinCutValue() {
        return minCutValue;
    }

    //checks edges and reverse edges but NO imaginary edges...
    //this is for the flipCut edge deletion version only
    protected boolean checkEdges(final boolean edgeDeletion) {
        if (edgeDeletion) {
            boolean deleted = false;
            // check edges from characters
            for (N character : characters) {
                deleted = deleted || character.edges.retainAll(taxa);
            }
            // check reverse edges from taxa
            for (N taxon : taxa) {
                deleted = deleted || taxon.edges.retainAll(characters);
            }
            return deleted;
        }
        return false;
    }


    //########## methods for guide tree mapping ##########
    protected void addTreeNodeCharGuideTreeMapping(TreeNode character, N c) {
        scaffoldCharacterMapping.put(c, character);
    }

    protected void removeTreeNodeCharGuideTreeMapping(TreeNode character) {
        scaffoldCharacterMapping.inverse().remove(character);
    }

    protected void removeTreeNodeCharGuideTreeMapping(N c) {
        scaffoldCharacterMapping.remove(c);
    }

    public void insertScaffPartData(AbstractFlipCutGraph<N> source, final Map<N, N> oldToNew) {
        activePartitions = new HashSet<>();
        if (!source.activePartitions.isEmpty()) {
            // multicutted version
            if (oldToNew != null) {
                scaffoldCharacterMapping = Maps.synchronizedBiMap(HashBiMap.create());
                for (Map.Entry<N, TreeNode> entry : source.scaffoldCharacterMapping.entrySet()) {
                    N sourceNode;
                    if (oldToNew != null) {
                        sourceNode = oldToNew.get(entry.getKey());
                    } else {
                        sourceNode = entry.getKey();
                    }
                    if (characters.contains(sourceNode)) {
                        addTreeNodeCharGuideTreeMapping(entry.getValue(), sourceNode);
                    }
                }

                for (N activePartition : source.activePartitions) {
                    N sourceNode;
                    if (oldToNew != null) {
                        sourceNode = oldToNew.get(activePartition);
                    } else {
                        sourceNode = activePartition;
                    }
                    if (characters.contains(sourceNode))
                        activePartitions.add(sourceNode);
                }
                // single cutted version
            } else {
                scaffoldCharacterMapping = source.scaffoldCharacterMapping;
                for (N sourceNode : source.activePartitions) {
                    if (characters.contains(sourceNode))
                        activePartitions.add(sourceNode);
                }
            }
        }
    }
    //########## methods for guide tree mapping END ##########

    //########## methods for edge identical character mapping ##########
    protected void createCharacterMapping() {
//        System.out.println("Creating character mapping for Graph: " + this.toString());
        characterToDummy = new ConcurrentHashMap<>(characters.size());
        dummyToCharacters = new ConcurrentHashMap<>(characters.size());
        Map<Set<N>, N> edgeSetToDummy = new HashMap<>();

        for (N character : characters) {
            //global character merge for chars with same edgeset
            N dummy = edgeSetToDummy.get(character.edges);
            if (dummy == null) {
                dummy = character.createDummy();
                edgeSetToDummy.put(dummy.edges, dummy);
                dummyToCharacters.put(dummy, Collections.newSetFromMap(new ConcurrentHashMap()));
                dummyToCharacters.put(dummy.clone, Collections.newSetFromMap(new ConcurrentHashMap()));
            }
            addCharacterToDummyMapping(character, dummy);
        }

        // remove single mappings
        Iterator<Set<N>> it = dummyToCharacters.values().iterator();
        while (it.hasNext()) {
            Set<N> chracters = it.next();
            if (chracters.size() <= 1) {
                it.remove();
                characterToDummy.remove(chracters.iterator().next());
            }
        }
//        System.out.println(characterToDummy.size() / 2 + " can be merged to " + dummyToCharacters.size() / 2 + " during mincut phases");
    }

    public void insertCharacterMapping(AbstractFlipCutGraph<N> source) {
        characterToDummy = source.characterToDummy;
        dummyToCharacters = source.dummyToCharacters;
    }

    public N getDummyFromMapping(N character) {
        N dummy = characterToDummy.get(character);
        if (dummy == null)
            return character;
        return dummy;
    }

    public Set<N> getCharactersFromMapping(N dummy) {
        Set<N> chars = dummyToCharacters.get(dummy);
        if (chars == null || chars.isEmpty())
            return Collections.singleton(dummy);
        return chars;
    }

    public abstract void addCharacterToDummyMapping(N character, N dummy);

    public abstract void removeCharacterFromDummyMapping(N character);
    //########## methods for edge identical character mappin END ##########

    @Override
    public List<? extends AbstractFlipCutGraph<N>> calculatePartition(GraphCutter c) {
        Cut<LinkedHashSet<N>> cut = c.cut(this);
        LinkedHashSet<N> minCut = cut.getCutSet();
        return split(minCut);
    }
}
