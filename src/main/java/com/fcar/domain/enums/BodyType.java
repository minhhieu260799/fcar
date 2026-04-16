package com.fcar.domain.enums;

public enum BodyType {
    SEDAN("Sedan"),
    HATCHBACK("Hatchback"),
    SUV("SUV"),
    CUV("CUV"),
    MPV_MINIVAN("MPV/Minivan"),
    PICKUP("Pickup"),
    COUPE("Coupe"),
    CONVERTIBLE("Convertible"),
    LIMOUSINE("Limousine"),
    OTHER("Other");

    private final String displayName;

    BodyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

