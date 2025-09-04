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
import wlroots.types.Cursor
import wlroots.types.input.InputDevice
import wlroots.types.input.KeyboardKeyEvent
import wlroots.types.input.Keyboard
import wlroots.types.output.EventRequestState
import wlroots.types.output.Output
import wlroots.types.output.OutputLayout
import wlroots.types.output.OutputState
import wlroots.types.input.PointerAxisEvent
import wlroots.types.input.PointerButtonEvent
import wlroots.types.input.PointerMotionAbsoluteEvent
import wlroots.types.input.PointerMotionEvent
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
import kotlin.properties.Delegates
import kotlin.system.exitProcess


data class TyOutput(
    val output: Output,
    val frameListener: Listener,
    val requestStateListener: Listener,
    val destroyListener: Listener
)


data class TyKeyboard(
    val keyboard: Keyboard,
    val keyListener: Listener,
    val modifiersListener: Listener,
    val destroyListener: Listener,
)


class TyToplevel(val xdgToplevel: XdgToplevel, val sceneTree: SceneTree) {
    lateinit var onMapListener: Listener
    lateinit var onCommitListener: Listener
    lateinit var onUnmapListener: Listener
    lateinit var onDestroyListener: Listener

    lateinit var onRequestMoveListener: Listener
    lateinit var onRequestResizeListener: Listener
    lateinit var onRequestMaximizeListener: Listener
    lateinit var onRequestFullscreenListener: Listener
}


class TyPopup(val xdgPopup: XdgPopup, val sceneTree: SceneTree) {
    lateinit var onCommitListener: Listener
    lateinit var onDestroyListener: Listener
}


object Tiny {
    lateinit var display: Display

    // Backend
    lateinit var backend: Backend
    lateinit var backendNewOutputListener: Listener
    lateinit var backendNewInputListener: Listener
    lateinit var backendDestroyListener: Listener

    lateinit var renderer: Renderer
    lateinit var allocator: Allocator

    // Cursor
    lateinit var cursor: Cursor
    lateinit var cursorMotionListener: Listener
    lateinit var cursorMotionAbsoluteListener: Listener
    lateinit var cursorButtonListener: Listener
    lateinit var cursorAxisListener: Listener
    lateinit var cursorFrameListener: Listener

    lateinit var xcursorManager: XcursorManager
    lateinit var cursorMode: CursorMode

    lateinit var scene: Scene
    lateinit var outputLayout: OutputLayout
    lateinit var sceneOutputLayout: SceneOutputLayout

    // XdgShell
    lateinit var xdgShell: XdgShell
    lateinit var xdgShellNewToplevelListener: Listener
    lateinit var xdgShellNewPopupListener: Listener
    lateinit var xdgShellDestroyListener: Listener

    // Seat
    lateinit var seat: Seat
    lateinit var seatRequestSetCursorListener: Listener
    lateinit var seatRequestSetSelectionListener: Listener
    lateinit var seatDestroyListener: Listener


    // Moving and resizing toplevel windows
    var grabbedToplevel: TyToplevel? = null
    lateinit var grabGeobox: Box
    var grabX: Double = 0.0
    var grabY: Double = 0.0
    lateinit var resizeEdges: EnumSet<Edge>


    // Used for cycling through toplevel windows
    var focusedToplevel by Delegates.notNull<Int>()


    val OUTPUTS = mutableListOf<TyOutput>()
    val KEYBOARDS = mutableListOf<TyKeyboard>()
    val TOPLEVELS = mutableListOf<TyToplevel>()
    val POPUPS = mutableListOf<TyPopup>()


