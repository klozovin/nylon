#!/bin/sh

lib="wlroots-0.18"
libDir="/usr/include/wlroots-0.18"
output="generated/main/java"

#<editor-fold desc="xkbcommon">

#jextract --library "xkbcommon" --output $output \
#    --target-package xcb                    \
#    "/usr/include/xkbcommon/xkbcommon.h"

#</editor-fold>

#<editor-fold desc="wayland-server">
jextract --library "wayland-server" --output $output \
    --target-package wayland              \
    --header-class-name "server_h"        \
    "/usr/include/wayland-server-core.h"  \

jextract --library "wayland-server" --output $output \
    --target-package wayland      \
    --header-class-name "util_h"  \
    "/usr/include/wayland-util.h"
#</editor-fold> \

#<editor-fold desc="wlroots">
jextract --library $lib --output $output    \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    --include-dir "/usr/include/pixman-1/"  \
    --target-package wlroots.render         \
    "$libDir/wlr/render/allocator.h"

jextract --library $lib --output $output    \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    --include-dir "/usr/include/pixman-1/"  \
    --target-package wlroots.render         \
    "$libDir/wlr/render/pass.h"

jextract --library $lib --output $output    \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    --include-dir "/usr/include/pixman-1/"  \
    --target-package wlroots.render         \
    "$libDir/wlr/render/wlr_renderer.h"

jextract --library $lib --output $output    \
    --target-package wlroots.types          \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    "$libDir/wlr/types/wlr_keyboard.h"

jextract --library $lib --output $output    \
    --target-package wlroots.types          \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    "$libDir/wlr/types/wlr_input_device.h"

jextract --library $lib --output $output    \
    --target-package wlroots.types          \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    --include-dir "/usr/include/pixman-1/"  \
    "$libDir/wlr/types/wlr_output.h"

jextract --library $lib --output $output  \
    --target-package wlroots.util         \
    "$libDir/wlr/util/log.h"

jextract --library $lib --output $output    \
    --target-package wlroots                \
    --define-macro WLR_USE_UNSTABLE         \
    --include-dir $libDir                   \
    --include-dir "/usr/include/pixman-1/"  \
    "$libDir/wlr/backend.h"

jextract --library $lib --output $output    \
    --target-package wlroots                \
    "$libDir/wlr/version.h"
#</editor-fold>