package mincut.cutGraphImpl.minCutKargerStein;

import com.google.common.collect.Iterables;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.Hyperedge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class CompressedKargerGraph implements Cloneable {
    private TIntObjectMap<RoaringBitmap> mergedTaxa;
    private RoaringBitmap[] characters = null;
    private double[] cumulativeWeights = null;


    public CompressedKargerGraph(CompressedBCDGraph sourceGraph) {
        this(sourceGraph, true);
    }

    public CompressedKargerGraph(CompressedBCDGraph sourceGraph, boolean preMergeInfinityChars) {
        this.mergedTaxa = new TIntObjectHashMap<>();

        if (preMergeInfinityChars && sourceGraph.hasGuideEdges()) {
            List<RoaringBitmap> charCandidates = new ArrayList<>();
            TDoubleList weights = new TDoubleArrayList();

            for (Hyperedge hyperedge : sourceGraph.hyperEdges()) {
                //check for normal edge
                if (!hyperedge.isInfinite()) {
                    RoaringBitmap taxa = hyperedge.ones().clone();
                    for (Hyperedge guide : sourceGraph.guideHyperEdges()) {
                        RoaringBitmap inter = RoaringBitmap.and(taxa, guide.ones());
                        //todo better performance with empty check???
                        taxa.xor(inter);
                    }
                    //add only if there are edges left after merging
                    if (taxa.getCardinality() > 1) {
                        charCandidates.add(taxa);
                        weights.add(weights.isEmpty() ? hyperedge.getWeight() : hyperedge.getWeight() + weights.get(weights.size()));
                    }

                }
            }

            characters = Iterables.toArray(charCandidates, RoaringBitmap.class);
            cumulativeWeights = weights.toArray();
        } else {
            characters = new RoaringBitmap[sourceGraph.numCharacter()];
            int i = 0;
            double current = 0;
            for (Hyperedge character : sourceGraph.hyperEdges()) {
                current += character.getWeight();
                cumulativeWeights[i] = current;
                characters[i] = character.ones().clone();
            }
        }
    }

    //clone constructor
    private CompressedKargerGraph(RoaringBitmap[] characters, double[] cumulativeWeights, TIntObjectMap<RoaringBitmap> mergedTaxa) {
        this.mergedTaxa = mergedTaxa;
        this.characters = characters;
        this.cumulativeWeights = cumulativeWeights;
    }

    public void contract() {
        contract(ThreadLocalRandom.current());
    }

    public void contract(final Random random) {
        final int selectedIndex = drawCharacter(random);
        // select the character from wich we want do merge (probs correspond to weight)
        final RoaringBitmap selectedCharacter = characters[selectedIndex];
        final int numberOfTaxaInCharacter = selectedCharacter.getCardinality();

        // select an edge to merge (equally distributed)
        //select random pair of taxa -> clique so every edge exists
        final int first = selectedCharacter.select(random.nextInt(numberOfTaxaInCharacter));
        int randSecond = random.nextInt(numberOfTaxaInCharacter - 1) + 1;
        int second = selectedCharacter.select(randSecond);
        if (second == first)
            second = selectedCharacter.first();

        // merge taxa
        if (!mergedTaxa.containsKey(first))
            mergedTaxa.put(first, RoaringBitmap.bitmapOf(first, second));
        else {
            RoaringBitmap mergred = mergedTaxa.get(first);
            if (mergedTaxa.containsKey(second)) {
                mergred.or(mergedTaxa.remove(second));
            } else {
                mergred.add(second);
            }
        }


        // contract selected edge(s)
        TIntSet toRemove = new TIntHashSet();
        for (int i = 0; i < characters.length; i++) {
            if (mergeEdgeIfExistsAndCheckIsEmpty(characters[i], first, second)) {
                toRemove.add(i);
            }
        }

        // refresh characters abd cumulative weights
        refreshCharactersAndCumulativeWeights(toRemove);
    }

    private boolean mergeEdgeIfExistsAndCheckIsEmpty(final RoaringBitmap connectedTaxa, final int first, final int last) {
        if (connectedTaxa.contains(last)) {
            connectedTaxa.flip(last);
            if (!connectedTaxa.contains(first))
                connectedTaxa.flip(first);
        }

        return connectedTaxa.getCardinality() < 2;
    }

    private int drawCharacter(final Random random) {
        double max = cumulativeWeights[cumulativeWeights.length - 1];
        double r = max * random.nextDouble();
        return Arrays.binarySearch(cumulativeWeights, r);
    }

    private void refreshCharactersAndCumulativeWeights(TIntSet toRemove) {
        if (!toRemove.isEmpty()) {
            RoaringBitmap[] nuChars = new RoaringBitmap[characters.length - toRemove.size()];
            double[] nuWeights = new double[cumulativeWeights.length - toRemove.size()];

            int j = 0;
            double before = 0d;
            double beforeNu = 0d;

            for (int i = 0; i < cumulativeWeights.length; i++) {
                if (!toRemove.contains(i)) {
                    nuWeights[j] = beforeNu + (cumulativeWeights[i] - before);
                    beforeNu = nuWeights[j];
                    nuChars[j] = characters[i];
                    j++;
                }

                before = cumulativeWeights[i];
            }
            characters = nuChars;
            cumulativeWeights = nuWeights;
        }
    }

    @Override
    public CompressedKargerGraph clone() throws CloneNotSupportedException {
        super.clone();
        final TIntObjectMap<RoaringBitmap> nuMergedTaxa = new TIntObjectHashMap<>();
        mergedTaxa.forEachEntry((a, b) -> nuMergedTaxa.put(a, b.clone()) != null);

        return new CompressedKargerGraph(
                (RoaringBitmap[]) Arrays.stream(characters).map(RoaringBitmap::clone).toArray(),
                Arrays.copyOf(cumulativeWeights, cumulativeWeights.length),
                nuMergedTaxa);

    }
}
