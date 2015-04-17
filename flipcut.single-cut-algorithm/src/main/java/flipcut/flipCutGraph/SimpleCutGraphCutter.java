package flipcut.flipCutGraph;


import flipcut.costComputer.CostComputer;
import flipcut.mincut.CutGraph;
import flipcut.mincut.bipartition.BasicCut;
import flipcut.mincut.goldberg_tarjan.GoldbergTarjanCutGraph;

import java.util.*;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 14.02.13
 * Time: 11:12
 */
public abstract class SimpleCutGraphCutter<T extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight>> extends CutGraphCutter<FlipCutNodeSimpleWeight, T> {
    protected static final boolean DEBUG = false;



    public SimpleCutGraphCutter(CutGraphTypes type) {
        super(type);
    }

    //################### graph creation ######################
    protected void createTarjanGoldbergHyperGraph(CutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        addTaxa(cutGraph);

        // add edges
        for (FlipCutNodeSimpleWeight character : source.characters) {
            for (FlipCutNodeSimpleWeight taxon : character.edges) {
                //todo what is a useful way for a constant
                long weight = CostComputer.ACCURACY * INFINITY;
                cutGraph.addEdge(character, taxon, weight);
                cutGraph.addEdge(taxon, character.clone, weight);
            }
        }
    }

    protected void createTarjanGoldbergHyperGraphMerged(CutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        addTaxa(cutGraph);
        Set<FlipCutNodeSimpleWeight> edgesAlreadySet = new HashSet<FlipCutNodeSimpleWeight>(dummyToMerged.size());

        // add edges //todo merge with character adding step
        for (FlipCutNodeSimpleWeight character : source.characters) {
            FlipCutNodeSimpleWeight dummy = nodeToDummy.get(character);
            if (dummy != null) {
                if (!edgesAlreadySet.contains(dummy)) {
                    edgesAlreadySet.add(dummy);
                    for (FlipCutNodeSimpleWeight taxon : dummy.edges) {
                        //todo what is a useful way for a constant
                        long weight = CostComputer.ACCURACY * INFINITY;
                        cutGraph.addEdge(dummy, taxon, weight);
                        cutGraph.addEdge(taxon, dummy.clone, weight);
                    }
                }
            }else{
                System.out.println("ERROR: somthing is wrong with merge map! Again!! Again!!");
            }
        }
    }

