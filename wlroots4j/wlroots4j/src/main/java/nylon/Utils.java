package nylon;

import java.lang.reflect.Array;
import java.util.Arrays;


public class Utils {

    /// Given a concrete enumeration with ".value" field, return its value
    public static <T extends Enum<T>> int getValueFromEnumeration(Class<T> enumClass, T enumeration) {
        try {
            return enumClass.getDeclaredField("value").getInt(enumeration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /// Useful hack - create an array lookup table for a given enum, so it's faster to look up an enumeration
    /// when given an int from C code. This should be used only from a `static {}` block.
    ///
    /// It should be only used for enums with smaller number of enumerations - dozens? Otherwise, use an
    /// iterator approach (or IntMap)
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T[] createLookupTableFromEnumClass(Class<T> enumClass) {
        var allEnums = enumClass.getEnumConstants();

        // Find the largest .value of all enum constants so that I can size the lookup array
        var largestEnumValue = Arrays
            .stream(allEnums)
            .mapToInt(enm -> getValueFromEnumeration(enumClass, enm))
            .max()
            .orElseThrow();

        // Create the lookup array to contain enums
        var lookupTable = (T[]) Array.newInstance(enumClass, largestEnumValue + 1);

        // Populate the table: enumeration.value -> enumeration
        for (var e : allEnums) {
            lookupTable[getValueFromEnumeration(enumClass, e)] = e;
        }
        return lookupTable;
    }
}