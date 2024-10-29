package dewey

import kotlinx.benchmark.*
import org.openjdk.jmh.annotations.Fork
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributes
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readAttributes


@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 4)
@Measurement(iterations = 8, time = 1, timeUnit = TimeUnit.SECONDS)
open class ReadFilesAttributesBenchmark {
    var directoryList = Path.of("/usr/lib").listDirectoryEntries()

    @Benchmark
    fun serialReadAttributes(): List<PosixFileAttributes> {
        val attributesList = directoryList.stream().map {
            it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
        }.collect(Collectors.toList())
        return attributesList
    }

    @Benchmark
    fun parallelStreamReadAttributes(): List<PosixFileAttributes> {
        val attributesList = directoryList.parallelStream().map {
            it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
        }.collect(Collectors.toList())
        return attributesList
    }
}