    fun main(args: Array<String>) {
        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Failed to create wlr_backend")
        renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")

        renderer.initWlDisplay(display)

        allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_allocator")

        backendNewOutputListener = backend.events.newOutput.add(::onNewOutput)
        backendNewInputListener = backend.events.newInput.add(::onNewInput)
        backendDestroyListener = backend.events.destroy.add(::onBackendDestroy)

        Compositor.create(display, 5, renderer)
        Subcompositor.create(display)
        DataDeviceManager.create(display)

        outputLayout = OutputLayout.create(display)

        scene = Scene.create()
        sceneOutputLayout = scene.attachOutputLayout(outputLayout)

        xdgShell = XdgShell.create(display, 3)
        xdgShellNewToplevelListener = xdgShell.events.newToplevel.add(::onNewXdgToplevel)
        xdgShellNewPopupListener = xdgShell.events.newPopup.add(::onNewXdgPopup)
        xdgShellDestroyListener = xdgShell.events.destroy.add(::onXdgShellDestroy)

        cursor = Cursor.create().apply {
            attachOutputLayout(outputLayout)
            cursorMotionListener = events.motion.add(::onCursorMotion)
            cursorMotionAbsoluteListener = events.motionAbsolute.add(::onCursorMotionAbsolute)
            cursorButtonListener = events.button.add(::onCursorButton)
            cursorAxisListener = events.axis.add(::onCursorAxis)
            cursorFrameListener = events.frame.add(::onCursorFrame)
        }
        xcursorManager = XcursorManager.create(null, 24) ?: error("Failed to create wlr_xcursor_manager")
        cursorMode = CursorMode.Passthrough

        seat = Seat.create(display, "seat0")
        seatRequestSetCursorListener = seat.events.requestSetCursor.add(::onSeatRequestSetCursor)
        seatRequestSetSelectionListener = seat.events.requestSetSelection.add(::onSeatRequestSetSelection)
        seatDestroyListener = seat.events.destroy.add(::onSeatDestroy)

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
        scene.tree().node.destroy()
        xcursorManager.destroy()

        cursorMotionListener.remove()
        cursorMotionAbsoluteListener.remove()
        cursorButtonListener.remove()
        cursorAxisListener.remove()
        cursorFrameListener.remove()
        cursor.destroy()


        allocator.destroy()
        renderer.destroy()
        backend.destroy()
        display.destroy()
    }

    // *** Helper functions ******************************************************************************* //


    fun focusToplevel(toplevel: TyToplevel) {
        val xdgToplevel = toplevel.xdgToplevel
        val previouslyFocusedSurface = seat.getKeyboardState().getFocusedSurface()
        val surface = xdgToplevel.base.surface

        focusedToplevel = TOPLEVELS.indexOfFirst { it.xdgToplevel == toplevel.xdgToplevel }

        // Don't refocus already focused surface
        if (previouslyFocusedSurface == surface) {
            return
        }

        // Deactivate the previously focused surface. This lets the client know it lost focus and it can
        // repaint accordingly (eg. stop displaying a caret).
        if (previouslyFocusedSurface != null) {
            XdgToplevel.tryFromSurface(previouslyFocusedSurface)?.let { previousTopLevel ->
                previousTopLevel.setActivated(false)
            }
        }

        toplevel.sceneTree.node.raiseToTop()
        toplevel.xdgToplevel.setActivated(true)

        seat.getKeyboard()?.let { keyboard ->
            seat.keyboardNotifyEnter(
                surface,
                keyboard.keycodesPtr(),
                keyboard.keycodesNum(),
                keyboard.modifiers()
            )
        }
    }


    data class UnderCursor(
        val tyToplevel: TyToplevel,
        val surface: Surface,
        val nx: Double,
        val ny: Double,
    )


    fun desktopToplevelAt(targetX: Double, targetY: Double): UnderCursor? {
        val (sceneNode, nx, ny) = scene.tree().node.nodeAt(targetX, targetY) ?: return null

        if (sceneNode.type != SceneNode.Type.BUFFER)
            return null

        val sceneBuffer = SceneBuffer.fromNode(sceneNode)
        val sceneSurface = SceneSurface.tryFromBuffer(sceneBuffer) ?: return null

        return sceneNode.parentIterator.firstNotNullOfOrNull { sc ->
            TOPLEVELS.find { it.sceneTree == sc }
        }?.let {
            UnderCursor(it, sceneSurface.surface(), nx, ny)
        }
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
        val idx = OUTPUTS.indexOfFirst { it.destroyListener == listener }
        OUTPUTS.get(idx).apply {
            frameListener.remove()
            requestStateListener.remove()
            destroyListener.remove()
        }
        OUTPUTS.removeAt(idx)
    }


    fun onBackendDestroy(backend: Backend) {
        backendNewOutputListener.remove()
        backendNewInputListener.remove()
        backendDestroyListener.remove()
    }


    // *** Input devices ********************************************************************************** //


