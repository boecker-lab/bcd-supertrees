package phylo.tree.algorithm.flipcut.cutter;

import mincut.cutGraphAPI.bipartition.CompressedBCDCut;
import mincut.cutGraphAPI.bipartition.MultiCut;
import mincut.cutGraphAPI.bipartition.VaziraniCut;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDMultiCutGraph;

import java.util.ArrayList;
import java.util.List;

public class MultiCutGrgaphCutterVaziraniCompressedBCD extends  AbstractMultiCutGraphCutterVazirani<RoaringBitmap,CompressedBCDMultiCutGraph>{

    int[] taxa;

    public MultiCutGrgaphCutterVaziraniCompressedBCD(CompressedBCDMultiCutGraph graphToCut) {
        super(graphToCut);
    }

    @Override
    protected void initialCut() {
       /* // get the mincut, fix s iterate over t
        int s; //taxon
        int t;
        VaziraniCut<RoaringBitmap> currentNode;
        CompressedBCDCut minCut;
        VaziraniCut<RoaringBitmap> lightestCut;

        // inti data structures
        taxa = source.getTaxa().toArray();
        initCuts = new VaziraniCutNode[taxa.size() - 1];
        queueAscHEAP = new PriorityQueue<>();

        //j=0
        GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight> cutGraph = new GoldbergTarjanCutGraph<>();
        dummyToMerged = SimpleCutGraphCutter.createTarjanGoldbergHyperGraphTaxaMerged(cutGraph, source, mapping, new ArrayList<>(taxa.size()));

        minCut = cutGraph.calculateMinSTCut(taxa.get(0), taxa.get(1));
        lightestCut = new VaziraniCutNode<FlipCutNodeSimpleWeight>(minCut.getCutSet(), minCut.minCutValue, 1);
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
            SimpleCutGraphCutter.createGoldbergTarjanCharacterWeights(cutGraph, characters);

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
            currentNode = new VaziraniCutNode<FlipCutNodeSimpleWeight>(minCut.getCutSet(), minCut.minCutValue, 1);
            initCuts[j] = currentNode;
            //save lightest cut for HEAP init
            if (currentNode.minCutValue() < lightestCut.minCutValue()) lightestCut = currentNode;
        }
        //initialize heap
        VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>> initialToHeap = new VaziraniCutNode<>(lightestCut.getCutSet(), lightestCut.minCutValue(), lightestCut.k()); //todo why new node?
        queueAscHEAP.add(initialToHeap);*/
    }

    @Override
    protected List<VaziraniCut<RoaringBitmap>> findCutsFromPartialCuts(VaziraniCut<RoaringBitmap> sourceCut, VaziraniCut<RoaringBitmap>[] initCuts) {
        return null;
    }

    @Override
    protected MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph> buildOutputCut(VaziraniCut<RoaringBitmap> currentNode) {
        return null;
    }

    @Override
    public boolean isBCD() {
        return true;
    }
}
