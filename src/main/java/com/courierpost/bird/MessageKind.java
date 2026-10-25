package com.courierpost.bird;

public enum MessageKind {
    GLOBAL,
    WHISPER,
    TEAM;

    public static MessageKind byName(String name) {
        for (MessageKind kind : values()) {
            if (kind.name().equalsIgnoreCase(name)) {
                return kind;
            }
        }
        return GLOBAL;
    }
}
