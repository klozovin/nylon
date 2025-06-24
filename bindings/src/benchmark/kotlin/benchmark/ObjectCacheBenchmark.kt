package benchmark


import kotlinx.benchmark.*


@State(Scope.Benchmark)
@Warmup(iterations = 4)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
open class ObjectCacheBenchmark {

    class DummyDataCarrier(val x: Long, val y: Int, val z: Float, val w: String)

    val dummyStore = HashMap<Int, DummyDataCarrier>().apply {
        put(100000, DummyDataCarrier(100000, 20, 30.30f, "dummy data"))
        put(200000, DummyDataCarrier(200000, 20, 30.30f, "dummy data 200000"))
        put(300000, DummyDataCarrier(300000, 20, 30.30f, "dummy data 300000"))
        put(400000, DummyDataCarrier(400000, 20, 30.30f, "dummy data 400000"))
        put(500000, DummyDataCarrier(500000, 20, 30.30f, "dummy data 500000"))
        put(600000, DummyDataCarrier(600000, 20, 30.30f, "dummy data 600000"))
        put(700000, DummyDataCarrier(700000, 20, 30.30f, "dummy data 700000"))
        put(800000, DummyDataCarrier(800000, 20, 30.30f, "dummy data 800000"))
        put(900000, DummyDataCarrier(900000, 20, 30.30f, "dummy data 900000"))
    }


    @Benchmark
    fun benchmarkDataCarrierCreate(): DummyDataCarrier {
        val dc = DummyDataCarrier(5000000, 20, 30.30f, "dummy data")
        return dc
    }


    @Benchmark
    fun benchmarkDataCacheLookup(): DummyDataCarrier {
        val dc = dummyStore.get(500000)!!
        return dc
    }
}