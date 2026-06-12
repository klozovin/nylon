package benchmark

import jextract.wlroots.wlr.*
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import wlroots.types.input.Keyboard
import wlroots.types.input.KeyboardModifier
import wlroots.types.input.KeyboardModifier.*
import java.util.EnumSet


val bitset = WLR_MODIFIER_CTRL() or WLR_MODIFIER_ALT() or WLR_MODIFIER_SHIFT()
val modifiersBitSet = Keyboard.Modifiers(bitset)
val modifiersEnumSet = KeyboardModifier.fromBitset(bitset)


@State(Scope.Benchmark)
@Warmup(iterations = 4)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class KeyboardModifierBenchmark_BitSet {

    @Benchmark
    fun individualLookup(): Boolean {
        val ctrl = modifiersBitSet.containsCtrl()
        val alt = modifiersBitSet.containsAlt()
        val shift = modifiersBitSet.containsShift()
        return ctrl && alt && shift
    }


    @Benchmark
    fun combinedLookup(): Boolean {
        return modifiersBitSet.containsCtrlAltShift()
    }
}



@State(Scope.Benchmark)
@Warmup(iterations = 4)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class KeyboardModifierBenchmark_EnumSet{

    @Benchmark
    fun individualLookup(): Boolean {
        val ctrl = modifiersEnumSet.contains(Control)
        val alt = modifiersEnumSet.contains(Alt)
        val shift = modifiersEnumSet.contains(Shift)
        return ctrl && alt && shift
    }


    @Benchmark
    fun individualLookup_contains(): Boolean {
        val ctrl = Control.containedIn(bitset)
        val alt = Alt.containedIn(bitset)
        val shift = Shift.containedIn(bitset)
        return ctrl && alt && shift
    }


    @Benchmark
    fun combinedLookup(): Boolean {
        return modifiersEnumSet.containsAll(EnumSet.of(Control, Alt, Shift))
    }


    @Benchmark
    fun parse_individualLookup(): Boolean {
        val modifiers = KeyboardModifier.fromBitset(bitset)
        val ctrl = modifiers.contains(Control)
        val shift= modifiers.contains(Shift)
        val alt= modifiers.contains(Alt)
        return ctrl && alt && shift
    }


    @Benchmark
    fun parse_combinedLookup(): Boolean {
        val modifiers = KeyboardModifier.fromBitset(bitset)
        val contains = modifiers.containsAll(EnumSet.of(Control, Alt, Shift))
        return contains
    }
}