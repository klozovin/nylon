package wlroots.types.input;

import org.jspecify.annotations.NullMarked;

import java.util.EnumSet;

import static jextract.wlroots.wlr.*;


@NullMarked
public enum KeyboardModifier {
    Control(WLR_MODIFIER_CTRL()),
    Alt(WLR_MODIFIER_ALT()),
    Shift(WLR_MODIFIER_SHIFT()),
    CapsLock(WLR_MODIFIER_CAPS()),

    Mod2(WLR_MODIFIER_MOD2()),
    Mod3(WLR_MODIFIER_MOD3()),
    Mod5(WLR_MODIFIER_MOD5()),

    Logo(WLR_MODIFIER_LOGO());


    public final int value;
    private static final KeyboardModifier[] enumerations = KeyboardModifier.values();


    KeyboardModifier(int value) {
        this.value = value;
    }


    public boolean containedIn(int bitset) {
        return (this.value & bitset) != 0;
    }


    public static EnumSet<KeyboardModifier> fromBitset(int bitset) {
        var set = EnumSet.noneOf(KeyboardModifier.class);
        for (var enumeration : enumerations) {
            if (enumeration.containedIn(bitset))
                set.add(enumeration);
        }
        return set;
    }
}