package astrotwin;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class DataCrawl {  
    // Both maps have celebrity names as key (with - to denote spaces)
    
    // returns the set of celebs added to charts and bios
    public List<Person> tellTalesMostPopular() throws IOException {
        List<String> names = new ArrayList<>();

        final String url = "https://www.telltalesonline.com/26925/popular-celebs/";
        try {
            if (GlobalConst.USE_NETWORK) {
                Document response = Jsoup.connect(url).get();
                Elements nameElements = response.select("div[data-id=\"19057e6e\"] h2");

            
                for (Element e : nameElements) {
                    String name = e.text().replaceAll("^[\\d]+.\\s", "");
                    names.add(name);
                }


            } else {
                // Sample set including names with 1-3 spaces, repeat names, fictional names and names of ancient women
                //names.add("Elizabeth Blackwell");
                //names.add("Elizabeth Cady Stanton");
                //names.add("Madonna");
                //names.add("Diana, Princess of Wales"); // includes comma and multiple words (not names)
                //names.add("Artemisia");     // ancient greek 
                //names.add("Molly Pitcher"); // fictional woman
                //names.add("Bruce Willis");
                //names.add("Megan Fox");
                names.add("Jennifer Love Hewitt");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return createPersons(names);
        
    }

    private List<Person> createPersons(List<String> names) {
        List<Person> personsAdded = new ArrayList<>();
        Random rand = new Random();
        int i = 0;
        for (String name : names) {
            System.out.println(name);
            try {      
                Thread.sleep((long) rand.nextDouble(5, 10) * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            // Loop through names and format <LastName>,_<FirstName>_<OptionalMiddle>
            String nameFormatted = formatName(name);
            System.out.println(nameFormatted);
            // Send url request
            Document response;
            try {
                response = Jsoup.connect(String.format(GlobalConst.CELEB_CHART, nameFormatted)).get();
            } catch (IOException e1) {
                System.out.println("Unable to search astro-databank for " + nameFormatted);
                continue; // Go onto next name in list
            }
            Elements elements = response.select("td");
              
            // Save strings from response with information we need to extract data to create Birthday for Person
            try {
            Person celeb = getPerson(name, elements);

                if (celeb != null) {
                    personsAdded.add(celeb);
                    i++;
                    System.out.println(name + " added");
                } else {
                    System.out.println(name + " discarded");
                }
            } catch (Exception e1) {
                System.out.println("Unable to add person encountered error while creating Person");
                e1.printStackTrace();
            }

            if (i > 10) {
                break;
            }

        }
        return personsAdded;
    }

    private String formatName(String name) {
        String temp = name.replaceAll("\\p{Punct}", "");
        String[] surnames = temp.split(" ");
        StringBuilder nameFormatted = new StringBuilder("");
        if (surnames.length > 1) {
            nameFormatted.append(surnames[surnames.length -1]); // get last name
            nameFormatted.append(",");
            for (int i = 0; i < surnames.length -1; i++) {
                nameFormatted.append("_" + surnames[i]);
            }
        } else {
            nameFormatted.append(surnames[0]);
        }
        return nameFormatted.toString();
    }

    private Person getPerson(String name, Elements elements) throws Exception {
        // Save strings from response with information we need to extract data to create Birthday for Person
        boolean savePlace = false;
        boolean saveDate = false;
        boolean saveTimezone = false;
        StringBuilder dateStr = new StringBuilder("");
        StringBuilder placeStr = new StringBuilder("");
        StringBuilder timezoneStr = new StringBuilder("");
        for (Element e : elements) {
            if (e.hasText()) {
                if (e.text().equals("Place")) {
                    savePlace = true; // Following line contains place information
                } else if (e.text().equals("born on")) {
                    saveDate = true; // Following line contains birthdate and time information
                } else if (e.text().equals("Timezone")) {
                    saveTimezone = true;
                } else if (saveDate) {
                    dateStr.append(e.text());
                    System.out.println("datestr: " + dateStr.toString());
                    saveDate = false;
                } else if (savePlace) {
                    placeStr.append(e.text());
                    System.out.println("placestr: " + placeStr.toString());
                    savePlace = false;
                } else if (saveTimezone) {
                    timezoneStr.append(e.text());
                    System.out.println("timezonestr: " + timezoneStr.toString());
                    break;
                }     
            }
        }
                

        LocalDateTime birthDateTime = extraDateTime(dateStr.toString());
        String[] placeTokens = placeStr.toString().split("\\,");
        String[] timeTokens = timezoneStr.toString().split(" ");

        if (placeTokens.length >= 2 && birthDateTime != null) {
            String town = placeTokens[0];
            String country = placeTokens[1];

            if (placeTokens.length == 4 && timeTokens.length >= 2) {
                // can create Atlas model using coordinates for better accuracy - ie dont have to do location search for chart
                String latitude = placeTokens[2].trim();
                String longitude = placeTokens[3].trim();
                String timezone = timeTokens[1];
                AtlasModel birthLocation = new AtlasModel(latitude, longitude, timezone, town, country);
                return new Person(name, birthDateTime, birthLocation);
            } else {
                return new Person(name, birthDateTime, town, country);
            }  
        
        } else {
            return null;
        }
    }

    private LocalDateTime extraDateTime(String s) {
        if (s.isEmpty()) return null;
        String[] tokens = s.split(" ");
        if (tokens.length > 4) {
            int day = Integer.parseInt(tokens[0]);
            Month month = Month.valueOf(tokens[1].toUpperCase().trim());
            int year = Integer.parseInt(tokens[2]);
            String[] timeTokens = tokens[4].split(":");
            if (timeTokens.length == 2) {
                int hour = Integer.parseInt(timeTokens[0]);
                int minute = Integer.parseInt(timeTokens[1]);
                return LocalDateTime.of(year, month, day, hour, minute);
            }      
        }
        return null;
    }
 
}
