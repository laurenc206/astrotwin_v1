package astrotwin;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;
import java.time.LocalDateTime;

public class DBService {
    /**
     * Execute the specified command on the database query connection
     * @throws SQLException
     * @throws IOException
     */
  public static String execute(Query q, String command) throws IOException {
    String[] tokens = tokenize(command.trim());
    String response;

    if (tokens.length == 0) {
      response = "Please enter a command";
    }
        
    // insert user and their chart into tables
    else if (tokens[0].equals("insert")) {
      try {
        System.out.println("Enter birth day and location to calculate chart: ");
        if (tokens.length == 1) {
          Person user = getBirthInput(GlobalConst.DEFAULT_NAME);
          response = q.insertUser(user);
        } else {
          StringBuilder name = new StringBuilder("");
          for (int i = 1; i < tokens.length; i++) {
            name.append(tokens[i] + " ");
          }
          Person user = getBirthInput(name.toString().trim());
          response = q.insertUser(user);
        }
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
      } 
      response = "Format Error: remove <id>";
    } 

    else if (tokens[0].equals("setVars")) {
      response = q.setVar(tokens[1], Double.valueOf(tokens[2]));
    }

    else if (tokens[0].equals("getVars")) {
      response = q.getVars();
    }
    
    //else if (tokens[0].equals("getPlanetMultipliers")) {
    //  try {
    //    return q.getPlanetMult();
    //  } catch (SQLException e) {
    //    e.printStackTrace();
    //  }
    //  response = "Error retrieving planet multipliers";
    //}

    //else if (tokens[0].equals("setPlanetMultiplier")) {
    //  if (tokens.length == 3) {
    //    Planet planet = Planet.valueOf(tokens[1].toUpperCase());
     //   Float value = Float.parseFloat(tokens[2]);
     //   if (planet != null) {
     //     return q.setPlanetMult(planet, value);
     //   } else {
     //     return "Invalid planet name";
     //   }
     // }
     // response = "Format Error: set <planet> <value>";
    //}

    else if (tokens[0].equals("getMatches")) {
      if (tokens.length == 2) {
        try {
          Integer userID = Integer.parseInt(tokens[1]);
          return q.calculateMatches(userID);
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      response = "Format Error: getMatches <userID>";
    } 
    
    else if (tokens[0].equals("viewMatch")) {
      if (tokens.length == 3) {
        Integer userID = Integer.parseInt(tokens[1]);
        Integer celebID = Integer.parseInt(tokens[2]);
        try {
          return q.displayMatch(userID, celebID);
        } catch (SQLException | InterruptedException | IOException e) {
          e.printStackTrace();
        }
        response = "Failure to view matches";
      }
      response = "Format Error: viewMatch <id>";
    } 
    
    //else if (tokens[0].equals("getChartMultipliers")) {
    //  return q.getMult();
    //}

    //else if (tokens[0].equals("setChartMultiplier")) {
    //  if (tokens.length == 3) {
    //    String component = tokens[1];
    //    Double value = Double.parseDouble(tokens[2]);
    //    return q.setMult(component,  value);
    //  } else {
    //    return "Format Error: setChartMultiplier <house/mode/element> <value>";
    //  }
    //} 
    
    else if (tokens[0].equals("viewUsers")) {
      return q.getUsers();
    
    
    } else if (tokens[0].equals("modifiers")) {
      return modifierMenu(q);
    }

    // quit
    else if (tokens[0].equals("quit")) {
      try {
        q.removeSessionData();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      response = "Goodbye\n";
    }

    // unknown command
    else {
      response = "Error: unrecognized command '" + tokens[0] + "'\n";
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
      System.out.println("> insert <name>");
      System.out.println("> getMatches <userID>");
      System.out.println("> viewMatch <userID> <celebID>");
      System.out.println("> viewUsers");
      //System.out.println("> datacrawl");
      //System.out.println("> removeUser <id>");
      System.out.println("> modifiers");

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

  private static String modifierMenu(Query q) throws IOException {
    System.out.println();
    System.out.println("Matches are calculated by adding up each planet that matches the user's chart and celeb's chart");
    System.out.println("Each planet is assigned a value. The higher a planets multiplier, the more weight is given to that match");
    System.out.println("Planets can match as being the same zodiac, mode OR element (if there is not a match the value = 0)");
    System.out.println("Finally, planets that share a componenet(zodiac, mode or element) get additional points for being in the same house");
    System.out.println("Thus matches are ranked by summing over (planetMultiplier * componentMultiplier * houseMultiplier)");
    System.out.println("for every planet in both charts");
    System.out.println();
    System.out.println("Use this menu to modify the importance placed on specific chart components or planets");
    System.out.println("* Warnings can be overridden by re-enterring the same value twice");
    while (true) {
      System.out.println("> getVars");
      System.out.println("> setVars");
      //System.out.println("> getPlanetMultipliers");
      //System.out.println("> getChartMultipliers");
      //System.out.println("> setMultiplier");
      System.out.println("> back");

      BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("> ");
      String command = r.readLine();

      if (command.equals("back")) {
        break;
      } else if (command.equalsIgnoreCase("setVars")) {
        String setCommand = getSetString(q);
        String response = execute(q, setCommand);
        System.out.println(response);
      } else if (command.equalsIgnoreCase("getVars")) {
        String response = execute(q, command);
        System.out.println(response);
      } else {
        System.out.println("Please enter a valid response");
        continue;
      }
    }
    return "";
  }

  private static String getSetString(Query q) throws IOException {
    StringBuilder sb = new StringBuilder("");
    boolean overrideValue = false;
    double prevValue = 0; 
    while (true) {
      boolean validCommand = false;
      System.out.print("Enter planet, Component['zodiac', 'mode', 'element'] or 'house': ");
      BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
      String command = r.readLine();

      if (command == null) {
        System.out.println("Please enter valid multiplier\n");
      }
      for (Variable v : Variable.values()) {
        if (command.equalsIgnoreCase(v.name)) {
          sb.append("setVars " + command);
          validCommand = true;
          break;
        }
      }

      if (validCommand) {
        break;
      } else {
        System.out.println("Please enter a valid multiplier\n");
      }
    }

    while (true) {
      System.out.print("Value: ");    
      BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
      String value = r.readLine();

      if (value == null) {
        System.out.println("Please enter a value");
      } 

      try {
        String command = sb.substring(8, sb.length());
        double d = Double.parseDouble(value);
        
        if (overrideValue && Double.compare(d, prevValue) == 0) {
          // values can be overridden if they are entered twice;
          sb.append(" " + d);
          break;
        } else if (d < 0) {
          // variables shouldn't be negative 
          System.out.println("Warning: Variables should not be negative");
          overrideValue = true;
          prevValue = d;
          continue;
        } else if ((command.equalsIgnoreCase("mode") || command.equalsIgnoreCase("element"))
                    && q.lessThanZodiac(d)) {
          System.out.println("Warning: Mode and Element variable should be less than Zodiac (Zodiac matches are made up of mode and element matches)");
          overrideValue = true;
          prevValue = d;
          continue;
        } else if (d < 1 && command.equalsIgnoreCase("house")) {
          // having the same house should not reduce match value
          System.out.println("Warning: House value shoud be greater than 1 (Otherwise value of match will be decreased if in the same house)");
          overrideValue = true;
          prevValue = d;
          continue;
        } else {
          sb.append(" " + d);
          break;
        }
      } catch (NumberFormatException e) {
        System.out.println("Please enter a valid value");
      }
    }
    System.out.println(sb.toString());
    return sb.toString();
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
