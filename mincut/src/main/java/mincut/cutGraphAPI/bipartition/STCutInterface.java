package mincut.cutGraphAPI.bipartition;

public interface STCutInterface<N, C> extends Cut<C> {
    N source();

    N target();
}
