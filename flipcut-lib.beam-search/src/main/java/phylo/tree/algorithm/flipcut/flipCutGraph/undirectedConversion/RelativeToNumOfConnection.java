package phylo.tree.algorithm.flipcut.flipCutGraph.undirectedConversion;

import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

public class RelativeToNumOfConnection implements ChracterScoreModifier {
    @Override
    public double modifyCharacterScore(FlipCutNodeSimpleWeight chracter) {
        return (double) chracter.getEdgeWeight() / (double) chracter.edges.size();
    }
}
