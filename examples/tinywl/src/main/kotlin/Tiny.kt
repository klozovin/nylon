import nylon.Tuple
import nylon.Tuple.Tuple4
import wayland.KeyboardKeyState
import wayland.PointerButtonState
import wayland.SeatCapability
import wayland.server.Display
import wayland.server.Listener
import wayland.util.Edge
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.Renderer
import wlroots.types.*
import wlroots.types.compositor.Compositor
import wlroots.types.compositor.Subcompositor
import wlroots.types.compositor.Surface
import wlroots.types.output.EventRequestState
import wlroots.types.output.Output
import wlroots.types.output.OutputLayout
import wlroots.types.output.OutputState
import wlroots.types.pointer.PointerAxisEvent
import wlroots.types.pointer.PointerButtonEvent
import wlroots.types.pointer.PointerMotionAbsoluteEvent
import wlroots.types.pointer.PointerMotionEvent
import wlroots.types.scene.*
import wlroots.types.seat.PointerRequestSetCursorEvent
import wlroots.types.seat.RequestSetSelectionEvent
import wlroots.types.seat.Seat
import wlroots.types.xdgshell.XdgPopup
import wlroots.types.xdgshell.XdgShell
import wlroots.types.xdgshell.XdgSurface
import wlroots.types.xdgshell.XdgToplevel
import wlroots.util.Box
import wlroots.util.Log
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey
import java.util.*
import kotlin.system.exitProcess


data class TyOutput(
    val output: Output,
    val frameListener: Listener,
    val requestStateListener: Listener,
    val destroyListener: Listener
)


class TyToplevel(val xdgToplevel: XdgToplevel, val sceneTree: SceneTree) {
    lateinit var onMapListener: Listener
    lateinit var onCommitListener: Listener
    lateinit var onUnmapListener: Listener
    lateinit var onDestroyListener: Listener

    lateinit var onRrequestMoveListener: Listener
    lateinit var onRrequestResizeListener: Listener
    lateinit var onRrequestMaximizeListener: Listener
    lateinit var onRrequestFullscreenListener: Listener
}


class TyPopup(val xdgPopup: XdgPopup, val sceneTree: SceneTree) {
    lateinit var onCommitListener: Listener
    lateinit var onDestroyListener: Listener
}


object Tiny {
    lateinit var display: Display
    lateinit var backend: Backend
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator

    lateinit var keyboard: Keyboard

    lateinit var cursor: Cursor
    lateinit var xcursorManager: XcursorManager
    lateinit var cursorMode: CursorMode

    lateinit var scene: Scene
    lateinit var outputLayout: OutputLayout
    lateinit var sceneOutputLayout: SceneOutputLayout

    lateinit var xdgShell: XdgShell
    lateinit var seat: Seat


    // Moving and resizing windows
    var grabbedToplevel: TyToplevel? = null
    lateinit var grabGeobox: Box
    var grabX: Double = 0.0
    var grabY: Double = 0.0
    lateinit var resizeEdges: EnumSet<Edge>


    val OUTPUTS = mutableListOf<TyOutput>()
    val TOPLEVELS = mutableListOf<TyToplevel>()
    val POPUPS = mutableListOf<TyPopup>()


