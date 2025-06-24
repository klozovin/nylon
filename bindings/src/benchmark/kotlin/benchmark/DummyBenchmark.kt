package benchmark

import jextract.wayland.server.server_h
import kotlinx.benchmark.*
import wayland.KeyboardKeyState
import wayland.PointerAxisSource


//@State(Scope.Benchmark)
//@Warmup(iterations = 4)
//@BenchmarkMode(Mode.Throughput)
//@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
open class DummyBenchmark {

//    @Benchmark
    fun benchmarkPointerAxis(): Boolean {
        val x = PointerAxisSource.of(server_h.WL_POINTER_AXIS_SOURCE_CONTINUOUS())
        val flag = x == PointerAxisSource.CONTINUOUS
        return flag
    }

//    @Benchmark
//    fun benchmarkPointerAxisIfElse(): Boolean {
//        val x = PointerAxisSource.ofIfElse(server_h.WL_POINTER_AXIS_SOURCE_CONTINUOUS())
//        val flag = x == PointerAxisSource.CONTINOUS
//        return flag
//    }
}