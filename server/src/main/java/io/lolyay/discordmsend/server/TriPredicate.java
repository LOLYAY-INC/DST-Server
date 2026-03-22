package io.lolyay.discordmsend.server;

public interface TriPredicate<T, U, V> {
    boolean test(T t, U u, V v);
}
