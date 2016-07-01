package phylo.tree.algorithm.flipcut.flipCutGraph;


import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.MaxFlowCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.BasicCut;
import phylo.tree.algorithm.flipcut.model.Cut;

import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 19.04.13
 * Time: 12:02
 */
public class MultiCutGraphCutterGreedy extends SimpleCutGraphCutter<FlipCutGraphMultiSimpleWeight> implements MultiCutter  {

    Set<FlipCutNodeSimpleWeight> blacklist = new HashSet<>();
    private boolean stopCutting;

    public MultiCutGraphCutterGreedy(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut) {
        super(type);
        source = graphToCut;
        stopCutting = false;
    }

//    @Override
    /*protected void createGoldbergTarjanCharacterWeights(CutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        // add characters, character clones and edges between them
        for (FlipCutNodeSimpleWeight character : source.characters) {
            cutGraph.addNode(character);
            cutGraph.addNode(character.clone);
            //character weight
            long characterCap;
            if (!blacklist.contains(character)) {

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
                if (MAX_FLIP_NORMALIZATION)
                    characterCap /= (character.edges.size() + character.imaginaryEdges.size());

            }else{
                characterCap =  CostComputer.ACCURACY * INFINITY;
            }

            cutGraph.addEdge(character.clone, character, characterCap);
        }
    }
*/
    protected void createGoldbergTarjanCharacterWeights(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        // add characters, character clones and edges between them
        for (FlipCutNodeSimpleWeight character : source.characters) {
            cutGraph.addNode(character);
            cutGraph.addNode(character.clone);
            //character weight
            long characterCap;
            if (!blacklist.contains(character)) {
                characterCap = calculateCharacterCap(character);
            }else{
                characterCap =  CostComputer.ACCURACY * INFINITY;
            }
            cutGraph.addEdge(character.clone, character, characterCap);
        }
    }

    @Override
    protected void createGoldbergTarjanCharacterWeightsMerged(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        Set<FlipCutNodeSimpleWeight> inGraph = new HashSet<FlipCutNodeSimpleWeight>();
        // add characters, character clones and edges between them

        for (FlipCutNodeSimpleWeight character : source.characters) {
            FlipCutNodeSimpleWeight dummy = nodeToDummy.get(character);
            if (dummy != null){
                if (!inGraph.contains(dummy)){
                    inGraph.add(dummy);
                    cutGraph.addNode(dummy);
                    cutGraph.addNode(dummy.clone);

                    //calculate character weight
                    long charCap;
                    if (!blacklist.contains(character)) {
                        charCap = 0;
                        for (FlipCutNodeSimpleWeight charac : dummyToMerged.get(dummy)) {
                            charCap += calculateCharacterCap(charac);
                        }
                    } else {
                        charCap =  CostComputer.ACCURACY * INFINITY;
                    }

                    cutGraph.addEdge(dummy.clone, dummy, charCap);
                }
            }else{
                System.out.println("ERROR: Something with the character merge map went wrong AGAIN!!!");
            }
        }
    }


