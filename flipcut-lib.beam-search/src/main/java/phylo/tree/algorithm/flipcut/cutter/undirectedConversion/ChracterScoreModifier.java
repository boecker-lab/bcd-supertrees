package phylo.tree.algorithm.flipcut.cutter.undirectedConversion;

import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

public interface ChracterScoreModifier {
    default double modifyCharacterScore(FlipCutNodeSimpleWeight character) {
        return character.getEdgeWeight();
    }
}
