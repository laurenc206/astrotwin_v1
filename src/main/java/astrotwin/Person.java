package astrotwin;

import java.io.IOException;
import java.time.LocalDateTime;

public class Person {
    public final String name;  // if a celebrity this will be name of celebrity otherwise "user"
    public final LocalDateTime birthDateTime;
    public final AtlasModel birthLocation;
    final Chart chart;

    public Person(String name, LocalDateTime birthDateTime, String town, String country) throws IOException, InterruptedException {
        this.name = name;
        this.birthDateTime = birthDateTime;
        this.birthLocation = new AtlasModel(town, country, birthDateTime.getYear());
        this.chart = new Chart(this);
    }

    public Person(String name, LocalDateTime birthDateTime, AtlasModel location) throws InterruptedException, IOException {
        this.name = name;
        this.birthDateTime = birthDateTime;
        this.birthLocation = location;
        this.chart = new Chart(this);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("");
        sb.append("Name: " + name + "\n");
        sb.append("Birthday: " + birthDateTime.toString() + "\n");
        sb.append("Chart: \n");
        sb.append(chart.toString());
        return sb.toString();
    }

    public String getYear() {
        return String.valueOf(birthDateTime.getYear());
    }

    public String getMonth() {
        return String.valueOf(birthDateTime.getMonthValue());
    }

    public String getDay() {
        return String.valueOf(birthDateTime.getDayOfMonth());
    }

    public String getTime() {
        StringBuilder sb = new StringBuilder("");
        sb.append(String.valueOf(birthDateTime.getHour()));
        sb.append(":");
        sb.append(String.valueOf(birthDateTime.getMinute()));
        return sb.toString();
    }

    public String getBirthday() {
        return this.birthDateTime.toString();
    }
 
    public String getBirthLocation() {
        return birthLocation.toString();
    }

    public String compareCharts(Person p) {
        StringBuilder sb = new StringBuilder("");
        for (Planet planet : Planet.values()) {
            Zodiac z1 = this.chart.signMap.get(planet);
            Zodiac z2 = p.chart.signMap.get(planet);
            sb.append(planet + "\n");
            sb.append(this.name + " : " + z1.toString() + "\t" + p.name + " : " + z2.toString() + "\n");
            if (z1.equals(z2)) {
                sb.append("Both are " + z1.toString() + "\n");
            } else if (z1.getElement().equals(z2.getElement())) {
                sb.append("Both are " + z1.getElement() + "\n");
            } else if (z1.getMode().equals(z2.getMode())) {
                sb.append("Both are " + z1.getMode()+ "\n");
            } else {
                sb.append("\n");
                continue;
            }
            
            if (this.chart.houseMap.get(planet).equals(p.chart.houseMap.get(planet))) {
                sb.append("Both have " + planet.toString() + " in the " + this.chart.houseMap.get(planet) + " house \n\n");
            } else {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}

