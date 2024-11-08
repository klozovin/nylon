package dewey

import io.github.jwharm.javagi.gobject.types.Types
import org.gnome.gio.ListModel
import org.gnome.glib.Type
import org.gnome.gobject.GObject
import java.lang.foreign.MemorySegment




class UnitListModel<T: GObject>(addres: MemorySegment) : ListModel<T>, GObject(addres) {
    override var size: Int = 0

//    companion object {
//        fun<T: GObject> newInstance(count: Int): UnitListModel<T> {
//            val model = newInstance<UnitListModel<T>>(gtype)
//            model.size = count
//            return model
//        }
//
//        val gtype: Type<T: GObject> = Types.register<UnitListModel<T>, ObjectClass>(UnitListModel::class.java)
//    }

}

// TODO: Implement as singleton object?
class UnitIndex(address: MemorySegment) : GObject(address) {
    var index: Int = 0

    fun getGType(): Type? = Companion.gtype

    companion object {
        val gtype = Types.register<UnitIndex, ObjectClass>(UnitIndex::class.java)

        fun newInstance(value: Int): UnitIndex {
            val instance: UnitIndex = newInstance(gtype)
            instance.index = value
            return instance
        }
    }
}

