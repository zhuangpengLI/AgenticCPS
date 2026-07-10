package cn.didi.union.errors;

public class KnownError extends ErrorBase {
    public KnownError(String message) { super(30000, message); }
}