    fun onNewInput(inputDevice: InputDevice) {
        val seatCapabilities = seat.capabilities()

        when (inputDevice.type) {
            InputDevice.Type.KEYBOARD -> {
                val keyboard = Keyboard.fromInputDevice(inputDevice)

                val context = XkbContext.of(XkbContext.Flags.NO_FLAGS) ?: error {
                    Log.logError("Failed to create XKB context")
                    exitProcess(1)
                }
                val keymap = context.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS) ?: error {
                    Log.logError("Failed to create XKB keymap")
                    exitProcess(1)
                }
                with(keyboard) {
                    setKeymap(keymap)
                    setRepeatInfo(25, 600)
                }

                seat.setKeyboard(keyboard)

                keymap.unref()
                context.unref()

                seatCapabilities.add(SeatCapability.KEYBOARD)

                KEYBOARDS.add(
                    TyKeyboard(
                        keyboard = keyboard,
                        keyListener = keyboard.events.key.add(::onKeyboardKey),
                        modifiersListener = keyboard.events.modifiers.add(::onKeyboardModifiers),
                        destroyListener = inputDevice.events.destroy.add(::onKeyboardDestroy)
                    )
                )
            }

            InputDevice.Type.POINTER -> {
                cursor.attachInputDevice(inputDevice)
                seatCapabilities.add(SeatCapability.POINTER)
            }

            else -> Log.logDebug("Unknown device type: ${inputDevice.type}")
        }