    fun main(args: Array<String>) {
        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Failed to create wlr_backend")
        renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")

        renderer.initWlDisplay(display)

        allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_allocator")

        backend.events.newOutput.add(::onNewOutput)
        backend.events.newInput.add(::onNewInput)

        Compositor.create(display, 5, renderer)
        Subcompositor.create(display)
        DataDeviceManager.create(display)

        outputLayout = OutputLayout.create(display)

        scene = Scene.create()
        sceneOutputLayout = scene.attachOutputLayout(outputLayout)

        xdgShell = XdgShell.create(display, 3)
        xdgShell.events.newToplevel.add(TopLevel::onNew)
        xdgShell.events.newPopup.add(::onNewPopup)

        cursor = Cursor.create().apply {
            attachOutputLayout(outputLayout)
            events.motion.add(::onCursorMotion)
            events.motionAbsolute.add(::onCursorMotionAbsolute)
            events.button.add(::onCursorButton)
            events.axis.add(::onCursorAxis)
            events.frame.add(::onCursorFrame)
        }
        xcursorManager = XcursorManager.create(null, 24) ?: error("Failed to create wlr_xcursor_manager")
        cursorMode = CursorMode.Passthrough

        seat = Seat.create(display, "seat0")
        seat.events.requestSetCursor.add(::onSeatRequestSetCursor)
        seat.events.requestSetSelection.add(::onSeatRequestSetSelection)

        val socket = display.addSocketAuto() ?: error {
            backend.destroy()
            exitProcess(1)
        }

        // Start the backend: enumerate outputs, inputs, become DRM master, events, ...
        if (!backend.start()) {
            backend.destroy()
            display.destroy()
            exitProcess(1)
        }

        // Run startup command or the default terminal
        ProcessBuilder().apply {
            if (args.isEmpty())
                command("/usr/bin/foot")
            else
                command(*args)
            environment().put("WAYLAND_DISPLAY", socket)
            start()
        }

        // Run the Wayland event loop, it does not return until you exit the compositor.
        Log.logInfo("Running Wayland compositor on WAYLAND_DISPLAY=$socket")
        display.run()

        // Cleanup after the wl_display_run() returns
        display.destroyClients()
        scene.tree().node().destroy()
        xcursorManager.destroy()
        cursor.destroy()
        allocator.destroy()
        renderer.destroy()
        backend.destroy()
        display.destroy()
    }

    // *** Helper functions ******************************************************************************* //


    // TODO: Remove surface parameter, can get it from toplevel
    fun focusToplevel(toplevel: TyToplevel?, surface: Surface?) {
        if (toplevel == null)
            return

        require(surface != null)

        val previouslyFocusedSurface = seat.keyboardState().focusedSurface()

        // Don't refocus already focused surface
        if (previouslyFocusedSurface?.surfacePtr == surface.surfacePtr) {
            return
        }

        // Deactivate the previously focused surface. This lets the client know it lost focus and it can
        // repaint accordingly (eg. stop displaying a caret).
        if (previouslyFocusedSurface != null) {
            XdgToplevel.tryFromSurface(previouslyFocusedSurface)?.let { previousTopLevel ->
                previousTopLevel.setActivated(false)
            }
        }

        toplevel.sceneTree.node().raiseToTop()
        toplevel.xdgToplevel.setActivated(true)

        seat.getKeyboard()?.let { keyboard ->
            seat.keyboardNotifyEnter(
                toplevel.xdgToplevel.base().surface(),
                keyboard.keycodesPtr(),
                keyboard.keycodesNum(),
                keyboard.modifiers()
            )
        }
    }

    // TODO: can ttl be !null and surface null?
    fun desktopToplevelAt(targetX: Double, targetY: Double): Tuple4<TyToplevel, Surface, Double, Double>? {
        val (sceneNode, nx, ny) = scene.tree().node().nodeAt(targetX, targetY) ?: return null

        if (sceneNode.type() != SceneNode.Type.BUFFER)
            return null

        val sceneBuffer = SceneBuffer.fromNode(sceneNode)
        val sceneSurface = SceneSurface.tryFromBuffer(sceneBuffer) ?: return null


        val toplevel9 = sceneNode.parentIterator.firstNotNullOfOrNull { sc ->
            TOPLEVELS.find { it.sceneTree.sceneTreePtr == sc.sceneTreePtr }
        }

        // TODO: Remove after sanity check
        var tree = sceneNode.parent()
        while (tree != null && TOPLEVELS.find { it.sceneTree.sceneTreePtr == tree.sceneTreePtr } == null)
            tree = tree.node().parent()
        val tytoplevel = TOPLEVELS.find { it.sceneTree.sceneTreePtr == tree!!.sceneTreePtr }


        require(toplevel9?.xdgToplevel?.xdgToplevelPtr == tytoplevel?.xdgToplevel?.xdgToplevelPtr)

        return if (tytoplevel != null)
            Tuple.of(tytoplevel, sceneSurface.surface(), nx, ny)
        else
            null
    }


    // *** Output events: ********************************************************************************* //


