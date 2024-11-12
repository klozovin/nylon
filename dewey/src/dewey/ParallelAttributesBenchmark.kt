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
    val path = Path.of("/usr/lib")
    val directoryEntries = path.listDirectoryEntries()

    //    @Benchmark
//    fun serialReadAttributes(): List<PosixFileAttributes> {
//        val attributesList = directoryList.stream().map {
//            it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
//        }.collect(Collectors.toList())
//        return attributesList
//    }
//
//    @Benchmark
    fun parallelStreamReadAttributes(): List<PosixFileAttributes> {
        val attributesList = directoryEntries.parallelStream().map {
            it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
        }.collect(Collectors.toList())
        return attributesList
    }

    //    @Benchmark
//    fun entriesInPathNavigator(): List<PathNavigator.Entry> {
//        val attributesList = directoryList.parallelStream().map {
//            it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
//        }.toList()
//
//        val pairs = (directoryList zip attributesList).map(
//            PathNavigator::Entry
//        )
//        return pairs
//    }

//    @Benchmark
    fun listDirectoryEntiresAndGetAttributes(): List<Pair<Path, PosixFileAttributes>> {
        val entries = path.listDirectoryEntries()
        val attributes = entries.parallelStream().map {
            it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
        }.toList()
        val pairs = (entries zip attributes)
        return pairs
    }

    @Benchmark
    fun sortDirectoryInPlace(): List<PosixFileAttributes> {
        val attributes = directoryEntries
            .parallelStream()
            .map { it.readAttributes<PosixFileAttributes>() }
            .collect(Collectors.toList())
        attributes.sortBy { it.isDirectory }
        return attributes
    }

    @Benchmark
    fun sortDirectoryInStream(): List<PosixFileAttributes> {
        val attributes = directoryEntries
            .parallelStream()
            .map { it.readAttributes<PosixFileAttributes>() }
            .sorted { x, y ->
                when {
                    x.isDirectory && y.isDirectory -> 0
                    x.isDirectory && !y.isDirectory -> -1
                    !x.isDirectory && y.isDirectory -> 1
                    !x.isDirectory && !y.isDirectory -> 0
                    else -> error("Unreachable")
                }
            }
            .collect(Collectors.toList())
        return attributes
    }

    @Benchmark
    fun sortDirectoryNewList(): List<PosixFileAttributes> {
        val attributes = directoryEntries
            .parallelStream()
            .map { it.readAttributes<PosixFileAttributes>() }
            .toList()
            .sortedBy { it.isDirectory }
        return attributes
    }
}