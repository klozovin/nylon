#!/bin/sh

lib="wlroots-0.20"
libDir="/usr/include/wlroots-0.20"

c_output="src/main/c"
java_output="src/main/java"

mkdir -p $c_output
mkdir -p $java_output

wayland-scanner server-header /usr/share/wayland-protocols/stable/xdg-shell/xdg-shell.xml "$c_output/xdg-shell-protocol.h"


# Linux input event codes
jextract --output $java_output      \
    --target-package jextract.linux \
    --header-class-name "linux"     \
    "/usr/include/linux/input-event-codes.h"

# Linux kernel DRM
jextract --library "drm" --output $java_output  \
    --target-package jextract.drm               \
    --header-class-name "fourcc"                \
    "/usr/include/libdrm/drm_fourcc.h"


# xkbcommon
jextract --library "xkbcommon"            \
    --output $java_output                 \
    --target-package jextract.xkbcommon   \
    --header-class-name "xkb"             \
    "/usr/include/xkbcommon/xkbcommon.h"


# Wayland
jextract --library "wayland-server"     \
    --output $java_output               \
    --target-package jextract.wayland   \
    --header-class-name "wl"       \
    "/usr/include/wayland-util.h"       \
    "/usr/include/wayland-server.h"     \
    "/usr/include/wayland-version.h"


# IMPORTANT: Only jextract files in wlr/interfaces/, don't also jextract their corresponding
#            /wlr/types/ file because jextract will overwrite already extracted code.

# wlroots
jextract --library $lib --output $java_output \
    --target-package jextract.wlroots         \
    --define-macro WLR_USE_UNSTABLE           \
    --include-dir $libDir                     \
    --include-dir "src/main/c"                \
    --include-dir "/usr/include/pixman-1/"    \
    --header-class-name "wlr"                 \
                                              \
    "$libDir/wlr/render/allocator.h"          \
    "$libDir/wlr/render/pass.h"               \
    "$libDir/wlr/render/wlr_renderer.h"       \
                                              \
    "$libDir/wlr/types/wlr_compositor.h"      \
    "$libDir/wlr/types/wlr_cursor.h"          \
    "$libDir/wlr/types/wlr_data_device.h"     \
    "$libDir/wlr/types/wlr_input_device.h"    \
    "$libDir/wlr/types/wlr_keyboard.h"        \
    "$libDir/wlr/types/wlr_output.h"          \
    "$libDir/wlr/types/wlr_output_layout.h"   \
    "$libDir/wlr/types/wlr_pointer.h"         \
    "$libDir/wlr/types/wlr_scene.h"           \
    "$libDir/wlr/types/wlr_seat.h"            \
    "$libDir/wlr/types/wlr_subcompositor.h"   \
    "$libDir/wlr/types/wlr_tablet_tool.h"     \
    "$libDir/wlr/types/wlr_touch.h"           \
    "$libDir/wlr/types/wlr_xcursor_manager.h" \
    "$libDir/wlr/types/wlr_xdg_shell.h"       \
                                              \
    "$libDir/wlr/interfaces/wlr_buffer.h"     \
                                              \
    "$libDir/wlr/util/log.h"                  \
    "$libDir/wlr/util/edges.h"                \
                                              \
    "$libDir/wlr/backend.h"                   \
    "$libDir/wlr/version.h"