package phylo.tree.algorithm.flipcut.flipCutGraph;


import com.google.common.collect.Sets;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.AhujaOrlinCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.CutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.MaxFlowCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.STCut;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 14.02.13
 * Time: 11:12
 */
public abstract class SimpleCutGraphCutter<T extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight>> extends CutGraphCutter<FlipCutNodeSimpleWeight, T> {
    public enum CutGraphTypes {MAXFLOW_TARJAN_GOLDBERG, MAXFLOW_AHOJI_ORLIN, HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG, type, HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN}

    protected static final boolean DEBUG = false;

    public static final boolean MAX_FLIP_NORMALIZATION = false;

    //THE "real" bcd without flip weighting
    public static final boolean IGNORE_MATRIX_ENTRIES = true;
    //only if ignore matrix entries (flips) false
    public static final boolean REAL_CHAR_DELETION = true;
    //if real char deletion false:
    public static final boolean ZEROES = true;

    protected final CutGraphTypes type;

    public SimpleCutGraphCutter(CutGraphTypes type) {
        super();
        this.type = type;
    }

    public SimpleCutGraphCutter(CutGraphTypes type, ExecutorService executorService, int threads) {
        super(executorService, threads);
        this.type = type;
    }

    //################### graph creation ######################
    protected void createTarjanGoldbergHyperGraph(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
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

    protected void createTarjanGoldbergHyperGraphMerged(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        addTaxa(cutGraph);
        Set<FlipCutNodeSimpleWeight> edgesAlreadySet = new HashSet<FlipCutNodeSimpleWeight>(dummyToMerged.size());
        // add edges
        for (FlipCutNodeSimpleWeight character : source.characters) {
            FlipCutNodeSimpleWeight dummy = nodeToDummy.get(character);
            if (dummy != null) {
                if (!edgesAlreadySet.contains(dummy)) {
                    edgesAlreadySet.add(dummy);

                    //add character and calculate weight
                    cutGraph.addNode(dummy);
                    cutGraph.addNode(dummy.clone);
                    long charCap = calculateCharacterCap(dummy); //calculate character weight
                    cutGraph.addEdge(dummy.clone, dummy, charCap);

                    for (FlipCutNodeSimpleWeight taxon : dummy.edges) {
                        //todo what is a useful way for a constant
                        long weight = CostComputer.ACCURACY * INFINITY;
                        cutGraph.addEdge(dummy, taxon, weight);
                        cutGraph.addEdge(taxon, dummy.clone, weight);
                    }
                }
            } else {
                System.out.println("ERROR: somthing is wrong with merge map! Again!! Again!!");
            }
        }
    }

    //helper method for the goldberg tarjan graphs
    protected void createTarjanGoldbergHyperGraphTaxaMerged(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph, Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> taxonToMerged, Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> taxonGroupTotrivialcharacters) {
        //init local merge map
        int size = source.characters.size();
        dummyToMerged = new HashMap<>(size);

       
        //add taxa
        cutGraphTaxa = new ArrayList<>(taxonToMerged.size());
        for (FlipCutNodeSimpleWeight mergedTaxon : taxonToMerged.values()) {
            cutGraphTaxa.add(mergedTaxon);
            cutGraph.addNode(mergedTaxon);
        }

        // add characters, character clones and edges between them
        final long infinity = CostComputer.ACCURACY * INFINITY;

        Set<FlipCutNodeSimpleWeight> alreadyIn = new HashSet<>(size);
        Map<Set<FlipCutNodeSimpleWeight>, FlipCutNodeSimpleWeight> charsToAdd = new HashMap<>(size);
//        Map<Object,Long> dummyToWeight = new HashMap<>(size);

        for (FlipCutNodeSimpleWeight tmp : source.characters) {
            //replace character with dummy
            FlipCutNodeSimpleWeight characterDummy = source.characterToDummy.get(tmp);
            //only if dummy is not already inserted berfor
            if (!alreadyIn.contains(characterDummy)) {
                Set<FlipCutNodeSimpleWeight> edgesTo = new HashSet<>();
                for (FlipCutNodeSimpleWeight t : characterDummy.edges) {
                    edgesTo.add(taxonToMerged.get(t));
                }
                //check if character is not trivial
                if (edgesTo.size() > 1) {
                    Set<FlipCutNodeSimpleWeight> set;
                    Set<FlipCutNodeSimpleWeight> cloneSet;
                    FlipCutNodeSimpleWeight n;

                    if (!charsToAdd.containsKey(edgesTo)) {
                        n = new FlipCutNodeSimpleWeight(edgesTo);
                        set = new HashSet<>();
                        cloneSet = new HashSet<>();

                        charsToAdd.put(edgesTo, n);
//                        dummyToWeight.put(characterDummy, 0l);

                        dummyToMerged.put(n, set);
                        dummyToMerged.put(n.clone, cloneSet);


                    } else {
                        n = charsToAdd.get(edgesTo);
                        set = dummyToMerged.get(n);
                        cloneSet = dummyToMerged.get(n.clone);
                    }


                    set.add(characterDummy);
                    cloneSet.add(characterDummy.clone);


                    //todo debug
                    long old = n.edgeWeight;
                    n.edgeWeight += calculateCharacterCap(characterDummy);
                    boolean overflow =  n.edgeWeight < old;
                    if(overflow)
                        System.out.println("### CHARACTER OVERFLOW W####");

                } else {
                    FlipCutNodeSimpleWeight tax = edgesTo.iterator().next();
                    //todo debug
                    if (tax ==null){
                        System.out.println("wtf");
                    }

                    if (!taxonGroupTotrivialcharacters.containsKey(tax)) {
                        taxonGroupTotrivialcharacters.put(tax, new HashSet<FlipCutNodeSimpleWeight>());
                    }
                    taxonGroupTotrivialcharacters.get(tax).add(characterDummy);
                    taxonGroupTotrivialcharacters.get(tax).add(characterDummy.clone);
                }
            }
        }

        //add merged chars
        for (Map.Entry<Set<FlipCutNodeSimpleWeight>, FlipCutNodeSimpleWeight> edgeToAdd : charsToAdd.entrySet()) {
            FlipCutNodeSimpleWeight dummyNode = edgeToAdd.getValue();
            //add character and its clone
            cutGraph.addNode(dummyNode);
            cutGraph.addNode(dummyNode.clone);
            //character weight
            cutGraph.addEdge(dummyNode.clone, dummyNode, dummyNode.edgeWeight);

            // add edges to taxa
            Set<FlipCutNodeSimpleWeight> edgesTo = edgeToAdd.getKey();
            for (FlipCutNodeSimpleWeight taxon : edgesTo) {
                cutGraph.addEdge(dummyNode, taxon, infinity);
                cutGraph.addEdge(taxon, dummyNode.clone, infinity);
            }
        }
        
        //#########################################################33DEBUG
        Set<Set<FlipCutNodeSimpleWeight>> charset = new HashSet<>(); //todo remove, debug
        for (FlipCutNodeSimpleWeight dumm : dummyToMerged.keySet()) {
            if (!dumm.isClone()) charset.add(new HashSet<>(dumm.edges));
        }

        boolean changes = true;
        while (changes) {
            changes = false;
            Iterator<Set<FlipCutNodeSimpleWeight>> it = charset.iterator();
            Set<FlipCutNodeSimpleWeight> merged = it.next();

            while (it.hasNext()) {
                Set<FlipCutNodeSimpleWeight> next = it.next();
                if (Sets.intersection(next, merged).size() > 0) {
                    merged.addAll(next);
                    it.remove();
                    changes = true;
                }
            }
        }

        /*System.out.println();
        if (charset.size() != 1)
            System.out.println("graph is not connected -> number of Compounts" + charset.size());

        for (Set<FlipCutNodeSimpleWeight> flipCutNodeSimpleWeights : charset) {
            System.out.println("Graph to cut taxa" + charset);
        }
        System.out.println();*/



    }


    protected void createGoldbergTarjan(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
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

    protected void createGoldbergTarjanMerged(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        addTaxa(cutGraph);
        Set<FlipCutNodeSimpleWeight> edgesAlreadySet = new HashSet<FlipCutNodeSimpleWeight>(dummyToMerged.size());

        // add edges
        for (FlipCutNodeSimpleWeight character : source.characters) {
            if (!nodeToDummy.containsKey(character)) {
                for (FlipCutNodeSimpleWeight taxon : character.edges) {
                    cutGraph.addEdge(character, taxon, character.edgeWeight);
                    cutGraph.addEdge(taxon, character.clone, character.edgeWeight);
                }
            } else {
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
    protected void addTaxa(CutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        cutGraphTaxa = new ArrayList<>(source.taxa.size());
        for (FlipCutNodeSimpleWeight node : source.taxa) {
            cutGraphTaxa.add(node);
            cutGraph.addNode(node);
        }
    }

    protected long calculateCharacterCap(FlipCutNodeSimpleWeight character) {
        long characterCap;
        if (IGNORE_MATRIX_ENTRIES) {
            characterCap = character.edgeWeight;
        } else {
            if (REAL_CHAR_DELETION) {
                characterCap = character.edgeWeight * (Math.min(character.imaginaryEdges.size(), character.edges.size()));
            } else {
                if (ZEROES) {
                    characterCap = character.edgeWeight * character.imaginaryEdges.size();
                } else {
                    characterCap = character.edgeWeight * character.edges.size();
                }
            }
        }
        if (MAX_FLIP_NORMALIZATION && !IGNORE_MATRIX_ENTRIES)
            characterCap /= (character.edges.size() + character.imaginaryEdges.size());

        return characterCap;
    }

    protected void createGoldbergTarjanCharacterWeights(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
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
    protected void createGoldbergTarjanCharacterWeightsMerged(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        Set<FlipCutNodeSimpleWeight> inGraph = new HashSet<FlipCutNodeSimpleWeight>();
        // add characters, character clones and edges between them
        for (FlipCutNodeSimpleWeight character : source.characters) {
            FlipCutNodeSimpleWeight dummy = nodeToDummy.get(character);
            if (dummy != null) {
                if (!inGraph.contains(dummy)) {
                    inGraph.add(dummy);
                    cutGraph.addNode(dummy);
                    cutGraph.addNode(dummy.clone);
                    //calculate character weight
                    long charCap = calculateCharacterCap(dummy);
                    cutGraph.addEdge(dummy.clone, dummy, charCap);
                }
            } else {
                System.out.println("ERROR: Something with the character merge map went wrong AGAIN!!!");
            }
        }
    }


    //micut calculation
    protected STCut<FlipCutNodeSimpleWeight> calculateTarjanMinCut(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        // get the mincut, fix s iterate over t
        STCut<FlipCutNodeSimpleWeight> minCut = STCut.MAX_CUT_DUMMY;
        try {
            FlipCutNodeSimpleWeight s = cutGraphTaxa.get(0);
            FlipCutNodeSimpleWeight t = null;
            for (int i = 1; i < cutGraphTaxa.size(); i++) {
                t = cutGraphTaxa.get(i);
                cutGraph.submitSTCutCalculation(s, t);
            }
            cutGraph.setExecutorService(executorService);
            cutGraph.setThreads(threads);
            minCut = cutGraph.calculateMinCut();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return minCut;
    }

    @Override
    protected void calculateMinCut() {
        MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph;
        if (type == CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG || type == CutGraphTypes.MAXFLOW_AHOJI_ORLIN) {

            if (type == CutGraphTypes.MAXFLOW_AHOJI_ORLIN)
                cutGraph = new AhujaOrlinCutGraph<>();
            else
                cutGraph = new GoldbergTarjanCutGraph<>();
            //              if (source.GLOBAL_CHARACTER_MERGE) {
            if (false) {
                System.out.println("WARNING: static character map is not implemented for edge! The slower NON STATIC Version is used instead");
//                    //todo make compatible
                // int merged = buildCharacterMergingMap(source);
//                    if (DEBUG) System.out.println(merged + "characters merged before mincut");
                createGoldbergTarjanCharacterWeightsMerged(cutGraph);
                createGoldbergTarjanMerged(cutGraph);
                STCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);

                mincut = new LinkedHashSet<>(source.characters.size() + source.taxa.size());
                for (FlipCutNodeSimpleWeight node : newMinCut.getCutSet()) {
                    Set<FlipCutNodeSimpleWeight> realNodes = dummyToMerged.get(node);
                    if (realNodes == null) {
                        mincut.add(node);//has to be a taxon
                    } else {
                        mincut.addAll(realNodes);
                    }
                }
            } else {
                createGoldbergTarjanCharacterWeights(cutGraph);
                createGoldbergTarjan(cutGraph);
                mincut = calculateTarjanMinCut(cutGraph).getCutSet();
            }

        } else if (type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG || type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN) {
            if (type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN)
                cutGraph = new AhujaOrlinCutGraph<>();
            else
                cutGraph = new GoldbergTarjanCutGraph<>();

            if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE && !source.activePartitions.isEmpty()) {
                //create mapping
                //todo optimize all these mapping stuff if it works well
                Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> taxonToDummy = new HashMap<>();
                Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToTaxa = new HashMap<>();
                Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> trivialcharacters = new HashMap<>();

                int mergedTaxonIndex = 0;
                for (FlipCutNodeSimpleWeight scaffChar : source.activePartitions) {
                    FlipCutNodeSimpleWeight mergeTaxon = new FlipCutNodeSimpleWeight("TaxonGroup_" + mergedTaxonIndex);
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
//                cutGraph = new GoldbergTarjanCutGraph<>();
                createTarjanGoldbergHyperGraphTaxaMerged(cutGraph, taxonToDummy, trivialcharacters);

                //calculate mincut
                STCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);

                //undo mapping
                mincut = new LinkedHashSet<>(source.characters.size() + source.taxa.size());
                for (FlipCutNodeSimpleWeight node : newMinCut.getCutSet()) {
                    //undo taxa mapping
                    if (node.isTaxon()) {
                        Set<FlipCutNodeSimpleWeight> trivials = trivialcharacters.get(node);
                        if (trivials != null) {
                            for (FlipCutNodeSimpleWeight trivial : trivials) {
                                mincut.addAll(source.dummyToCharacters.get(trivial));
                            }

                        }
                        Set<FlipCutNodeSimpleWeight> realT = dummyToTaxa.get(node);
                        if (realT != null)
                            mincut.addAll(realT);
                        else
                            mincut.add(node);

                        //undo character mapping
                    } else {
                        Set<FlipCutNodeSimpleWeight> realNodes = new HashSet<>(source.characters.size());
                        for (FlipCutNodeSimpleWeight realNode : dummyToMerged.get(node)) {
                            realNodes.addAll(source.dummyToCharacters.get(realNode));
                        }
                        mincut.addAll(realNodes);
                    }
                }
            } else if (AbstractFlipCutGraph.GLOBAL_CHARACTER_MERGE) {
                dummyToMerged = source.dummyToCharacters;
                nodeToDummy = source.characterToDummy;
                int merged = nodeToDummy.size() - dummyToMerged.size();
                if (DEBUG) System.out.println(merged + "characters merged before mincut");

//                cutGraph = new GoldbergTarjanCutGraph<>();
                createTarjanGoldbergHyperGraphMerged(cutGraph);
                STCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);

                mincut = new LinkedHashSet<>(source.characters.size() + source.taxa.size());
                for (FlipCutNodeSimpleWeight node : newMinCut.getCutSet()) {
                    Set<FlipCutNodeSimpleWeight> realNodes = dummyToMerged.get(node);

                    if (realNodes == null) {
                        mincut.add(node); // node has to be a taxon
                    } else {
                        mincut.addAll(realNodes);
                    }
                }
            } else {
                cutGraph = new GoldbergTarjanCutGraph<>();
                createGoldbergTarjanCharacterWeights(cutGraph);
                createTarjanGoldbergHyperGraph(cutGraph);
                mincut = calculateTarjanMinCut(cutGraph).getCutSet();
            }
        }

    }

//    public abstract CutterFactory<? extends GraphCutter<FlipCutNodeSimpleWeight,T>,FlipCutNodeSimpleWeight,T> getFactory (CutGraphTypes type);
}
