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
    else if (tokens[0].equals("createChart")) {
      try {
        System.out.println("Enter birth day and location to calculate chart: ");
        Person user = getBirthInput(GlobalConst.DEFAULT_NAME);
        response = q.insertUser(user);
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        response = "Error creating person object";
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
      System.out.println("> createChart");
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

  private static Person getBirthInput(String name) throws InterruptedException, IOException {
    LocalDateTime bday = getBdayDateTime();
    AtlasModel location = getBdayLocation();

    if (location == null) {
      System.out.println("Unable to complete location look-up");
      return null;
    } else {
      return new Person(name, bday, location);
    }
  }

  private static LocalDateTime getBdayDateTime() {
    BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
    LocalDateTime result = LocalDateTime.MIN;
    StringBuilder prompt = new StringBuilder("");
    
    while (result.equals(LocalDateTime.MIN)) {
      int year; 
      int month;
      int date;
      int hour;
      int minute;
      // Year Input
      prompt.append("\t> Year: ");
      int promptLen = prompt.length();
      prompt.append(" ".repeat(GlobalConst.PROMPT_LEN - promptLen));
      while (true) {
        System.out.print(prompt); 
        String yearStr;
        try {
          yearStr = r.readLine();
          if (Integer.valueOf(yearStr) > 0 && Integer.valueOf(yearStr) <= GlobalConst.CURRENT_YEAR) {
            year = Integer.valueOf(yearStr);
            break;
          } else {
            System.out.println("\tEnter a valid year between 0 and " + GlobalConst.CURRENT_YEAR);
          }
        } catch (IOException e) {
          System.out.print("I/O error occured. Try again");
        }
      }

      // Month Input
      prompt.delete(0, prompt.length());
      prompt.append("\t> Month: ");
      promptLen = prompt.length();
      prompt.append(" ".repeat(GlobalConst.PROMPT_LEN - promptLen));
      while (true) {
        System.out.print(prompt); 
        String monthStr;
        try {
          monthStr = r.readLine();
          if (Integer.valueOf(monthStr) > 0 && Integer.valueOf(monthStr) <= 12) {
            month = Integer.valueOf(monthStr);
            break;
          } else {
            System.out.println("\tEnter a valid month between 1 and 12");
          }
        } catch (IOException e) {
          System.out.print("I/O error occured. Try again");
        }
      }

      // Date Input
      prompt.delete(0, prompt.length());
      prompt.append("\t> Date: ");
      promptLen = prompt.length();
      prompt.append(" ".repeat(GlobalConst.PROMPT_LEN - promptLen));
      while (true) {
        System.out.print(prompt); 
        String dateStr;
        try {
          dateStr = r.readLine();
          int daysInMonth = getMaxDays(month, year);
          if (Integer.valueOf(dateStr) > 0 && Integer.valueOf(dateStr) <= daysInMonth) {
            date = Integer.valueOf(dateStr);
            break;
          } else {
            System.out.println("\tEnter a valid date between 1 and " + daysInMonth + " for month " + month);
          }
        } catch (IOException e) {
          System.out.print("I/O error occured. Try again");
        }
      }

      // Hour Input
      prompt.delete(0, prompt.length());
      prompt.append("\t> Hour (between 0-23): ");
      promptLen = prompt.length();
      prompt.append(" ".repeat(GlobalConst.PROMPT_LEN - promptLen));
      while (true) {
        System.out.print(prompt);
        String hourStr;
        try {
          hourStr = r.readLine();
          if (Integer.valueOf(hourStr) >= 0 && Integer.valueOf(hourStr) <= 23) {
            hour = Integer.valueOf(hourStr);
            break;
          } else {
            System.out.println("\tEnter a valid hour between 0 and 23");
          }
        } catch (IOException e) {
          System.out.print("I/O error occured. Try again");
        }
      }

      // Minute Input
      prompt.delete(0, prompt.length());
      prompt.append("\t> Minute: ");
      promptLen = prompt.length();
      prompt.append(" ".repeat(GlobalConst.PROMPT_LEN - promptLen));
      while (true) { 
        System.out.print(prompt);
        String minuteStr;
        try {
          minuteStr = r.readLine();
          if (Integer.valueOf(minuteStr) >= 0 && Integer.valueOf(minuteStr) <= 59) {
            minute = Integer.valueOf(minuteStr);
            break;
          } else {
            System.out.println("\tEnter a valid minute between 0 and 59");
          }    
        } catch (IOException e) {
          System.out.print("I/O error occured. Try again");
        }
      }
      result = LocalDateTime.of(year, month, date, hour, minute);
    }

    return result;
  }

  private static AtlasModel getBdayLocation() {
    BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
    String town;
    String country;
    String locationSpecifier;
    AtlasModel result = null;
    StringBuffer prompt = new StringBuffer("");

    prompt.append("\t> Town: ");
    int promptLen = prompt.length();
    prompt.append(" ".repeat(GlobalConst.PROMPT_LEN - promptLen));
    while (true) {
      System.out.print(prompt);
      try {
        town = r.readLine();
        break;
      } catch (IOException e1) {
        System.out.println("I/O error try again");
      }
    }

    prompt.delete(0, prompt.length());
    prompt.append("\t> Country: ");
    promptLen = prompt.length();
    prompt.append(" ".repeat(GlobalConst.PROMPT_LEN - promptLen));
    while (true) {
      System.out.print(prompt);
      try {
        country = r.readLine();
        break;
      } catch (IOException e) {
        System.out.println("I/O error try again");
      }
    }
  
    if (country.equalsIgnoreCase("USA") || 
      country.equalsIgnoreCase("United States") || 
      country.equalsIgnoreCase("United States of America") ||
      country.equalsIgnoreCase("America") ||
      country.equalsIgnoreCase("Canada") ||
      country.equalsIgnoreCase("CA")) {
        
        prompt.delete(0, prompt.length());
        prompt.append("\t> State/Provence: ");
        promptLen = prompt.length();
        prompt.append(" ".repeat(GlobalConst.PROMPT_LEN - promptLen));
        while (true) {
          System.out.print(prompt);
          try {
            locationSpecifier = r.readLine();
            break;
          } catch (IOException e) {
            System.out.println("I/O error try again");
          }
        }
    } else {
        locationSpecifier = country;
    }

    try {
      result = new AtlasModel(town, locationSpecifier, GlobalConst.CURRENT_YEAR);
    } catch (IOException e) {
      System.out.println();
      System.out.println("I/O error occured. Retry location look-up");
      result = getBdayLocation();
    } catch (InterruptedException e) {
      System.out.println();
      System.out.println("Error occurred. Retry location look-up");
      result = getBdayLocation();
    } catch (IllegalStateException e) {
      System.out.println();
      System.out.println("Retry location look-up");
      result = getBdayLocation();
    }

    return result;
  }

  private static int getMaxDays(int month, int year) {
    if (month == 2) {
      if (year % 4 == 0) {
        return 29;
      } else {
        return 28;
      }
    } else if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
      return 31;
    } else {
      return 30;
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
