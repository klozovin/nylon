import nylon.Tuple
import nylon.Tuple.Tuple4
import wayland.KeyboardKeyState
import wayland.PointerButtonState
import wayland.SeatCapability
import wayland.server.Display
import wayland.server.Listener
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
import wlroots.util.Log
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey
import java.util.*
import kotlin.system.exitProcess


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


object Tiny {

    lateinit var display: Display
    lateinit var backend: Backend
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator
    lateinit var output: Output

    lateinit var keyboard: Keyboard

    lateinit var cursor: Cursor
    lateinit var xcursorManager: XcursorManager
    lateinit var cursorMode: CursorMode

    lateinit var scene: Scene
    lateinit var outputLayout: OutputLayout
    lateinit var sceneOutputLayout: SceneOutputLayout

    lateinit var xdgShell: XdgShell
    lateinit var seat: Seat

    var grabX: Double = 0.0
    var grabY: Double = 0.0

    val tyToplevels = mutableListOf<TyToplevel>()


    // TODO: Better way to handle this? (what if multiple outputs, output gets reconnected...?)
    val listeners = mutableMapOf<String, Listener>()

    var grabbedToplevel: TyToplevel? = null


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
            if (args.isEmpty()) command("/usr/bin/foot")
            else command(*args)
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

    data class DesktopToplevelAtResult(
        val ttl: TyToplevel,
        val surface: Surface,
        val sx: Double,
        val sy: Double,
    )

    // TOOD: can ttl be !null and surface null?
    fun desktopToplevelAt(x: Double, y: Double): Tuple4<TyToplevel, Surface, Double, Double>? {
//        println("Looking at: x=${cursor.x()} y=${cursor.y()}")

        val (sceneNode, nx, ny) = scene.tree().node().nodeAt(x, y) ?: return null

//        println(sceneNode)

        if (sceneNode.type() != SceneNode.Type.BUFFER)
            return null

        val sceneBuffer = SceneBuffer.fromNode(sceneNode)
        val sceneSurface = sceneBuffer.getSceneSurface() ?: return null

//        println(sceneBuffer)
//        println(sceneSurface)



        //TODO: rewrite to break
        var tree = sceneNode.parent()
        while (tree != null && tyToplevels.find { it.sceneTree.sceneTreePtr == tree.sceneTreePtr } == null) {
            tree = tree.node().parent()
        }
        val tytoplevel = tyToplevels.find { it.sceneTree.sceneTreePtr == tree!!.sceneTreePtr }


        return if (tytoplevel != null)
            Tuple.of(tytoplevel, sceneSurface.surface(), nx, ny)
        else
            null
    }

    //
    // *** Event handlers ***
    //

    //
    // Backend events: newOutput, newInput
    //

    fun onNewOutput(newOutput: Output) {
        require(!::output.isInitialized) { "Only one output supported." }

        output = newOutput
        output.initRender(allocator, renderer)

        OutputState.allocateConfined { outputState ->
            outputState.init()
            outputState.setEnabled(true)
            output.preferredMode()?.let { outputState.setMode(it) }
            output.commitState(outputState)
            outputState.finish()
        }

        listeners["output.frame"] = output.events.frame.add(::onOutputFrame)
        listeners["output.request_state"] = output.events.requestState.add(::onOutputRequestState)
        listeners["output.destroy"] = output.events.destroy.add(::onOutputDestroy)

        val outputLayoutOutput = outputLayout.addAuto(output)
        val sceneOutput = SceneOutput.create(scene, output)
        sceneOutputLayout.addOutput(outputLayoutOutput, sceneOutput)
    }


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

    //
    // Output events
    //

    fun onOutputFrame(output: Output) {
        val sceneOutput = scene.getSceneOutput(output)!!
        sceneOutput.commit()
        sceneOutput.sendFrameDone()
    }

    fun onOutputRequestState(event: EventRequestState) {
        output.commitState(event.state)
    }

    fun onOutputDestroy(output: Output) {
        TODO()
        listeners["output.frame"]!!.remove()
        listeners["output.request_state"]!!.remove()
        listeners["output.destroy"]!!.remove()
    }


    //
    // Keyboard events
    //

    object TyKeyboard

    fun onKeyboardKey(event: KeyboardKeyEvent) {
        println("Handling key")
        val keycode = event.keycode() + 8
        val keysym = keyboard.xkbState().keyGetOneSym(keycode)

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
//        println("onCursorMotion: deltaX=${event.deltaX}, deltaY=${event.deltaY}")
        cursor.move(event.pointer.base(), event.deltaY, event.deltaY)
        processCursorMotion(event.timeMsec)
    }


    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {
//        println("onCursorMotionAbsolute: x=${event.x}, y=${event.y}")
        cursor.warpAbsolute(event.pointer.base(), event.x, event.y)
        processCursorMotion(event.timeMsec)
    }


    fun onCursorButton(event: PointerButtonEvent) {
        seat.pointerNotifyButton(event.timeMsec, event.button, event.state)

        val toplevel = desktopToplevelAt(cursor.x(), cursor.y())

        // TODO: Why not check if we're in move/resize state?
        if (event.state == PointerButtonState.RELEASED)
            resetCursorMode()
        else
            focusToplevel(toplevel?._1, toplevel?._2)
    }


    fun onCursorAxis(event: PointerAxisEvent) {
        TODO()
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

            else -> Unit
        }

        // Forward events to the client under the pointer

        val toplevelResult = desktopToplevelAt(cursor.x(), cursor.y())

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


    // Move the grabbed toplevel to new position
    private fun processCursorMove(timeMsec: Int) {
        grabbedToplevel!!.sceneTree.node().setPosition(
            (cursor.x() - grabX).toInt(),
            (cursor.y() - grabY).toInt()
        )
    }


