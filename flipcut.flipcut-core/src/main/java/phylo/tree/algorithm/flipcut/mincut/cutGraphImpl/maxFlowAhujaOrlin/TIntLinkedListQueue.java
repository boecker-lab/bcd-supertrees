package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.maxFlowAhujaOrlin;

import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.queue.TIntQueue;

/**
 * Created by fleisch on 22.09.15.
 */
public class TIntLinkedListQueue extends TIntLinkedList implements TIntQueue {

    @Override
    public int element() {
        if (isEmpty())
            throw new NullPointerException("Queue is empty!!!");
        return poll();
    }

    @Override
    public boolean offer(int e) {
        return add(e);
    }

    @Override
    public int peek() {
        return get(0);
    }

    @Override
    public int poll() {
        return removeAt(0) ;
    }
}