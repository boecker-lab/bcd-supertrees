package phylo.tree.algorithm.flipcut;

import org.junit.Test;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDSourceGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedGraphFactory;
import phylo.tree.algorithm.flipcut.costComputer.FlipCutWeights;
import phylo.tree.algorithm.flipcut.costComputer.SimpleCosts;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphSimpleWeight;
import phylo.tree.io.Newick;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompressedBCDSimpleGraphTest {

    @Test
    public void testLargeGraphCreation(){
        List<Tree> largeInput = new ArrayList(Arrays.asList(Newick.getTreeFromFile(new File("/home/fleisch/work/data/biological/ommWorkspace/omm/Source_Trees/RaxML/omm.source.Trees.tre"))));
        long time = System.currentTimeMillis();
        CompressedBCDSourceGraph test = CompressedGraphFactory.createSourceGraph(SimpleCosts.newCostComputer(
                TreeUtils.cloneTrees(TreeUtils.cloneTrees(largeInput)),
                FlipCutWeights.Weights.UNIT_COST), 0, true);

        System.out.println("Created Compressed Graph in: " + (double) (System.currentTimeMillis() - time) / 1000d);

        time = System.currentTimeMillis();
        FlipCutGraphSimpleWeight g1 = new FlipCutGraphSimpleWeight(SimpleCosts.newCostComputer(
                TreeUtils.cloneTrees(TreeUtils.cloneTrees(largeInput)),
                FlipCutWeights.Weights.UNIT_COST));
        System.out.println("Created Standard Graph in: " + (double) (System.currentTimeMillis() - time) / 1000d);
        assert time != 0;
    }
}
