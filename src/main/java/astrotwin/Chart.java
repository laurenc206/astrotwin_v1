package astrotwin;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.imageio.IIOException;

public class Chart {
    Person person;
    // planet maps
    public final Map<Planet, Zodiac> signMap;
    public final Map<Planet, Integer> houseMap;
    private final Map<Integer, Zodiac> cuspMap;
    // chart analysis
    //public final Map<ChartComponent, Double> percentMap;
    public final List<ChartNode> modePercent;
    public final List<ChartNode> elementPercent;
    public final List<ChartNode> zodiacPercent;
    public final List<ChartNode> planetPercent;



    public Chart(Person p) throws InterruptedException, IOException {
        if (p.birthLocation.getLatitude() == null || p.birthLocation.getLongitude() == null || p.birthLocation.getZone() == null) {
            throw new IllegalStateException("Atlas location is null. Unable to retrieve chart");
        }
        this.person = p;
        this.signMap = new HashMap<>();
        this.houseMap = new HashMap<>();
        this.cuspMap = new HashMap<>();

        this.modePercent = new ArrayList<>(Mode.values().length);
        this.elementPercent = new ArrayList<>(Element.values().length);
        this.zodiacPercent = new ArrayList<>(Zodiac.values().length);
        this.planetPercent = new ArrayList<>(Planet.values().length);

        //this.percentMap = new HashMap<>();
        
        String inputStr = getChartData();
        System.out.println(inputStr);
        String[] astrologInput = inputStr.split(System.lineSeparator());
        System.out.println(inputStr);
        if (astrologInput.length < 2) throw new IIOException("Unable to retrieve chart from astrolog");
        
        extractPlanetMap(astrologInput);
        extractAnalysis(astrologInput);
    }


    public String getChartData() throws InterruptedException, IOException  {
        String[] args = {System.getProperty("user.dir").concat(GlobalConst.ASTROLOG_FPATH),  
                            "-v", "-j", "-qa", person.getMonth(), person.getDay(), person.getYear(), person.getTime(), 
                            person.birthLocation.getZone(), person.birthLocation.getLongitude(), person.birthLocation.getLatitude()};
        
        StringBuilder sb = new StringBuilder("");
        ProcessBuilder pb = new ProcessBuilder(args);
        Process process = pb.start(); 
        int waitFlag = process.waitFor();
        if (waitFlag == 0) {
            if (process.exitValue() == 0) {
                BufferedInputStream in = (BufferedInputStream) process.getInputStream();
                byte[] contents = new byte[1024];
                int bytesRead = 0;

                while ((bytesRead = in.read(contents)) != -1) {
                    sb.append(new String(contents, 0, bytesRead));
                }
            }
        }
        System.out.println(sb.toString());
        return sb.toString();
    }

    private void extractPlanetMap(String[] astrologInput) {
        // set chart planets -> zodiac
        if (!astrologInput[1].substring(0, 4).equalsIgnoreCase("body")) {
                throw new IllegalStateException("Chart string format: 'body' is ".concat(astrologInput[2].substring(0, 4)));
        }
        for (int i = 3; i < 15; i ++) {
            String cuspAbv = astrologInput[i].substring(astrologInput[i].length() - 5, astrologInput[i].length() - 2);
            Zodiac cusp = getAbvEnum(Zodiac.values(), cuspAbv);
            cuspMap.put(i - 2, cusp);

            if (i != GlobalConst.CHART_NODE_IDX) {
                String planetAbv = astrologInput[i].substring(0, 3);
                String zodiacAbv = astrologInput[i].substring(8, 11);
                String house = astrologInput[i].substring(29, 31);
    
                Planet planet = getAbvEnum(Planet.values(), planetAbv);
                Zodiac zodiac = getAbvEnum(Zodiac.values(), zodiacAbv);
    
                signMap.put(planet, zodiac);
                houseMap.put(planet, Integer.parseInt(house.trim()));

            } 
        }

        System.out.println("Signs");
        for (Map.Entry<?, ?> e : signMap.entrySet()) {
            System.out.println(e.getKey() + " = " + e.getValue());
        }
        System.out.println("House");
        for (Map.Entry<?, ?> e : houseMap.entrySet()) {
            System.out.println(e.getKey() + " = " + e.getValue());
        }
        System.out.println("Cusp");
        for (Map.Entry<?, ?> e : cuspMap.entrySet()) {
            System.out.println(e.getKey() + " = " + e.getValue());
        }

    }

    private void extractAnalysis(String[] astrologInput) {
        extractPlanet(astrologInput);
        extractZodiac(astrologInput);
        extractModes(astrologInput);
        extractElement(astrologInput);
    }

