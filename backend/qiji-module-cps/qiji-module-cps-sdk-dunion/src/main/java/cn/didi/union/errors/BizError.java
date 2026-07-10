package cn.didi.union.errors;
public class BizError extends ErrorBase { public BizError(int code, String message) { super(code, message); } public BizError(String message) { this(20000, message); } }
