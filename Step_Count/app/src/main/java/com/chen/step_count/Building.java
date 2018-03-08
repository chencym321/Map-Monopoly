package com.chen.step_count;

/**
 * Created by chen on 12/16/17.
 */

public class Building {
    double latitude;
    double longtitude;
    String name;
    int price;
    String owner;

    public Building(double latitude, double longtitude, String name, int price, String owner) {
        this.latitude = latitude;
        this.longtitude = longtitude;
        this.name = name;
        this.price = price;
        this.owner = owner;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return "Building{" +
                "latitude=" + latitude +
                ", longtitude=" + longtitude +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", owner='" + owner + '\'' +
                '}';
    }
}
