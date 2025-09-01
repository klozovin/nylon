package benchmark

import jextract.wlroots.util.edges_h.*
import kotlinx.benchmark.*
import wayland.util.Edge


class EdgeSet(val value: Int) {
    fun containsTop(): Boolean {
        return value and WLR_EDGE_TOP() != 0
    }

    fun containsBottom(): Boolean {
        return value and WLR_EDGE_BOTTOM() != 0
    }

    fun containsLeft(): Boolean {
        return value and WLR_EDGE_LEFT() != 0
    }

    fun containsRight(): Boolean {
        return value and WLR_EDGE_RIGHT() != 0
    }
}


@State(Scope.Benchmark)
@Warmup(iterations = 4, time = 1000, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 4)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
open class EdgeSetBenchmark {

    val setTop = WLR_EDGE_TOP()
    val setLeftRight = WLR_EDGE_LEFT() or WLR_EDGE_RIGHT()

    @Benchmark
    fun enumSetTop(): Boolean {
        val set = Edge.fromBitset(setTop)
        val flag = set.contains(Edge.TOP)
        return flag
    }


    @Benchmark
    fun bitSetTop(): Boolean {
        val set = EdgeSet(setTop)
        val flag = set.containsTop()
        return flag
    }

    @Benchmark
    fun enumSetLeftRight(): Boolean {
        val set = Edge.fromBitset(setLeftRight)
        val flag = set.contains(Edge.LEFT) && set.contains(Edge.RIGHT)
        return flag
    }


    @Benchmark
    fun bitSetLeftRight(): Boolean {
        val set = EdgeSet(setLeftRight)
        val flag = set.containsLeft() && set.containsRight()
        return flag
    }
}


@State(Scope.Benchmark)
@Warmup(iterations = 4, time = 1000, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 4)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
open class OnlyAccessBenchmark {

    val enumSet = Edge.fromBitset(WLR_EDGE_LEFT() or WLR_EDGE_RIGHT())
    val bitSet = EdgeSet(WLR_EDGE_LEFT() or WLR_EDGE_RIGHT())

    @Benchmark
    fun benchmarkEnumSet(): Boolean {
        val left = enumSet.contains(Edge.LEFT)
        val right = enumSet.contains(Edge.RIGHT)
        val bottom = enumSet.contains(Edge.BOTTOM)
        return left && right && !bottom
    }

    @Benchmark
    fun benchmarkBitSet(): Boolean {
        val left = bitSet.containsLeft()
        val right = bitSet.containsRight()
        val bottom = bitSet.containsBottom()
        return left && right && !bottom
    }
}