    fun onNewOutput(output: Output) {
        output.initRender(allocator, renderer)

        OutputState.allocateConfined { outputState ->
            outputState.init()
            outputState.setEnabled(true)
            output.preferredMode()?.let { outputState.setMode(it) }
            output.commitState(outputState)
            outputState.finish()
        }

        val outputLayoutOutput = outputLayout.addAuto(output)
        val sceneOutput = SceneOutput.create(scene, output)
        sceneOutputLayout.addOutput(outputLayoutOutput, sceneOutput)

        OUTPUTS.add(
            TyOutput(
                output = output,
                frameListener = output.events.frame.add(::onOutputFrame),
                requestStateListener = output.events.requestState.add(::onOutputRequestState),
                destroyListener = output.events.destroy.add(::onOutputDestroy)
            )
        )
    }


    fun onOutputFrame(output: Output) {
        val sceneOutput = scene.getSceneOutput(output)!!
        sceneOutput.commit()
        sceneOutput.sendFrameDone()
    }


    fun onOutputRequestState(event: EventRequestState) {
        event.output.commitState(event.state)
    }


    fun onOutputDestroy(listener: Listener, output: Output) {
        OUTPUTS.find { it.destroyListener == listener }!!.apply {
            frameListener.remove()
            requestStateListener.remove()
            destroyListener.remove()
        }
    }


    // *** Input devices ********************************************************************************** //


    fun onNewInput(inputDevice: InputDevice) {
        when (inputDevice.type()) {

            // Typing device
            InputDevice.Type.KEYBOARD -> {
                require(!::keyboard.isInitialized) { "Only one keyboard input device supported" }
                keyboard = inputDevice.keyboardFromInputDevice()
                val context = XkbContext.of(XkbContext.Flags.NO_FLAGS) ?: error {
                    Log.logError("Failed to create XKB context")
                    exitProcess(1)
                }
                val keymap = context.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS) ?: error {
                    Log.logError("Failed to create XKB context")
                    exitProcess(1)
                }
                with(keyboard) {
                    setKeymap(keymap)
                    setRepeatInfo(25, 600)
                    events.modifiers.add(::onKeyboardModifiers)
                    events.key.add(::onKeyboardKey)
                }
                inputDevice.events.destroy.add(::onKeyboardDestroy)
                seat.setKeyboard(keyboard)

                keymap.unref()
                context.unref()
            }

            // Pointing device
            InputDevice.Type.POINTER -> cursor.attachInputDevice(inputDevice)

            else -> println("Unknown device type: ${inputDevice.type()}")
        }