    //helper method for the goldberg tarjan graphs
    protected void createTarjanGoldbergHyperGraphTaxaMerged(CutGraph<FlipCutNodeSimpleWeight> cutGraph, Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> taxonToMerged, Map<FlipCutNodeSimpleWeight,Set<FlipCutNodeSimpleWeight>> taxonGroupTotrivialcharacters) {
        //add taxa
        cutGraphTaxa = new ArrayList<>(taxonToMerged.values());
        for (FlipCutNodeSimpleWeight mergedTaxon : cutGraphTaxa) {
            cutGraph.addNode(mergedTaxon);
        }

        // add characters, character clones and edges between them
        long infinity = CostComputer.ACCURACY * INFINITY;
        Map<Set<FlipCutNodeSimpleWeight>,FlipCutNodeSimpleWeight> charsToAdd = new HashMap<>();
        Map<Object,Long> dummyToWeight = new HashMap<>();

        for (FlipCutNodeSimpleWeight character : source.characters) {
            Set<FlipCutNodeSimpleWeight> edgesTo = new HashSet<>();
            for (FlipCutNodeSimpleWeight t : character.edges) {
                edgesTo.add(taxonToMerged.get(t));
            }
            //check if character is not trivial
            if (edgesTo.size() > 1) {
                Set<FlipCutNodeSimpleWeight> set;
                Set<FlipCutNodeSimpleWeight> cloneSet;
                FlipCutNodeSimpleWeight n;
                if (!charsToAdd.containsKey(edgesTo)) {
                    set = new HashSet<>();
                    cloneSet = new HashSet<>();

                    charsToAdd.put(edgesTo, character);
                    dummyToWeight.put(character, 0l);

                    dummyToMerged.put(character,set);
                    dummyToMerged.put(character.clone,cloneSet);

                    n = character;
                }else{
                    n = charsToAdd.get(edgesTo);
                    set = dummyToMerged.get(n);
                    cloneSet = dummyToMerged.get(n);
                }


                set.add(character);
                cloneSet.add(character.clone);
                dummyToWeight.put(n, (dummyToWeight.get(n) + calculateCharacterCap(character)));

            }else {
                FlipCutNodeSimpleWeight tax = edgesTo.iterator().next();
                if (!taxonGroupTotrivialcharacters.containsKey(tax)){
                    taxonGroupTotrivialcharacters.put(tax, new HashSet<FlipCutNodeSimpleWeight>());
                }
                taxonGroupTotrivialcharacters.get(tax).add(character);
                taxonGroupTotrivialcharacters.get(tax).add(character.clone);
            }
        }

        //add merged chars
        for (Map.Entry<Set<FlipCutNodeSimpleWeight>, FlipCutNodeSimpleWeight> edgeToAdd : charsToAdd.entrySet()) {
            FlipCutNodeSimpleWeight dummyNode = edgeToAdd.getValue();

            //add character and its clone
                cutGraph.addNode(dummyNode);
                cutGraph.addNode(dummyNode.clone);
                //character weight
                cutGraph.addEdge(dummyNode.clone, dummyNode, dummyToWeight.get(dummyNode));

            // add edges to taxa
            Set<FlipCutNodeSimpleWeight> edgesTo = edgeToAdd.getKey();
            for (FlipCutNodeSimpleWeight taxon : edgesTo) {
                cutGraph.addEdge(dummyNode, taxon, infinity);
                cutGraph.addEdge(taxon, dummyNode.clone, infinity);
            }
        }
    }


    protected void createGoldbergTarjan(CutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        addTaxa(cutGraph);
        // add edges
        for (FlipCutNodeSimpleWeight character : source.characters) {
            for (FlipCutNodeSimpleWeight taxon : character.edges) {
                cutGraph.addEdge(character, taxon, character.edgeWeight);
                cutGraph.addEdge(taxon, character.clone, character.edgeWeight);
            }
        }

        //return cutGraph;
    }

    protected void createGoldbergTarjanMerged(CutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        //todo proof
        addTaxa(cutGraph);
        Set<FlipCutNodeSimpleWeight> edgesAlreadySet = new HashSet<FlipCutNodeSimpleWeight>(dummyToMerged.size());

        // add edges
        for (FlipCutNodeSimpleWeight character : source.characters) {
            if (!nodeToDummy.containsKey(character)) {
                for (FlipCutNodeSimpleWeight taxon : character.edges) {
                    cutGraph.addEdge(character, taxon, character.edgeWeight);
                    cutGraph.addEdge(taxon, character.clone, character.edgeWeight);
                }
            }else{
                FlipCutNodeSimpleWeight dummy = nodeToDummy.get(character);
                if (!edgesAlreadySet.contains(dummy)) {
                    edgesAlreadySet.add(dummy);
                    for (FlipCutNodeSimpleWeight taxon : dummy.edges) {
                        cutGraph.addEdge(dummy, taxon, dummy.edgeWeight);
                        cutGraph.addEdge(taxon, dummy.clone, dummy.edgeWeight);
                    }
                }
            }
        }
        //return cutGraph;
    }

    //helper method for the goldberg tarjan graphs
    protected  void addTaxa(CutGraph<FlipCutNodeSimpleWeight> cutGraph){
        for (FlipCutNodeSimpleWeight node : source.taxa) {
            cutGraph.addNode(node);
        }
    }

