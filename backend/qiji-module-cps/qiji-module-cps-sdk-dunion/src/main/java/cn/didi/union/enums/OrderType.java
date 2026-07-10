package cn.didi.union.enums;
public enum OrderType { OnlineCar("online_car"), Energy("energy"), Freight("freight"), KingFlower("king_flower"), Daijia("daijia"), All(""); private final String value; OrderType(String value) { this.value = value; } public String getValue() { return value; } }
