package phylo.tree.algorithm.flipcut.flipCutGraph.undirectedConversion;

import mincut.cutGraphAPI.KargerSteinCutGraph;
import mincut.cutGraphAPI.bipartition.FlipCutCutFactory;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

public class StaticKargerGraphCreator implements KargerGraphCreator{
    @Override
    public KargerSteinCutGraph<FlipCutNodeSimpleWeight, FlipCutCutFactory> createGraph(ChracterScoreModifier modder, FlipCutGraphSimpleWeight source) {
        KargerSteinCutGraph<FlipCutNodeSimpleWeight, FlipCutCutFactory> cutGraph = new KargerSteinCutGraph<>(new FlipCutCutFactory());
        for (FlipCutNodeSimpleWeight character : source.characters) {
            final double weight = modder.modifyCharacterScore(character);
            for (FlipCutNodeSimpleWeight e1 : character.edges) {
                for (FlipCutNodeSimpleWeight e2 : character.edges) {
                    if (e1 != e2) {
                        boolean guide = character.getEdgeWeight() == CutGraphCutter.getInfinity();
                        cutGraph.addEdge(e1, e2, weight, null, guide);
                    }
                }
            }
        }
        return cutGraph;
    }
}
