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
    public static EnumSet<Edge> fromBitset(int bitSet) {
        assert bitSet >= 0;
        var edges = EnumSet.noneOf(Edge.class);
        for (var edge : values())
            if ((edge.value & bitSet) != 0)
                edges.add(edge);
        return edges;
    }
}