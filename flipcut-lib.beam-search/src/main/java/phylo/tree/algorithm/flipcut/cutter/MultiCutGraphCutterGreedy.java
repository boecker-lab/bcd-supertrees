package phylo.tree.algorithm.flipcut.cutter;


import mincut.cutGraphAPI.AhujaOrlinCutGraph;
import mincut.cutGraphAPI.CutGraph;
import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.MaxFlowCutGraph;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.STCut;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.cutter.blacklists.BlackList;
import phylo.tree.algorithm.flipcut.cutter.blacklists.GreedyBlackList;
import mincut.cutGraphAPI.bipartition.DefaultMultiCut;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphTypes;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 19.04.13
 * Time: 12:02
 */
public class MultiCutGraphCutterGreedy extends SimpleCutGraphCutter<FlipCutGraphMultiSimpleWeight> implements MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> {

    protected final Queue<BlackList> blacklists;
    protected BlackList blacklist = null;
    protected int cuts = 0;
    private final FlipCutGraphMultiSimpleWeight source;//todo make reusable??

    public MultiCutGraphCutterGreedy(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut) {
        this(type, graphToCut, new GreedyBlackList());
    }

    public MultiCutGraphCutterGreedy(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut, BlackList initialBlackList) {
        super(type);
        source = graphToCut;
        initialBlackList.setNumberOfCharacters(source.characters.size());
        blacklists = initBlackLists(initialBlackList);
    }

    Queue<BlackList> initBlackLists(final BlackList initial) {
        LinkedList<BlackList> bls = new LinkedList<>();
        bls.add(initial);
        return bls;
    }

    protected Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> calculateNexCut() {
        Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> mincut = null;
        if (type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG || type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN) {
            if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE) {
                //create mapping
                BlackListVertexMapping mapping = new BlackListVertexMapping();
                mapping.createMapping(source, blacklist);

                if (mapping.mergedTaxonIndex + mapping.singleTaxonIndex > 1) {
                    MaxFlowCutGraph<FlipCutNodeSimpleWeight> cutGraph;
                    if (type == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN)
                        cutGraph = new AhujaOrlinCutGraph<>();
                    else
                        cutGraph = new GoldbergTarjanCutGraph<>();

                    //create cutgraph
                    List<FlipCutNodeSimpleWeight> cutGraphTaxa = new ArrayList<>();
                    final Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToMerged = createTarjanGoldbergHyperGraphTaxaMerged((CutGraph<FlipCutNodeSimpleWeight>) cutGraph, source, mapping, cutGraphTaxa);

                    //calculate mincut
                    STCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph, cutGraphTaxa);

                    //undo mapping
                    mincut = mapping.undoMapping((Cut<LinkedHashSet<FlipCutNodeSimpleWeight>>) newMinCut, dummyToMerged);
                } else {
                    mincut = null;
                }
            } else {
                throw new IllegalArgumentException("SCAFFOLD MERGE has to be enabled");
            }
        } else {
            throw new IllegalArgumentException("Hypergraph max flow has to be enabled");
        }
        return null;
    }


    public DefaultMultiCut getNextCut() {
        if (cuts >= source.getK()) return null;
        Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> mincut = null;
        while (mincut == null) {
            blacklist = blacklists.poll();
            if (blacklist != null) {
                calculateNexCut();
            } else {
                if (DEBUG) System.out.println("Stop  Cutting");
                return null;
            }
        }

        cuts++;
        getFillBlacklist(mincut.getCutSet());
        return new DefaultMultiCut(mincut, source);
    }

    @Override
    public DefaultMultiCut getMinCut() {
        return getNextCut();
    }

    protected void getFillBlacklist(final LinkedHashSet<FlipCutNodeSimpleWeight> mincut) {
        Set<FlipCutNodeSimpleWeight> removed = new HashSet<>();
        for (FlipCutNodeSimpleWeight node : mincut) {
            if (!node.isTaxon()) {
                // it is character or a character clone
                // check if the other one is also in the set
                if (!mincut.contains(node.clone)) {
                    FlipCutNodeSimpleWeight c = node.isClone() ? node.clone : node;
                    if (DEBUG) {
                        if (blacklist.contains(c)) {
                            System.out.println("BLACKLIST cut!!!!!!!!!!");
                        }
                    }
                    removed.add(c);
                }
            }
        }
        blacklists.addAll(blacklist.createBlackLists(removed));
    }

    @Override
    public Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> cut(SourceTreeGraph<LinkedHashSet<FlipCutNodeSimpleWeight>> source) {
        if (source.equals(this.source))
            return getMinCut();
        return null;
    }


    static class Factory implements MultiCutterFactory<MultiCutGraphCutterGreedy, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, MaxFlowCutterFactory<MultiCutGraphCutterGreedy, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> {
        private final CutGraphTypes type;
        private final BlackList blPrototype;

        Factory(CutGraphTypes type, BlackList blPrototype) {
            this.type = type;
            this.blPrototype = blPrototype;
        }

        @Override
        public MultiCutGraphCutterGreedy newInstance(FlipCutGraphMultiSimpleWeight graph) {
            return new MultiCutGraphCutterGreedy(type, graph, blPrototype.newInitialInstance());
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