        val capabilities = EnumSet.of(SeatCapability.POINTER)
        if (::keyboard.isInitialized)
            capabilities.add(SeatCapability.KEYBOARD)
        seat.setCapabilities(capabilities)
    }


    // *** Keyboard events ******************************************************************************** //


    fun onKeyboardKey(event: KeyboardKeyEvent) {
        println("Handling key")
        val keycode = event.keycode() + 8
        val keysym = keyboard.xkbState().keyGetOneSym(keycode)

        // TODO: Just return this flag from if-when
        var handledInCompositor = false

        if (keyboard.modifiers.isAltDown && event.state() == KeyboardKeyState.PRESSED) {
            when (keysym) {
                XkbKey.Escape -> {
                    display.terminate()
                    handledInCompositor = true
                }

                XkbKey.F1 -> {
                    // TODO: 	case XKB_KEY_F1
                    // cycle
                    handledInCompositor = true
                }

                XkbKey.F2 -> {
                    println("Scene tree parent: ${scene.tree().node().parent()}")
                    handledInCompositor = true

                }

                XkbKey.F12 -> {
                    Log.logInfo("Heeelllooouuu from TinyWL")
                    handledInCompositor = true
                }
            }
        }

        if (!handledInCompositor) {
            seat.setKeyboard(keyboard) // TODO: What if multiple keyboards
            seat.keyboardNotifyKey(event.timeMsec(), event.keycode(), event.state())
        }
    }

    fun onKeyboardModifiers(keyboard: Keyboard) {
        seat.setKeyboard(keyboard) // TODO: What if multiple keyboards
        seat.keyboardNotifyModifiers(keyboard.modifiers())
    }

    fun onKeyboardDestroy(device: InputDevice) {
        TODO()
    }


    // *** Cursor events ********************************************************************************** //


    fun onCursorMotion(event: PointerMotionEvent) {
        cursor.move(event.pointer.base(), event.deltaY, event.deltaY)
        processCursorMotion(event.timeMsec)
    }


    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {
        cursor.warpAbsolute(event.pointer.base(), event.x, event.y)
        processCursorMotion(event.timeMsec)
    }


    // Mouse click event
    fun onCursorButton(event: PointerButtonEvent) {
        // TODO: Raise on click, swallow mouse event (have to detect chaning of focused toplevel window)

        // Notify the client with "pointer focus" that a button press has occurred
        seat.pointerNotifyButton(event.timeMsec, event.button, event.state)

        // TODO: Can go under when-pressed branch, don't need to pass null into focusTopLevel
        val toplevel = desktopToplevelAt(cursor.x(), cursor.y())

        when (event.state) {
            PointerButtonState.PRESSED -> focusToplevel(toplevel?._1, toplevel?._2)

            // TODO: why not check if we're in the move/resize state?
            PointerButtonState.RELEASED -> resetCursorMode()
        }
    }


    // Mouse scroll event
    fun onCursorAxis(event: PointerAxisEvent) {
        seat.pointerNotifyAxis(
            event.timeMsec,
            event.orientation,
            event.delta,
            event.deltaDiscrete,
            event.source,
            event.relativeDirection
        )
    }


    fun onCursorFrame(cursor: Cursor) {
        seat.pointerNotifyFrame()
    }


    fun processCursorMotion(timeMsec: Int) {
        // Process the event in the compositor: either moving or resizing the window
        when (cursorMode) {
            CursorMode.Move -> {
                processCursorMove(timeMsec)
                return
            }

            CursorMode.Resize -> {
                processCursorResize(timeMsec)
                return
            }

            // TODO: Put everything else in Passthrough branch
            else -> Unit
        }


        // Forward events to the client under the pointer

        val toplevelResult = desktopToplevelAt(cursor.x(), cursor.y())

        toplevelResult?.let {
            val parent = it._1.xdgToplevel.parent()
            println("processCursorMotion >>> ${it._1.xdgToplevel.xdgToplevelPtr}.parent=${parent?.xdgToplevelPtr}")
        }


        // TODO: Unify these conditions, better handle null
        if (toplevelResult == null) {
            cursor.setXcursor(xcursorManager, "default")
        }
        if (toplevelResult?._2 != null) {
            seat.pointerNotifyEnter(toplevelResult._2, toplevelResult._3, toplevelResult._4)
            seat.pointerNotifyMotion(timeMsec, toplevelResult._3, toplevelResult._4)
        } else {
            seat.pointerClearFocus()
        }
    }


    fun beginInteractive(tytoplevel: TyToplevel, mode: CursorMode, edges: EnumSet<Edge>?) {
        println("beginInteractive >>> tytoplevel=${tytoplevel}")
        println("beginInteractive>> mode=$mode")

        val focusedSurface = seat.pointerState().focusedSurface()
        if (tytoplevel.xdgToplevel.base().surface().surfacePtr != focusedSurface.rootSurface.surfacePtr) {
            println("Returning!!! Ohnoes")
            return
        }

        grabbedToplevel = tytoplevel
        cursorMode = mode

        val sceneNode = tytoplevel.sceneTree.node()
        when (mode) {
            CursorMode.Move -> {
                grabX = cursor.x() - sceneNode.x()
                grabY = cursor.y() - sceneNode.y()
                println("$grabX, $ grabY")
            }

            CursorMode.Resize -> {
                require(edges != null)
                val geoBox = tytoplevel.xdgToplevel.base().getGeometry()

                val borderX = (sceneNode.x() + geoBox.x()) + if (Edge.RIGHT in edges) geoBox.width() else 0
                val borderY = (sceneNode.y() + geoBox.y()) + if (Edge.BOTTOM in edges) geoBox.height() else 0

                grabX = cursor.x() - borderX
                grabY = cursor.y() - borderY

                grabGeobox = geoBox
                with(grabGeobox) {
                    x(x() + sceneNode.x())
                    y(y() + sceneNode.y())
                }

                resizeEdges = edges
                println(edges)
            }

            CursorMode.Passthrough -> TODO()
        }
    }


    // Move the grabbed toplevel to new position
    private fun processCursorMove(timeMsec: Int) {
        grabbedToplevel!!.sceneTree.node().setPosition(
            (cursor.x() - grabX).toInt(),
            (cursor.y() - grabY).toInt()
        )
    }


    private fun processCursorResize(timeMsec: Int) {
        val grabbedXdgToplevel = grabbedToplevel!!.xdgToplevel
        val grabbedSceneTree = grabbedToplevel!!.sceneTree

        val borderX = cursor.x() - grabX
        val borderY = cursor.y() - grabY

        var newLeft = grabGeobox.x()
        var newRight = grabGeobox.x() + grabGeobox.width()

        var newTop = grabGeobox.y()
        var newBottom = grabGeobox.y() + grabGeobox.height()


        if (Edge.TOP in resizeEdges) {
            newTop = borderY.toInt()
            if (newTop >= newBottom)
                newTop = newBottom - 1
        } else if (Edge.BOTTOM in resizeEdges) {
            newBottom = borderY.toInt()
            if (newBottom <= newTop)
                newBottom = newTop + 1
        }

        if (Edge.LEFT in resizeEdges) {
            newLeft = borderX.toInt()
            if (newLeft >= newRight)
                newLeft = newRight - 1
        } else if (Edge.RIGHT in resizeEdges) {
            newRight = borderX.toInt()
            if (newRight <= newLeft)
                newRight = newLeft + 1
        }

        val geoBox = grabbedXdgToplevel.base().getGeometry()
        grabbedSceneTree.node().setPosition(newLeft - geoBox.x(), newTop - geoBox.y())
        grabbedXdgToplevel.setSize(newRight - newLeft, newBottom - newTop)


    }


    fun resetCursorMode() {
        cursorMode = CursorMode.Passthrough
        grabbedToplevel = null
    }


    // *** XDG Shell: Top level *************************************************************************** //


    // TODO: Remove object, only used for grouping
    object TopLevel {

        fun onNew(toplevel: XdgToplevel) {
            val sceneTree = scene.tree().xdgSurfaceCreate(toplevel.base())
            val tytop = TyToplevel(toplevel, sceneTree)

            // Event handlers for the base Surface
            with(toplevel.base().surface().events) {
                tytop.onMapListener = map.add(::onMap)
                tytop.onUnmapListener = unmap.add(::onUnmap)
                tytop.onCommitListener = commit.add(::onCommit)
            }

            // Event handlers for the XDG Toplevel surface
            with(toplevel.events) {
                tytop.onDestroyListener = destroy.add(::onXdgToplevelDestroy)
                tytop.onRrequestMoveListener = requestMove.add(::onXdgToplevelRequestMove)
                tytop.onRrequestResizeListener = requestResize.add(::onXdgToplevelRequestResize)
                tytop.onRrequestMaximizeListener = requestMaximize.add(::onXdgToplevelRequestMaximize)
                tytop.onRrequestFullscreenListener = requestFullscreen.add(::onXdgToplevelRequestFullscreen)
            }
            TOPLEVELS.add(tytop)
        }


        fun onCommit(listener: Listener, surface: Surface) {
            val xdgToplevel = TOPLEVELS.find { it.onCommitListener == listener }!!.xdgToplevel

            // When an xdg_surface performs an initial commit, the compositor must reply with a configure so the
            // client can map the surface. tinywl configures the xdg_toplevel with 0,0 size to let the client pick
            // the dimensions itself.
            if (xdgToplevel.base().initialCommit())
                xdgToplevel.setSize(0, 0)
        }


        fun onMap(listener: Listener) {
            val tinyXdgToplevel = TOPLEVELS.find { it.onMapListener == listener }!!
            focusToplevel(tinyXdgToplevel, tinyXdgToplevel.xdgToplevel.base().surface())
        }


        fun onUnmap(listener: Listener) {
            val tyToplevel = TOPLEVELS.find { it.onUnmapListener == listener }!!

            // Reset the cursor mode if the grabbed toplevel was unmapped
            // TODO: Test this
            if (grabbedToplevel?.xdgToplevel?.xdgToplevelPtr == tyToplevel.xdgToplevel.xdgToplevelPtr)
                resetCursorMode()
        }


        fun onXdgToplevelDestroy(listener: Listener) {
            val idx = TOPLEVELS.indexOfFirst { it.onDestroyListener == listener }
            val element = TOPLEVELS[idx]

            element.onMapListener.remove()
            element.onUnmapListener.remove()
            element.onCommitListener.remove()
            element.onDestroyListener.remove()

            element.onRrequestMoveListener.remove()
            element.onRrequestResizeListener.remove()
            element.onRrequestMaximizeListener.remove()
            element.onRrequestFullscreenListener.remove()

            TOPLEVELS.removeAt(idx)
        }
    }


    fun onXdgToplevelRequestMove(event: XdgToplevel.MoveEvent) {
        println(event)
        beginInteractive(
            TOPLEVELS.find { it.xdgToplevel.xdgToplevelPtr == event.toplevel.xdgToplevelPtr }!!,
            CursorMode.Move,
            null
        )
    }


    fun onXdgToplevelRequestResize(event: XdgToplevel.Events.Resize) {
        val tyToplevel = TOPLEVELS.find { it.xdgToplevel == event.toplevel }!!
        beginInteractive(tyToplevel, CursorMode.Resize, event.edges)
    }


    // Client wants to fullscreen itself, but we don't support that.
    fun onXdgToplevelRequestFullscreen(listener: Listener) {
        TOPLEVELS.find { it.onRrequestFullscreenListener == listener }?.let {
            if (it.xdgToplevel.base().initialized())
                it.xdgToplevel.base().scheduleConfigure()
        }
    }


    // Client wants to maximize itself, but we don't support that. Just send configure, by xdg-shell protocol
    // specification.
    fun onXdgToplevelRequestMaximize(listener: Listener) {
        val xdgToplevel = TOPLEVELS.find { it.onRrequestMaximizeListener == listener }!!.xdgToplevel
        if (xdgToplevel.base().initialized())
            xdgToplevel.base().scheduleConfigure()
    }


    // *** XDG Shell: Popups ****************************************************************************** //


    fun onNewPopup(popup: XdgPopup) {
        val parent = XdgSurface.tryFromSurface(popup.parent()) ?: error("Popup's parent can't be null")
        val parentSceneTree = TOPLEVELS.find { it.xdgToplevel.base() == parent }!!.sceneTree
        val popupSceneTree = parentSceneTree.xdgSurfaceCreate(popup.base())

        POPUPS.add(TyPopup(popup, popupSceneTree).apply {
            onCommitListener = popup.base().surface().events.commit.add(::onXdgPopupCommit)
            onDestroyListener = popup.events.destroy.add(::onXdgPopupDestroy)
        })
    }


    fun onXdgPopupCommit(listener: Listener, surface: Surface) {
        val popup = POPUPS.find { it.onCommitListener == listener }?.xdgPopup
            ?: error("Cant proceed without popup")
        if (popup.base().initialCommit())
            popup.base().scheduleConfigure()
    }


    fun onXdgPopupDestroy(listener: Listener) {
        val popup = POPUPS.find { it.onDestroyListener == listener } ?: error("Can't proceed without popup")
        popup.onCommitListener.remove()
        popup.onDestroyListener.remove()
    }


    // *** Seat events ************************************************************************************ //


    fun onSeatRequestSetCursor(event: PointerRequestSetCursorEvent) {
        val focusedClient = seat.pointerState().focusedClient()
        if (focusedClient?.seatClientPtr == event.seatClient().seatClientPtr)
            cursor.setSurface(event.surface(), event.hotspotX(), event.hotspotY())
    }


    fun onSeatRequestSetSelection(event: RequestSetSelectionEvent) {
        seat.setSelection(event.source, event.serial)
    }


    enum class CursorMode {
        Passthrough,
        Move,
        Resize
    }
}


fun main(args: Array<String>) {
    Log.init(Log.Importance.DEBUG)
    Tiny.main(args)
    // TODO: take startup command from input
}


inline fun error(block: () -> Any): Nothing = error(block())