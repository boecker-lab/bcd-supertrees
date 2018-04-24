package mincut.cutGraphImpl.minCutKargerStein;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import mincut.cutGraphAPI.bipartition.HashableCut;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.Hyperedge;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CompressedKargerGraph implements KargerGraph<CompressedKargerGraph, RoaringBitmap> {

    private int numberOfvertices;
    private final TIntObjectMap<RoaringBitmap> mergedTaxa;
    private ArrayList<RoaringBitmap> hyperEdges = null; //todo use a hashSet here. bitmap is stupit for single entry add and remove
    private TDoubleList cumulativeWeights = null;
    private int hashCache = 0;


    //this is more for testing than anything else
    /*public CompressedKargerGraph(List<? extends TIntCollection> hyperedges, List<? extends Number> weights) {
        final RoaringBitmap allTaxa = new RoaringBitmap();


        if (hyperedges == null || weights == null || hyperedges.size() != weights.size())
            throw new IllegalArgumentException("Input must not be null and there has to be a weight for every edge.");

        //add weights
        cumulativeWeights = new double[weights.size()];
        int i = 0;
        double current = 0d;
        for (Number weight : weights) {
            current += weight.doubleValue();
            cumulativeWeights[i++] = current;
        }

        //add hyperedges
        hyperEdges = new RoaringBitmap[hyperedges.size()];
        i = 0;
        for (TIntCollection hyperedge : hyperedges) {
            hyperEdges[i] = RoaringBitmap.bitmapOf(hyperedge.toArray());
            allTaxa.or(hyperEdges[i++]);
        }

        numberOfvertices = allTaxa.getCardinality();
        mergedTaxa = createMergedTaxaMap(allTaxa, numberOfvertices);

    }*/

    public CompressedKargerGraph(CompressedBCDGraph sourceGraph) {
        this(sourceGraph, true);
    }

    public CompressedKargerGraph(CompressedBCDGraph sourceGraph, boolean preMergeInfinityChars) {
        long start = System.currentTimeMillis();
        final RoaringBitmap allTaxa = new RoaringBitmap();

        if (preMergeInfinityChars && sourceGraph.hasGuideEdges()) {
            TObjectDoubleMap<RoaringBitmap> charCandidates = new TObjectDoubleHashMap<>();

            for (Hyperedge hyperedge : sourceGraph.hyperEdges()) {
                //check for normal edge
                if (!hyperedge.isInfinite()) {
                    RoaringBitmap taxa = hyperedge.ones().clone();
                    for (Hyperedge guide : sourceGraph.guideHyperEdges()) {
                        RoaringBitmap intersection = RoaringBitmap.and(taxa, guide.ones());
                        if (!intersection.isEmpty()) {
                            taxa.xor(intersection);
                            taxa.add(guide.ones().first());
                        }
                    }
                    //add only if there are edges left after merging
                    if (taxa.getCardinality() > 0) {
                        allTaxa.or(taxa);
                        if (taxa.getCardinality() > 1) //add characters and merge identical ones
                            charCandidates.adjustOrPutValue(taxa, hyperedge.getWeight(), hyperedge.getWeight());
                    }

                }
            }

            // write merged character candidates to data structures
            hyperEdges = new ArrayList<>(charCandidates.size());
            cumulativeWeights = new TDoubleArrayList(charCandidates.size());
            final TObjectDoubleIterator<RoaringBitmap> it = charCandidates.iterator();
            double before = 0;
            for (int i = 0; i < charCandidates.size(); i++) {
                it.advance();
                before += it.value();
                hyperEdges.add(it.key());
                cumulativeWeights.add(before);
            }

            numberOfvertices = allTaxa.getCardinality();
            mergedTaxa = createMergedTaxaMap(allTaxa, numberOfvertices);
            //add pre merged taxa
            for (Hyperedge hyperedge : sourceGraph.guideHyperEdges()) {
                mergedTaxa.get(hyperedge.ones().first()).or(hyperedge.ones());
            }
        } else {
            hyperEdges = new ArrayList<>(sourceGraph.numCharacter());
            cumulativeWeights = new TDoubleArrayList(sourceGraph.numCharacter());
            double current = 0;
            for (Hyperedge character : sourceGraph.hyperEdges()) {
                RoaringBitmap he = character.ones().clone();
                current += character.getWeight();
                cumulativeWeights.add(current);
                hyperEdges.add(he);
                allTaxa.or(he);
            }
            numberOfvertices = allTaxa.getCardinality();
            mergedTaxa = createMergedTaxaMap(allTaxa, numberOfvertices);
        }

        System.out.println("Create GRAPH took:" + (System.currentTimeMillis() - start) / 1000d + "s");
    }

    //clone constructor
    private CompressedKargerGraph(ArrayList<RoaringBitmap> hyperEdges, TDoubleList cumulativeWeights, TIntObjectMap<RoaringBitmap> mergedTaxa, int numVertices) {
        this.mergedTaxa = mergedTaxa;
        this.hyperEdges = hyperEdges;
        this.cumulativeWeights = cumulativeWeights;
        this.numberOfvertices = numVertices;
    }

    private TIntObjectMap<RoaringBitmap> createMergedTaxaMap(final RoaringBitmap allTaxa, final int numOfTaxa) {
        final TIntObjectMap<RoaringBitmap> mergedTaxa = new TIntObjectHashMap<>(numOfTaxa);
        allTaxa.forEach((IntConsumer) value -> {
            mergedTaxa.put(value, RoaringBitmap.bitmapOf(value));
        });
        return mergedTaxa;
    }

    public void contract() {
        contract(ThreadLocalRandom.current());
    }

    public void contract(final Random random) {
        final int selectedIndex = drawCharacter(random);
        // select the character from wich we want do merge (probs correspond to weight)
        final RoaringBitmap selectedCharacter = hyperEdges.get(selectedIndex);
        final int numberOfTaxaInCharacter = selectedCharacter.getCardinality();

        // select an edge to merge (equally distributed)
        //select random pair of taxa -> clique so every edge exists
        final int firstDrawn;
        int secondDrawn;
        if (selectedCharacter.getCardinality() == 2) {
            firstDrawn = selectedCharacter.first();
            secondDrawn = selectedCharacter.last();
        } else {
            firstDrawn = selectedCharacter.select(random.nextInt(numberOfTaxaInCharacter));
            int randSecond = random.nextInt(numberOfTaxaInCharacter - 1) + 1; //draw from all but the first one
            secondDrawn = selectedCharacter.select(randSecond);
            if (secondDrawn == firstDrawn) //if firstDrawn equals secondDrawn use the first one which was not part of the random selections
                secondDrawn = selectedCharacter.first();
        }

        assert firstDrawn != secondDrawn;

        // merge taxa
        mergedTaxa.get(firstDrawn).or(mergedTaxa.remove(secondDrawn));

        numberOfvertices--;

        // refresh hyperEdges and cumulative weights
        refreshCharactersAndCumulativeWeights(firstDrawn, secondDrawn);
    }

    private void refreshCharactersAndCumulativeWeights(int firstDrawn, int secondDrawn) {
        final ArrayList<RoaringBitmap> nuChars = new ArrayList<>(hyperEdges.size());
        final TDoubleList nuWeights = new TDoubleArrayList(hyperEdges.size());

        double before = 0d;
        double beforeNu = 0d;

        for (int i = 0; i < cumulativeWeights.size(); i++) {
            final double currentWeight = cumulativeWeights.get(i);
            final RoaringBitmap hyperEdge = hyperEdges.get(i);

            if (!mergeEdgeIfExistsAndCheckIsEmpty(hyperEdge, firstDrawn, secondDrawn)) {
                beforeNu = beforeNu + (currentWeight - before);
                nuWeights.add(beforeNu);
                nuChars.add(hyperEdge);
            }

            before = currentWeight;
        }

        hyperEdges = nuChars;
        cumulativeWeights = nuWeights;

        /*if (!toRemove.isEmpty()) {
            RoaringBitmap[] nuChars = new RoaringBitmap[hyperEdges.length - toRemove.size()];
            double[] nuWeights = new double[cumulativeWeights.length - toRemove.size()];

            int j = 0;
            double before = 0d;
            double beforeNu = 0d;

            for (int i = 0; i < cumulativeWeights.length; i++) {
                if (!toRemove.contains(i)) {
                    nuWeights[j] = beforeNu + (cumulativeWeights[i] - before);
                    beforeNu = nuWeights[j];
                    nuChars[j] = hyperEdges[i];
                    j++;
                }

                before = cumulativeWeights[i];
            }
            hyperEdges = nuChars;
            cumulativeWeights = nuWeights;
        }*/

    }


    public boolean isCutted() {
        return mergedTaxa.size() == 2 && numberOfvertices == 2;
    }

    @Override
    public double getSumOfWeights() {
        return cumulativeWeights.get(cumulativeWeights.size() - 1);
    }

    @Override
    public int getNumberOfVertices() {
        return numberOfvertices;
    }

    private boolean mergeEdgeIfExistsAndCheckIsEmpty(final RoaringBitmap connectedTaxa, final int firstDrawn, final int secondDrawn) {
        if (connectedTaxa.contains(secondDrawn)) {
            connectedTaxa.flip(secondDrawn);
            if (!connectedTaxa.contains(firstDrawn))
                connectedTaxa.flip(firstDrawn);
        }

        return connectedTaxa.getCardinality() < 2;
    }

    private int drawCharacter(final Random random) {
        double max = cumulativeWeights.get(cumulativeWeights.size() - 1);
        double r = max * random.nextDouble();
        int i = cumulativeWeights.binarySearch(r); //todo equal to java collections api?
        return i < 0 ? Math.abs(i) - 1 : i;
    }


    @Override
    public CompressedKargerGraph clone() {
        long start = System.currentTimeMillis();
        final TIntObjectMap<RoaringBitmap> nuMergedTaxa = new TIntObjectHashMap<>(mergedTaxa.size());
        mergedTaxa.forEachEntry((a, b) -> {
            nuMergedTaxa.put(a, b.clone());
            return true;
        });
        long taxaTime = System.currentTimeMillis();
//        System.out.println("Cloning taxa took" + (taxaTime - taxaTime) / 1000d + "s");
        ArrayList<RoaringBitmap> chars = hyperEdges.stream().map(RoaringBitmap::clone).collect(Collectors.toCollection(ArrayList::new));
//        System.out.println("Cloning chars took" + (System.currentTimeMillis() - taxaTime) / 1000d + "s");
        CompressedKargerGraph clone = new CompressedKargerGraph(
                chars,
                new TDoubleArrayList(cumulativeWeights),
                nuMergedTaxa,
                numberOfvertices
        );
//        System.out.println("Cloning took" + (System.currentTimeMillis() - start) / 1000d + "s");
        return clone;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompressedKargerGraph)) return false;

        CompressedKargerGraph graph = (CompressedKargerGraph) o;

        if (Double.compare(graph.getSumOfWeights(), getSumOfWeights()) != 0) return false;
        if (isCutted() != graph.isCutted()) return false;
        boolean r = mergedTaxa.valueCollection().equals(graph.mergedTaxa.valueCollection());
        if (r && hashCode() != graph.hashCode())
            throw new RuntimeException("Hash exception!!!!!!!");
        return r;

    }

    @Override
    public int hashCode() {
        if (!isCutted() || hashCache == 0) {
            int result;
            long temp;
            temp = Double.doubleToLongBits(getSumOfWeights());
            result = (int) (temp ^ (temp >>> 32));
            result = 31 * result + mergedTaxa.valueCollection().hashCode();
            result = 31 * result + (isCutted() ? 1 : 0);
            hashCache = result;
        }
        return hashCache;
    }

    public HashableCut<RoaringBitmap> asCut() {
        if (!isCutted())
            throw new IllegalStateException("Graph has to be cutted to get Cut representation");
        TIntObjectIterator<RoaringBitmap> it = mergedTaxa.iterator();
        it.advance();
        RoaringBitmap s = it.value();
        it.advance();
        RoaringBitmap t = it.value();
        return new HashableCut<>(s, t, getSumOfWeights());
    }
}
