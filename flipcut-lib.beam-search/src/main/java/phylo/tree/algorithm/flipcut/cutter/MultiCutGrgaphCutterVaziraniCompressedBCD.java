package phylo.tree.algorithm.flipcut.cutter;

import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import mincut.cutGraphAPI.bipartition.CompressedBCDMultiCut;
import mincut.cutGraphAPI.bipartition.MultiCut;
import mincut.cutGraphAPI.bipartition.VaziraniCut;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.CutGraphImpl;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.Node;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDMultiCutGraph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class MultiCutGrgaphCutterVaziraniCompressedBCD extends AbstractMultiCutGraphCutterVazirani<RoaringBitmap, CompressedBCDMultiCutGraph> {

    int[] taxa;


    public MultiCutGrgaphCutterVaziraniCompressedBCD(CompressedBCDMultiCutGraph graphToCut) {
        super(graphToCut);
    }

    //creates a cutset, that contains taxa an characters to delete
    //todo, we nee also to map the taxa back
    private RoaringBitmap getCutSet(CutGraphImpl hipri, CompressedBCDGraph source, TIntObjectMap<Node.IntNode> cutgraphTaxa, TIntObjectMap<TIntList> charMapping) {
        RoaringBitmap cutSet = new RoaringBitmap();
        RoaringBitmap taxa = new RoaringBitmap();
        for (Node node : hipri.getNodes()) {
            int nodeIndex = ((Node.IntNode) node).getIntName();
            if (hipri.isInSourceSet(node)) {
                if (source.isCharacter(nodeIndex)) {
                    int[] arr = charMapping.get(nodeIndex).toArray();
                    //collect taxa form characters
                    for (int i : arr) {
                        taxa.or(source.getEdge(i).ones);
                    }
                    //collect only chracters that have to be deleted to disconnect the graph
                    if (hipri.isInSourceSet(cutgraphTaxa.get(source.getCloneIndex(nodeIndex))))
                        cutSet.add(arr);
                }
            }
        }
        cutSet.or(taxa);
        return cutSet;
    }

    private List<RoaringBitmap> mergeGuideEdgesWithTaxonSet(final List<RoaringBitmap> guideEdges, final RoaringBitmap taxonSet) {
        int overlappingGuides = 0;
        //merge taxa with giude edge if they do overlap
        Iterator<RoaringBitmap> it = guideEdges.iterator();
        while (it.hasNext()) {
            RoaringBitmap guideEdge = it.next();
            if (RoaringBitmap.intersects(taxonSet, guideEdge)) {
                taxonSet.or(guideEdge);
                it.remove();
                overlappingGuides++;
            }
        }
        guideEdges.add(taxonSet); //add (merged) taxa to set og guide edges
        System.out.println("Overlapping Guides: " + overlappingGuides);
        return guideEdges;
    }

    @Override
    protected void initialCut() {
        // get the mincut, fix s iterate over t
        VaziraniCut<RoaringBitmap> currentNode;
        VaziraniCut<RoaringBitmap> lightestCut;

        // inti data structures
        TIntObjectMap<TIntList> charMapping = new TIntObjectHashMap<>();
        TIntObjectMap<Node.IntNode> cutgraphTaxa = new TIntObjectHashMap<>(source.numTaxa());

        CutGraphImpl hipri = CompressedSingleCutter.createHipri(
                source.getSource(), CompressedSingleCutter.createGuideEdges(source.getSource()), charMapping, cutgraphTaxa
        );

        taxa = charMapping.keys();
        initCuts = new VaziraniCut[taxa.length - 1];
        queueAscHEAP = new PriorityQueue<>();

        //j=0
        hipri.setSource(cutgraphTaxa.get(taxa[0]));
        hipri.setSink(cutgraphTaxa.get(taxa[1]));
        hipri.calculateMaxFlow(false);

        long cutValue = hipri.getValue();
        //createCutset
        RoaringBitmap cutSet = getCutSet(hipri, source.getSource(), cutgraphTaxa, charMapping);

        //here the cutset is the set of characters to delete
        lightestCut = new VaziraniCut<>(cutSet, cutValue, 1);
        initCuts[0] = lightestCut;

        //ATTENTION this is  the undirected graph version as tweak for symmetric bcd graph
        for (int j = 1; j < taxa.length - 1; j++) {
            //build cutgraph from patial cut
            //merge taxa together for a partial cut
            RoaringBitmap sSet = new RoaringBitmap();
            for (int i = 0; i <= j; i++) {
                sSet.add(taxa[i]);
            }

            //add merged taxa as a guide tree
            List<RoaringBitmap> guideEdges = mergeGuideEdgesWithTaxonSet(
                    CompressedSingleCutter.createGuideEdges(source.getSource()), sSet
            );

            //create graph: add characters and remaining taxa to cutgraph and merge everything that has to
            charMapping.clear();
            cutgraphTaxa.clear();
            hipri = CompressedSingleCutter.createHipri(source.getSource(), guideEdges, charMapping, cutgraphTaxa);

            //calculate partial cut
            hipri.setSource(cutgraphTaxa.get(taxa[j]));
            hipri.setSink(cutgraphTaxa.get(taxa[j + 1]));
            hipri.calculateMaxFlow(false);

            cutValue = hipri.getValue();
            cutSet = getCutSet(hipri, source.getSource(), cutgraphTaxa, charMapping);
            currentNode = new VaziraniCut<>(cutSet, cutValue, 1);
            initCuts[j] = currentNode;
            //save lightest cut for HEAP init
            if (currentNode.minCutValue() < lightestCut.minCutValue()) lightestCut = currentNode;
        }
        //initialize heap
        VaziraniCut<RoaringBitmap> initialToHeap = new VaziraniCut<>(lightestCut.getCutSet(), lightestCut.minCutValue(), lightestCut.k()); //todo why new node?
        queueAscHEAP.add(initialToHeap);
    }


    @Override
    protected List<VaziraniCut<RoaringBitmap>> findCutsFromPartialCuts(VaziraniCut<RoaringBitmap> sourceCut, VaziraniCut<RoaringBitmap>[] initCuts) {
        RoaringBitmap cut = sourceCut.getCutSet();
        List<VaziraniCut<RoaringBitmap>> cuts = new ArrayList<>(taxa.length - sourceCut.k());// todo is the size correct?

        // finding all partial mincut
        for (int k = sourceCut.k(); k < taxa.length; k++) {
            RoaringBitmap sSet = new RoaringBitmap();
            RoaringBitmap tSet = new RoaringBitmap();

            int taxon;
            for (int i = 0; i < k; i++) {
                //todo maybe use Bitmap operations here
                taxon = taxa[i];
                if (cut.contains(taxon)) {
                    tSet.add(taxon);
                } else {
                    sSet.add(taxon);
                }
            }

            //change position of taxon number k
            taxon = taxa[k];
            if (!cut.contains(taxon)) {
                tSet.add(taxon);
            } else {
                sSet.add(taxon);
            }


            if (!tSet.isEmpty()) {
                //build guide tree edgeSet and merge it with s and t set if nessecary
                List<RoaringBitmap> guideEdges = mergeGuideEdgesWithTaxonSet(
                        mergeGuideEdgesWithTaxonSet(
                                CompressedSingleCutter.createGuideEdges(source.getSource()
                                ), sSet
                        ), tSet
                );

                TIntObjectMap<TIntList> charMapping = new TIntObjectHashMap<>();
                TIntObjectMap<Node.IntNode> cutgraphTaxa = new TIntObjectHashMap<>(source.numTaxa());

                CutGraphImpl hipri = CompressedSingleCutter.createHipri(source.getSource(), guideEdges, charMapping, cutgraphTaxa);
                hipri.setSource(cutgraphTaxa.get(sSet.first()));
                hipri.setSink(cutgraphTaxa.get(tSet.first()));

                hipri.calculateMaxFlow(false);
                long cutValue = hipri.getValue();
                RoaringBitmap cutSet = getCutSet(hipri, source.getSource(), cutgraphTaxa, charMapping);

                VaziraniCut<RoaringBitmap> currentCut = new VaziraniCut<>(cutSet, cutValue, k + 1);
                cuts.add(currentCut);

            } else if (sSet.getCardinality() < taxa.length) {
                //find cut for 0^k case
                //tSet empty --> adding new init Graph! 0^k case);
                VaziraniCut<RoaringBitmap> initCut = null;
                //find best
                for (int i = k; i < initCuts.length; i++) {
                    if (initCut == null || initCuts[i].minCutValue() < initCut.minCutValue()) {
                        initCut = initCuts[i];
                    }
                }
                //copy to new object
                initCut = new VaziraniCut<>(initCut.getCutSet(), initCut.minCutValue(), k + 1);
                cuts.add(initCut);
            }
        }
        return cuts;
    }


    @Override
    protected MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph> buildOutputCut(VaziraniCut<RoaringBitmap> currentNode) {
        //remove taxa indeces from cutset
        //todo maybe do that in place, hence taxa the vazicut is not longer needed when this method is called
        RoaringBitmap cutset = RoaringBitmap.and(currentNode.cutSet, source.getSource().characters);
        return new CompressedBCDMultiCut(cutset, currentNode.minCutValue, source);
    }

    @Override
    public boolean isBCD() {
        return true;
    }
}
