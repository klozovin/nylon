# Linux desktop kitchen sink.

## Compositor

Minimal Wayland compositor in Kotlin, based on wlroots.

## Dewey

Simple GUI file manager, based on GTK and inspired by ranger.

## Bindings

Java bindings (using Panama and handwritten wrappers) to various libraries useful for writing Wayland compositors: currently wlroots, wayland and xkbcommon.

## Examples

Ports of wlroots/examples:

* [simple.c](examples/simple)
* [cairo-buffer.c](examples/cairo-buffer)
* [scene-graph.c](examples/scene-graph)
* [pointer.c](examples/pointer)

Port of TinyWL (wlroots/tinywl) example compositor: [TinyWL](examples/tinywl) 