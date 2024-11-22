package dewey

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.*
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible


/**
 * Instead of Java Optional<> class, because RxJava can't handle null values.
 */
sealed interface Maybe<V : Any> {

    class None<V : Any> : Maybe<V> {
        override fun toString(): String = "None"
    }

    @JvmInline
    value class Just<V : Any>(val value: V) : Maybe<V>

    companion object {
        fun <V : Any> of(value: V?): Maybe<V> {
            return if (value != null) Just(value)
            else None()
        }
    }
}

/**
 * Non-nullable.
 */
class SubjectDelegate<T : Any>(initial: T? = null) {

    val observable get() = subject

    private var variable: T?
    private val subject = BehaviorSubject.create<T>()

    init {
        variable = initial
        if (variable != null) {
            subject.onNext(initial)
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return variable!!
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        subject.onNext(value)
        variable = value
    }

    companion object {
        fun <V : Any> KMutableProperty0<V>.asObservable(): Observable<V> {
            this.isAccessible = true
            val delegate = this.getDelegate() as SubjectDelegate<V>
            return delegate.observable
        }
    }
}

/**
 * Can be null, use Maybe to interop with RxJava.
 */
class NullableSubjectDelegateMaybe<T : Any>(initial: T? = null) {

    val observable get() = subject

    private var variable: T? = null
    private val subject = BehaviorSubject.create<Maybe<T>>()

    init {
        // TODO: Maybe if initialized with null push that as well (otherwise some widgets might be uninitialized.
        if (initial != null) {
            subject.onNext(Maybe.of(initial))
            variable = initial
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return variable
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        subject.onNext(Maybe.of(value))
        variable = value
    }

    companion object {
        fun <V : Any> KMutableProperty0<V?>.asObservable(): Observable<Maybe<V>> {
            this.isAccessible = true
            val delegate = this.getDelegate() as NullableSubjectDelegateMaybe<V>
            return delegate.observable
        }
    }
}


/**
 * Can be null, use java.util.Optional to interop with RxJava
 */
class NullableSubjectDelegateOptional<T : Any> {

    val observable get() = subject

    private var variable: T? = null
    private val subject = BehaviorSubject.create<Optional<T>>()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return variable
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        subject.onNext(Optional.ofNullable(value))
        variable = value
    }

    companion object {
        fun <V : Any> KMutableProperty0<V?>.asObservable(): Observable<Optional<V>> {
            this.isAccessible = true
            val delegate = this.getDelegate() as NullableSubjectDelegateOptional<V>
            return delegate.observable
        }
    }
}


/*


Usecase1

class Usecase1 {
    var selectedItem: DirectoryEntry
}
use1 = Usecase1
use1.selectedItem = blabha

=====================

class Usecase2 {
    val selectedItem: ObservableWrapper<DirectoryEntry>
}

use2 = Usecase2
use2.selectedItem() // invoke operator overload


 */