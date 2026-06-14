package drm;

import static jextract.drm.fourcc.*;


/// FourCC code
public enum DrmFormat {
    ARGB8888(DRM_FORMAT_ARGB8888());

    public final int value;


    DrmFormat(int value) {
        this.value = value;
    }


    public static DrmFormat of(int value) {
        for (var e : values())
            if (e.value == value)
                return e;
        throw new RuntimeException("Invalid enum value from C code for DRM_FORMAT_####");
    }
}