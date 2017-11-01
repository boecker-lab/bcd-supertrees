package phylo.tree.algorithm.flipcut.flipCutGraph;

public enum CutGraphTypes {
    MAXFLOW_TARJAN_GOLDBERG(false),
    MAXFLOW_AHOJI_ORLIN(false),
    HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG(true),
    HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN(true);

    private final boolean bcd;

    public boolean isBCD() {
        return bcd;
    }

    public boolean isFlipCut() {
        return !isBCD();
    }

    CutGraphTypes(boolean bcd) {
        this.bcd = bcd;
    }
}

