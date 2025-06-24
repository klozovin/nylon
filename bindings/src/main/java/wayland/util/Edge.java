package wayland.util;

import java.util.EnumSet;

import static jextract.wlroots.util.edges_h.*;

/// `enum wlr_edges`
public enum Edge {
    NONE(WLR_EDGE_NONE()),
    TOP(WLR_EDGE_TOP()),
    BOTTOM(WLR_EDGE_BOTTOM()),
    LEFT(WLR_EDGE_LEFT()),
    RIGHT(WLR_EDGE_RIGHT());

    public final int value;


    Edge(int value) {
        this.value = value;
    }


    /// Create an EnumSet of Edges from the C bitmask
    public static EnumSet<Edge> fromBitset(int bitset) {
        // TODO: What if WLR_EDGE_NONE gets passed in, and when does that happen?
        assert bitset >= 0;

        var edges = EnumSet.noneOf(Edge.class);

        if ((WLR_EDGE_NONE()   & bitset) != 0) edges.add(NONE);
        if ((WLR_EDGE_TOP()    & bitset) != 0) edges.add(TOP);
        if ((WLR_EDGE_BOTTOM() & bitset) != 0) edges.add(BOTTOM);
        if ((WLR_EDGE_LEFT()   & bitset) != 0) edges.add(LEFT);
        if ((WLR_EDGE_RIGHT()  & bitset) != 0) edges.add(RIGHT);

        return edges;
    }
}