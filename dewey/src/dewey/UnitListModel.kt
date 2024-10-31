package dewey

import io.github.jwharm.javagi.gobject.types.Types
import org.gnome.gio.ListModel
import org.gnome.glib.Type
import org.gnome.gobject.GObject
import java.lang.foreign.MemorySegment

class UnitListModel(addres: MemorySegment) : ListModel, GObject(addres) {
    var size: Int = 0

    companion object {
        fun newInstance(count: Int): UnitListModel {
            val model = newInstance<UnitListModel>(gtype)
            model.size = count
            return model
        }

        val gtype = Types.register<UnitListModel, ObjectClass>(UnitListModel::class.java)
    }

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