package dewey

import java.nio.file.attribute.PosixFilePermission
import kotlin.IllegalStateException

fun posixPermissionsToString(permissions: Set<PosixFilePermission>): String {
    val permissionsOrdered = arrayOf(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_EXECUTE,

        PosixFilePermission.GROUP_READ,
        PosixFilePermission.GROUP_WRITE,
        PosixFilePermission.GROUP_EXECUTE,

        PosixFilePermission.OTHERS_READ,
        PosixFilePermission.OTHERS_WRITE,
        PosixFilePermission.OTHERS_EXECUTE,
    )
    val permissionChars = charArrayOf('r', 'w', 'x')
    val permissionStringBuilder = StringBuilder(9)

    for ((idx, permission) in permissionsOrdered.withIndex())
        permissionStringBuilder.insert(
            idx,
            if (permission in permissions) permissionChars[idx % 3] else '-'
        )

    return permissionStringBuilder.toString()
}

fun unreachable(): Nothing = throw IllegalStateException("Unreachable code path")