    protected long calculateCharacterCap(FlipCutNodeSimpleWeight character) {
        long characterCap;
        if (IGNORE_MATRIX_ENTRIES) {
            characterCap = character.edgeWeight;
        }else{
            if (REAL_CHAR_DELETION) {
                characterCap = character.edgeWeight * (Math.min(character.imaginaryEdges.size(),character.edges.size()));
            } else {
                if (ZEROES) {
                    characterCap = character.edgeWeight * character.imaginaryEdges.size();
                }else{
                    characterCap = character.edgeWeight * character.edges.size();
                }
            }
        }
        if (MAX_FLIP_NORMALIZATION && !IGNORE_MATRIX_ENTRIES)
            characterCap /= (character.edges.size() + character.imaginaryEdges.size());

        return characterCap;
    }

    protected void createGoldbergTarjanCharacterWeights(CutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        // add characters, character clones and edges between them
        for (FlipCutNodeSimpleWeight character : source.characters) {
            cutGraph.addNode(character);
            cutGraph.addNode(character.clone);
            //character weight
            long characterCap = calculateCharacterCap(character);

            cutGraph.addEdge(character.clone, character, characterCap);
        }
    }



    //helper method for the goldberg tarjan graphs
    protected void createGoldbergTarjanCharacterWeightsMerged(CutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        Set<FlipCutNodeSimpleWeight> inGraph = new HashSet<FlipCutNodeSimpleWeight>();
        // add characters, character clones and edges between them
        //todo do as default and use map to iterate. no double iterations needed.
        for (FlipCutNodeSimpleWeight character : source.characters) {
            FlipCutNodeSimpleWeight dummy = nodeToDummy.get(character);
            if (dummy != null){
                if (!inGraph.contains(dummy)){
                    inGraph.add(dummy);
                    cutGraph.addNode(dummy);
                    cutGraph.addNode(dummy.clone);
                    //calculate character weight
//                    long charCap = dummy.edgeWeight;
                    long charCap = 0;//todo only global map compatible
                    for (FlipCutNodeSimpleWeight charac : dummyToMerged.get(dummy)) {
                        charCap += calculateCharacterCap(charac);
                    }
                    cutGraph.addEdge(dummy.clone, dummy, charCap);
                }
            }else{
                System.out.println("ERROR: Something with the character merge map went wrong AGAIN!!!");
            }
        }
    }



    //micut calculation
    protected BasicCut<FlipCutNodeSimpleWeight> calculateTarjanMinCut(GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight> cutGraph) {

        List<FlipCutNodeSimpleWeight> taxa;

        if (cutGraphTaxa != null) {
            taxa = cutGraphTaxa;
        } else {
            taxa = new ArrayList<>(source.taxa);
        }

        // get the mincut, fix s iterate over t
        FlipCutNodeSimpleWeight s = taxa.get(0);
        BasicCut<FlipCutNodeSimpleWeight> minCut = BasicCut.MAX_CUT_DUMMY;
        FlipCutNodeSimpleWeight t = null;
        for (int i = 1; i < taxa.size(); i++) {
            t = taxa.get(i);
            cutGraph.calculate(s, t);
            BasicCut<FlipCutNodeSimpleWeight> next =  cutGraph.getMinCut();
            if (next.minCutValue < minCut.minCutValue) {
                minCut = next;
            }
            //Attention! no handling of cooptimal mincut!
        }
        return minCut;
    }

