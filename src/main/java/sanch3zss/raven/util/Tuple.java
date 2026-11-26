package sanch3zss.raven.util;

public record Tuple<A, B>(A a, B b) {
    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }
}
