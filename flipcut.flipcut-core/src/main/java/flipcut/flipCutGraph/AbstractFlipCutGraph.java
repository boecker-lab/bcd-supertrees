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

    protected  Map<T,TreeNode> charToTreeNode = null;
    protected  Map<TreeNode,T> treeNodeToChar = null;

    /**
     * Turn on/off scm tree based taxa merging
     */
    public static final boolean SCAFF_TAXA_MERGE = true;
    public static final boolean ADAPTIVE_LEVEL = false;

    protected  Set<T> activePartitions = null;

    /**
     * The character vertex set
     */
    public final List<T> characters;
    /**
     * The taxa vertex set
     */
    public final List<T> taxa;

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


    protected AbstractFlipCutGraph(CostComputer costs, int bootstrapThreshold){
        List<List<T>> data = createGraphData(costs,bootstrapThreshold);
        this.characters = data.get(0);
        this.taxa = data.get(1);
        parentNode = null;
        treeNode = new TreeNode();
        /*
       Sort the leaves alphabetically to ensure we find the same mincut
       for multiple runs
        */
        sortTaxa();
    }



    /**
     * Create a new graph with a list of characters and taxa and a tree node. The parent node
     * is optional and can be null if this is the root graph. No checks for edges that
     * connect the given vertex set to nodes no in this set are done!
     *
     * @param characters the character
     * @param taxa       the taxa
     */
    protected AbstractFlipCutGraph(List<T> characters, List<T> taxa, TreeNode parentNode) {
        this.characters = characters;
        this.taxa = taxa;
        this.parentNode = parentNode;
        treeNode = new TreeNode();
        /*
       Sort the leaves alphabetically to ensure we find the same mincut
       for multiple runs
        */
        sortTaxa();
    }



    /**
     * Takes a list  of nodes and splits them into characters and taxa. Then we check for
     * edges that connect the vertices to nodes not contained in this graphs vertex set and remove those edges.
     *
     * @param nodes the nodes
     */
    public AbstractFlipCutGraph(List<T> nodes, TreeNode parentNode) {
        characters = new ArrayList<T>();
        taxa = new ArrayList<T>();
        for (T node : nodes) {
            if (node.isTaxon()) {
                taxa.add(node);
            } else {
                characters.add(node);
            }
        }

        // checks an removes edges to taxa that are not in this component!!!
        checkEdges();

        this.parentNode = parentNode;
        treeNode = new TreeNode();

        /*
       Sort the leaves alphabetically to ensure we find the same mincut
       for multiple runs
        */
        sortTaxa();
    }

    /*
       Sorts the leaves alphabetically to ensure we find the same mincut
       for multiple runs
     */
    protected void sortTaxa() {
        Collections.sort(taxa, new Comparator<T>() {
            public int compare(T o1, T o2) {
                return o1.name.compareTo(o2.name);
            }
        });
    }



    public abstract int mergeRetundantCharacters();
    protected abstract List<List<T>> createGraphData(CostComputer costs, int bootstrapThreshold);

    protected void removeCharacters(Collection<T> toRemove, Collection<T> characters){
        //remove chracters and edges to them from graph

        for (T remove : toRemove) {
            if (characters.remove(remove)) {
                // remove edges to taxa
                for (T taxon : remove.edges) {
                    taxon.edges.remove(remove);
                }
                //check that no active scaffold character gets removded
                //JUST for DEBUGGING
                if (SCAFF_TAXA_MERGE && DEBUG){
                    if (charToTreeNode.containsKey(remove)){
                        System.out.println("ERROR: Illegal SCAFFOLD character deletion!!! " + toRemove.toString());
                    }
                }
            }
        }

        if (ADAPTIVE_LEVEL) {
            for (T character : characters) {
                character.parents.removeAll(toRemove);
            }
        }
    }

    protected void removeCharacters(Collection<T> toRemove){
        removeCharacters(toRemove,characters);
    }

    /**
     * Remove semi universal characters
     */
    public  List<T> deleteSemiUniversals() {
        List<T> toRemove = new ArrayList<T>();
        for (T character : characters) {
            if (character.isSemiUniversal()) {
                toRemove.add(character);
                //remove deleted partitions an insert child partitions

                if (SCAFF_TAXA_MERGE){
                    TreeNode node = charToTreeNode.get(character);
                    if (node!= null){
                        Set<T> partition = new HashSet<>(character.edges);
                        Set<T> contolPartition = new HashSet<>(character.edges.size()); //todo debugging maybe remove
                        Set<T> toInsert = new HashSet(node.childCount());
                        for (TreeNode child : node.getChildren()) {
                            if (child.isInnerNode()) {
                                T n = treeNodeToChar.get(child);
                                toInsert.add(n);
//                                contolPartition.addAll(n.edges); //todo DBUG
                            }
                        }
                        activePartitions.remove(character);
                        activePartitions.addAll(toInsert);
                        /*if (partition.equals(contolPartition)) {
                            activePartitions.remove(character);
                            activePartitions.addAll(toInsert);
                        }else
                            System.out.println("ERROR: something goes WRONG DURING SCAFFOLD PARTITIONning");*/

                        removeTreNodeCharMapping(character);
                    }
                }
            }
        }
        if (DEBUG)
            System.out.println("Removing " + toRemove.size() + " semiUniversals");
        removeCharacters(toRemove);
        return toRemove;
    }


    /**
     * Splits this graph into two disconnected graphs, one consisting of the given set
     * of nodes, the other graph consists of all vertices not contained in the given
     * set of nodes.
     *
     * @param sinkNodes the set of nodes for
     * @return graphs list of two graphs created
     */
    abstract List<? extends AbstractFlipCutGraph<T>> split(List<T> sinkNodes);



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
    protected void checkEdges(){
        // check edges from characters
        for (T character : characters) {
            character.edges.retainAll(taxa); //Todo retain all ineffective???
        }

        // check reverse edges from taxa
        for (T taxon : taxa) {
            taxon.edges.retainAll(characters);
        }
    }

    protected void addTreNodeCharMapping(TreeNode character, T c){
        charToTreeNode.put(c,character);
        treeNodeToChar.put(character,c);
    }

    protected void removeTreNodeCharMapping(TreeNode character){
        charToTreeNode.remove(treeNodeToChar.get(character));
        treeNodeToChar.remove(character);
    }

    protected void removeTreNodeCharMapping(T c){
        treeNodeToChar.remove(charToTreeNode.get(c));
        charToTreeNode.remove(c);
    }

    public void insertScaffPartData(AbstractFlipCutGraph<T> source, final Map<T,T> oldToNew){
        charToTreeNode = new HashMap();
        treeNodeToChar = new HashMap();
        for (Map.Entry<T, TreeNode> entry : source.charToTreeNode.entrySet()) {
            T sourceNode;
            if (oldToNew != null) {
                sourceNode = oldToNew.get(entry.getKey());
            } else {
                sourceNode = entry.getKey();
            }
            if (characters.contains(sourceNode)){
                addTreNodeCharMapping(entry.getValue(),sourceNode);
            }
        }

        activePartitions = new HashSet<>();
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
