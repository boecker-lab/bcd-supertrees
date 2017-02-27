package phylo.tree.algorithm.flipcut;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 26.02.17.
 */

import phylo.tree.algorithm.flipcut.cli.BCDCLI;
import phylo.tree.algorithm.flipcut.clo.BCDBeamCLI;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class BCDSupertreesBeam extends BCDSupertrees {
    public static void main(String[] args){
        CLI = new BCDBeamCLI(BCDCLI.DEFAULT_PROPERTIES_FILE);
        run(args);
    }
}
