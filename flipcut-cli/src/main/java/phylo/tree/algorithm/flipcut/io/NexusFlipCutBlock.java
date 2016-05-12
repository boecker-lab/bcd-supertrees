package phylo.tree.algorithm.flipcut.io;

import org.biojavax.bio.phylo.io.nexus.NexusBlock;
import org.biojavax.bio.phylo.io.nexus.NexusFileFormat;
import phylo.tree.io.Newick;
import phylo.tree.model.Tree;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Florian on 22.04.2014.
 */
public abstract class NexusFlipCutBlock extends NexusBlock.Abstract {

    public static final String FlipCut_BLOCK = "FlipCut";
    /**
     * Quiet run
     */
    private static final String QUIET_FlipCut = "quit warntsave=no;";

    /**
     * Create a new paup block
     */
    public NexusFlipCutBlock() {
        super(FlipCut_BLOCK);
    }


    protected void writeBlockContents(Writer writer) throws IOException {
        List<String> cmds = createCommands();
        if (cmds != null) {
            for (String cmd : cmds) {

                writer.write(cmd);
                writer.write(NexusFileFormat.NEW_LINE);
            }
        }
        writer.write(NexusFileFormat.NEW_LINE);
        writer.write(QUIET_FlipCut);
        writer.write(NexusFileFormat.NEW_LINE);
    }

    protected abstract List<String> createCommands();

    public static class Builder{

        private static final String NEWLINE = NexusFileFormat.NEW_LINE;
        public static enum Method {HYPERGRAPH_TARJAN, MAXFLOW_TARJAN}
        public static enum Weights{UNIT_COSTS, EDGES_LEVELS, BOOTSTRAP_VALUE, BOOTSTRAP_LEVELS}

        private StringBuffer commands;
        Newick newick = new Newick();

        public Builder(){
            this.commands=new StringBuffer();
        }

        public NexusFlipCutBlock get(){
            return new NexusFlipCutBlock(){
                protected List<String> createCommands() {
                    return Arrays.asList(commands.toString());
                }
            };
        }

        public Builder setMethod(Method method){
            commands.append("setMethod=").append(method).append(";").append(NEWLINE);
            return this;

        }

        public Builder setWeights(Weights weights){
            commands.append("setWeights=").append(weights).append(";").append(NEWLINE);
            return this;

        }

        public Builder setTree(File file){
            Tree[] treeArray = newick.getTreeFromFile(file);
            for(int i=0; i<treeArray.length;i++)
            commands.append("setTree").append(" file=").append(newick.getStringFromTree(newick.getTreeFromFile(file)[i])).append(NEWLINE);
            return this;


        }

        public Builder setBootThresh(int tresh){
            commands.append("setBootstrapThreshold=").append(tresh).append(";").append(NEWLINE);
            return this;
        }



    }

    public static void main(String[] args) throws IOException{  //Just for testing

        final Builder test2 = new Builder();
        test2.setMethod(Builder.Method.HYPERGRAPH_TARJAN);
        test2.setWeights(Builder.Weights.BOOTSTRAP_LEVELS);
        test2.setBootThresh(5);
        test2.setTree(new File("/home/martin-laptop/Uni/m2"));
        test2.setTree(new File("/home/martin-laptop/Uni/m3"));


         NexusFlipCutBlock test = new NexusFlipCutBlock() {
            @Override
            protected List<String> createCommands() {
               return Arrays.asList(test2.commands.toString());
            }
        };

        File file = new File("/home/martin-laptop/Uni/m4");
        PrintWriter writer = new PrintWriter(new FileWriter(file));


        test.writeBlockContents(writer);
       writer.flush();

    }
}
