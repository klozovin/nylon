# wlroots4j - Java bindings for wlroots library

Java bindings for the wlroots library, using the Panama FFM API.

Lowest level code that talks directly with C code of wlroots is generated with the `jextract` tool. Higher level bindings are handwritten and trying to stay as close as possible to wlroots, but with some Java niceties where applicable.

## [TinyWL](tinywl)

Port of [tinywl](https://gitlab.freedesktop.org/wlroots/wlroots/-/tree/master/tinywl) example compositor from wlroots.

## Examples

Ports of [examples](https://gitlab.freedesktop.org/wlroots/wlroots/-/tree/master/examples) from wlroots library.

* [simple.c](examples/simple)
* [cairo-buffer.c](examples/cairo-buffer)
* [scene-graph.c](examples/scene-graph)
* [pointer.c](examples/pointer)