package astrotwin;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

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

    public String compareCharts(Person p, EnumMap<Variable, Double> variableMap) {
        StringBuilder sb = new StringBuilder("");
        List<String> colHeader = new ArrayList<>(Arrays.asList("Planet", this.name, p.name, "Planet Value", "Match Value", "House Value", "Total"));
        // save variables for equations so we dont have to look-up every match
        Double zodiacVal = variableMap.get(Variable.valueOf("Zodiac".toUpperCase()));
        Double modeVal = variableMap.get(Variable.valueOf("Mode".toUpperCase()));
        Double elemVal = variableMap.get(Variable.valueOf("Element".toUpperCase()));
        Double houseVal = variableMap.get(Variable.valueOf("House".toUpperCase()));
        Double total = 0.0;
        sb.append(getLine(colHeader) + "\n");
        
        for (Planet planet : Planet.values()) {
            boolean match = false;
            
            List<String> col = new ArrayList<>();
            col.add(planet.toString());
            //StringBuilder equation = new StringBuilder("");
            Double planetValue = variableMap.get(Variable.valueOf(planet.toString().toUpperCase()));
            Double colTotal = planetValue;
            //equation.append(planetValue + " (planet value) * ");
            //sb.append(planet + " (value: " + planetValue + ")\n");


            Zodiac userSign = this.chart.signMap.get(planet);
            col.add(userSign.toString());
            Zodiac celebSign = p.chart.signMap.get(planet);
            col.add(celebSign.toString());
            col.add(planetValue.toString());
            //List<String> col = new ArrayList(Arrays.asList(this.name, z1.toString(), p.name, z2.toString()));
            //sb.append(getLine(col));
            if (userSign.equals(celebSign)) {
                col.add(zodiacVal.toString() + " (zodiac)");
                colTotal *= zodiacVal;
                match = true;
                //sb.append("Both are " + z1.toString() + "\n");
                //equation.append(zodiacVal + " (zodiac match) * ");
            } else if (userSign.getElement().equals(celebSign.getElement())) {
                col.add(elemVal.toString() + " (element)");
                colTotal *= elemVal;
                match = true;
                //sb.append("Both are " + z1.getElement() + "\n");
                //equation.append(elemVal + " (element match) * ");
            } else if (userSign.getMode().equals(celebSign.getMode())) {
                col.add(modeVal.toString() + " (mode)");
                colTotal *= modeVal;
                match = true;
                //sb.append(modeVal + " (mode match) * ");
            } else {
                col.add("");
                colTotal *= 0;
                //equation.append("0 (no match) * "); 
                //sb.append("\n");
                //continue;
            }
            
            if (match && this.chart.houseMap.get(planet).equals(p.chart.houseMap.get(planet))) {
                //sb.append("Both have " + planet.toString() + " in the " + this.chart.houseMap.get(planet) + " house \n\n");
                //equation.append(hosueVal + " (house match)");
                col.add(houseVal.toString() + " (" + this.chart.houseMap.get(planet) + " house)");
                colTotal *= houseVal;

            } else {
                col.add("");
                //colTotal *= 0;
            }

            if (colTotal > 0) {
                col.add(colTotal.toString());
            }
            
            sb.append(getLine(col) + "\n");
            total += colTotal;
        }
        sb.append(" ".repeat(6 * GlobalConst.ONE_MATCH_LEN) + total);
        return sb.toString();
    }

    private String getLine(List<String> columnVals) {
        StringBuilder retStr = new StringBuilder();
        for(String col : columnVals) {
            StringBuilder cell = new StringBuilder("");
            cell.append(col);
            int len = cell.length();
            cell.append(" ".repeat(GlobalConst.ONE_MATCH_LEN - len));
            retStr.append(cell);
        }
        retStr.append("\n");
        return retStr.toString();
    }
}