    private void extractPlanet(String[] astrologInput) {
        if (!astrologInput[GlobalConst.PLANET_IDX - 1].substring(2, 8).equalsIgnoreCase("planet")) {
            throw new IllegalStateException("Chart string format: 'planet' is ".concat(astrologInput[25].substring(2, 8)));
        }
        
        for (int i = GlobalConst.PLANET_IDX; i < GlobalConst.PLANET_IDX + GlobalConst.NUM_SIGN; i ++) {
            // North node we dont use
            if (i - GlobalConst.PLANET_IDX == GlobalConst.NORTH_IDX) continue;
            
            String[] fragments = astrologInput[i].split(":");
            String planet = fragments[0].trim();
            String[] equation = fragments[1].split("/");
            String percent = equation[1].replaceAll("\\s+|%", "");
      
            //Ascendant last letter gets cut off
            if (i - GlobalConst.PLANET_IDX == GlobalConst.ASC_IDX) planet = planet.concat("t");
            planetPercent.add(new ChartNode(Planet.valueOf(planet.toUpperCase()), Double.valueOf(percent)));
        }
        Collections.sort(planetPercent, (n2, n1) -> n1.value.compareTo(n2.value));
    }

    private void extractZodiac(String[] astrologInput) {
        if (!astrologInput[GlobalConst.ZODIAC_IDX - 1].substring(7, 11).equalsIgnoreCase("sign")) {
            throw new IllegalStateException("Chart string format: 'sign' is ".concat(astrologInput[41]).substring(7, 11));
        }
        
        for (int i = GlobalConst.ZODIAC_IDX; i < GlobalConst.ZODIAC_IDX + GlobalConst.NUM_SIGN; i ++) {
            // Zodiacs
            String[] fragments = astrologInput[i].split("%");
            String[] zodiacsStr = fragments[0].split("[:|/]");
            String zodiac = zodiacsStr[0].trim();
            String percent = zodiacsStr[2].trim();

            zodiacPercent.add(new ChartNode(Zodiac.valueOf(zodiac.toUpperCase()), Double.valueOf(percent)));
        }
        Collections.sort(zodiacPercent, (n2, n1) -> n1.value.compareTo(n2.value));
    }
    
    private void extractElement(String[] astrologInput) {
        for (int i = GlobalConst.ELEM_IDX; i < GlobalConst.ELEM_IDX + Element.values().length; i ++) {

            String[] fragments = astrologInput[i].split("%");
            if (fragments.length < 2) {
                throw new IllegalStateException("Chart String format issue Elements");
            }
            String[] elementStr = fragments[1].split("[:|/]");
            String element = elementStr[0].replaceAll("\\s+|-", "");
            String percent = elementStr[elementStr.length - 1].trim();

            elementPercent.add(new ChartNode(Element.valueOf(element.toUpperCase()), Double.valueOf(percent)));
        }
        Collections.sort(elementPercent, (n2, n1) -> n1.value.compareTo(n2.value));
    }

    private void extractModes(String[] astrologInput) {
        for (int i = GlobalConst.MODE_IDX; i < GlobalConst.MODE_IDX + Mode.values().length; i ++) {

            String[] fragments = astrologInput[i].split("%");
            if (fragments.length < 2) {
                throw new IllegalStateException("Chart String format issue Mode");
            }
            String[] modeStr = fragments[1].split("[:|/]");
            String mode = modeStr[0].replaceAll("\\s+|-", "");
            String percent = modeStr[modeStr.length - 1].trim();

            modePercent.add(new ChartNode(Mode.valueOf(mode.toUpperCase()), Double.valueOf(percent)));
        }
        Collections.sort(modePercent, (n2, n1) -> n1.value.compareTo(n2.value));
    }


    private <T extends Enum<T> & SignComponent> T getAbvEnum(T[] enumVals, String abv) {
        T retVal = null;
        for (T x : enumVals) {
            if (x.abv().equals(abv)) {
                if (retVal == null) {
                        retVal = x;
                } else {
                        throw new IllegalArgumentException("More than one SignComponent matches abv");
                }
            }
        }
        if (retVal == null) {
            throw new IllegalStateException("SignComponent is null for chart abv");
        }
        return retVal;
    }

    public Map<Planet, Zodiac> getSigns() {
        return this.signMap;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("");        
        sb.append(person.birthLocation.toString() +  "\n");
        
        for (Map.Entry<Planet, Zodiac> entry : signMap.entrySet()) {
            sb.append(entry.getKey() + " = " + entry.getValue());
            sb.append("\n");
        }
        sb.append("Planets:\n");
        for (ChartNode n : planetPercent) {
            sb.append(n.node.toString() + " = " + n.value + "\n");
        }
        sb.append("Zodiacs:\n");
        for (ChartNode n : zodiacPercent) {
            sb.append(n.node.toString() + " = " + n.value + "\n");
        }
        sb.append("Modes:\n");
        for (ChartNode n : modePercent) {
            sb.append(n.node.toString() + " = " + n.value + "\n");
        }
        sb.append("Elements:\n");
        for (ChartNode n : elementPercent) {
            sb.append(n.node.toString() + " = " + n.value + "\n");
        }
        return sb.toString();
    }

}

