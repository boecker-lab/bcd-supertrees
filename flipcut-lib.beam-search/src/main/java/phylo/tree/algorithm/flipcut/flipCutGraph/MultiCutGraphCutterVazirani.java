package phylo.tree.algorithm.flipcut.flipCutGraph;

import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.bipartition.BasicCut;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.STCut;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.algorithm.flipcut.cutter.MaxFlowCutterFactory;
import phylo.tree.algorithm.flipcut.cutter.SimpleCutGraphCutter;
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

public class MultiCutGraphCutterVazirani extends SimpleCutGraphCutter<FlipCutGraphMultiSimpleWeight> implements MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> {

    private PriorityQueue<VaziraniCut> queueAscHEAP = null;
    private VaziraniCut<FlipCutNodeSimpleWeight> currentNode = null;
    private VaziraniCut<FlipCutNodeSimpleWeight>[] initCuts;

    private ArrayList<FlipCutNodeSimpleWeight> taxa;
    private VertexMapping<FlipCutGraphMultiSimpleWeight> mapping = new VertexMapping<>();
    private Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToMerged;

    private LinkedHashSet<FlipCutNodeSimpleWeight> characters = null;
    private final FlipCutGraphMultiSimpleWeight source;//todo make reusable??


    @Override
    public void clear() {
        super.clear();
        queueAscHEAP = null;
        currentNode = null;
        initCuts = null;
        taxa = null;
        mapping = null;
        dummyToMerged = null;
        characters = null;
    }

    public MultiCutGraphCutterVazirani(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut) {
        super(type);
        source = graphToCut;
    }

