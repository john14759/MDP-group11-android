package com.example.mdp_group11.enums;

public enum FaceDirection {
    NORTH,
    SOUTH,
    WEST,
    EAST,
    NONE;

    public int toInt(){
        switch (this) {
            case NORTH: return 0;
            case EAST: return 2;
            case SOUTH: return 4;
            case WEST: return 6;
            default: return -1;
        }
    }
}


