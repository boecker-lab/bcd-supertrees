package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.impl.Constants;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CompressedGraphFactory {

    public static CompressedBCDSourceGraph createSourceGraph(CostComputer costComputer, double bootstrapTheshold) {

        Tree scaffold = costComputer.getScaffoldTree();
        List<Tree> trees = costComputer.getTrees();


        LinkedList<RoaringBitmap> oneEdges = new LinkedList<>();
        LinkedList<RoaringBitmap> zeroEdges = new LinkedList<>();
        TObjectLongMap<Character> charsToWeight = new TObjectLongHashMap<>();


        TMap<RoaringBitmap, RoaringBitmap> chars = new THashMap<>();

        int leafIndex = 0;
        AtomicInteger characterIndex = new AtomicInteger(0);
        TIntObjectMap<RoaringBitmap> scaffoldMapping = new TIntObjectHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);

        TObjectIntMap<String> leafs = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);


        RoaringBitmap activeScaffoldCharacters = null;
        if (scaffold != null) {
            TreeNode scaffoldRoot = scaffold.getRoot();

            for (TreeNode scaffoldNode : scaffold.vertices()) {
                if (scaffoldNode.isLeaf()) {
                    leafs.put(scaffoldNode.getLabel(), leafIndex++);
                }
            }

            RoaringBitmap treeTaxa = new RoaringBitmap();
            treeTaxa.add(0L, leafIndex);

            activeScaffoldCharacters = addChildren(scaffoldRoot.getChildren(), characterIndex, costComputer,
                    treeTaxa, leafs, charsToWeight, oneEdges, zeroEdges, scaffoldMapping);
        }


        //do character stuff
        for (Tree tree : trees) {
            if (tree != scaffold) {
                TreeNode root = tree.getRoot();
                RoaringBitmap treeTaxa = new RoaringBitmap();
                List<TreeNode> inner = new LinkedList<>();

                for (TreeNode node : root.depthFirstIterator()) {
                    if (node.isLeaf()) {
                        if (scaffold == null && leafs.putIfAbsent(node.getLabel(), leafIndex) == leafs.getNoEntryValue())
                            leafIndex++;
                        treeTaxa.add(leafs.get(node.getLabel()));
                    } else if (!node.equals(root)) {
                        inner.add(node);
                    }
                }

                for (TreeNode node : inner) {
                    //collect no edges and zero edges
                    RoaringBitmap ones = new RoaringBitmap();
                    for (TreeNode leaf : node.getLeaves()) {
                        ones.add(leafs.get(leaf));
                    }

                    //remove duplicate objects
                    RoaringBitmap o = chars.putIfAbsent(ones, ones);
                    ones = o == null ? ones : o;

                    //remove duplicate objects
                    RoaringBitmap zeroes = RoaringBitmap.xor(ones, treeTaxa);
                    RoaringBitmap z = chars.putIfAbsent(zeroes, zeroes);
                    zeroes = z == null ? zeroes : z;

                    //check for identical characters
                    Character c = new Character(ones, zeroes);
                    long weight = costComputer.getEdgeWeight(node);

                    assert weight != CutGraphCutter.getInfinity();

                    long before = charsToWeight.get(c);

                    //check if no scaffold ist involved && sum weight or add new
                    if (before != CutGraphCutter.getInfinity() && charsToWeight.adjustOrPutValue(c, weight, weight) == weight) {
                        oneEdges.add(ones);
                        zeroEdges.add(zeroes);
                        characterIndex.getAndIncrement();
                    }
                }
            }
        }

        //just to free the memory
        chars = null;

        assert characterIndex.get() == oneEdges.size() && characterIndex.get() == zeroEdges.size();
        assert leafs.size() == leafIndex;

        final long[] weights = new long[zeroEdges.size()];
        final RoaringBitmap[] ONES = new RoaringBitmap[zeroEdges.size()];
        final RoaringBitmap[] ZEROS = new RoaringBitmap[zeroEdges.size()];
        for (int i = 0; i < weights.length; i++) {
            Character c = new Character(oneEdges.poll(), zeroEdges.poll());
            weights[i] = charsToWeight.get(c);
            ONES[i] = c.ones;
            ZEROS[i] = c.zeroes;
        }

        final String[] taxa = new String[leafs.size()];
        leafs.forEachEntry((a, b) -> {
            taxa[b] = a;
            return true;
        });

        return new CompressedBCDSourceGraph(taxa, ONES, ZEROS, activeScaffoldCharacters, weights, scaffoldMapping);

    }

    private static RoaringBitmap addChildren(final List<TreeNode> children, final AtomicInteger charIndex,
                                             final CostComputer costComputer,
                                             final RoaringBitmap treeTaxa,
                                             TObjectIntMap<String> leafs,
                                             final TObjectLongMap<Character> charsToWeight,
                                             final LinkedList<RoaringBitmap> oneEdges,
                                             final LinkedList<RoaringBitmap> zeroEdges,
                                             TIntObjectMap<RoaringBitmap> scaffoldMapping) {

        RoaringBitmap childrenI = new RoaringBitmap();

        for (TreeNode scaffoldNode : children) {
            if (scaffoldNode.isInnerNode()) {
                //collect ones
                RoaringBitmap ones = new RoaringBitmap();
                for (TreeNode leaf : scaffoldNode.getLeaves()) {
                    ones.add(leafs.get(leaf));
                }

                //create zeroes
                RoaringBitmap zeroes = RoaringBitmap.xor(ones, treeTaxa);

                //add scaffold char and weight
                Character c = new Character(ones, zeroes);
                long weight = costComputer.getEdgeWeight(scaffoldNode);

                assert weight == CutGraphCutter.getInfinity();

                charsToWeight.put(c, weight);
                oneEdges.add(ones);
                zeroEdges.add(zeroes);
                childrenI.add(charIndex.get());
                scaffoldMapping.put(charIndex.getAndIncrement(),
                        addChildren(scaffoldNode.getChildren(), charIndex, costComputer, treeTaxa, leafs, charsToWeight, oneEdges, zeroEdges, scaffoldMapping));
            }
        }

        return childrenI;


    }


    public static class Character {
        final RoaringBitmap ones;
        final RoaringBitmap zeroes;

        public Character(RoaringBitmap ones, RoaringBitmap zeroes) {
            this.ones = ones;
            this.zeroes = zeroes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Character)) return false;

            Character character = (Character) o;

            if (!ones.equals(character.ones)) return false;
            return zeroes.equals(character.zeroes);
        }

        @Override
        public int hashCode() {
            int result = ones.hashCode();
            result = 31 * result + zeroes.hashCode();
            return result;
        }
    }

}
