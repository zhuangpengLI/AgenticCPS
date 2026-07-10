package cn.didi.union.enums;

public enum ErrorCodeEnum {
    PARAM_ERROR(10000), BIZ_ERROR(20000), KNOWN_ERROR(30000), SERVER_ERROR(40000);
    private final int value;
    ErrorCodeEnum(int value) { this.value = value; }
    public int getValue() { return value; }
}
