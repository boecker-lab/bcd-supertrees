package phylo.tree.algorithm.flipcut.flipCutGraph;

import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.bipartition.STCut;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;
import phylo.tree.algorithm.flipcut.model.VaziraniCut;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 14:05
 */

/**
 * VAZIRANI ALGORITHM
 */
//TODO THE VAZIRANI should be an own Cutgraph not a cut graph cutter --> it should extend directed cut graph an use some max flow implemetation

/*public class MergeableVziraniCutter extends SimpleCutGraphCutter<FlipCutGraphMultiSimpleWeight> implements MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {

    private PriorityQueue<VaziraniCut> queueAscHEAP = null;
    private VaziraniCut<FlipCutNodeSimpleWeight> currentVaziraniCut = null;
    private VaziraniCut<FlipCutNodeSimpleWeight>[] initCuts;
    private final ArrayList<FlipCutNodeSimpleWeight> taxa;
    private final LinkedHashSet<FlipCutNodeSimpleWeight> characters;
    private VertexMapping mapping = new VertexMapping();

    public MergeableVziraniCutter(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut) {
        super(type);
        source = graphToCut;
        mapping.createMapping(source);
        taxa = new ArrayList<>(mapping.taxonToDummy.values());
        characters = source.characters;
    }

    private List<VaziraniCut> findCutsFromPartialCuts(VaziraniCut sourceCut, VaziraniCut[] initCuts) {
        Set<FlipCutNodeSimpleWeight> cutTset = new HashSet<>(sourceCut.cut.gettSet());
        List<VaziraniCut> cuts = new ArrayList<VaziraniCut>(taxa.size() - sourceCut.k);

        VaziraniCut currentCut;
        GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight> cutGraph;
        Set<FlipCutNodeSimpleWeight> sSet;
        Set<FlipCutNodeSimpleWeight> tSet;

        // finding all partial mincut
        for (int k = sourceCut.k; k < taxa.size(); k++) {
            sSet = new HashSet<FlipCutNodeSimpleWeight>();
            tSet = new HashSet<FlipCutNodeSimpleWeight>();

            for (int i = 0; i < k; i++) {
                if (cutTset.contains(taxa.get(i))) {
                    tSet.add(taxa.get(i));
                } else {
                    sSet.add(taxa.get(i));
                }
            }

            //change position of taxon number k
            if (!cutTset.contains(taxa.get(k))) {
                tSet.add(taxa.get(k));
            } else {
                sSet.add(taxa.get(k));
            }

            //build cutgraph from patial cut
            cutGraph = new GoldbergTarjanCutGraph<>();

            // add characters, character clones and edges between them
            //todo character merging stuff
            switch (type) {
                case MAXFLOW_TARJAN_GOLDBERG: {
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    break;
                }
                case HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG: {
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    break;
                }
            }
            //add taxa > k
            for (int i = k + 1; i < taxa.size(); i++) {
                cutGraph.addNode(taxa.get(i));
            }
            FlipCutNodeSimpleWeight randomS = sSet.iterator().next();
            cutGraph.addNode(randomS);

            if (!tSet.isEmpty()) {
                FlipCutNodeSimpleWeight randomT = tSet.iterator().next();
                cutGraph.addNode(randomT);

                // add edges from character to taxa and merge s and t nodes
                for (FlipCutNodeSimpleWeight character : characters) {
                    long sWeight = 0;
                    long tWeight = 0;
                    for (FlipCutNodeSimpleWeight taxon : character.edges) {
                        long weight = 0;
                        switch (type) {
                            case MAXFLOW_TARJAN_GOLDBERG: {
                                weight = character.edgeWeight;
                                break;
                            }
                            case HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG: {
                                weight = CostComputer.ACCURACY * INFINITY;
                                break;
                            }
                        }

                        if (sSet.contains(taxon)) {
                            sWeight = sWeight + weight;
                        } else if (tSet.contains(taxon)) {
                            tWeight = tWeight + weight;
                        } else {
                            cutGraph.addEdge(character, taxon, weight);
                            cutGraph.addEdge(taxon, character.clone, weight);
                        }
                    }
                    if (sWeight > 0) {
                        cutGraph.addEdge(character, randomS, sWeight);
                        cutGraph.addEdge(randomS, character.clone, sWeight);
                    }
                    if (tWeight > 0) {
                        cutGraph.addEdge(character, randomT, tWeight);
                        cutGraph.addEdge(randomT, character.clone, tWeight);
                    }
                }


                // compute mincut an put it to results
                STCut<FlipCutNodeSimpleWeight> tmpCut = cutGraph.calculateMinSTCut(randomS, randomT);
                currentCut = new VaziraniCut(tmpCut, k + 1);
                cuts.add(currentCut);

            } else {
                //find cut for 0^k case
                if (sSet.size() < taxa.size()) {
                    //tSet empty --> adding new init Graph! 0^k case);
                    VaziraniCut initCut = null;
                    //find best
                    for (int i = k; i < initCuts.length; i++) {
                        if (initCut == null || initCuts[i].minCutValue() < initCut.minCutValue()) {
                            initCut = initCuts[i];
                        }
                    }
                    //copy to new object
                    initCut = new VaziraniCut(initCut.cut, k + 1);
                    cuts.add(initCut);
                }
            }
        }
        return cuts;
    }


    public DefaultMultiCut getNextCut() {
        if (queueAscHEAP != null && queueAscHEAP.isEmpty()) {
            // all mincut calculated
            return null;
        } else {
            if (queueAscHEAP == null) {
                initialCut();
            }
            nextCut();
            mincut = mapping.undoMapping(currentVaziraniCut.cut, dummyToMerged);
            mincutValue = currentVaziraniCut.minCutValue();

            return new DefaultMultiCut(mincut, mincutValue, source);
        }
    }

    private void nextCut() {
        //Starting find subobtimal mincut mincut with vaziranis algo
        currentVaziraniCut = queueAscHEAP.poll();
        //compute next cut candidates with vaziranis algo
        List<VaziraniCut> toHeap = findCutsFromPartialCuts(currentVaziraniCut, initCuts);
        for (VaziraniCut node : toHeap) {
            queueAscHEAP.add(node);
        }
    }

    private void initialCut() {
        GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight> cutGraph = null;

        // get the mincut, fix s iterate over t
        FlipCutNodeSimpleWeight s;
        FlipCutNodeSimpleWeight t;
        VaziraniCut currentNode;
        STCut minCut;
        VaziraniCut lightestCut;
        initCuts = new VaziraniCut[taxa.size() - 1];
        queueAscHEAP = new PriorityQueue<VaziraniCut>();

        //j=0
       *//* //todo integrate character merging stuff!
        switch (type) {
            case MAXFLOW_TARJAN_GOLDBERG: {
                cutGraph = new GoldbergTarjanCutGraph<>();
                createGoldbergTarjanCharacterWeights(cutGraph);
                createGoldbergTarjan(cutGraph);
                break;
            }
            case HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG: {
                cutGraph = new GoldbergTarjanCutGraph<>();
                createGoldbergTarjanCharacterWeights(cutGraph);
                createTarjanGoldbergHyperGraph(cutGraph);
                break;
            }
        }*//*

        //create cutgraph
        cutGraph = new GoldbergTarjanCutGraph<>();
        createTarjanGoldbergHyperGraphTaxaMerged(cutGraph, mapping);


        minCut = cutGraph.calculateMinSTCut(taxa.get(0), taxa.get(1));
        lightestCut = new VaziraniCut(minCut, 1);
        initCuts[0] = lightestCut;

        //ATTENTION this is  the undirected graph version as tweak for the symmetric flipCut Graph
        for (int j = 1; j < taxa.size() - 1; j++) {
            s = taxa.get(j);
            t = taxa.get(j + 1);


            //build cutgraph from patial cut
            cutGraph = new GoldbergTarjanCutGraph<>();
            Set<FlipCutNodeSimpleWeight> sSet = new HashSet<FlipCutNodeSimpleWeight>();
            for (int i = 0; i <= j; i++) {
                sSet.add(taxa.get(i));
            }


            // add characters, character clones and edges between them
            //todo character merging stuff
            switch (type) {
                case MAXFLOW_TARJAN_GOLDBERG: {
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    break;
                }
                case HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG: {
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    break;
                }
            }

            //add taxa
            for (int i = j + 1; i < taxa.size(); i++) {
                cutGraph.addNode(taxa.get(i));
                //System.out.println("adding taxa " + node.name);
            }

            // add edges from character to taxa and merge s and t nodes
            for (FlipCutNodeSimpleWeight character : characters) {
                long sWeight = 0;
                for (FlipCutNodeSimpleWeight taxon : character.edges) {
                    long weight = 0;
                    switch (type) {
                        case MAXFLOW_TARJAN_GOLDBERG: {
                            weight = character.edgeWeight;
                            break;
                        }
                        case HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG: {
                            weight = CostComputer.ACCURACY * INFINITY;
                            break;
                        }
                    }

                    if (sSet.contains(taxon)) {
                        sWeight = sWeight + weight;
                    } else {
                        cutGraph.addEdge(character, taxon, weight);
                        cutGraph.addEdge(taxon, character.clone, weight);
                    }
                }
                if (sWeight > 0) {
                    cutGraph.addEdge(character, s, sWeight);
                    cutGraph.addEdge(s, character.clone, sWeight);
                }
            }

            minCut = cutGraph.calculateMinSTCut(s, t);
            currentNode = new VaziraniCut(minCut, 1);
            initCuts[j] = currentNode;
            //save lightest cut for HEAP init
            if (currentNode.minCutValue() < lightestCut.minCutValue()) lightestCut = currentNode;
        }
        //initialize heap
        VaziraniCut initialToHeap = new VaziraniCut(lightestCut); //todo why new node?
        queueAscHEAP.add(initialToHeap);
        //this.currentNode =  initialToHeap;
    }

    @Override
    public List<FlipCutGraphMultiSimpleWeight> cut(FlipCutGraphMultiSimpleWeight source) {
        return getNextCut().getSplittedGraphs();

    }

    static class Factory implements MultiCutterFactory<MergeableVziraniCutter, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight>, MaxFlowCutterFactory<MergeableVziraniCutter, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
        private final CutGraphTypes type;

        Factory(CutGraphTypes type) {
            this.type = type;
        }

        @Override
        public MergeableVziraniCutter newInstance(FlipCutGraphMultiSimpleWeight graph) {
            return new MergeableVziraniCutter(type, graph);
        }

        @Override
        public MergeableVziraniCutter newInstance(FlipCutGraphMultiSimpleWeight graph, ExecutorService executorService, int threads) {
            return newInstance(graph);
        }

        @Override
        public CutGraphTypes getType() {
            return type;
        }
    }
}*/
