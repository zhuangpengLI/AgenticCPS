package cn.didi.union.client;
public class DunionClientException extends RuntimeException {
    private final Integer httpStatus;
    public DunionClientException(String message, Throwable cause) { super(message, cause); this.httpStatus = null; }
    public DunionClientException(int httpStatus, String message) { super(message); this.httpStatus = httpStatus; }
    public Integer getHttpStatus() { return httpStatus; }
}
