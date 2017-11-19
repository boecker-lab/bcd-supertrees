package mincut.cutGraphImpl.maxFlowGoldbergTarjan;/*
 * Epos Phylogeny Framework
 * Copyright (C) 2009.  University of Jena
 *
 * This file is part of Epos.
 *
 * Epos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Epos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Epos.  If not, see <http://www.gnu.org/licenses/>
 */

import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.bipartition.STCut;
import org.junit.Test;
import mincut.cutGraphImpl.minCutKargerStein.GraphUtils;
import phylo.tree.io.Newick;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Thasso Griebel (thasso.griebel@gmail.com)
 */
public class GoldbergTarjanCutGraphTest {

    @Test
    public void testExample(){
        GoldbergTarjanCutGraph hp = new GoldbergTarjanCutGraph();
        hp.addNode(1);
        hp.addNode(2);
        hp.addNode(3);
        hp.addNode(4);
        hp.addNode(5);
        hp.addNode(6);
        hp.addNode(7);
        hp.addNode(8);


        hp.addEdge(1, 2, 5);
        hp.addEdge(2, 3, 5);
        hp.addEdge(3, 4, 5);
        hp.addEdge(3, 5, 2);
        hp.addEdge(4, 2, 5);
        hp.addEdge(4, 7, 2);
        hp.addEdge(5, 6, 5);
        hp.addEdge(6, 6, 5);
        hp.addEdge(6, 8, 4);
        hp.addEdge(7, 5, 5);
        hp.addEdge(7, 8, 1);

        STCut cut = hp.calculateMinSTCut(1, 8);
        System.out.println(cut.minCutValue());
        System.out.println(cut.getCutSet());


        assertEquals(4, cut.minCutValue());
        assertEquals(4, cut.getCutSet().size());
        assertTrue(cut.getCutSet().contains(5));
        assertTrue(cut.getCutSet().contains(6));
        assertTrue(cut.getCutSet().contains(7));
        assertTrue(cut.getCutSet().contains(8));

        /*
        p max 8 11
        n 1 s
        n 8 t
        a 1 2 5
        a 2 3 5
        a 3 4 5
        a 3 5 2
        a 4 2 5
        a 4 7 2
        a 5 6 5
        a 6 7 5
        a 6 8 4
        a 7 5 5
        a 7 8 1
        */

    }

    @Test
    public void KargerExample()  {

        String testFile = getClass().getResource("/kargerAdj.txt").getFile();
        int[][] arr = GraphUtils.getArray(testFile);
        long time =  System.currentTimeMillis();
        GoldbergTarjanCutGraph gold = new GoldbergTarjanCutGraph();
        for (int i = 0; i < arr.length; i++) {
            Integer s = i;
            int[] ints = arr[i];
            for (int j = 0; j < ints.length; j++) {
                Integer t = ints[j];
                gold.addEdge(s,t,1);
                gold.addEdge(t,s,1);
            }
        }
        for (int i = 0; i < arr.length-1; i++) {
            for (int j = i+1; j < arr.length; j++) {
                gold.submitSTCutCalculation(i,j);
            }
        }

        try {
            STCut cut = gold.calculateMinCut();
            System.out.println("time" +  (System.currentTimeMillis()-time)/1000d);
            System.out.println("Golderg mincut value: " + cut.minCutValue());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void multiThreadingTest(){
        Newick newick = new Newick();
        File file = new File(getClass().getResource("/sm.9.sourceTrees_OptSCM-Rooting.tre").getFile());
        Tree[] tree_arr = newick.getTreeFromFile(file);
        int CORES_AVAILABLE =  Runtime.getRuntime().availableProcessors();
        for (int t = 1;t <= CORES_AVAILABLE; t++ ) {

            ExecutorService s =  Executors.newFixedThreadPool(t);
            GoldbergTarjanCutGraph gold = new GoldbergTarjanCutGraph();
            gold.setThreads(t);
            gold.setExecutorService(s);

            HashMap<String, TreeNode> taxalist = new HashMap<String, TreeNode>();

            for (Tree tree : tree_arr) { //For every tree in the file
                for (TreeNode node : tree.getLeaves()) {
                    if (!taxalist.containsKey(node.getLabel()))     //Add all leaves to the taxalist
                        taxalist.put(node.getLabel(), node);
                }
                for (TreeNode vert : tree.vertices()) {
                    if (vert.isInnerNode()) {
                        TreeNode out = vert;
                        TreeNode in = vert.cloneNode();
                        in.setLabel(out.toString() + "in");
                        long weight = 10;
                        if (!vert.equals(tree.getRoot())) {
                            weight = Long.parseLong(vert.getLabel());
                        }
                        gold.addEdge(out, in, weight); //add edge with label weight
                        TreeNode[] leaves = vert.getLeaves();
                        for (TreeNode leave : leaves) {  //add edges with inf weight between nodes
                            TreeNode l = taxalist.get(leave.getLabel());
                            if (l != null) {
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
                long time =  System.currentTimeMillis();
                STCut cut = gold.calculateMinCut();
                System.out.println("time" +  (System.currentTimeMillis()-time)/1000d + " with " + t + " threads");
                System.out.println("Golderg mincut value: " + cut.minCutValue());
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            s.shutdownNow();
        }
    }

}