    private List<VaziraniCut> findCutsFromPartialCuts(VaziraniCut sourceCut, VaziraniCut[] initCuts) {
        Set<FlipCutNodeSimpleWeight> cut = sourceCut.getCutSet();

        List<VaziraniCut> cuts = new ArrayList<VaziraniCut>(taxa.size() - sourceCut.k);

        VaziraniCut currentCut;
        GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight> cutGraph;
        Set<FlipCutNodeSimpleWeight> sSet;
        Set<FlipCutNodeSimpleWeight> tSet;


        // finding all partial mincut
        for (int k = sourceCut.k; k < taxa.size(); k++) {
//            System.out.println("Taxa number: " + taxa.size() + " - k=" + k);
            sSet = new HashSet<FlipCutNodeSimpleWeight>();
            tSet = new HashSet<FlipCutNodeSimpleWeight>();

            for (int i = 0; i < k; i++) {
                if (cut.contains(taxa.get(i))) {
                    tSet.add(taxa.get(i));
                } else {
                    sSet.add(taxa.get(i));
                }
            }

            //change position of taxon number k
            if (!cut.contains(taxa.get(k))) {
                tSet.add(taxa.get(k));
            } else {
                sSet.add(taxa.get(k));
            }

            //build cutgraph from patial cut
            cutGraph = new GoldbergTarjanCutGraph<>();

            // add characters, character clones and edges between them
            createGoldbergTarjanCharacterWeights(cutGraph, characters);

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
                        long weight = CutGraphCutter.getInfinity();

                        if (sSet.contains(taxon)) {
                            sWeight = CutGraphCutter.getInfinity();
                        } else if (tSet.contains(taxon)) {
                            tWeight = CutGraphCutter.getInfinity();
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
                currentCut = new VaziraniCut(tmpCut, tSet, k + 1);
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
                    initCut = new VaziraniCut(initCut.getCutSet(), initCut.minCutValue(), k + 1);
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
            BasicCut<FlipCutNodeSimpleWeight> unMapped = mapping.undoMapping(currentNode, dummyToMerged);
            return new DefaultMultiCut(unMapped, source);
        }
    }

    private void nextCut() {
        //Starting find subobtimal mincut mincut with vaziranis algo
        currentNode = queueAscHEAP.poll();
        //compute next cut candidates with vaziranis algo
        List<VaziraniCut> toHeap = findCutsFromPartialCuts(currentNode, initCuts);
        for (VaziraniCut node : toHeap) {
            queueAscHEAP.add(node);
        }
    }

    private void initialCut() {
        // get the mincut, fix s iterate over t
        FlipCutNodeSimpleWeight s;
        FlipCutNodeSimpleWeight t;
        VaziraniCut currentNode;
        STCut minCut;
        VaziraniCut lightestCut;

        // inti data structures
        taxa = mapping.createMapping(source);
        initCuts = new VaziraniCut[taxa.size() - 1];
        queueAscHEAP = new PriorityQueue<VaziraniCut>();

        //j=0
        GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight> cutGraph = new GoldbergTarjanCutGraph<>();
        dummyToMerged = createTarjanGoldbergHyperGraphTaxaMerged(cutGraph,source, mapping, new ArrayList<>(taxa.size()));

        minCut = cutGraph.calculateMinSTCut(taxa.get(0), taxa.get(1));
        lightestCut = new VaziraniCut(minCut.getCutSet(), minCut.minCutValue, 1);
        initCuts[0] = lightestCut;


        characters = new LinkedHashSet<>();
        for (Object o : cutGraph.getNodes().keySet()) {
            FlipCutNodeSimpleWeight node = (FlipCutNodeSimpleWeight) o;
            if (!node.isClone() && (node.isCharacter() || node.isDummyCharacter())) {
                characters.add(node);
                if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE) {
                    if (node.edgeWeight == CutGraphCutter.getInfinity())
                        System.out.println("SCM node in graph, but should be merged!");
                }
            }
        }

        for (Set<FlipCutNodeSimpleWeight> simpleWeights : mapping.trivialcharacters.values()) {
            if (characters.removeAll(simpleWeights))
                System.out.println("trivials removed");
        }

        //ATTENTION this is  the undirected graph version as tweak for flipCut Graph
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
            createGoldbergTarjanCharacterWeights(cutGraph, characters);

            //add taxa
            for (int i = j + 1; i < taxa.size(); i++) {
                cutGraph.addNode(taxa.get(i));
                //System.out.println("adding taxa " + node.name);
            }

            // add edges from character to taxa and merge s and t nodes
            for (FlipCutNodeSimpleWeight character : characters) {
                long sWeight = 0;
                for (FlipCutNodeSimpleWeight taxon : character.edges) {
                    long weight = CutGraphCutter.getInfinity();

                    if (!sSet.contains(taxon)) {
                        cutGraph.addEdge(character, taxon, weight);
                        cutGraph.addEdge(taxon, character.clone, weight);
                    } else {
                        sWeight = CutGraphCutter.getInfinity();
                    }
                }
                if (sWeight > 0) {
                    cutGraph.addEdge(character, s, sWeight);
                    cutGraph.addEdge(s, character.clone, sWeight);
                }
            }

            minCut = cutGraph.calculateMinSTCut(s, t);
            currentNode = new VaziraniCut(minCut.getCutSet(), minCut.minCutValue, 1);
            initCuts[j] = currentNode;
            //save lightest cut for HEAP init
            if (currentNode.minCutValue() < lightestCut.minCutValue()) lightestCut = currentNode;
        }
        //initialize heap
        VaziraniCut initialToHeap = new VaziraniCut(lightestCut.getCutSet(), lightestCut.minCutValue(), lightestCut.k); //todo why new node?
        queueAscHEAP.add(initialToHeap);
        //this.currentNode =  initialToHeap;
    }

    @Override
    public Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> cut(SourceTreeGraph<LinkedHashSet<FlipCutNodeSimpleWeight>> source) {
        if (source.equals(this.source))
            return getMinCut();
        return null;
    }

    @Override
    public Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> getMinCut() {
        return getNextCut();
    }

    static class Factory implements MultiCutterFactory<MultiCutGraphCutterVazirani, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, MaxFlowCutterFactory<MultiCutGraphCutterVazirani, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> {
        private final CutGraphTypes type;

        Factory(CutGraphTypes type) {
            this.type = type;
        }

        @Override
        public MultiCutGraphCutterVazirani newInstance(FlipCutGraphMultiSimpleWeight graph) {
            return new MultiCutGraphCutterVazirani(type, graph);
        }

        @Override
        public MultiCutGraphCutterVazirani newInstance(FlipCutGraphMultiSimpleWeight graph, ExecutorService executorService, int threads) {
            return newInstance(graph);
        }

        @Override
        public CutGraphTypes getType() {
            return type;
        }
    }
}
