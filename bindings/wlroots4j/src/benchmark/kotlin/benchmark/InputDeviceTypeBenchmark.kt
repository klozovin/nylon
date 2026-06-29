package benchmark

import jextract.wlroots.wlr.WLR_INPUT_DEVICE_KEYBOARD
import jextract.wlroots.wlr.WLR_INPUT_DEVICE_SWITCH
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import wlroots.types.input.InputDeviceType


@State(Scope.Benchmark)
@Warmup(iterations = 4, time = 1000, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 4)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class InputDeviceTypeKeyboardBenchmark {

    val keyboardDeviceType = WLR_INPUT_DEVICE_KEYBOARD()


    @Benchmark
    fun lookupIterateOverValues(): InputDeviceType {
        return InputDeviceType.ofIterate(keyboardDeviceType)
    }


    @Benchmark
    fun lookupDirectlyInArray(): InputDeviceType {
        return InputDeviceType.of(keyboardDeviceType)
    }
}


@State(Scope.Benchmark)
@Warmup(iterations = 4, time = 1000, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 4)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class InputDeviceTypeSwitchBenchmark {

    val switchDeviceType = WLR_INPUT_DEVICE_SWITCH()


    @Benchmark
    fun lookupIterateOverValues(): InputDeviceType {
        return InputDeviceType.ofIterate(switchDeviceType)
    }


    @Benchmark
    fun lookupDirectlyInArray(): InputDeviceType {
        return InputDeviceType.of(switchDeviceType)
    }
}