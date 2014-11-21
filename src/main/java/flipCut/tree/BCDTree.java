package flipCut.tree;

import java.util.ArrayList;

/**
 * Created by fleisch on 06.11.14.
 */
public class BCDTree extends ArrayList<BCDTreeNode> {
    public BCDTreeNode getRoot(){
        return get(0);
    }
}
