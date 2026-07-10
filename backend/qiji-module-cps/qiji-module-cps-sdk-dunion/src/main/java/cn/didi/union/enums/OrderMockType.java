package cn.didi.union.enums;
public enum OrderMockType { MockPay(0), MockRefund(1); private final int value; OrderMockType(int value) { this.value = value; } public int getValue() { return value; } }
