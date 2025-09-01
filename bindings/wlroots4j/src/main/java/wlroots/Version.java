package wlroots;

import static jextract.wlroots.version_h.*;


public final class Version {
    public static final String STR = WLR_VERSION_STR().getString(0);

    public static final int MAJOR = WLR_VERSION_MAJOR();
    public static final int MINOR = WLR_VERSION_MINOR();
    public static final int MICRO = WLR_VERSION_MICRO();

    public static final int NUM = WLR_VERSION_NUM();
}