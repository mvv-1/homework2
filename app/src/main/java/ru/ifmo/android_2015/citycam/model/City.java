package ru.ifmo.android_2015.citycam.model;

/**
 * Город
 */
public final class City {

    /**
     * Название
     */
    public final String name;

    /**
     * Широта
     */
    public final double latitude;

    /**
     * Долгота
     */
    public final double longitude;


    public City(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "City[name=\"" + name + "\" lat=" + latitude + " lon=" + longitude + "]";
    }
}
