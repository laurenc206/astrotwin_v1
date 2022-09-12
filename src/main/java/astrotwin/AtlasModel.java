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
    public AtlasModel(String town, String country, int year) throws IOException, InterruptedException {
        this.town = town;
        this.country = country;
        String placeIdentifyStr;
        if (States.stateMap.containsKey(country)) {
            placeIdentifyStr = States.stateMap.get(country);
        } else {
            placeIdentifyStr = country;
        }

        String output = getLocationData(GlobalConst.DEFAULT_NAME, town, year);
        String[] lines = output.split("\n");
            
        for (int i = 1; i < lines.length; i++) { // first line is just a header and not part of results
            String[] tokens = lines[i].split("[(|)]");
                
            String location = tokens[0].replaceAll("^\\s+[\\d]+:\\s", "");
            String[] locTokens = location.split(",", 2);

            if (locTokens.length == 2) {
                if (locTokens[0].equalsIgnoreCase(town) && (locTokens[1].contains(placeIdentifyStr)) && tokens.length > 1) {                              
                    String removePunct = tokens[1].replace(",", "");
                    String[] polarTokens = removePunct.split("\\s+");
                    if (polarTokens.length == 3) {
                        this.longitude = polarTokens[0];
                        this.latitude = polarTokens[1];
                        this.zone = polarTokens[2];
                        break;
                    } else {
                        System.out.println("Polar Format Error: Should have (<lat> <long>, <timezone>)");
                        System.out.println(tokens[1]);
                        throw new IllegalStateException();
                    }
                } else {
                    System.out.println("No cities match " + town + ", " + country + " (" + placeIdentifyStr + ")");
                    System.out.println("Results: ");
                    System.out.print(output);
                    throw new IllegalStateException();
                }
                
            } else {
                System.out.println("Results in wrong format- cant be split using , between town, (state), country (<lat> <long>, <timezone>)");
                System.out.println("Results: ");
                System.out.print(output);
                throw new IllegalStateException();
            }
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