package astrotwin;

enum Variable {
    // Planets
    ASCENDANT("Ascendant", 2.0), SUN("Sun", 3.0), MOON("Moon", 3.0), 
    MERCURY("Mercury", 1.5), VENUS("Venus", 1.5), MARS("Mars", 1.5),
    JUPITER("Jupiter", .5), SATURN("Saturn", .5), URANUS("Uranus", .5), 
    NEPTUNE("Neptune", .25), PLUTO("Pluto", .25),
    // Match Type
    ZODIAC("Zodiac", 1.0), ELEMENT("Element", .5), MODE("Mode", .25),
    // House Multiplier
    HOUSE("House", 1.2);
    String name;
    public Double initValue;

    private Variable(String name, Double initValue) {
        this.name = name;
        this.initValue = initValue;
    }
}
