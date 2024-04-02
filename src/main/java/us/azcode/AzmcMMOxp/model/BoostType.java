package us.azcode.AzmcMMOxp.model;

public class BoostType {
    private final String name;
    private final double multiplier;
    private final int duration;
    private final double price;

    public BoostType(String name, double multiplier, int duration, double price) {
        this.name = name;
        this.multiplier = multiplier;
        this.duration = duration;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public int getDuration() {
        return duration;
    }

    public double getPrice() {
        return price;
    }
}
