#!/bin/sh

lib="wlroots-0.18"
libDir="/usr/include/wlroots-0.18"
output="src/generated/java"


jextract --library "drm" --output $output \
    --target-package jextract.drm         \
    "/usr/include/libdrm/drm_fourcc.h"


#<editor-fold desc="xkbcommon">
jextract --library "xkbcommon" --output $output \
    --target-package jextract.xkbcommon               \
    "/usr/include/xkbcommon/xkbcommon.h"
#</editor-fold>

#<editor-fold desc="wayland-server">
jextract --library "wayland-server" --output $output  \
    --target-package jextract.wayland.server          \
    --header-class-name "server_core_h"                    \
    "/usr/include/wayland-server-core.h"              \

jextract --library "wayland-server" --output $output  \
    --target-package jextract.wayland.util            \
    --header-class-name "util_h"                      \
    "/usr/include/wayland-util.h"

jextract --library "wayland-server" --output $output  \
    --target-package jextract.wayland                 \
    --header-class-name "version_h"                      \
    "/usr/include/wayland-version.h"
#</editor-fold> \

#<editor-fold desc="wlroots">
jextract --library $lib --output $output    \
    --target-package jextract.wlroots.render\
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    --include-dir "/usr/include/pixman-1/"  \
    "$libDir/wlr/render/allocator.h"

jextract --library $lib --output $output    \
    --target-package jextract.wlroots.render\
    --include-dir "/usr/include/pixman-1/"  \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    "$libDir/wlr/render/pass.h"

jextract --library $lib --output $output    \
    --target-package jextract.wlroots.render\
    --include-dir "/usr/include/pixman-1/"  \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    "$libDir/wlr/render/wlr_renderer.h"

# IMPORTANT: Only jextract files in wlr/interfaces/, don't also jextract their corresponding
#            /wlr/types/ file because jextract will overwrite already extracted code.

# wlr/interfaces/
jextract --library $lib --output $output    \
    --target-package jextract.wlroots.types \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir "/usr/include/pixman-1/"  \
    --include-dir $libDir                   \
    "$libDir/wlr/interfaces/wlr_buffer.h"


jextract --library $lib --output $output    \
    --target-package jextract.wlroots.types \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    "$libDir/wlr/types/wlr_input_device.h"

jextract --library $lib --output $output    \
    --target-package jextract.wlroots.types \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    "$libDir/wlr/types/wlr_keyboard.h"

jextract --library $lib --output $output    \
    --target-package jextract.wlroots.types \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    --include-dir "/usr/include/pixman-1/"  \
    "$libDir/wlr/types/wlr_output.h"

jextract --library $lib --output $output    \
    --target-package jextract.wlroots.types \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    --include-dir "/usr/include/pixman-1/"  \
    "$libDir/wlr/types/wlr_scene.h"

jextract --library $lib --output $output  \
    --target-package jextract.wlroots.util\
    "$libDir/wlr/util/log.h"

jextract --library $lib --output $output    \
    --target-package jextract.wlroots       \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    --include-dir "/usr/include/pixman-1/"  \
    "$libDir/wlr/backend.h"

jextract --library $lib --output $output    \
    --target-package jextract.wlroots             \
    "$libDir/wlr/version.h"
#</editor-fold>