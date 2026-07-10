package cn.didi.union.errors;

public class ErrorBase {
    private int code;
    private String message;
    public ErrorBase() { }
    public ErrorBase(int code, String message) { this.code = code; this.message = message; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
