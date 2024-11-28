package wlroots;


public final class Version {

    public static final String VERSION_STR = version_h.WLR_VERSION_STR().getString(0);

    public static final int VERSION_MAJOR = version_h.WLR_VERSION_MAJOR();
    public static final int VERSION_MINOR = version_h.WLR_VERSION_MINOR();
    public static final int VERSION_MICRO = version_h.WLR_VERSION_MICRO();

    public static final int VERSION_NUM = version_h.WLR_VERSION_NUM();
}