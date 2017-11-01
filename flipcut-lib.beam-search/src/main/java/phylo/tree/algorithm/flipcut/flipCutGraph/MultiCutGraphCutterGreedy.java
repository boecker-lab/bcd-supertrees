package phylo.tree.algorithm.flipcut.flipCutGraph;


import mincut.cutGraphAPI.AhujaOrlinCutGraph;
import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.MaxFlowCutGraph;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.STCut;
import phylo.tree.algorithm.flipcut.flipCutGraph.blacklists.BlackList;
import phylo.tree.algorithm.flipcut.flipCutGraph.blacklists.GreedyBlackList;
import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 19.04.13
 * Time: 12:02
 */
public class MultiCutGraphCutterGreedy extends SimpleCutGraphCutter<FlipCutGraphMultiSimpleWeight> implements MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {

    protected final Queue<BlackList> blacklists;
    protected BlackList blacklist = null;
    protected int cuts = 0;

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

    @Override
    protected void calculateMinCut() {
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
                    final Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToMerged = createTarjanGoldbergHyperGraphTaxaMerged(cutGraph, mapping);

                    //calculate mincut
                    STCut<FlipCutNodeSimpleWeight> newMinCut = calculateTarjanMinCut(cutGraph);

                    //undo mapping
                    mincut = mapping.undoMapping((Cut<FlipCutNodeSimpleWeight>) newMinCut, dummyToMerged);
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
        if (cuts >= source.getK()) return null;
        mincut = null;
        while (mincut == null) {
            blacklist = blacklists.poll();
            if (blacklist != null) {
                calculateMinCut();
            } else {
                if (DEBUG) System.out.println("Stop  Cutting");
                return null;
            }
        }

        cuts++;
        getFillBlacklist(mincut.getCutSet());
        return new DefaultMultiCut(mincut, source);
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
    public List<FlipCutGraphMultiSimpleWeight> cut(FlipCutGraphMultiSimpleWeight source) {
        return getNextCut().getSplittedGraphs();
    }


    static class Factory implements MultiCutterFactory<MultiCutGraphCutterGreedy, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight>, MaxFlowCutterFactory<MultiCutGraphCutterGreedy, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
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
