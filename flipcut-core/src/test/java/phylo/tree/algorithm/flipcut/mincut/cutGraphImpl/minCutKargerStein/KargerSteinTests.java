package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerStein;

import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.BasicCut;
import org.junit.Test;
import phylo.tree.io.Newick;
import phylo.tree.model.tree.Tree;
import phylo.tree.model.tree.TreeNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

/**
 * Created by martin-laptop on 15.09.15.
 */
public class KargerSteinTests {

    @Test
    public void testTarjan() {

        Newick newick = new Newick();
        File file = new File(getClass().getResource("/phylo/tree/algorithm/flipcut/SMIDGenOGTestInstances/100.50/smo.1.sourceTrees.tre").getFile());
        Tree[] tree_arr = newick.getTreeFromFile(file);

        GoldbergTarjanCutGraph gold = new GoldbergTarjanCutGraph();
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
                    } else {
//                            System.out.println("add root");
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


    @Test
    public void kargerTest() {
        KargerSteinMinCutAlgorithm test;
        //parse trees

        Newick newick = new Newick();
        File file = new File(getClass().getResource("/phylo/tree/algorithm/flipcut/SMIDGenOGTestInstances/100.50/smo.1.sourceTrees.tre").getFile());
        Tree[] tree_arr = newick.getTreeFromFile(file);
        ArrayList<Edge> edge_list = new ArrayList<Edge>();


        int color_2=0;
        HashSet<String> globallist= new HashSet<>();
        for(int i=0; i<tree_arr.length;i++) {

            for (TreeNode vert : tree_arr[i].vertices()) {


                if (vert.isInnerNode() /*&& tree_arr[i].getRoot() != vert*/) {//lets test this with roots
                    //System.out.println(vert.getLabel());

                    TreeNode[] leaves = vert.getLeaves();


                    ArrayList<Integer> leavelist = new ArrayList<Integer>();
                    for (TreeNode leave : leaves) {
                        leavelist.add(Integer.parseInt(leave.getLabel().substring(1)));
                        globallist.add(leave.getLabel());
                    }

                    Collections.sort(leavelist);

                    for (int x = 0; x < leavelist.size() - 1; x++) {
                        for (int j = x + 1; j < leavelist.size(); j++) {
                            int weight = 10; //some weight for the root. doesnt matter for testing
                            if (!tree_arr[i].getRoot().equals(vert)) {
                                weight =  Integer.parseInt(vert.getLabel());
                            }
                            Edge new_edge = new Edge(leavelist.get(x), leavelist.get(j),weight , color_2);
                            // System.out.println(new_edge.outgoing + ":" + new_edge.ingoing + ":" + new_edge.weight + ":" + new_edge.color);
                            edge_list.add(new_edge);
                        }
                    }

                }

                color_2 += 1;
            }
        }
        ArrayList<String> globallist2 = new ArrayList(globallist);
        Collections.sort(globallist2);

        Edge[] edgearr = new Edge[edge_list.size()];
        edgearr=edge_list.toArray(edgearr);









        // Edge[] edgearr = {new Edge(1,4,4,1), new Edge(1,3,4,2),new Edge(2, 4, 4, 3), new Edge(2, 3, 4, 1)};

        test = new KargerSteinMinCutAlgorithm(edgearr);

//creates random connected Graph with n vertices and n-1 edges

      /*  int nr_vertices = 1000;
        Random ran = new Random();
        Edge[] edgeset = new Edge[nr_vertices - 1];
        HashMap<Integer, LinkedList<Integer>> vertex_need_work = new HashMap<>();
        LinkedList<Integer> lista = new LinkedList<Integer>();
        LinkedList<Integer> listb = new LinkedList<Integer>();
        vertex_need_work.put(1, lista);
        vertex_need_work.put(0, listb);
        for (int i = 0; i < nr_vertices; i++) {
            vertex_need_work.get(1).add(i);

        }

        int ran_a = ran.nextInt(vertex_need_work.get(1).size());
        int start_a = vertex_need_work.get(1).get(ran_a);
        vertex_need_work.get(1).remove(ran_a);
        vertex_need_work.get(0).add(start_a);

        int ran_b = ran.nextInt(vertex_need_work.get(1).size());
        int start_b = vertex_need_work.get(1).get(ran_b);
        vertex_need_work.get(1).remove(ran_b);
        vertex_need_work.get(0).add(start_b);

        edgeset[0] = new Edge(Math.min(start_a, start_b), Math.max(start_a, start_b), ran.nextInt(100) + 1, 0);


        for (int i = 1; i < nr_vertices - 1; i++) {
           // System.out.println(ran.nextInt(vertex_need_work.get(1).size()));

            int chosen_new_random = ran.nextInt(vertex_need_work.get(1).size());
            int chosen_new_vertex = vertex_need_work.get(1).get(chosen_new_random);

            vertex_need_work.get(1).remove(chosen_new_random);

            int chosen_old_random = ran.nextInt(vertex_need_work.get(0).size());
            int chosen_old = vertex_need_work.get(0).get(chosen_old_random);
            vertex_need_work.get(0).add(chosen_new_vertex);
            int weight = ran.nextInt(100) + 1;
            int color = ran.nextInt(14000);
            edgeset[i] = new Edge(Math.min(chosen_old, chosen_new_vertex), Math.max(chosen_new_vertex, chosen_old), weight, i);


        }

        test = new KargerStein(edgeset, nr_vertices);*/


    }
}
