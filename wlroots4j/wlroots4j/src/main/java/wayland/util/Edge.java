package wayland.util;

import org.jspecify.annotations.NullMarked;

import java.util.EnumSet;

import static jextract.wlroots.wlr.*;


/// `enum wlr_edges`
@NullMarked
public enum Edge {
    None(WLR_EDGE_NONE()),
    Top(WLR_EDGE_TOP()),
    Bottom(WLR_EDGE_BOTTOM()),
    Left(WLR_EDGE_LEFT()),
    Right(WLR_EDGE_RIGHT());

    public final int value;


    Edge(int value) {
        this.value = value;
    }


    /// Create an EnumSet of Edges from the C bitmask
    public static EnumSet<Edge> fromBitset(int bitset) {
        // TODO: What if WLR_EDGE_NONE gets passed in, and when does that happen?
        assert bitset >= 0;

        var edges = EnumSet.noneOf(Edge.class);

        if ((WLR_EDGE_NONE()   & bitset) != 0) edges.add(None);
        if ((WLR_EDGE_TOP()    & bitset) != 0) edges.add(Top);
        if ((WLR_EDGE_BOTTOM() & bitset) != 0) edges.add(Bottom);
        if ((WLR_EDGE_LEFT()   & bitset) != 0) edges.add(Left);
        if ((WLR_EDGE_RIGHT()  & bitset) != 0) edges.add(Right);

        return edges;
    }
}