    @Override
    protected void calculateMinCut() {
        CutGraph<FlipCutNodeSimpleWeight> cutGraph;
        switch (type) {
            case MAXFLOW_TARJAN_GOLDBERG: {
                if (mergeCharacters) {
                    if (source.GLOBAL_CHARACTER_MERGE)
                        System.out.println("WARNING: static character map is not implemented for edge! The slower NON STATIC Version is used instead");
                    int merged = buildCharacterMergingMap(source, false);
                    if (DEBUG) System.out.println(merged + "characters merged before mincut");
                    cutGraph = new GoldbergTarjanCutGraph<>();
                    createGoldbergTarjanCharacterWeightsMerged(cutGraph);
                    createGoldbergTarjanMerged(cutGraph);
                    BasicCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut((GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight>) cutGraph);

                    mincut =  new LinkedHashSet<>(source.characters.size() + source.taxa.size());
                    for (FlipCutNodeSimpleWeight node : newMinCut.getSinkSet()) {
                        Set<FlipCutNodeSimpleWeight> realNodes = dummyToMerged.get(node);
                        if (realNodes == null) {
                            if (node.isTaxon()) { //todo debug if --> remove!
                                mincut.add(node);
                            } else {
                                System.out.println("ERROR: something with the character merge map went wrong");
                            }
                        } else {
                            mincut.addAll(realNodes);
                        }
                    }
                } else {
                    cutGraph = new GoldbergTarjanCutGraph<>();
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    createGoldbergTarjan(cutGraph);
                    mincut = calculateTarjanMinCut((GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight>) cutGraph).getSinkSet();
                }


                break;
            }
          /*  case HYPERGRAPH_MINCUT: {
                //Todo implement if hypergraph mincut immplemetation is avalible
                if (mergeCharacters) {

                } else {

                }
                break;
            }*/
            case HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW: {
                if (mergeCharacters) {
                    int merged = buildCharacterMergingMap(source, source.GLOBAL_CHARACTER_MERGE);
                    if (DEBUG) System.out.println(merged + "characters merged before mincut");

                    cutGraph = new GoldbergTarjanCutGraph<>();
                    createGoldbergTarjanCharacterWeightsMerged(cutGraph);
                    createTarjanGoldbergHyperGraphMerged(cutGraph);
                    BasicCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut((GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight>) cutGraph);
                    mincut =  new LinkedHashSet<>(source.characters.size() + source.taxa.size());
                    for (FlipCutNodeSimpleWeight node : newMinCut.getSinkSet()) {
                        Set<FlipCutNodeSimpleWeight> realNodes = dummyToMerged.get(node);

                        if (realNodes == null) {
                            if (node.isTaxon()) { //todo debug if --> remove!
                                mincut.add(node);
                            } else {
                                System.out.println("ERROR: something with the character merge map went wrong");
                            }
                        } else {
                            mincut.addAll(realNodes);
                        }
                    }







                } else if (source.SCAFF_TAXA_MERGE && source.activePartitions != null) {
                    //create mapping
                    //todo optimize all these mapping stuff if it works good
                    Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> taxonToDummy = new HashMap<>();
                    Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToTaxa = new HashMap<>();
                    Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> trivialcharacters = new HashMap<>();
                    dummyToMerged = new HashMap<>();//todo incompatible with character merging. find better solution

                    int mergedTaxonIndex = 0;
                    for (FlipCutNodeSimpleWeight scaffChar : source.activePartitions) {
                        FlipCutNodeSimpleWeight mergeTaxon = new FlipCutNodeSimpleWeight("TaxonGrooup_" + mergedTaxonIndex);
                        for (FlipCutNodeSimpleWeight taxon : scaffChar.edges) {
                            taxonToDummy.put(taxon, mergeTaxon);
                        }
                        dummyToTaxa.put(mergeTaxon, scaffChar.edges);
                        mergedTaxonIndex++;
                    }
                    for (FlipCutNodeSimpleWeight taxon : source.taxa) {
                        if (!taxonToDummy.containsKey(taxon)) {
                            taxonToDummy.put(taxon, taxon);
                        }
                    }

                    //create cutgraph
                    cutGraph = new GoldbergTarjanCutGraph<>();
                    createTarjanGoldbergHyperGraphTaxaMerged(cutGraph, taxonToDummy, trivialcharacters);

                    //calculate mincut
                    BasicCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut((GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight>) cutGraph);

                    //undo mapping
                    mincut =  new LinkedHashSet<>(source.characters.size() + source.taxa.size());
                    for (FlipCutNodeSimpleWeight node : newMinCut.getSinkSet()) {

                        //undo taxa mapping
                        if (node.isTaxon()) {
                            Set<FlipCutNodeSimpleWeight> trivials = trivialcharacters.get(node);
                            if (trivials != null)
                                mincut.addAll(trivials);

                            Set<FlipCutNodeSimpleWeight> realT = dummyToTaxa.get(node);
                            if (realT != null)
                                mincut.addAll(realT);
                            else
                                mincut.add(node);

                            //undo character mapping
                        } else {
                            Set<FlipCutNodeSimpleWeight> realNodes = dummyToMerged.get(node);
                            mincut.addAll(realNodes);
                        }
                    }
                } else {
                    cutGraph = new GoldbergTarjanCutGraph<>();
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    createTarjanGoldbergHyperGraph(cutGraph);
                    mincut = calculateTarjanMinCut((GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight>) cutGraph).getSinkSet();
                }
                break;
            }
        }
    }

