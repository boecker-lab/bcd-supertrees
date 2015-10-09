package flipcut.mincut.cutGraphImpl.kargerStein;

import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import epos.model.tree.io.Newick;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import flipcut.mincut.cutGraphAPI.bipartition.BasicCut;
import flipcut.mincut.cutGraphAPI.GoldbergTarjanCutGraph;

/**
 * Created by martin-laptop on 15.09.15.
 */
public class TestTharjan {

    public static void main(String[] args) {

        Newick newick = new Newick();
        //        File file = new File("/home/martin-laptop/trees/SMIDGenOG_Martin/500.75/smo.1.sourceTrees.tre");
        File file = new File("/home/fleisch/Work/data/simulated/SMIDGenOG_Martin/100.50/smo.1.sourceTrees.tre");
        Tree[] tree_arr = newick.getTreeFromFile(file);

        GoldbergTarjanCutGraph gold = new GoldbergTarjanCutGraph();
        HashMap<String, TreeNode> taxalist = new HashMap<String, TreeNode>();

        for (Tree tree : tree_arr) { //For every tree in the file
            for (TreeNode node : tree.getLeaves()) {
                if (!taxalist.containsKey(node.getLabel()))     //Add all leaves to the taxalist
                    taxalist.put(node.getLabel(), node);
            }
            for (TreeNode vert : tree.vertices()) {
                if (vert.isInnerNode()) { //TODO: t89 hack //For every node thats not a leaf/root
                    TreeNode out = vert;
                    TreeNode in = vert.cloneNode();
                    in.setLabel(out.toString() + "in");
                    long weight = 10;
                    if (!vert.equals(tree.getRoot())) {
                        weight = Long.parseLong(vert.getLabel());
                    } else {
//                            System.out.println("add root");
                    }
                    //                        gold.addNode(out);       //add outgoing node
                    //                        gold.addNode(in);       //add ingoing node
                    gold.addEdge(out, in, weight); //add edge with label weight
                    TreeNode[] leaves = vert.getLeaves();
                    for (TreeNode leave : leaves) {  //add edges with inf weight between nodes
                        TreeNode l = taxalist.get(leave.getLabel());
                        if (l != null) {
                            //                                gold.addNode(l);
                            gold.addEdge(in, l, 1000000);
                            gold.addEdge(l, out, 1000000);
                        } else {
                            System.out.println("strange!!!!!!!!");
                        }
                    }
                }
            }
        }
        TreeNode[] leavearr = new TreeNode[taxalist.size()];
        leavearr = new ArrayList<>(taxalist.values()).toArray(leavearr);
//        ArrayList ert = new ArrayList<>(taxalist.keySet());
//        Collections.sort(ert);

        System.out.println("Starting mitcut calculation...");
        for (int i = 0; i < leavearr.length - 1; i++) {
            for (int j = i + 1; j < leavearr.length; j++) {
                try {
                    gold.submitSTCutCalculation(leavearr[i], leavearr[j]);
                } catch (Exception e) {
                    System.out.println(leavearr[i] + "." + leavearr[j]);
                }
            }
        }
        try {
            long time = System.currentTimeMillis();
            BasicCut cut = gold.calculateMinCut();
            System.out.println("time" + (System.currentTimeMillis() - time) / 1000d);
            System.out.println("Golderg mincut value: " + cut.minCutValue);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
