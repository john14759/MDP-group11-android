package com.example.mdp_group11.enums;

public enum SettingEnvironment {

    NULL(0),
    INSIDE(1),
    OUTSIDE(2);
    private final int value;

    private SettingEnvironment(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
