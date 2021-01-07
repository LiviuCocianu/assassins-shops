package io.github.idoomful.assassinseconomy.data.SQL;

public interface Callback<T> {
    public void done(T result);
}