    @Override
    protected void calculateMinCut() {
        GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight> cutGraph;
        switch (type) {
            /*case MAXFLOW_TARJAN_GOLDBERG: {
                    cutGraph = new CutGraph<FlipCutNodeSimpleWeight>();
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    createGoldbergTarjan(cutGraph);
                    calculateTarjanMinCut(cutGraph);
                break;
            }
            case HYPERGRAPH_MINCUT: {
                //Todo implement if hypergraph mincut immplemetation is avalible
                if (mergeCharacters) {

                } else {

                }
                break;
            }*/
           /* case HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG: {
                    cutGraph = new CutGraph<FlipCutNodeSimpleWeight>();
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    createTarjanGoldbergHyperGraph(cutGraph);
                    calculateTarjanMinCut(cutGraph);
                break;
            }*/
            //todo same code as in singe cut... finde better solution!!!!
            case HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG: {
                if(source.SCAFF_TAXA_MERGE && !source.activePartitions.isEmpty()) {
                    //create mapping
                    //todo optimize all thes mapping stuff if it works good
                    Map<FlipCutNodeSimpleWeight,FlipCutNodeSimpleWeight> taxonToDummy = new HashMap<>();
                    Map<FlipCutNodeSimpleWeight,Set<FlipCutNodeSimpleWeight>> dummyToTaxa = new HashMap<>();
                    Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> trivialcharacters = new HashMap<>();
                    dummyToMerged =  new HashMap<>();

                    int mergedTaxonIndex = 0;
                    for (FlipCutNodeSimpleWeight scaffChar : source.activePartitions) {
                        FlipCutNodeSimpleWeight mergeTaxon = new FlipCutNodeSimpleWeight("TaxonGrooup_" + mergedTaxonIndex);
                        for (FlipCutNodeSimpleWeight taxon : scaffChar.edges) {
                            taxonToDummy.put(taxon,mergeTaxon);
                        }
                        dummyToTaxa.put(mergeTaxon,scaffChar.edges);
                        mergedTaxonIndex++;
                    }
                    for (FlipCutNodeSimpleWeight taxon : source.taxa) {
                        if (!taxonToDummy.containsKey(taxon)) {
                            taxonToDummy.put(taxon, taxon);
                        }
                    }

                    //create cutgraph
                    cutGraph = new GoldbergTarjanCutGraph<>();
                    createTarjanGoldbergHyperGraphTaxaMerged(cutGraph,taxonToDummy,trivialcharacters);

                    //calculate mincut
                    BasicCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);
                    mincut =  new LinkedHashSet<>(source.taxa.size() + source.characters.size());

                    //undo mapping
                    for (FlipCutNodeSimpleWeight node : newMinCut.getCutSet()) {

                        //undo taxa mapping
                        if (node.isTaxon()){
                            Set<FlipCutNodeSimpleWeight> trivials = trivialcharacters.get(node);
                            if (trivials != null)
                                mincut.addAll(trivials);

                            Set<FlipCutNodeSimpleWeight> realT = dummyToTaxa.get(node);
                            if (realT != null)
                                mincut.addAll(realT);
                            else
                                mincut.add(node);

                            //undo character mapping
                        }else{
                            //todo merge back
                            Set<FlipCutNodeSimpleWeight> realNodes = dummyToMerged.get(node);
                            mincut.addAll(realNodes);
//                            newMinCut.add(node);
                        }
                    }
                } else if (source.GLOBAL_CHARACTER_MERGE) {
                    dummyToMerged = source.dummyToCharacters;
                    nodeToDummy = source.characterToDummy;
                    int merged = nodeToDummy.size() - dummyToMerged.size();
                    if (DEBUG) System.out.println(merged + "characters merged before mincut");
                    cutGraph = new GoldbergTarjanCutGraph<>();
                    createGoldbergTarjanCharacterWeightsMerged(cutGraph);
                    createTarjanGoldbergHyperGraphMerged(cutGraph);
                    BasicCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);
                    mincut =  new LinkedHashSet<>(source.taxa.size() + source.characters.size());
                    for (FlipCutNodeSimpleWeight node : newMinCut.getCutSet()) {
                        Set<FlipCutNodeSimpleWeight> realNodes = dummyToMerged.get(node);

                        if (realNodes == null){
                            if (node.isTaxon()) { //todo debug if remove!
                                mincut.add(node);
                            }else{
                                System.out.println("ERROR: something with the character merge map went wrong");
                            }
                        }else{
                            mincut.addAll(realNodes);
                        }
                    }
                } else {
                    cutGraph = new GoldbergTarjanCutGraph<>();
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    createTarjanGoldbergHyperGraph(cutGraph);
                    mincut = calculateTarjanMinCut(cutGraph).getCutSet();
                }
                break;
            }
        }
    }

    public Cut getNextCut() {
        if (stopCutting)
            return null;
        calculateMinCut();
        if (mincutValue >= (CostComputer.ACCURACY * INFINITY)){
            stopCutting = true;
            return null;
        }
        List<FlipCutNodeSimpleWeight> toBlacklist =  source.checkRemoveCharacter(mincut);
        blacklist.addAll(toBlacklist);
        return new Cut(mincut,mincutValue,source);
    }

    public CutGraphTypes getType() {
        return type;
    }

    @Override
    public List<FlipCutGraphMultiSimpleWeight> cut(FlipCutGraphMultiSimpleWeight source) {
        return getNextCut().getSplittedGraphs();
    }
}
