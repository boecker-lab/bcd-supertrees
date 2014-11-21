package flipCut.flipCutGraph;

import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import flipCut.costComputer.CostComputer;

import java.util.*;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 09.01.13
 * Time: 14:58
 */
public class FlipCutGraphSimpleWeight extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight> {

    public FlipCutGraphSimpleWeight(CostComputer costs, double bootstrapThreshold) {
        super(costs, bootstrapThreshold);
    }

    protected FlipCutGraphSimpleWeight(List<FlipCutNodeSimpleWeight> characters, List<FlipCutNodeSimpleWeight> taxa, TreeNode parentNode) {
        super(characters, taxa, parentNode);
    }

    public FlipCutGraphSimpleWeight(List<FlipCutNodeSimpleWeight> nodes, TreeNode parentNode) {
        super(nodes, parentNode);
    }

    @Override
    List<? extends FlipCutGraphSimpleWeight> split(List<FlipCutNodeSimpleWeight> sinkNodes) {
        List<List<List<FlipCutNodeSimpleWeight>>> graphData = splitToGraphData(sinkNodes);
        List<FlipCutGraphSimpleWeight> graphs = new LinkedList<FlipCutGraphSimpleWeight>();
        FlipCutGraphSimpleWeight g1 =  new FlipCutGraphSimpleWeight(graphData.get(0).get(0),graphData.get(0).get(1),treeNode);
        FlipCutGraphSimpleWeight g2 = new FlipCutGraphSimpleWeight(graphData.get(1).get(0),graphData.get(1).get(1),treeNode);
        graphs.add(g1);
        graphs.add(g2);

        return graphs;
    }

    @Override
    protected List<List<FlipCutNodeSimpleWeight>> createGraphData(CostComputer costs, double bootstrapThreshold){
        List<Tree> inputTrees = new ArrayList<>(costs.getTrees());
        Tree scaff = null;
        if (SCAFF_TAXA_MERGE){
            scaff = costs.getScaffoldTree();
            charToTreeNode = new HashMap<>();
            treeNodeToChar = new HashMap<>();
            activePartitions =  new HashSet<>();
        }
        Map<FlipCutNodeSimpleWeight,TreeNode> charToNode;
        Map<TreeNode,FlipCutNodeSimpleWeight> nodeToChar;
        if (ADAPTIVE_LEVEL){
            charToNode = new HashMap<>();
            nodeToChar =  new HashMap<>();
        }
        Map<String, FlipCutNodeSimpleWeight> taxa = new HashMap<String, FlipCutNodeSimpleWeight>();
        List<FlipCutNodeSimpleWeight> characters = new LinkedList<FlipCutNodeSimpleWeight>();

        //create taxon list
        for (Tree tree : inputTrees) {
            for (TreeNode taxon : tree.getLeaves()) {
                if(!taxa.containsKey(taxon.getLabel())){
                    FlipCutNodeSimpleWeight n = new FlipCutNodeSimpleWeight(taxon.getLabel());
                    taxa.put(n.name, n);
                }
            }
        }

        //create character list
        for (Tree tree : inputTrees) {
            Map<String, FlipCutNodeSimpleWeight> leaves = new HashMap<String, FlipCutNodeSimpleWeight>();
            for (TreeNode treeNode : tree.getLeaves()) {
                leaves.put(treeNode.getLabel(), taxa.get(treeNode.getLabel()));
            }

            for (TreeNode character : tree.vertices()) {
                //skip leaves
                if(character.isLeaf()) continue;
                // also skip root
                if(character == tree.getRoot()) continue;


                // skip character with small bootstrap value
                if (character.getLabel() != null){
                    double bootstrap = Double.valueOf(character.getLabel());
                    if (bootstrap < bootstrapThreshold){
                        System.out.println("Ignore character with bootstrap value: " + bootstrap + " < " + bootstrapThreshold);
                        continue;
                    }
                }

                Map<String, FlipCutNodeSimpleWeight> chracterLeaves = new HashMap<String, FlipCutNodeSimpleWeight>();
                for (TreeNode treeNode : character.getLeaves()) {
                    chracterLeaves.put(treeNode.getLabel(), taxa.get(treeNode.getLabel()));
                }

                FlipCutNodeSimpleWeight c = new FlipCutNodeSimpleWeight(null,new HashSet<FlipCutNodeSimpleWeight>(chracterLeaves.size()), new HashSet<FlipCutNodeSimpleWeight>(leaves.size() - chracterLeaves.size()));

                c.edgeWeight = costs.getEdgeWeight(character,null,(TreeNode)null);

                if (ADAPTIVE_LEVEL){
                    c.parents =  new HashSet<>();
                    nodeToChar.put(character,c);
                    charToNode.put(c,character);
                }

                //insert scaffold characters to mapping if activated
                if (SCAFF_TAXA_MERGE){
                    if (scaff != null && tree.equals(scaff)){
                        addTreNodeCharMapping(character,c);
                        //create set of active partitions
                        if (character.getParent().equals(scaff.getRoot()))
                            activePartitions.add(c);
                    }
                }

                for (FlipCutNodeSimpleWeight taxon : leaves.values()) {
                    //add leaves and set to "1"
                    if (chracterLeaves.containsKey(taxon.name)) {
                        c.addEdgeTo(taxon);
                        // add reverse edge
                        taxon.addEdgeTo(c);
                        //now set all nodes of this tree to "0"
                    } else {
                        c.addImaginaryEdgeTo(taxon);
                    }
                }
                // add to characters list
                characters.add(c);
            }
        }

        if (ADAPTIVE_LEVEL) {
            for (FlipCutNodeSimpleWeight c : characters) {
                TreeNode child = charToNode.get(c);
                TreeNode parent;

                while ( (parent = child.getParent()) != null ){
                    c.parents.add(nodeToChar.get(parent));
                    child = parent;
                }
            }
        }

        if (DEBUG)
            if (charToTreeNode != null)
                System.out.println("Scaffold node number: " + charToTreeNode.size());
        return Arrays.asList(characters, new ArrayList<FlipCutNodeSimpleWeight>(taxa.values()));
    }

