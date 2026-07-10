package cn.didi.union.models;

import cn.didi.union.errors.ErrorBase;

public final class Result<T> {
    private final boolean success;
    private final ErrorBase error;
    private final T model;
    private Result(Builder<T> builder) { this.success = builder.success; this.error = builder.error; this.model = builder.model; }
    public boolean isSuccess() { return success; }
    public ErrorBase getError() { return error; }
    public T getModel() { return model; }
    public static final class Builder<T> {
        private boolean success; private ErrorBase error; private T model;
        private Builder() { }
        public static <K> Builder<K> builder() { return new Builder<>(); }
        public Builder<T> success(boolean value) { success = value; return this; }
        public Builder<T> error(ErrorBase value) { error = value; return this; }
        public Builder<T> model(T value) { model = value; return this; }
        public Result<T> build() { return new Result<>(this); }
    }
}
