package flipCut.flipCutGraph;

import cuts.CutGraph;
import flipCut.costComputer.CostComputer;
import flipCut.model.Cut;
import flipCut.model.VaziraniNode;

import java.util.*;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 14:05
 */

/**
 * VAZIRANI ALGORITHM
 */


public class MultiCutGraphCutter extends SimpleCutGraphCutter<FlipCutGraphMultiSimpleWeight> implements MultiCutter {

    private static final boolean PRINT_GRAPHS = false;
    private PriorityQueue<VaziraniNode> queueAscHEAP = null;
    private VaziraniNode<FlipCutNodeSimpleWeight> currentNode = null;
    private VaziraniNode<FlipCutNodeSimpleWeight>[] initCuts;


    public MultiCutGraphCutter(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut) {
        super(type);
        source = graphToCut;
    }

    private List<VaziraniNode> findCutsFromPartialCuts(VaziraniNode sourceCut, VaziraniNode[] initCuts) {
        Set<FlipCutNodeSimpleWeight> cut = (Set<FlipCutNodeSimpleWeight>) sourceCut.cut;


        List<VaziraniNode> cuts = new ArrayList<VaziraniNode>(source.taxa.size() - sourceCut.k);

        VaziraniNode currentCut;
        CutGraph<FlipCutNodeSimpleWeight> cutGraph;
        Set<FlipCutNodeSimpleWeight> sSet;
        Set<FlipCutNodeSimpleWeight> tSet;

        // finding all partial cuts
        for (int k = sourceCut.k; k < source.taxa.size(); k++) {
            sSet = new HashSet<FlipCutNodeSimpleWeight>();
            tSet = new HashSet<FlipCutNodeSimpleWeight>();

            for (int i = 0; i < k; i++) {
                if (cut.contains(source.taxa.get(i))) {
                    tSet.add(source.taxa.get(i));
                } else {
                    sSet.add(source.taxa.get(i));
                }
            }

            //change position of taxon number k
            if (!cut.contains(source.taxa.get(k))) {
                tSet.add(source.taxa.get(k));
            } else {
                sSet.add(source.taxa.get(k));
            }

            //build cutgraph from patial cut
            cutGraph = new CutGraph<FlipCutNodeSimpleWeight>();

            // add characters, character clones and edges between them
            //todo character merging stuff
            switch (type) {
                case MAXFLOW_TARJAN_GOLDBERG: {
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    break;
                }
                case HYPERGRAPH_MINCUT: {
                    //todo implement
                    break;
                }
                case HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW: {
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    break;
                }
            }
            //add taxa > k
            for (int i = k + 1; i < source.taxa.size(); i++) {
                cutGraph.addNode(source.taxa.get(i));
            }
            FlipCutNodeSimpleWeight randomS = sSet.iterator().next();
            cutGraph.addNode(randomS);

            if (!tSet.isEmpty()) {
                FlipCutNodeSimpleWeight randomT = tSet.iterator().next();
                cutGraph.addNode(randomT);

                // add edges from character to taxa and merge s and t nodes
                for (FlipCutNodeSimpleWeight character : source.characters) {
                    long sWeight = 0;
                    long tWeight = 0;
                    for (FlipCutNodeSimpleWeight taxon : character.edges) {
                        long weight = 0;
                        //todo character merging stuff
                        switch (type) {
                            case MAXFLOW_TARJAN_GOLDBERG: {
                                weight = character.edgeWeight;
                                break;
                            }
                            case HYPERGRAPH_MINCUT: {
                                //todo implement
                                break;
                            }
                            case HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW: {
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
                if (DEBUG && PRINT_GRAPHS) {
                    System.out.println("SOURCE = " + randomS);
                    System.out.println("TARGET = " + randomT);
                    cutGraph.printGraph();
                }

                // compute mincut an put it to results
                long minCutValue = cutGraph.getMinCutValue(randomS, randomT);
                Set<FlipCutNodeSimpleWeight> setT = new HashSet<FlipCutNodeSimpleWeight>(cutGraph.getMinCut(randomS, randomT));
                setT.addAll(tSet);

                currentCut = new VaziraniNode(setT, minCutValue, k + 1);
                cuts.add(currentCut);

            } else {
                //find cut for 0^k case
                if (sSet.size() < source.taxa.size()) {
                    //tSet empty --> adding new init Graph! 0^k case);
                    VaziraniNode initCut = null;
                    //find best
                    for (int i = k; i < initCuts.length; i++) {
                        if (initCut == null || initCuts[i].cutWeight < initCut.cutWeight) {
                            initCut = initCuts[i];
                        }
                    }
                    //copy to new object
                    initCut = new VaziraniNode(initCut.cut, initCut.cutWeight, k+1);
                    cuts.add(initCut);
                }
            }
        }
        return cuts;
    }



    public Cut getNextCut() {
        if (queueAscHEAP != null && queueAscHEAP.isEmpty()) {
            // all cuts calculated
            return null;
        }else{
            if (queueAscHEAP == null){
                initialCut();
            }
            nextCut();
            mincut = new ArrayList<FlipCutNodeSimpleWeight>(currentNode.cut); //todo not really needed remove later!
            mincutValue = currentNode.cutWeight; //todo not really needed remove later!
            return new Cut(mincut,mincutValue,source);
        }
    }

    public CutGraphTypes getType() {
        return type;
    }


    private void nextCut() {
        //Starting find subobtimal cuts cuts with vaziranis algo
        currentNode = queueAscHEAP.poll();
        //compute next cut candidates with vaziranis algo
        List<VaziraniNode> toHeap = findCutsFromPartialCuts(currentNode, initCuts);
        for (VaziraniNode node : toHeap) {
            queueAscHEAP.add(node);
        }
    }

    private void initialCut(){
        CutGraph<FlipCutNodeSimpleWeight> cutGraph = null;

        // get the mincut, fix s iterate over t
        FlipCutNodeSimpleWeight s;
        FlipCutNodeSimpleWeight t;
        VaziraniNode currentNode;

        //ArrayList<FlipCutNodeSimpleWeight> innerNodes;
        List<FlipCutNodeSimpleWeight> minCut;
        long minCutValue;

        VaziraniNode lightestCut;
        initCuts = new VaziraniNode[source.taxa.size()-1];
        queueAscHEAP = new PriorityQueue<VaziraniNode>();

        //j=0
        //todo integrate character merging stuff!
        switch (type) {
            case MAXFLOW_TARJAN_GOLDBERG: {
                cutGraph = new CutGraph<FlipCutNodeSimpleWeight>();
                createGoldbergTarjanCharacterWeights(cutGraph);
                createGoldbergTarjan(cutGraph);
                break;
            }
            case HYPERGRAPH_MINCUT: {
                //todo implement
                break;
            }
            case HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW: {
                cutGraph = new CutGraph<FlipCutNodeSimpleWeight>();
                createGoldbergTarjanCharacterWeights(cutGraph);
                createTarjanGoldbergHyperGraph(cutGraph);
                break;
            }
        }
        minCut = cutGraph.getMinCut(source.taxa.get(0), source.taxa.get(1));
        minCutValue = cutGraph.getMinCutValue(source.taxa.get(0), source.taxa.get(1));
        lightestCut = new VaziraniNode(new HashSet<FlipCutNodeSimpleWeight>(minCut), minCutValue, 1);
        initCuts[0] = lightestCut;

        //ATTENTION this is  the undirected graph version as tweak for flipCut Graph
        for (int j = 1; j < source.taxa.size() - 1; j++) {
            s = source.taxa.get(j);
            t = source.taxa.get(j + 1);


            //build cutgraph from patial cut
            cutGraph = new CutGraph<FlipCutNodeSimpleWeight>();
            Set<FlipCutNodeSimpleWeight> sSet = new HashSet<FlipCutNodeSimpleWeight>();
            for (int i = 0; i <= j; i++) {
                sSet.add(source.taxa.get(i));
            }


            // add characters, character clones and edges between them
            //todo character merging stuff
            switch (type) {
                case MAXFLOW_TARJAN_GOLDBERG: {
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    break;
                }
                case HYPERGRAPH_MINCUT: {
                    //todo implement
                    break;
                }
                case HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW: {
                    createGoldbergTarjanCharacterWeights(cutGraph);
                    break;
                }
            }

            //add taxa
            for (int i = j + 1; i < source.taxa.size(); i++) {
                cutGraph.addNode(source.taxa.get(i));
                //System.out.println("adding taxa " + node.name);
            }

            // add edges from character to taxa and merge s and t nodes
            for (FlipCutNodeSimpleWeight character : source.characters) {
                long sWeight = 0;
                for (FlipCutNodeSimpleWeight taxon : character.edges) {
                    long weight = 0;
                    switch (type) {
                        case MAXFLOW_TARJAN_GOLDBERG: {
                            weight = character.edgeWeight;
                            break;
                        }
                        case HYPERGRAPH_MINCUT: {
                            //todo implement
                            break;
                        }
                        case HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW: {
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

            minCut = cutGraph.getMinCut(s, t);
            minCutValue = cutGraph.getMinCutValue(s, t);
            currentNode = new VaziraniNode(new HashSet<FlipCutNodeSimpleWeight>(minCut), minCutValue, 1);
            initCuts[j] = currentNode;
            //save lightest cut for HEAP init
            if (currentNode.cutWeight < lightestCut.cutWeight) lightestCut = currentNode;
        }
        //initialize heap
        VaziraniNode initialToHeap = new VaziraniNode(lightestCut); //todo why new node?
        queueAscHEAP.add(initialToHeap);
        //this.currentNode =  initialToHeap;
    }

    @Override
    public List<FlipCutGraphMultiSimpleWeight> cut(FlipCutGraphMultiSimpleWeight source) {
        return getNextCut().getSplittedGraphs();

    }
}
