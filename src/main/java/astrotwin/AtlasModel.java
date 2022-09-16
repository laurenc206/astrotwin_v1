package astrotwin;

import java.io.BufferedInputStream;
import java.io.IOException;

public class AtlasModel {
    private String longitude;
    private String latitude;
    private String zone;
    private String town;
    private String country;

    // Takes astrolog output when chart calls getLocation and 
    // parses output to create location model used for chart calculations
    public AtlasModel(String town, String country, int year) throws InterruptedException, IOException, IllegalStateException {
        Boolean locationFound = false;
        this.town = town;
        this.country = country;
        String placeIdentifyStr;
        if (States.stateMap.containsKey(country)) {
            placeIdentifyStr = States.stateMap.get(country);
        } else {
            placeIdentifyStr = country;
        }
        System.out.println("Location searching: " + town + ", " + country);

        String output = getLocationData(GlobalConst.DEFAULT_NAME, town, year);
        String[] lines = output.split("\n");
            
        for (int i = 1; i < lines.length; i++) { // first line is just a header and not part of results         
            String line = lines[i].replaceAll("^\\s+[\\d]+:\\s", "");          
            String[] tokens = line.split(",", 2);
            if (tokens.length != 2) {
                System.out.println("Results Format Error: Cant split using \",\" between town and (state), country (lat long, timezone)");
                System.out.println("Line is: " + lines[i]);
                throw new IOException();
            }

            String townStr = tokens[0];
            String remaining = tokens[1];
            String[] remainingTokens = remaining.split("[(|)]", 2);
            if (remainingTokens.length != 2) {
                System.out.println("Results format error: Can't split between country and (lat, long, timezone)");
                System.out.println("Line is: " + lines[i]);
                throw new IOException();
            }

            String countryStr = remainingTokens[0];
            if (townStr.equalsIgnoreCase(town) && (countryStr.contains(placeIdentifyStr))) {
                
                // this line is a match so extract polar coordinates and time zone     
                String polarStr = remainingTokens[1].replace(")", "");               
                String removePunct = polarStr.replace(",", "");
                String[] polarTokens = removePunct.split("\\s+");
                if (polarTokens.length != 3) {
                    System.out.println("Polar Format Error: Should have (lat long, timezone)");
                    System.out.println(polarStr);
                    throw new IOException();
                } else {
                    this.longitude = polarTokens[0];
                    this.latitude = polarTokens[1];
                    this.zone = polarTokens[2];
                    System.out.println("Location selected: " + line);
                    locationFound = true;
                    break;
                }
            }
        }

        if (!locationFound) {
            System.out.println("No cities match " + town + ", " + country + " (" + placeIdentifyStr + ")");
            System.out.print(output);
            throw new IllegalStateException();
        }
    }

    public String getLocationData(String name, String town, int birthYear) throws InterruptedException, IOException {
        String[] args = {System.getProperty("user.dir").concat(GlobalConst.ASTROLOG_FPATH), 
                         "-e", "-e", "-zi", name, town,
                         "-qy", String.valueOf(birthYear) ,"-N"};
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
        return sb.toString();
    }

    public AtlasModel(String latitude, String longitude, String zone, String town, String country) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.zone = zone;
        this.town = town;
        this.country = country;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getZone() {
        return zone;
    }

    public String getTown() {
        return town;
    }

    public String getCountry() {
        return country;
    }

    public boolean isValid() {
        return longitude != null && latitude != null && zone != null && town != null && country != null;
    }

    public String toString() {
        if (this.latitude != null && this.longitude != null && this.zone != null) {
            return this.town + ", " + this.country +  ", " + this.latitude + ", " + this.longitude + ", " + this.zone;
        } else {
            return this.town + ", " + this.country;
        }
    }
}