    //merges characters with same edgeset and sums up the weights to reduce node and edgeset before mincut!
    @Override
    protected int buildCharacterMergingMap(T source, final boolean globalMap){

        int dupletsCounter;
        if (globalMap) {
            dummyToMerged = source.dummyToCharacters;
            nodeToDummy = source.characterToDummy;
            dupletsCounter = nodeToDummy.size() - dummyToMerged.size();
        } else {
            //todo this is stupid!!! OPTIMIZE after proof
            Set<FlipCutNodeSimpleWeight> alreadyInMergeSet = new HashSet<FlipCutNodeSimpleWeight>();
            dummyToMerged = new HashMap<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>>();
            nodeToDummy = new HashMap<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight>();
            List<FlipCutNodeSimpleWeight> characters = new ArrayList<>(source.characters);
            List<FlipCutNodeSimpleWeight> charactersAdded = new ArrayList<>(source.characters);
            dupletsCounter = 0;
            //ATTENTION: size -1 does not work, because if last note is alone in a group, it wont be added to the cut graph
            for (int i = 0; i < characters.size(); i++) {
                FlipCutNodeSimpleWeight charac1 = characters.get(i);
                if (!alreadyInMergeSet.contains(charac1)) {
                    FlipCutNodeSimpleWeight dummy = new FlipCutNodeSimpleWeight(charac1.edges);
                    dummy.edgeWeight = charac1.edgeWeight;
                    Set<FlipCutNodeSimpleWeight> toMerge = new HashSet<FlipCutNodeSimpleWeight>();
                    toMerge.add(charac1);
                    Set<FlipCutNodeSimpleWeight> toMergeClones = new HashSet<FlipCutNodeSimpleWeight>();
                    toMergeClones.add(charac1.clone);
                    alreadyInMergeSet.add(charac1); //not really needed but consistent
                    //just for debugging
                    charactersAdded.add(charac1);

                    for (int j = i + 1; j < characters.size(); j++) {
                        FlipCutNodeSimpleWeight charac2 = characters.get(j);
                        if (!alreadyInMergeSet.contains(charac2)) {
                            if (charac1.compareCharLazy(charac2)) {
                                alreadyInMergeSet.add(charac2);
                                toMerge.add(charac2);
                                toMergeClones.add(charac2.clone);
                                nodeToDummy.put(charac2,dummy);
                                //sum up edge weights
                                dummy.edgeWeight += charac2.edgeWeight;
                                charactersAdded.add(charac2);
                                dupletsCounter++;
                            }
                        }
                    }
                    dummyToMerged.put(dummy,toMerge);
                    dummyToMerged.put(dummy.clone,toMergeClones);
                    nodeToDummy.put(charac1,dummy);
                    dummy.edges.addAll(charac1.edges);
                }
            }
        }
        return dupletsCounter;
    }
}
