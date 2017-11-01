package phylo.tree.algorithm.flipcut.flipCutGraph;


import mincut.cutGraphAPI.AhujaOrlinCutGraph;
import mincut.cutGraphAPI.CutGraph;
import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.MaxFlowCutGraph;
import mincut.cutGraphAPI.bipartition.BasicCut;
import mincut.cutGraphAPI.bipartition.STCut;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 14.02.13
 * Time: 11:12
 */
public abstract class SimpleCutGraphCutter<T extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight>> extends CutGraphCutter<FlipCutNodeSimpleWeight, T> {

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
                long weight = CutGraphCutter.getInfinity();
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
                        long weight = CutGraphCutter.getInfinity();
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
    protected void createTarjanGoldbergHyperGraphTaxaMerged(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph, final VertexMapping<T> mapping) {
        final Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> taxonToMerged = mapping.taxonToDummy;
        final Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> taxonGroupTotrivialcharacters = mapping.trivialcharacters;

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
        final long infinity = CutGraphCutter.getInfinity();

        Set<FlipCutNodeSimpleWeight> alreadyIn = new HashSet<>(size);
        Map<Set<FlipCutNodeSimpleWeight>, FlipCutNodeSimpleWeight> charsToAdd = new HashMap<>(size);

        for (FlipCutNodeSimpleWeight tmp : source.characters) {
            //replace character with dummy
            FlipCutNodeSimpleWeight characterDummyGlobalMerged = source.characterToDummy.get(tmp);
            //only if dummy is not already inserted berfor
            if (!alreadyIn.contains(characterDummyGlobalMerged)) {
                alreadyIn.add(characterDummyGlobalMerged);
                Set<FlipCutNodeSimpleWeight> edgesTo = new HashSet<>();
                for (FlipCutNodeSimpleWeight t : characterDummyGlobalMerged.edges) {
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

                        dummyToMerged.put(n, set);
                        dummyToMerged.put(n.clone, cloneSet);


                    } else {
                        n = charsToAdd.get(edgesTo);
                        set = dummyToMerged.get(n);
                        cloneSet = dummyToMerged.get(n.clone);
                    }


                    set.add(characterDummyGlobalMerged);
                    cloneSet.add(characterDummyGlobalMerged.clone);

                    n.edgeWeight += calculateCharacterCap(characterDummyGlobalMerged);

                } else {
                    FlipCutNodeSimpleWeight tax = edgesTo.iterator().next();

                    if (!taxonGroupTotrivialcharacters.containsKey(tax)) {
                        taxonGroupTotrivialcharacters.put(tax, new HashSet<FlipCutNodeSimpleWeight>());
                    }
                    taxonGroupTotrivialcharacters.get(tax).add(characterDummyGlobalMerged);
                    taxonGroupTotrivialcharacters.get(tax).add(characterDummyGlobalMerged.clone);
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
    }

    protected void createGoldbergTarjanMerged(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph) {
        addTaxa(cutGraph);
        Set<FlipCutNodeSimpleWeight> edgesAlreadySet = new HashSet<>(dummyToMerged.size());

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

    protected void createGoldbergTarjanCharacterWeights(MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph, final Collection<FlipCutNodeSimpleWeight> characters) {
        // add characters, character clones and edges between them
        for (FlipCutNodeSimpleWeight character : characters) {
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
            System.out.println("WARNING: static character map is not implemented for edge! The slower NON STATIC Version is used instead");
            createGoldbergTarjanCharacterWeights(cutGraph, source.characters);
            createGoldbergTarjan(cutGraph);
            mincut = calculateTarjanMinCut(cutGraph);
            //todo this was flipcut code from times where character merging was not compatible. but i think it is if we use the multicut strategy
        } else if (type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG || type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN) {
            if (type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN)
                cutGraph = new AhujaOrlinCutGraph<>();
            else
                cutGraph = new GoldbergTarjanCutGraph<>();

            if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE && !source.activePartitions.isEmpty()) {
                //create Mapping
                VertexMapping<T> mapping = new VertexMapping<>();
                mapping.createMapping(source);

                //create cutgraph
                createTarjanGoldbergHyperGraphTaxaMerged(cutGraph, mapping);

                //calculate mincut
                STCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);

                //undo mapping
                mincut = mapping.undoMapping(newMinCut, dummyToMerged);
            } else {
                dummyToMerged = source.dummyToCharacters;
                nodeToDummy = source.characterToDummy;
                int merged = nodeToDummy.size() - dummyToMerged.size();
                if (DEBUG) System.out.println(merged + "characters merged before mincut");

                createTarjanGoldbergHyperGraphMerged(cutGraph);
                STCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);

                LinkedHashSet<FlipCutNodeSimpleWeight> nuCutSet = new LinkedHashSet<>(source.characters.size() + source.taxa.size());

                for (FlipCutNodeSimpleWeight node : newMinCut.getCutSet()) {
                    Set<FlipCutNodeSimpleWeight> realNodes = dummyToMerged.get(node);

                    if (realNodes == null) {
                        nuCutSet.add(node); // node has to be a taxon
                    } else {
                        nuCutSet.addAll(realNodes);
                    }
                }
                mincut = new BasicCut<>(nuCutSet, newMinCut.minCutValue);
            }
        } else {
            System.err.println("ERROR: Unsupported cut-graph type!");
        }
    }


}
