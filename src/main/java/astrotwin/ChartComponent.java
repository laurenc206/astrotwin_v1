package astrotwin;


interface ChartComponent {
        public String toString();
}

interface SignComponent extends ChartComponent{
    public String abv();
}

enum Element implements ChartComponent{
    FIRE("Fire"), EARTH("Earth"), AIR("Air"), WATER("Water");
    private String elementName;

    private Element(String element) {
        this.elementName = element;

    }

    @Override
    public String toString() {
        return elementName;
    }

 }

 enum Mode implements ChartComponent{
    CARDINAL("Cardinal"), FIXED("Fixed"), MUTABLE("Mutable");
    private String modeName;

    private Mode(String mode) {
        this.modeName = mode;
    }

    @Override 
    public String toString() {
        return this.modeName;
    }

} 

enum Zodiac implements SignComponent{
    ARIES ("Aries", Mode.CARDINAL, Element.FIRE), 
    TAURUS("Taurus", Mode.FIXED, Element.EARTH), 
    GEMINI("Gemini", Mode.MUTABLE, Element.AIR), 
    CANCER("Cancer", Mode.CARDINAL, Element.WATER), 
    LEO("Leo", Mode.FIXED, Element.FIRE), 
    VIRGO("Virgo", Mode.MUTABLE, Element.EARTH), 
    LIBRA("Libra", Mode.CARDINAL, Element.AIR), 
    SCORPIO("Scorpio", Mode.FIXED, Element.WATER), 
    SAGITTARIUS("Sagittarius", Mode.MUTABLE, Element.FIRE), 
    CAPRICORN("Capricorn", Mode.CARDINAL, Element.EARTH), 
    AQUARIUS("Aquarius", Mode.FIXED, Element.AIR), 
    PISCES("Pisces", Mode.MUTABLE, Element.WATER);
    private String zodiacName;
    private Mode mode;
    private Element element;

    private Zodiac(String zodiac, Mode mode, Element element) {
        this.zodiacName = zodiac;
        this.mode = mode;
        this.element = element;
    }

    @Override
    public String toString() {
        return zodiacName;
    }

    public String abv() {
        return zodiacName.substring(0, 3);
    }

    public String getMode() {
        return mode.toString();
    }

    public String getElement() {
        return element.toString();
    }
}

enum Planet implements SignComponent{
    ASCENDANT("Ascendant"), SUN("Sun"), MOON("Moon"), 
    MERCURY("Mercury"), VENUS("Venus"), MARS("Mars"),
    JUPITER("Jupiter"), SATURN("Saturn"), URANUS("Uranus"), 
    NEPTUNE("Neptune"), PLUTO("Pluto");
    private String planetName;


    private Planet(String planet) {
        this.planetName = planet;
    }

    @Override
    public String toString() {
        return planetName;
    }

    public String abv() {
        return planetName.substring(0, 3);
    }
 }
