package phylo.tree.algorithm.flipcut.flipCutGraph;


import com.google.common.collect.Sets;
import mincut.cutGraphAPI.AhujaOrlinCutGraph;
import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.MaxFlowCutGraph;
import mincut.cutGraphAPI.bipartition.STCut;
import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 *         Date: 19.04.13
 *         Time: 12:02
 */
public class MultiCutGraphCutterGreedy extends SimpleCutGraphCutter<FlipCutGraphMultiSimpleWeight> implements MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {

    protected Set<FlipCutNodeSimpleWeight> blacklist = new HashSet<>();
    protected boolean stopCutting;
    protected int cuts = 0;

    public MultiCutGraphCutterGreedy(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut) {
        super(type);
        source = graphToCut;
        stopCutting = false;
    }

    @Override
    protected void calculateMinCut() {
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

                int singleTaxonIndex = 0;
                for (FlipCutNodeSimpleWeight taxon : source.taxa) {
                    if (!taxonToDummy.containsKey(taxon)) {
                        taxonToDummy.put(taxon, taxon);
                        singleTaxonIndex++;
                    }
                }

                if (mergedTaxonIndex + singleTaxonIndex > 1) {
                    MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph;
                    if (type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN)
                        cutGraph = new AhujaOrlinCutGraph<>();
                    else
                        cutGraph = new GoldbergTarjanCutGraph<>();

                    //create cutgraph
                    createTarjanGoldbergHyperGraphTaxaMerged(cutGraph, taxonToDummy, trivialcharacters);

                    //calculate mincut
                    STCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);
//                    mincutValue = newMinCut.minCutValue();

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
                } else {
                    mincut = null;
                }
            } else {
                throw new IllegalArgumentException("SCAFFOLD MERGE has to be enabled");
            }
        } else {
            throw new IllegalArgumentException("Hypergraph max flow has to be enabled");
        }
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
        return new DefaultMultiCut(mincut, mincutValue, source);
    }

    protected long getMinCutValueAndFillBlacklist(final Set<FlipCutNodeSimpleWeight> blacklistToAddTo, final LinkedHashSet<FlipCutNodeSimpleWeight> mincut) {
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
