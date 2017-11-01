package phylo.tree.algorithm.flipcut.flipCutGraph.undirectedConversion;

import mincut.cutGraphAPI.KargerSteinCutGraph;
import mincut.cutGraphAPI.bipartition.FlipCutCutFactory;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphCutter;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

public interface KargerGraphCreator {
    default KargerSteinCutGraph<FlipCutNodeSimpleWeight, FlipCutCutFactory> createGraph(final ChracterScoreModifier modder, final FlipCutGraphSimpleWeight source) {
        KargerSteinCutGraph<FlipCutNodeSimpleWeight, FlipCutCutFactory> cutGraph = new KargerSteinCutGraph<>(new FlipCutCutFactory());
        for (FlipCutNodeSimpleWeight character : source.characters) {
            double weight = modder.modifyCharacterScore(character);
            for (FlipCutNodeSimpleWeight e1 : character.edges) {
                for (FlipCutNodeSimpleWeight e2 : character.edges) {
                    if (e1 != e2) {
                        boolean guide = character.getEdgeWeight() == CutGraphCutter.getInfinity();
                        cutGraph.addEdge(e1, e2, weight, character, guide);
                    }
                }
            }
        }
        return cutGraph;
    }

    default KargerSteinCutGraph<FlipCutNodeSimpleWeight, FlipCutCutFactory> createGraph(final FlipCutGraphSimpleWeight source) {
        return createGraph(new ChracterScoreModifier(){},source);
    }
}