    //todo maybe some optimization possible!
    @Override
    public int mergeRetundantCharacters() {
        Set<FlipCutNodeSimpleWeight> toRemove = new HashSet<FlipCutNodeSimpleWeight>();
        int dupletsCounter = 0;
        for (int i = 0; i < characters.size() - 1; i++) {
            if (!toRemove.contains(characters.get(i))) {
                for (int j = i + 1; j < characters.size(); j++) {
                    FlipCutNodeSimpleWeight charac = characters.get(j);
                    if (!toRemove.contains(charac)) {
                        if (characters.get(i).compareChar(charac)) {
                            toRemove.add(charac);
                            //sum up edge weights
                            characters.get(i).edgeWeight += charac.edgeWeight;
                            dupletsCounter++;
                        }
                    }
                }
            }
        }
        removeCharacters(toRemove);
        return dupletsCounter;
    }

    protected List<List<List<FlipCutNodeSimpleWeight>>> splitToGraphData(List<FlipCutNodeSimpleWeight> sinkNodes) {
        List<FlipCutNodeSimpleWeight> g1Characters = new LinkedList<FlipCutNodeSimpleWeight>();
        List<FlipCutNodeSimpleWeight> g1Taxa = new LinkedList<FlipCutNodeSimpleWeight>();
        List<FlipCutNodeSimpleWeight> g2Characters = new LinkedList<FlipCutNodeSimpleWeight>();
        List<FlipCutNodeSimpleWeight> g2Taxa = new LinkedList<FlipCutNodeSimpleWeight>();
        List<FlipCutNodeSimpleWeight> charactersToRemove = new ArrayList<FlipCutNodeSimpleWeight>();

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
        g2Characters.addAll(characters);
        g2Characters.removeAll(g1Characters);
        g2Taxa.addAll(taxa);
        g2Taxa.removeAll(g1Taxa);

        // remove characters from g1 if we have to remove any
        removeCharacters(charactersToRemove,g1Characters);

        // remove characters from g2 if we have to remove any
        removeCharacters(charactersToRemove,g2Characters);

        // remove all edges between g2 characters and g1 taxa
        removeEdgesToOtherGraph(g2Characters, g1Taxa);

        // remove edges between g1 characters and g2 taxa
        removeEdgesToOtherGraph(g1Characters, g2Taxa);

        // create the sub graphs
        List<List<FlipCutNodeSimpleWeight>> g1 = Arrays.asList(g1Characters, g1Taxa);
        List<List<FlipCutNodeSimpleWeight>> g2 = Arrays.asList(g2Characters, g2Taxa);

        return Arrays.asList(g1, g2);
    }

    //helper method for split
    protected void removeEdgesToOtherGraph(List<FlipCutNodeSimpleWeight> aCharacters, List<FlipCutNodeSimpleWeight> bTaxa){
        for (FlipCutNodeSimpleWeight aCharacter : aCharacters) {
            for (FlipCutNodeSimpleWeight bTaxon : bTaxa) {
                //update zero edge counter
                if (aCharacter.imaginaryEdges.remove(bTaxon)) { // < 0
                    //todo maybe include semiuniversal deletion here???
                //remove edge to other side
                } else if (aCharacter.edges.remove(bTaxon)) { // > 0
                    // remove reverse edge
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
            oldToNew.put(taxon,taxon.copy());
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
    protected void checkEdges() {
        // check edges from characters
        for (FlipCutNodeSimpleWeight character : characters) {
            character.edges.retainAll(taxa);
            character.imaginaryEdges.retainAll(taxa);
        }

        // check reverse edges from taxa
        for (FlipCutNodeSimpleWeight taxon : taxa) {
            taxon.edges.retainAll(characters);
        }
    }
}
