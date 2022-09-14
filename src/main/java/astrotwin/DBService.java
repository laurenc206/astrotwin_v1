package astrotwin;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.Month;

public class DBService {

    /**
     * Execute the specified command on the database query connection
     * @throws SQLException
     * @throws IOException
     */
  public static String execute(Query q, String command) {
    String[] tokens = tokenize(command.trim());
    String response;

    if (tokens.length == 0) {
      response = "Please enter a command";
    }
        
    // insert user and their chart into tables
    else if (tokens[0].equals("insert")) {
      if (tokens.length == 8) {
        int year = Integer.parseInt(tokens[1]);
        Month month = Month.of(Integer.parseInt(tokens[2]));
        int day = Integer.parseInt(tokens[3]);
        int hour = Integer.parseInt(tokens[4]);
        int minute = Integer.parseInt(tokens[5]);

        String town = tokens[6].replace("-", " ");
        String country = tokens[7].replace("-", " ");
        
        LocalDateTime bday = LocalDateTime.of(year, month, day, hour, minute);
        System.out.println(bday.toString());
        try {
          Person user = new Person("user", bday, town, country);
          response = q.insertUser(user);
        } catch (IOException | InterruptedException e) {
          e.printStackTrace();
          response = "Error creating person object";
        }      
      } else {
        response = "Error: Please provide birthday and birthtime";
      }
    }

      // datacrawl
    else if (tokens[0].equals("datacrawl")) {
        DataCrawl d = new DataCrawl();
        try {
          List<Person> celebs = d.tellTalesMostPopular();
          for (Person celeb : celebs) {
            System.out.println("Inserting: " + celeb.name);
            q.insertCeleb(celeb);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        response = "Success";
    }

    else if (tokens[0].equals("removeUser")) {
      if (tokens.length == 2) {
        int id = Integer.parseInt(tokens[1]);
        response = q.removeUser(id);  
      } else {
        response = "Format Error: remove <id>";
      }

    } else if (tokens[0].equals("getPlanetMultipliers")) {
      try {
        return q.getPlanetMult();
      } catch (SQLException e) {
        e.printStackTrace();
      }
      response = "Error retrieving planet multipliers";
    }

    else if (tokens[0].equals("setPlanetMultipliers")) {
      if (tokens.length == 3) {
        Planet planet = Planet.valueOf(tokens[1].toUpperCase());
        Float value = Float.parseFloat(tokens[2]);
        if (planet != null) {
          response = q.setPlanetMult(planet, value);
        } else {
          response = "Invalid planet name";
        }
      } else {
        response = "Format Error: set <planet> <value>";
      }
    }

    else if (tokens[0].equals("getMatches")) {
      try {
        return q.calculateMatches();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      response = "Error getting matches";
    }

    else if (tokens[0].equals("viewMatch")) {
      if (tokens.length == 2) {
        Integer id = Integer.parseInt(tokens[1]);
        try {
          return q.displayMatch(id);
        } catch (SQLException | InterruptedException | IOException e) {
          e.printStackTrace();
        }
        response = "Failure to view matches";
      } else {
        response = "Format Error: viewMatch <id>";
      }

    } else if (tokens[0].equals("getChartMultipliers")) {
      return q.getMult();
    }


    else if (tokens[0].equals("setChartMultiplier")) {
      if (tokens.length == 3) {
        String component = tokens[1];
        Double value = Double.parseDouble(tokens[2]);
        return q.setMult(component,  value);
      } else {
        return "Format Error: setChartMultiplier <house/mode/element> <value>";
      }
    }

        // quit
    else if (tokens[0].equals("quit")) {
      response = "Goodbye\n";
    }

        // unknown command
    else {
      response = "Error: unrecognized command '" + tokens[0] + "'";
    }

    return response;
  }


    /**
   * Establishes an application-to-database connection and runs the
   * application REPL
   * 
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, SQLException {
    /* prepare the database connection stuff */
    Query q = new Query();
    menu(q);
    q.closeConnection();
  }

    /**
   * REPL (Read-Execute-Print-Loop) for Flights application for the specified
   * application-to-database connection
   * 
   * @param q
   * @throws IOException
   */
  private static void menu(Query q) throws IOException {
    while (true) {
      // print the command options
      System.out.println();
      System.out.println(" *** Please enter one of the following commands *** ");
      System.out.println("> insert <year> <month> <date> <hour> <minute> <town> <country> if town or country are more than 1 word, sepeate words using - ");
      System.out.println("> datacrawl");
      System.out.println("> removeUser <id>");
      System.out.println("> getPlanetMultipliers");
      System.out.println("> setPlanetMultipliers <planet> <value>");
      System.out.println("> getChartMultipliers");
      System.out.println("> setChartMultipliers <house/mode/element> <value>");
      System.out.println("> getMatches");
      System.out.println("> viewMatch <id>");
      System.out.println("> quit");

      // read an input command from the REPL
      BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("> ");
      String command = r.readLine();

      // execute the given input command
      String response = execute(q, command);
      System.out.print(response);
      if (response.equals("Goodbye\n")) {
        break;
      }
    }
  }



  /**
   * Tokenize a string into a string array
   */
  private static String[] tokenize(String command) {
    String regex = "\"([^\"]*)\"|(\\S+)";
    Matcher m = Pattern.compile(regex).matcher(command);
    List<String> tokens = new ArrayList<>();
    while (m.find()) {
      if (m.group(1) != null)
        tokens.add(m.group(1));
      else
        tokens.add(m.group(2));
    }
    return tokens.toArray(new String[0]);
  }
    
}
