package phylo.tree.algorithm.flipcut.flipCutGraph;


import com.google.common.collect.Sets;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.AhujaOrlinCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.MaxFlowCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.STCut;
import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;

import java.util.*;
import java.util.concurrent.ExecutorService;

import static phylo.tree.algorithm.flipcut.costComputer.CostComputer.ACCURACY;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 *         Date: 19.04.13
 *         Time: 12:02
 */
public class MultiCutGraphCutterGreedy extends SimpleCutGraphCutter<FlipCutGraphMultiSimpleWeight> implements MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {

    protected Set<FlipCutNodeSimpleWeight> blacklist = new HashSet<>();
    protected boolean stopCutting;
    protected int cuts = 0;
    protected long mincutValue2; //todo debug

    public MultiCutGraphCutterGreedy(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut) {
        super(type);
        source = graphToCut;
        stopCutting = false;
    }

    @Override
    protected void calculateMinCut() {
        System.out.println("--------------------------------------------------------");

        if (type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG || type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN) {
            if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE) {

                Set<Set<FlipCutNodeSimpleWeight>> activePartitions = new HashSet<>();
                for (FlipCutNodeSimpleWeight character : source.activePartitions) {
                    activePartitions.add(new HashSet<>(character.edges));
                }
                for (FlipCutNodeSimpleWeight character : blacklist) {
                    activePartitions.add(new HashSet<>(character.edges));
                }

                //todo is there a less ugly way to do this?
                if (!activePartitions.isEmpty()) {
                    Set<Set<FlipCutNodeSimpleWeight>> finalActivePartitions = new HashSet<>();
                    while (!activePartitions.isEmpty()) {
                        Set<FlipCutNodeSimpleWeight> merged = activePartitions.iterator().next();
                        activePartitions.remove(merged);
                        boolean changes = true;
                        while (changes) {
                            Iterator<Set<FlipCutNodeSimpleWeight>> it = activePartitions.iterator();
                            changes = false;
                            while (it.hasNext()) {
                                Set<FlipCutNodeSimpleWeight> next = it.next();
                                if (Sets.intersection(next, merged).size() > 0) {
                                    merged.addAll(next);
                                    it.remove();
                                    changes = true;
                                }
                            }
                        }
                        finalActivePartitions.add(merged);
                    }
                    activePartitions = finalActivePartitions;
                }


                //todo debug check
                for (Set<FlipCutNodeSimpleWeight> ap1 : activePartitions) {
                    for (Set<FlipCutNodeSimpleWeight> ap2 : activePartitions) {
                        if (ap1 != ap2) {
                            Sets.SetView<FlipCutNodeSimpleWeight> inter = Sets.intersection(ap1, ap2);
                            if (!inter.isEmpty())
                                System.out.println("not independent!");
                        }
                    }
                }


                //create mapping
                //todo optimize all these mapping stuff if it works well
                Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> taxonToDummy = new HashMap<>();
                Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToTaxa = new HashMap<>();
                Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> trivialcharacters = new HashMap<>();

                int mergedTaxonIndex = 0;
                for (Set<FlipCutNodeSimpleWeight> scaffChar : activePartitions) {
                    FlipCutNodeSimpleWeight mergeTaxon = new FlipCutNodeSimpleWeight("TaxonGroup_" + mergedTaxonIndex);
                    for (FlipCutNodeSimpleWeight taxon : scaffChar) {
                        taxonToDummy.put(taxon, mergeTaxon);
                    }
                    dummyToTaxa.put(mergeTaxon, scaffChar);
                    mergedTaxonIndex++;
                }

                if (!taxonToDummy.values().containsAll(dummyToTaxa.keySet())) {
                    System.out.println("demaged before");
                }

                int singleTaxonIndex = 0;
                for (FlipCutNodeSimpleWeight taxon : source.taxa) {
                    if (!taxonToDummy.containsKey(taxon)) {
                        taxonToDummy.put(taxon, taxon);
                        singleTaxonIndex++;
                    }
                }

                if (!taxonToDummy.values().containsAll(dummyToTaxa.keySet())) {
                    System.out.println("demaged after");
                }


                if (mergedTaxonIndex + singleTaxonIndex > 1) {
                    MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph;
                    if (type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN)
                        cutGraph = new AhujaOrlinCutGraph<>();
                    else
                        cutGraph = new GoldbergTarjanCutGraph<>();

                    //create cutgraph
                    createTarjanGoldbergHyperGraphTaxaMerged(cutGraph, taxonToDummy, trivialcharacters);

                    if (!taxonToDummy.values().containsAll(dummyToTaxa.keySet())) {
                        System.out.println("demaged after after");
                    }

                    //todo debug
                    Map<FlipCutNodeSimpleWeight, Object> tarjannodes = ((GoldbergTarjanCutGraph) cutGraph).nodes;
                    int nodesToHave = mergedTaxonIndex + singleTaxonIndex + dummyToMerged.size() /*- trivialcharacters.size()*/;

                    if (tarjannodes.size() != nodesToHave) {
                        System.out.println("not all nodes in graph " + tarjannodes.size() + " / " + nodesToHave);
                        List<FlipCutNodeSimpleWeight> missings = new LinkedList<>();
                        Set<FlipCutNodeSimpleWeight> nodes = tarjannodes.keySet();
                        for (FlipCutNodeSimpleWeight dum : dummyToMerged.keySet()) {
                            if (!nodes.contains(dum)) {
                                System.out.println("Character missing: " + dum);
                                missings.add(dum);
                            }
                        }
                        for (FlipCutNodeSimpleWeight dum : dummyToTaxa.keySet()) {
                            if (!nodes.contains(dum)) {
                                System.out.println("TaxonSet missing: " + dum);
                                missings.add(dum);
                            }
                        }
                        System.out.println();

                    } else {
                        System.out.println("ALL fine");
                    }

                    //degub done

                    //calculate mincut
                    STCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);
                    mincutValue2 = newMinCut.minCutValue();

                    //undo mapping
                    mincut = new LinkedHashSet<>(source.characters.size() + source.taxa.size());
                    for (FlipCutNodeSimpleWeight node : newMinCut.getCutSet()) {
                        //undo taxa mapping
                        if (node.isTaxon()) {
                            Set<FlipCutNodeSimpleWeight> trivials = trivialcharacters.get(node);
                            if (trivials != null) {
                                System.out.println("Add trivials");
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
                } else {
                    mincut = null;
                    mincutValue2 = -1;
                }
            } else {
                throw new IllegalArgumentException("SCAFFOLD MERGE has to be enabled");
            }
        } else {
            throw new IllegalArgumentException("Hypergraph max flow has to be enabled");
        }
        System.out.println("--------------------------------------------------------");
    }


    public DefaultMultiCut getNextCut() {
//        if (DEBUG) System.out.println("cut number: " + (++cuts));
        if (stopCutting) {
            if (DEBUG) System.out.println("cutting stopped");
            return null;
        }

        calculateMinCut();

        if (mincut == null) {
            if (DEBUG) System.out.println("Stop  Cutting");
            stopCutting = true;
            return null;
        }

        //todo optimize
        mincutValue = getMinCutValueAndFillBlacklist(blacklist, mincut);
        System.out.println("###### MinCutValue ########");
        System.out.println(mincutValue);
        System.out.println(mincutValue2);
        System.out.println(CutGraphCutter.INFINITY * ACCURACY);
        System.out.println(Long.MAX_VALUE);
        System.out.println("###########################");
        return new DefaultMultiCut(mincut, mincutValue, source);
    }

    protected long getMinCutValueAndFillBlacklist(final Set<FlipCutNodeSimpleWeight> blacklistToAddTo, final LinkedHashSet<FlipCutNodeSimpleWeight> mincut) {
        long mincutValue = 0;
        for (FlipCutNodeSimpleWeight node : mincut) {
            if (!node.isTaxon()) {
                // it is character or a character clone
                // check if the other one is also in the set
                if (!mincut.contains(node.clone)) {
                    FlipCutNodeSimpleWeight c = node.isClone() ? node.clone : node;
                    if (DEBUG) {
                        if (blacklistToAddTo.contains(c)) {
                            System.out.println("BLACKLIST cut!!!!!!!!!!");
                        }
                    }
                    blacklistToAddTo.add(c);
                    mincutValue += c.edgeWeight;
                }
            }
        }
        if (mincutValue != mincutValue2) {
            System.out.println("Check!!!!!!!");
        }
        return mincutValue;
    }

    @Override
    public List<FlipCutGraphMultiSimpleWeight> cut(FlipCutGraphMultiSimpleWeight source) {
        return getNextCut().getSplittedGraphs();
    }

    static class Factory implements MultiCutterFactory<MultiCutGraphCutterGreedy, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight>, MaxFlowCutterFactory<MultiCutGraphCutterGreedy, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
        private final CutGraphTypes type;

        Factory(CutGraphTypes type) {
            this.type = type;
        }

        @Override
        public MultiCutGraphCutterGreedy newInstance(FlipCutGraphMultiSimpleWeight graph) {
            return new MultiCutGraphCutterGreedy(type, graph);
        }

        @Override
        public MultiCutGraphCutterGreedy newInstance(FlipCutGraphMultiSimpleWeight graph, ExecutorService executorService, int threads) {
            return newInstance(graph);
        }

        @Override
        public CutGraphTypes getType() {
            return type;
        }
    }
}
