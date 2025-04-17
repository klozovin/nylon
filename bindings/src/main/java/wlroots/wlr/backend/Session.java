package wlroots.wlr.backend;

import java.lang.foreign.MemorySegment;

/// A session manages access to physical devices (such as GPUs and input devices).
///
/// A session is only required when running on bare metal (e.g. with the KMS or libinput backends).
///
/// The session listens for device hotplug events, and relays that information via the add_drm_card
/// event and the change/remove events on struct wlr_device. The session provides functions to gain
/// access to physical device (which is a privileged operation), see wlr_session_open_file(). The
/// session also keeps track of the virtual terminal state (allowing users to switch between
/// compositors or TTYs), see wlr_session_change_vt() and the active event.
public final class Session {
    public final MemorySegment sessionPtr;

    public Session(MemorySegment sessionPtr) {
        this.sessionPtr = sessionPtr;
    }
}