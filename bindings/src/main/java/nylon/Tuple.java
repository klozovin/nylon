package nylon;


import org.jspecify.annotations.NullMarked;

@NullMarked
public class Tuple {

    // TODO: tuple equality

    public static <A, B> Tuple2<A, B> of(A a, B b) {
        return new Tuple2<>(a, b);
    }


    public static <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
        return new Tuple3<>(a, b, c);
    }


    public static <A, B, C, D> Tuple4<A, B, C, D> of(A val1, B val2, C val3, D val4) {
        return new Tuple4<>(val1, val2, val3, val4);
    }


    ///  Pair
    public static class Tuple2<A, B> {
        public final A _1;
        public final B _2;


        public Tuple2(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }


        public A component1() {
            return _1;
        }


        public B component2() {
            return _2;
        }
    }


    ///  Triplet
    public static class Tuple3<A, B, C> extends Tuple2<A, B> {
        public final C _3;


        public Tuple3(A a, B b, C c) {
            super(a, b);
            this._3 = c;
        }


        public C component3() {
            return _3;
        }
    }


    ///  Quartet
    public static class Tuple4<A, B, C, D> extends Tuple3<A, B, C> {
        public final D _4;


        public Tuple4(A _1, B _2, C _3, D _4) {
            super(_1, _2, _3);
            this._4 = _4;
        }


        public D component4() {
            return _4;
        }
    }


//
//    public static class Tuple5<A, B, C, D, E> {
//
//    }
}