        seat.setCapabilities(seatCapabilities)
    }


    // *** Keyboard events ******************************************************************************** //


    fun onKeyboardKey(listener: Listener, event: KeyboardKeyEvent) {
        val keyboard = KEYBOARDS.find { it.keyListener == listener }!!.keyboard
        val keycode = event.keycode + 8
        val keysym = keyboard.xkbState().keyGetOneSym(keycode)

        var handledInCompositor = false
        if (keyboard.modifiers.containsAlt() && event.state == KeyboardKeyState.PRESSED) {
            when (keysym) {
                XkbKey.F1 -> {
                    if (TOPLEVELS.isNotEmpty()) {
                        val nextIdx = focusedToplevel + 1
                        if (nextIdx < TOPLEVELS.size)
                            focusToplevel(TOPLEVELS[nextIdx])
                        else
                            focusToplevel(TOPLEVELS[0])
                    }
                    handledInCompositor = true
                }

                XkbKey.Escape -> {
                    display.terminate()
                    handledInCompositor = true
                }
            }
        }

        if (!handledInCompositor) {
            seat.setKeyboard(keyboard)
            seat.keyboardNotifyKey(event.timeMsec, event.keycode, event.state)
        }
    }


    fun onKeyboardModifiers(keyboard: Keyboard) {
        seat.setKeyboard(keyboard)
        seat.keyboardNotifyModifiers(keyboard.modifiers())
    }


    fun onKeyboardDestroy(listener: Listener, device: InputDevice) {
        val idx = KEYBOARDS.indexOfFirst { it.destroyListener == listener }
        KEYBOARDS.get(idx).apply {
            modifiersListener.remove()
            keyListener.remove()
            destroyListener.remove()
        }
        KEYBOARDS.removeAt(idx)
    }


    // *** Cursor events ********************************************************************************** //


    fun onCursorMotion(event: PointerMotionEvent) {
        cursor.move(event.pointer.base(), event.deltaX, event.deltaY)
        processCursorMotion(event.timeMsec)
    }


    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {
        cursor.warpAbsolute(event.pointer.base(), event.x, event.y)
        processCursorMotion(event.timeMsec)
    }


    // Mouse click event
    fun onCursorButton(event: PointerButtonEvent) {
        // Notify the client with "pointer focus" that a button press has occurred
        seat.pointerNotifyButton(event.timeMsec, event.button, event.state)

        when (event.state) {
            PointerButtonState.PRESSED ->
                desktopToplevelAt(cursor.x, cursor.y)?.let { (tytoplevel, _, _, _) ->
                    focusToplevel(tytoplevel)
                }

            PointerButtonState.RELEASED ->
                if (cursorMode == CursorMode.Move || cursorMode == CursorMode.Resize)
                    resetCursorMode()
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
        when (cursorMode) {
            CursorMode.Move -> processCursorMove(timeMsec)

            CursorMode.Resize -> processCursorResize(timeMsec)

            CursorMode.Passthrough ->
                when (val underCursor = desktopToplevelAt(cursor.x, cursor.y)) {
                    // Find the toplevel under the pointer and send the cursor events along.
                    is UnderCursor -> {
                        val (_, surface, nx, ny) = underCursor
                        seat.pointerNotifyEnter(surface, nx, ny)
                        seat.pointerNotifyMotion(timeMsec, nx, ny)
                    }

                    // Clear pointer focus so the future button events are not sent to the last client to
                    // have the cursor over it.
                    null -> {
                        seat.pointerClearFocus()
                        cursor.setXcursor(xcursorManager, "default")
                    }
                }
        }
    }


    fun beginInteractive(tytoplevel: TyToplevel, mode: CursorMode, edges: EnumSet<Edge>?) {
        val focusedSurface = seat.pointerState.focusedSurface

        if (tytoplevel.xdgToplevel.base.surface != focusedSurface?.rootSurface) {
            return
        }

        grabbedToplevel = tytoplevel
        cursorMode = mode

        val sceneNode = tytoplevel.sceneTree.node
        when (mode) {
            CursorMode.Move -> {
                grabX = cursor.x - sceneNode.x
                grabY = cursor.y - sceneNode.y
            }

            CursorMode.Resize -> {
                require(edges != null)
                val geoBox = tytoplevel.xdgToplevel.base.geometry

                val borderX = (sceneNode.x + geoBox.x) + if (Edge.RIGHT in edges) geoBox.getWidth() else 0
                val borderY = (sceneNode.y + geoBox.y) + if (Edge.BOTTOM in edges) geoBox.getHeight() else 0

                grabX = cursor.x - borderX
                grabY = cursor.y - borderY

                grabGeobox = Box.allocateCopy(geoBox)
                with(grabGeobox) {
                    x += sceneNode.x
                    y += sceneNode.y
                }

                resizeEdges = edges
            }

            CursorMode.Passthrough -> error("Unreachable")
        }
    }


    // Move the grabbed toplevel to new position
    private fun processCursorMove(timeMsec: Int) {
        grabbedToplevel!!.sceneTree.node.setPosition(
            (cursor.x - grabX).toInt(),
            (cursor.y - grabY).toInt()
        )
    }


    private fun processCursorResize(timeMsec: Int) {
        val grabbedXdgToplevel = grabbedToplevel!!.xdgToplevel
        val grabbedSceneTree = grabbedToplevel!!.sceneTree

        val borderX = cursor.x - grabX
        val borderY = cursor.y - grabY

        var newLeft = grabGeobox.x
        var newRight = grabGeobox.x + grabGeobox.width

        var newTop = grabGeobox.y
        var newBottom = grabGeobox.y + grabGeobox.height


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

        val geoBox = grabbedXdgToplevel.base.geometry
        grabbedSceneTree.node.setPosition(newLeft - geoBox.x, newTop - geoBox.y)
        grabbedXdgToplevel.setSize(newRight - newLeft, newBottom - newTop)
    }


    fun resetCursorMode() {
        cursorMode = CursorMode.Passthrough
        grabbedToplevel = null
    }

    // *** XDG Shell ************************************************************************************** //

    fun onXdgShellDestroy(xdgShell: XdgShell) {
        xdgShellNewToplevelListener.remove()
        xdgShellNewPopupListener.remove()
        xdgShellDestroyListener.remove()
    }


    // *** XDG Shell: Top level *************************************************************************** //


    fun onNewXdgToplevel(toplevel: XdgToplevel) {
        val sceneTree = scene.tree().xdgSurfaceCreate(toplevel.base)
        val tytop = TyToplevel(toplevel, sceneTree)

        // Event handlers for the base Surface
        with(toplevel.base.surface.events) {
            tytop.onMapListener = map.add(::onXdgToplevelMap)
            tytop.onUnmapListener = unmap.add(::onXdgToplevelUnmap)
            tytop.onCommitListener = commit.add(::onXdgToplevelCommit)
        }

        // Event handlers for the XDG Toplevel surface
        with(toplevel.events) {
            tytop.onDestroyListener = destroy.add(::onXdgToplevelDestroy)
            tytop.onRequestMoveListener = requestMove.add(::onXdgToplevelRequestMove)
            tytop.onRequestResizeListener = requestResize.add(::onXdgToplevelRequestResize)
            tytop.onRequestMaximizeListener = requestMaximize.add(::onXdgToplevelRequestMaximize)
            tytop.onRequestFullscreenListener = requestFullscreen.add(::onXdgToplevelRequestFullscreen)
        }
        TOPLEVELS.add(tytop)
    }


    fun onXdgToplevelCommit(listener: Listener, surface: Surface) {
        val xdgToplevel = TOPLEVELS.find { it.onCommitListener == listener }!!.xdgToplevel

        // When an xdg_surface performs an initial commit, the compositor must reply with a configure so the
        // client can map the surface. tinywl configures the xdg_toplevel with 0,0 size to let the client pick
        // the dimensions itself.
        if (xdgToplevel.base.getInitialCommit())
            xdgToplevel.setSize(0, 0)
    }


    fun onXdgToplevelMap(listener: Listener) {
        val tytoplevel = TOPLEVELS.find { it.onMapListener == listener }!!
        focusToplevel(tytoplevel)
    }


    fun onXdgToplevelUnmap(listener: Listener) {
        val tyToplevel = TOPLEVELS.find { it.onUnmapListener == listener }!!

        // Reset the cursor mode if the grabbed toplevel was unmapped
        if (grabbedToplevel?.xdgToplevel == tyToplevel.xdgToplevel)
            resetCursorMode()
    }


    fun onXdgToplevelDestroy(listener: Listener) {
        val idx = TOPLEVELS.indexOfFirst { it.onDestroyListener == listener }
        TOPLEVELS.get(idx).apply {
            onMapListener.remove()
            onUnmapListener.remove()
            onCommitListener.remove()
            onDestroyListener.remove()

            onRequestMoveListener.remove()
            onRequestResizeListener.remove()
            onRequestMaximizeListener.remove()
            onRequestFullscreenListener.remove()
        }
        TOPLEVELS.removeAt(idx)
    }


    fun onXdgToplevelRequestMove(event: XdgToplevel.MoveEvent) {
        // Fix: Clients (using winit Rust library) trying to initiate drag after mouse button has been released
        if (!seat.validatePointerGrabSerial(seat.pointerState.focusedSurface!!, event.serial))
            return
        beginInteractive(
            TOPLEVELS.find { it.xdgToplevel == event.toplevel }!!,
            CursorMode.Move,
            null
        )
    }


    fun onXdgToplevelRequestResize(event: XdgToplevel.ResizeEvent) {
        if (!seat.validatePointerGrabSerial(seat.pointerState.focusedSurface!!, event.serial))
            return

        beginInteractive(
            TOPLEVELS.find { it.xdgToplevel == event.toplevel }!!,
            CursorMode.Resize,
            event.edges
        )
    }


    // Client wants to fullscreen itself, but we don't support that.
    fun onXdgToplevelRequestFullscreen(listener: Listener) {
        TOPLEVELS.find { it.onRequestFullscreenListener == listener }?.let {
            if (it.xdgToplevel.base.getInitialized())
                it.xdgToplevel.base.scheduleConfigure()
        }
    }


    // Client wants to maximize itself, but we don't support that. Just send configure, by xdg-shell protocol
    // specification.
    fun onXdgToplevelRequestMaximize(listener: Listener) {
        val xdgToplevel = TOPLEVELS.find { it.onRequestMaximizeListener == listener }!!.xdgToplevel
        if (xdgToplevel.base.getInitialized())
            xdgToplevel.base.scheduleConfigure()
    }


    // *** XDG Shell: Popups ****************************************************************************** //


    fun onNewXdgPopup(popup: XdgPopup) {
        val parent = XdgSurface.tryFromSurface(popup.parent) ?: error("Popup's parent can't be null")

        // Have to search both TOPLEVELS and POPUPS, because there can be nested popups (ie. popup whose parent is a popup itself.
        val parentSceneTree = TOPLEVELS.find { it.xdgToplevel.base == parent }?.sceneTree
            ?: POPUPS.find { it.xdgPopup.base == parent }!!.sceneTree

        val popupSceneTree = parentSceneTree.xdgSurfaceCreate(popup.base)

        POPUPS.add(
            TyPopup(popup, popupSceneTree).apply {
                onCommitListener = popup.base.surface.events.commit.add(::onXdgPopupCommit)
                onDestroyListener = popup.events.destroy.add(::onXdgPopupDestroy)
            })
    }


    fun onXdgPopupCommit(listener: Listener, surface: Surface) {
        val popup = POPUPS.find { it.onCommitListener == listener }?.xdgPopup
            ?: error("Cant proceed without popup")
        if (popup.base.getInitialCommit())
            popup.base.scheduleConfigure()
    }


    fun onXdgPopupDestroy(listener: Listener) {
        val idx = POPUPS.indexOfFirst { it.onDestroyListener == listener }
        POPUPS.get(idx).apply {
            onCommitListener.remove()
            onDestroyListener.remove()
        }
        POPUPS.removeAt(idx) // Mandatory!!!
    }


    // *** Seat events ************************************************************************************ //


    fun onSeatRequestSetCursor(event: PointerRequestSetCursorEvent) {
        val focusedClient = seat.pointerState.focusedClient
        if (focusedClient == event.seatClient)
            cursor.setSurface(event.surface, event.hotspotX, event.hotspotY)
    }


    fun onSeatRequestSetSelection(event: RequestSetSelectionEvent) {
        seat.setSelection(event.source, event.serial)
    }


    fun onSeatDestroy(seat: Seat) {
        seatRequestSetCursorListener.remove()
        seatRequestSetSelectionListener.remove()
        seatDestroyListener.remove()
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
}


inline fun error(block: () -> Any): Nothing = error(block())