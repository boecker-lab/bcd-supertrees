package mincut.cutGraphImpl.minCutKargerStein;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.Hyperedge;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CompressedKargerGraph implements KargerGraph<CompressedKargerGraph> {

    private int numberOfvertices;
    private final TIntObjectMap<RoaringBitmap> mergedTaxa;
    private RoaringBitmap[] hyperEdges = null;
    private double[] cumulativeWeights = null;
    private int hashCache = 0;


    //this is more for testing than anything else
    public CompressedKargerGraph(List<? extends TIntCollection> hyperedges, List<? extends Number> weights) {
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

    }

    public CompressedKargerGraph(CompressedBCDGraph sourceGraph) {
        this(sourceGraph, true);
    }

    public CompressedKargerGraph(CompressedBCDGraph sourceGraph, boolean preMergeInfinityChars) {
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
            hyperEdges = new RoaringBitmap[charCandidates.size()];
            cumulativeWeights = new double[charCandidates.size()];
            final TObjectDoubleIterator<RoaringBitmap> it = charCandidates.iterator();
            double before = 0;
            for (int i = 0; i < hyperEdges.length; i++) {
                it.advance();
                before += it.value();
                hyperEdges[i] = it.key();
                cumulativeWeights[i] = before;
            }

            numberOfvertices = allTaxa.getCardinality();
            mergedTaxa = createMergedTaxaMap(allTaxa, numberOfvertices);
            //add pre merged taxa
            for (Hyperedge hyperedge : sourceGraph.guideHyperEdges()) {
                mergedTaxa.get(hyperedge.ones().first()).or(hyperedge.ones());
            }
        } else {
            hyperEdges = new RoaringBitmap[sourceGraph.numCharacter()];
            cumulativeWeights = new double[sourceGraph.numCharacter()];
            int i = 0;
            double current = 0;
            for (Hyperedge character : sourceGraph.hyperEdges()) {
                current += character.getWeight();
                cumulativeWeights[i] = current;
                hyperEdges[i] = character.ones().clone();
                allTaxa.or(hyperEdges[i]);
                i++;
            }
            numberOfvertices = allTaxa.getCardinality();
            mergedTaxa = createMergedTaxaMap(allTaxa, numberOfvertices);
        }
    }

    //clone constructor
    private CompressedKargerGraph(RoaringBitmap[] hyperEdges, double[] cumulativeWeights, TIntObjectMap<RoaringBitmap> mergedTaxa, int numVertices) {
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
        final RoaringBitmap selectedCharacter = hyperEdges[selectedIndex];
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

        // contract selected edge(s)
        TIntSet hyperEdgesToRemove = new TIntHashSet();
        for (int i = 0; i < hyperEdges.length; i++) {
            if (mergeEdgeIfExistsAndCheckIsEmpty(hyperEdges[i], firstDrawn, secondDrawn)) {
                hyperEdgesToRemove.add(i);
            }
        }

        numberOfvertices--;

        // refresh hyperEdges and cumulative weights
        refreshCharactersAndCumulativeWeights(hyperEdgesToRemove);
    }


    public Set<RoaringBitmap> getTaxaCutSets() {
        if (mergedTaxa.size() != 2)
            throw new NoResultException("Number of cutsets != 2!");
        return new HashSet<>(mergedTaxa.valueCollection());
    }


    public boolean isCutted() {
        return mergedTaxa.size() == 2 && numberOfvertices == 2;
    }

    @Override
    public double getSumOfWeights() {
        return cumulativeWeights[cumulativeWeights.length - 1];
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
        double max = cumulativeWeights[cumulativeWeights.length - 1];
        double r = max * random.nextDouble();
        int i = Arrays.binarySearch(cumulativeWeights, r);
        return i < 0 ? Math.abs(i) - 1 : i;
    }

    private void refreshCharactersAndCumulativeWeights(TIntSet toRemove) {
        if (!toRemove.isEmpty()) {
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
        }
    }

    @Override
    public CompressedKargerGraph clone() {
        final TIntObjectMap<RoaringBitmap> nuMergedTaxa = new TIntObjectHashMap<>();
        mergedTaxa.forEachEntry((a, b) -> {
            nuMergedTaxa.put(a, b.clone());
            return true;
        });

        return new CompressedKargerGraph(
                Arrays.stream(hyperEdges).map(RoaringBitmap::clone).toArray(RoaringBitmap[]::new),
                Arrays.copyOf(cumulativeWeights, cumulativeWeights.length),
                nuMergedTaxa,
                numberOfvertices
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompressedKargerGraph)) return false;

        CompressedKargerGraph graph = (CompressedKargerGraph) o;

        if (Double.compare(graph.getSumOfWeights(), getSumOfWeights()) != 0) return false;
        if (isCutted() != graph.isCutted()) return false;
        return mergedTaxa.valueCollection().equals(graph.mergedTaxa.valueCollection());

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
}
