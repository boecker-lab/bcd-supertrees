package flipcut.flipCutGraph;


import epos.model.tree.TreeNode;
import flipcut.costComputer.CostComputer;

import java.util.*;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:36
 */
public abstract class AbstractFlipCutGraph<T extends AbstractFlipCutNode<T>> {

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
    protected Map<T, TreeNode> charToTreeNode = null;
    protected Map<TreeNode, T> treeNodeToChar = null;
    // active partitions for guide tree based taxa merging
    protected Set<T> activePartitions = null;

    /**
     * Turn on/off global character merging for characters with identical edgeset --> works for hypergraph version only
     */
    public static final boolean GLOBAL_CHARACTER_MERGE = true;
    /**
     * Mapping for edge  based character merging (Global character Map)
     */
    public Map<T, T> characterToDummy = null;
    public Map<T, Set<T>> dummyToCharacters = null;


    /**
     * The character vertex set
     */
    public final LinkedHashSet<T> characters;
    /**
     * The taxa vertex set
     */
    public final LinkedHashSet<T> taxa;
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
        List<LinkedHashSet<T>> data = createGraphData(costs, bootstrapThreshold);
        this.characters = data.get(0);
        this.taxa = data.get(1);
        parentNode = null;
        treeNode = new TreeNode();
        /*
       Sort the leaves alphabetically to ensure we find the same mincut
       for multiple runs
        */
//        sortTaxa();
    }


    /**
     * Create a new graph with a list of characters and taxa and a tree node. The parent node
     * is optional and can be null if this is the root graph. No checks for edges that
     * connect the given vertex set to nodes no in this set are done!
     *
     * @param characters the character
     * @param taxa       the taxa
     */
    protected AbstractFlipCutGraph(LinkedHashSet<T> characters, LinkedHashSet<T> taxa, TreeNode parentNode) {
        this.characters = characters;
        this.taxa = taxa;
        this.parentNode = parentNode;
        treeNode = new TreeNode();

       /*
       * Sort the leaves alphabetically to ensure we find the same mincut
       * for multiple runs
       */

//        sortTaxa();
    }


    /**
     * Takes a list  of nodes and splits them into characters and taxa. Then we check for
     * edges that connect the vertices to nodes not contained in this graphs vertex set and remove those edges.
     *
     * @param nodes the nodes
     */
    public AbstractFlipCutGraph(List<T> nodes, TreeNode parentNode) {
        characters = new LinkedHashSet<>(nodes.size());
        taxa = new LinkedHashSet<>(nodes.size());
        for (T node : nodes) {
            if (node.isTaxon()) {
                taxa.add(node);
            } else {
                characters.add(node);
            }
        }

        // checks an removes edges to taxa that are not in this component!!!
//        if (GLOBAL_CHARACTER_MERGE) { //todo deactivate for merge //has to be false for original edge deletion flipCut
        if (checkEdges())
            System.out.println("Edges between graphs deleted!");
//        }

        this.parentNode = parentNode;
        treeNode = new TreeNode();

        /*
       Sort the leaves alphabetically to ensure we find the same mincut
       for multiple runs
        */
//        sortTaxa();
    }

    /*
       Sorts the leaves alphabetically to ensure we find the same mincut
       for multiple runs
     */
    /*protected void sortTaxa() {
        Collections.sort(taxa, new Comparator<T>() {
            public int compare(T o1, T o2) {
                return o1.name.compareTo(o2.name);
            }
        });
    }*/

    protected abstract List<LinkedHashSet<T>> createGraphData(CostComputer costs, int bootstrapThreshold);

    protected void removeAdjacentEdges(T characterToRemove) {
        // remove edges to taxa
        for (T taxon : characterToRemove.edges) {
            taxon.edges.remove(characterToRemove);
        }
        //check that no active scaffold character gets removded
        //JUST for DEBUGGING
        if (DEBUG) {
            if (charToTreeNode.containsKey(characterToRemove)) {
                System.out.println("ERROR: Illegal SCAFFOLD character deletion!!! " + characterToRemove.toString());
            }
        }
    }

    protected void removeCharacters(Collection<T> toRemove, Collection<T> characters) {
        //remove chracters and edges to them from graph
        for (T remove : toRemove) {
            if (characters.remove(remove)) {
                removeAdjacentEdges(remove);
            }
        }
    }

    protected void removeCharacters(Collection<T> toRemove) {
        removeCharacters(toRemove, characters);
    }

    /**
     * Remove semi universal characters
     */
    public void deleteSemiUniversals() {
        Iterator<T> it = characters.iterator();
        while (it.hasNext()) {
            T character = it.next();
            if (character.isSemiUniversal()) {
                //remove deleted partitions an insert child partitions

                if (GLOBAL_CHARACTER_MERGE) {
                    removeCharacterFromDummyMapping(character);
                }

                if (SCAFF_TAXA_MERGE && !activePartitions.isEmpty()) {
                    TreeNode node = charToTreeNode.get(character);
                    if (node != null) {
                        Set<T> toInsert = new HashSet(node.childCount());
                        for (TreeNode child : node.getChildren()) {
                            if (child.isInnerNode()) {
                                T n = treeNodeToChar.get(child);
                                toInsert.add(n);
                            }
                        }
                        activePartitions.remove(character);
                        activePartitions.addAll(toInsert);
                        removeTreNodeCharGuideTreeMapping(character);
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
    abstract List<? extends AbstractFlipCutGraph<T>> split(LinkedHashSet<T> sinkNodes);


    /**
     * Does a DFS search for connected components and returns a list of components
     *
     * @return components list of components
     */
    public List<List<T>> getComponents() {
        List<List<T>> components = new ArrayList<List<T>>(2);
        List<T> currentComponent;
        for (T node : characters) {
            node.color = WHITE;
        }
        for (T node : taxa) {
            node.color = WHITE;
        }

        for (T t : taxa) {
            if (t.color == WHITE) {
                currentComponent = new ArrayList<T>();
                components.add(currentComponent);
                dfs(t, currentComponent);
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
    protected void dfs(T v, List<T> component) {
        v.color = GREY;
        component.add(v);
        for (T edge : v.edges) {
            //for (int i = 0; i < v.edges.size(); i++) {
            //T next = v.edges.get(i);
            if (edge != null && edge.color == WHITE) {
                dfs(edge, component);
            }
        }
    }

    protected abstract Map<T, T> copyNodes();


    public long getMinCutValue() {
        return minCutValue;
    }

    //checks edges and reverse edges but NO imaginary edges...
    //this is for the flipCut edge deletion version only
    protected boolean checkEdges() {
        boolean deleted = false;
        // check edges from characters
        for (T character : characters) {
            deleted = deleted || character.edges.retainAll(taxa);
        }
        // check reverse edges from taxa
        for (T taxon : taxa) {
            deleted = deleted || taxon.edges.retainAll(characters);
        }
        return deleted;
    }

    //########## methods for edge identical character mappin ##########
    public abstract void addCharacterToDummyMapping(T character, T dummy);

    public abstract void removeCharacterFromDummyMapping(T character);

    public abstract void insertCharacterMapping(AbstractFlipCutGraph<T> source, final Map<T, T> oldToNew);
    //########## methods for edge identical character mappin END ##########


    //########## methods for guide tree mapping ##########
    protected void addTreeNodeCharGuideTreeMapping(TreeNode character, T c) {
        charToTreeNode.put(c, character);
        treeNodeToChar.put(character, c);
    }

    protected void removeTreeNodeCharGuideTreeMapping(TreeNode character) {
        charToTreeNode.remove(treeNodeToChar.get(character));
        treeNodeToChar.remove(character);
    }

    protected void removeTreNodeCharGuideTreeMapping(T c) {
        treeNodeToChar.remove(charToTreeNode.get(c));
        charToTreeNode.remove(c);
    }

    //todo maybe use one global map via references and only actualize active partitions --> because maps are read only. we do not need to split them into separate ones
    public void insertScaffPartData(AbstractFlipCutGraph<T> source, final Map<T, T> oldToNew) {
        charToTreeNode = new HashMap();
        treeNodeToChar = new HashMap();
        activePartitions = new HashSet<>();
        if (!source.activePartitions.isEmpty()) {
            for (Map.Entry<T, TreeNode> entry : source.charToTreeNode.entrySet()) {
                T sourceNode;
                if (oldToNew != null) {
                    sourceNode = oldToNew.get(entry.getKey());
                } else {
                    sourceNode = entry.getKey();
                }
                if (characters.contains(sourceNode)) {
                    addTreeNodeCharGuideTreeMapping(entry.getValue(), sourceNode);
                }
            }

            for (T activePartition : source.activePartitions) {
                T sourceNode;
                if (oldToNew != null) {
                    sourceNode = oldToNew.get(activePartition);
                } else {
                    sourceNode = activePartition;
                }
                if (characters.contains(sourceNode))
                    activePartitions.add(sourceNode);
            }
        }
    }
    //########## methods for guide tree mapping END ##########
}
