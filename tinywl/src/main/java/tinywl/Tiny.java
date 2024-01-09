package tinywl;

//import jdk.incubator.foreign.CLinker;
//import jdk.incubator.foreign.FunctionDescriptor;
//import jdk.incubator.foreign.ResourceScope;
import java.lang.foreign.SymbolLookup;
//import tinywl.wlroots.util.log_h;
import static java.lang.System.out;
import java.lang.invoke.MethodType;
import java.net.SocketOption;
import java.util.List;
import java.util.PrimitiveIterator;

import tinywl.wlroots.version_h;

public class Tiny {

    public static void main(String[] args) throws Throwable {
        System.loadLibrary("wlroots");
        System.loadLibrary("wayland-server");

//        var linker = CLinker.getInstance();
//        var sysLookup = CLinker.systemLookup();
        var libLookup = SymbolLookup.loaderLookup();

        // Check whether we can resolve wayland-server/wlroots symbols
        var functions = List.of(
                "wl_display_run",
                "wl_display_create",
                "wl_display_destroy",
                "wl_display_get_event_loop",
                "wlr_seat_get_keyboard",
                "wlr_backend_autocreate",
                "wlr_log_init",
                "wlr_matrix_project_box",
                "wlr_log_importance",
                "wlr_log_get_verbosity"
        );
        for (var fn : functions) {
            var memoryAddress = libLookup.find(fn);
            System.out.println(fn + ": " + memoryAddress.isPresent());
        }
    }

//    private static void printPid(CLinker linker, SymbolLookup sysLookup) throws Throwable {
//        var getpid = linker.downcallHandle(
//                sysLookup.lookup("getpid").get(),
//                MethodType.methodType(int.class),
//                FunctionDescriptor.of(CLinker.C_INT));
//        System.out.println("PID: " + (int) getpid.invokeExact());
//    }
}