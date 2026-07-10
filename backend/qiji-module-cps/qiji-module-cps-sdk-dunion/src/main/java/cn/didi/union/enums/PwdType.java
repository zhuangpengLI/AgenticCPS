package cn.didi.union.enums;

public enum PwdType {
    Coupon("coupon");
    private final String value;
    PwdType(String value) { this.value = value; }
    public String getValue() { return value; }
}