    private fun processCursorResize(timeMsec: Int) {
        TODO()
    }


    fun beginInteractive(tytoplevel: TyToplevel, mode: CursorMode, foobar: Int) {
        println(tytoplevel)
        println(mode)

        val focusedSurface = seat.pointerState().focusedSurface()
        if (tytoplevel.xdgToplevel.base().surface().surfacePtr != focusedSurface.rootSurface.surfacePtr) {
            println("Returning!!! Ohnoes")
            return
        }

        grabbedToplevel = tytoplevel
        cursorMode = mode

        if (mode == CursorMode.Move) {
            grabX = cursor.x() - tytoplevel.sceneTree.node().x()
            grabY = cursor.y() - tytoplevel.sceneTree.node().y()
            println("$grabX, $ grabY")
        } else {
            TODO()
        }

    }


    fun resetCursorMode() {
        cursorMode = CursorMode.Passthrough
        grabbedToplevel = null
    }


    // *** XDG Shell: Top level *************************************************************************** //


    object TopLevel {

        fun onNew(toplevel: XdgToplevel) {
            val sceneTree = scene.tree().xdgSurfaceCreate(toplevel.base())

            val tytl = TyToplevel(toplevel, sceneTree)

            // Event handlers for the base Surface
            with(toplevel.base().surface().events) {
                tytl.onMapListener = map.add(::onMap)
                tytl.onUnmapListener = unmap.add(::onUnmap)
                tytl.onCommitListener = commit.add(::onCommit)
                tytl.onDestroyListener = destroy.add(::onXdgToplevelDestroy)
            }

            // Event handlers for the XDG Toplevel surface
            with(toplevel.events) {
                tytl.onRrequestMoveListener = requestMove.add(::onXdgToplevelRequestMove)
                tytl.onRrequestResizeListener = requestResize.add(::onXdgToplevelRequestResize)
                tytl.onRrequestMaximizeListener = requestMaximize.add(::onXdgToplevelRequestMaximize)
                tytl.onRrequestFullscreenListener = requestFullscreen.add(::onXdgToplevelRequestFullscreen)
            }

            tyToplevels.add(tytl)
        }


        fun onCommit(surface: Surface) {
            val topLevel = tyToplevels
                .find { it.xdgToplevel.base().surface().surfacePtr == surface.surfacePtr }!!
                .xdgToplevel

            // When an xdg_surface performs an initial commit, the compositor must reply with a configure so the
            // client can map the surface. tinywl configures the xdg_toplevel with 0,0 size to let the client pick
            // the dimensions itself.
            if (topLevel.base().initialCommit())
                topLevel.setSize(0, 0)
        }

        fun onMap(listener: Listener) {
            val tinyXdgToplevel = tyToplevels.find { it.onMapListener.listenerPtr == listener.listenerPtr }!!
            focusToplevel(tinyXdgToplevel, tinyXdgToplevel.xdgToplevel.base().surface())
        }


        fun onUnmap(listener: Listener) {
            val tyToplevel = tyToplevels.find { it.onUnmapListener.listenerPtr == listener.listenerPtr }!!

            // Reset the cursor mode if the grabbed toplevel was unmapped
            // TODO: Test this
            if (grabbedToplevel?.xdgToplevel?.xdgToplevelPtr == tyToplevel.xdgToplevel.xdgToplevelPtr)
                resetCursorMode()
        }


        fun onXdgToplevelDestroy(listener: Listener, surface: Surface) {
            val idx = tyToplevels.indexOfFirst { it.onDestroyListener.listenerPtr == listener.listenerPtr }
            val element = tyToplevels[idx]

            element.onMapListener.remove()
            element.onUnmapListener.remove()
            element.onCommitListener.remove()
            element.onDestroyListener.remove()

            element.onRrequestMoveListener.remove()
            element.onRrequestResizeListener.remove()
            element.onRrequestMaximizeListener.remove()
            element.onRrequestFullscreenListener.remove()

            tyToplevels.removeAt(idx)
        }
    }

    //

    fun onNewPopup(popup: XdgPopup) {
        TODO()

        val parent = XdgSurface.tryFromSurface(popup.parent()) ?: error("Popup parent can't be null")

        // TODO: Is this way ok???
        val parentSceneTree =
            tyToplevels.find { it.xdgToplevel.base().xdgSurfacePtr == parent.xdgSurfacePtr }!!.sceneTree
        parentSceneTree.xdgSurfaceCreate(popup.base())

        popup.base().surface().events.commit.add(::onXdgPopupCommit)
        popup.events.destroy.add(::onXdgPopupDestroy)
    }

    fun onXdgToplevelRequestMove(event: XdgToplevel.MoveEvent) {
        println(event)
        beginInteractive(
            tyToplevels.find { it.xdgToplevel.xdgToplevelPtr == event.toplevel.xdgToplevelPtr }!!,
            CursorMode.Move,
            0
        )
    }


    fun onXdgToplevelRequestResize(event: XdgToplevel.ResizeEvent) {
        TODO()
    }


    fun onXdgToplevelRequestMaximize() {
        TODO()
    }

    fun onXdgToplevelRequestFullscreen() {
        TODO()
    }

    fun onXdgPopupCommit(surface: Surface) {
        TODO()
    }

    fun onXdgPopupDestroy() {
        TODO()
    }

    //
    // Seat events
    //

    fun onSeatRequestSetCursor(event: PointerRequestSetCursorEvent) {
        val focusedClient = seat.pointerState().focusedClient()
        if (focusedClient.seatClientPtr == event.seatClient().seatClientPtr)
            cursor.setSurface(event.surface(), event.hotspotX(), event.hotspotY())
    }

    fun onSeatRequestSetSelection(selection: RequestSetSelectionEvent) {
        TODO()

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