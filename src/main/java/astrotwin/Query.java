package astrotwin;

import java.io.*;
import java.sql.*;
import java.util.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class Query {
    // Database Connection
    private Connection conn;
    static double totalPoints = 1;
    Map<Integer, Person> usersCreated;
    int sessionID;
    
    // multipliers we can change
    private static Double zodiacMult = GlobalConst.ZODIAC_MULT;
    private static Double elementMult = GlobalConst.ELEMENT_MULT;
    private static Double modeMult = GlobalConst.MODE_MULT;
    private static Double houseMult = GlobalConst.HOUSE_MULT;
    static EnumMap<Planet, Float> planetMult;

    // Canned queries
    // insert user
    private static final String INSERT_USER = "INSERT INTO Users(uid, username, bday, bplace) VALUES(?, ?, ?, ?)";
    private PreparedStatement insertUserStatement;
    private static final String REMOVE_USER = " DELETE FROM User_Charts WHERE id = ?" +
                                                " DELETE FROM Users WHERE uid = ?;";
    private PreparedStatement removeUserStatement;

    private static final String INSERT_CELEB = "INSERT INTO Celebs(cid, name, bday, bplace) VALUES(?, ?, ?, ?)";
    private PreparedStatement insertCelebStatement;

    private static final String MAX_CELEB_ID = "SELECT MAX(cid) AS maxid FROM Celebs;";
    private PreparedStatement maxCelebIDStatement;

    private static final String MAX_USER_ID = "SELECT MAX(uid) AS maxid FROM Users;";
    private PreparedStatement maxUserIDStatement;

    private static final String GET_SESSION = "SELECT MAX(sessionID) AS maxID FROM PlanetMultiplier";
    private PreparedStatement getSessionStatement;
  
    private static final String INSERT_CELEB_CHART = "INSERT INTO Celeb_Charts(id, planet, zodiac, element, mode, house) VAlUES (?, ?, ?, ?, ?, ?)";
    private PreparedStatement insertCelebChartStatement;

    private static final String INSERT_USER_CHART = "INSERT INTO User_Charts(id, planet, zodiac, element, mode, house) VAlUES (?, ?, ?, ?, ?, ?)";
    private PreparedStatement insertUserChartStatement;
 
    private static final String CALC_MATCHES = "WITH Multiplier AS (SELECT c1.id AS id1, c2.id AS id2, c1.planet AS planet," +
                                               " CASE WHEN c1.zodiac = c2.zodiac THEN ? " + 
                                               " WHEN c1.element = c2.element THEN ? "+ 
                                               " WHEN c1.mode = c2.mode THEN ? ELSE 0 END AS componentMult," +
                                               " CASE WHEN c1.house = c2.house THEN ? ELSE 1 END AS houseMult" +
                                               " FROM [dbo].[User_Charts] AS c1, [dbo].[Celeb_Charts] AS c2" +
                                               " WHERE c1.id = ? AND c1.planet = c2.planet AND (c1.zodiac = c2.zodiac OR c1.element = c2.element OR c1.mode = c2.mode))" +
                                               " SELECT id2, celebDB.name AS name, SUM(pm.val * componentMult * houseMult) AS total" +
                                               " FROM Multiplier AS m, [dbo].[PlanetMultiplier] AS pm, [dbo].[Celebs] AS celebDB" +
                                               " WHERE m.planet = pm.planet AND id2 = celebDB.cid" +
                                               " GROUP BY id2, celebDB.name" +
                                               " ORDER BY total DESC;";
    private PreparedStatement calcMatchesStatement;

    private static final String SET_PLANET_MULT = "UPDATE [dbo].[PlanetMultiplier] SET val = ? WHERE planet = ? AND sessionID = ?";
    private PreparedStatement setPlanetMultStatement;
    private static final String REMOVE_PLANET_MULT = "DELETE FROM PlanetMultiplier WHERE sessionID = ?";
    private PreparedStatement removePlanetStatement;
    private static final String INSERT_PLANET_MULT = "INSERT INTO PlanetMultiplier(planet, val, sessionID) VALUES (?, ?, ?)";
    private PreparedStatement insertPlanetMultStatement;

    private static final String GET_CELEB = "SELECT * FROM [dbo].[Celebs] WHERE cid = ?;";
    private PreparedStatement getCelebStatement;
    // For check dangling
    private static final String TRANCOUNT_SQL = "SELECT @@TRANCOUNT AS tran_count";
    private PreparedStatement tranCountStatement;
    
    private static final String DB_ERROR = "Database Error";

    public Query() throws SQLException, IOException {
        this(null, null, null, null);
    }

    protected Query(String serverURL, String dbName, String adminName, String password) throws SQLException, IOException {
        conn = dbName == null ? openConnectionFromDbConn() 
            : openConnectionFromCredential(serverURL, dbName, adminName, password);

        usersCreated = new HashMap<>();
        planetMult = new EnumMap<>(Planet.class);
        
        prepareStatements();
        sessionID = getSessionID();
        initializePlanetMultipliers();
        totalPoints = getTotal();
        System.out.println("total points: " + totalPoints);
    }

    /**
     * Return a connecion by using dbconn.properties file
     *
     * @throws SQLException
     * @throws IOException
     */
    public static Connection openConnectionFromDbConn() throws SQLException, IOException {
        // Connect to the database with the provided connection configuration
        Properties configProps = new Properties();
        configProps.load(new FileInputStream("dbconn.properties"));
        String serverURL = configProps.getProperty("app.server_url");
        String dbName = configProps.getProperty("app.database_name");
        String adminName = configProps.getProperty("app.username");
        String password = configProps.getProperty("app.password");

        return openConnectionFromCredential(serverURL, dbName, adminName, password);
    }

    /**
     * Return a connecion by using the provided parameter.
     *
     * 
     * @param dbName                    database name
     * @param instanceConnectionName    The instance connection (found on instance details page)
     * @param user                      MySQL username
     * @param password                  MySQL user's password
     *
     * @throws SQLException
     */
    protected static Connection openConnectionFromCredential(String serverURL, String dbName,
                                                             String adminName, String password) throws SQLException {
        String connectionUrl =
            String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30", serverURL,
                dbName, adminName, password);
        
        //String connectionUrl = "jdbc:sqlserver://celeb-astro.database.windows.net:1433;database=celeb_astro;user=LaurenC;password={Haley923};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";
        Connection conn = DriverManager.getConnection(connectionUrl);

        // By default, automatically commit after each statement
        conn.setAutoCommit(false);

        // By default, set the transaction isolation level to serializable
        conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    
        return conn;   
    }

    /**
     * Get underlying connection
     */
    public Connection getConnection() {
        return conn;
    }

    /**
     * Closes the application-to-database connection
     */
    public void closeConnection() throws SQLException {
        conn.close();
    }

    public void removeSessionData() throws SQLException {
        for (Integer ID: usersCreated.keySet()) {
            removeUser(ID);
        }
        removePlanetSession();
    }

    /*
     * prepare all the SQL statements in this method.
     */
    private void prepareStatements() throws SQLException {
        tranCountStatement = conn.prepareStatement(TRANCOUNT_SQL);
        insertUserStatement = conn.prepareStatement(INSERT_USER);
        removeUserStatement = conn.prepareStatement(REMOVE_USER);
        insertCelebStatement = conn.prepareStatement(INSERT_CELEB);
        maxCelebIDStatement = conn.prepareStatement(MAX_CELEB_ID);
        maxUserIDStatement = conn.prepareStatement(MAX_USER_ID);
        insertUserChartStatement = conn.prepareStatement(INSERT_USER_CHART);
        insertCelebChartStatement = conn.prepareStatement(INSERT_CELEB_CHART);
        calcMatchesStatement = conn.prepareStatement(CALC_MATCHES);
        setPlanetMultStatement = conn.prepareStatement(SET_PLANET_MULT);
        getCelebStatement = conn.prepareStatement(GET_CELEB);
        getSessionStatement = conn.prepareStatement(GET_SESSION);
        removePlanetStatement = conn.prepareStatement(REMOVE_PLANET_MULT);
        insertPlanetMultStatement = conn.prepareStatement(INSERT_PLANET_MULT);
    }

    private void initializePlanetMultipliers() throws SQLException {
        for (Planet p : Planet.values()) {
            insertPlanetMultStatement.clearParameters();
            insertPlanetMultStatement.setString(1, p.toString());
            insertPlanetMultStatement.setFloat(2, p.getMult());
            insertPlanetMultStatement.setInt(3, sessionID);
            insertPlanetMultStatement.execute();
            planetMult.put(p, p.getMult());
        }
        conn.commit();
    }

    /*
     * Inserts user into database. Since Person contains chart,
     * all chart information goes into tables (chart, percentages)
     * and user with user id is inserted into people 
     * 
     * returns unique id code for user to pass to frontend to retrieve information
     */
    public String insertUser(Person user) {  
        int id;
        try {
            id = getUniqueUserID();
            insertUserStatement.clearParameters();
            insertUserStatement.setInt(1, id);// get size of user table
            insertUserStatement.setString(2, user.name);
            insertUserStatement.setString(3, user.getBirthday());
            insertUserStatement.setString(4, user.getBirthLocation());
            insertUserStatement.execute();

            insertUserChart(user, id);
            conn.commit();
            usersCreated.put(id, user);
            return "Created user " + user.name + " with id: " + id + "\n";
        } catch (SQLException e) {
            if (isDeadLock(e)) {
                return insertUser(user);
            } else {
                try {
                  conn.rollback();
                } catch (SQLException e1) {
                  e1.printStackTrace();
                }
                e.printStackTrace();
                return "Inserting User failed\n";
            }
        } finally {
            checkDanglingTransaction();
        }
    }

    public String insertCeleb(Person celeb) {
        int id;
        try {
            id = getUniqueCelebID();
            insertCelebStatement.clearParameters();
            insertCelebStatement.setInt(1, id);// get size of user table
            insertCelebStatement.setString(2, celeb.name);
            insertCelebStatement.setString(3, celeb.getBirthday());
            insertCelebStatement.setString(4, celeb.getBirthLocation());
            insertCelebStatement.execute();
            insertCelebChart(celeb, id);
            conn.commit();
            return "Created celeb " + celeb.name + " with id: " + id + "\n";
        } catch (SQLException e) {
            if (isDeadLock(e)) {
                return insertCeleb(celeb);
            } else {
                try {
                  conn.rollback();
                } catch (SQLException e1) {
                  e1.printStackTrace();
                }
                e.printStackTrace();
                return "Inserting celeb failed\n";
            }
        } finally {
            checkDanglingTransaction();
        }
    }

    private void insertCelebChart(Person p, int id) throws SQLException {
        for (Planet planet : Planet.values()) {
            Zodiac sign = p.chart.signMap.get(planet);
            int house = p.chart.houseMap.get(planet);
            insertCelebChartStatement.clearParameters();
            insertCelebChartStatement.setInt(1, id);
            insertCelebChartStatement.setString(2, planet.toString());
            insertCelebChartStatement.setString(3, sign.toString());
            insertCelebChartStatement.setString(4, sign.getElement());
            insertCelebChartStatement.setString(5, sign.getMode());
            insertCelebChartStatement.setInt(6, house);

            insertCelebChartStatement.execute();
        }
    }

    private void insertUserChart(Person p, int id) throws SQLException {
        for (Planet planet : Planet.values()) {
            Zodiac sign = p.chart.signMap.get(planet);
            int house = p.chart.houseMap.get(planet);
            insertUserChartStatement.clearParameters();
            insertUserChartStatement.setInt(1, id);
            insertUserChartStatement.setString(2, planet.toString());
            insertUserChartStatement.setString(3, sign.toString());
            insertUserChartStatement.setString(4, sign.getElement());
            insertUserChartStatement.setString(5, sign.getMode());
            insertUserChartStatement.setInt(6, house);
            insertUserChartStatement.execute();
        }
    }

    private int getUniqueCelebID() throws SQLException {
        maxCelebIDStatement.clearParameters();
        ResultSet results = maxCelebIDStatement.executeQuery();
        results.next();
        int maxid = results.getInt("maxid");
        results.close();
        return maxid + 1;
    }

    private int getUniqueUserID() throws SQLException {
        maxUserIDStatement.clearParameters();
        ResultSet results = maxUserIDStatement.executeQuery();
        results.next();
        int maxid = results.getInt("maxid");
        results.close();
        return maxid + 1;
    }

    public String calculateMatches(int userID) throws SQLException {
        if (!usersCreated.containsKey(userID)) {
            return "User with ID " + userID + " has not been created in this session";
        }     
        StringBuilder sb = new StringBuilder("");
        calcMatchesStatement.clearParameters();
        calcMatchesStatement.setDouble(1, zodiacMult);
        calcMatchesStatement.setDouble(2, elementMult);
        calcMatchesStatement.setDouble(3, modeMult);
        calcMatchesStatement.setDouble(4, houseMult);
        calcMatchesStatement.setInt(5, userID);
        ResultSet results = calcMatchesStatement.executeQuery();

        while(results.next()) {
            int id = results.getInt("id2");
            double sum = results.getDouble("total");
            String name = results.getString("name");
            double total = (sum / totalPoints) * 100;
            System.out.println("total points: " + totalPoints);
            sb.append("id: " + id + "\t name: " + name + "\t sum: " + sum + "\t percent match: " + total + " \n");  
        }
        results.close();
        return sb.toString();
    }

    public String displayMatch(int personID, int celebID) throws SQLException, InterruptedException, IOException {
        Person celeb = getCeleb(celebID);
        if (celeb == null) {
            return "Error creating celeb with " + celebID;
        }
        if (usersCreated.containsKey(personID)) {
            Person user = usersCreated.get(personID);
            return user.compareCharts(celeb);
        } 
        return "User hasn't been inserted into database yet";
    }

    private Person getCeleb(int celebID) throws SQLException, InterruptedException, IOException {
        Person celeb = null;
        getCelebStatement.clearParameters();
        getCelebStatement.setInt(1, celebID);
        ResultSet results = getCelebStatement.executeQuery();
        while(results.next() && celeb == null) {
            String bdayStr = results.getString("bday");
            String bplaceStr = results.getString("bplace");
            String name = results.getString("name");
            LocalDateTime date = LocalDateTime.parse(bdayStr);
            String[] tokens = bplaceStr.split(",");
            String town = tokens[0].trim();
            String country = tokens[1].trim();
            String latitude = tokens[2].trim();
            String longitude = tokens[3].trim();
            String timeZone = tokens[4].trim();
            AtlasModel location = new AtlasModel(latitude, longitude, timeZone, town, country);
            celeb = new Person(name, date, location);
        }
        results.close();
        return celeb;
    }

    public String removeUser(int personID) {
        try {
            removeUserStatement.clearParameters();
            removeUserStatement.setInt(1, personID);
            removeUserStatement.setInt(2, personID);
            removeUserStatement.execute();
            conn.commit();
            return "Successfully removed user " + String.valueOf(personID);  
        } catch (SQLException e) {
            if (isDeadLock(e)) {
                return removeUser(personID);
            } else {
                try {
                  conn.rollback();
                } catch (SQLException e1) {
                  e1.printStackTrace();
                }
                return "Error removing id: " + String.valueOf(personID);
            }
        } finally {
            checkDanglingTransaction();
        }     
    }

    public String getPlanetMult() throws SQLException {
        List<Node> list = new ArrayList<>();
        StringBuilder retStr = new StringBuilder("");
        for (Map.Entry<Planet, Float> entry : planetMult.entrySet()) {
            list.add(new Node(entry.getKey().toString(), (double) entry.getValue()));
        }
        Collections.sort(list, (n2, n1) -> n1.value.compareTo(n2.value));
        
        for (Node n : list) {
            StringBuilder line = new StringBuilder("");
            line.append(n.node);
            int sbLen = line.length();
            line.append(" ".repeat(GlobalConst.MULT_COL_LEN - sbLen));
            line.append(n.value + "\n");
            retStr.append(line);
        }
        return retStr.toString(); 
    }

    public String setPlanetMult(Planet planet, Float value) {
        try {
            setPlanetMultStatement.clearParameters();
            setPlanetMultStatement.setFloat(1, value);
            setPlanetMultStatement.setString(2, planet.toString());
            setPlanetMultStatement.setInt(3, sessionID);
            setPlanetMultStatement.execute();
            conn.commit();
            double oldValue = planetMult.get(planet);
            planetMult.put(planet, value);
            totalPoints = getTotal();
            return "Success updating planet " + planet.toString() + " from " + oldValue + " to value " + value.toString() + "\n";
        } catch (SQLException e) {
            if (isDeadLock(e)) {
                return setPlanetMult(planet, value);
            } else {
                try {
                  conn.rollback();
                } catch (SQLException e1) {
                  e1.printStackTrace();
                }
                return "Error modifying planet " + planet.toString() + " to value " + value.toString() + "\n";
            }
        } finally {
            checkDanglingTransaction();
        } 
    }

    public static String setMult(String multiplier, double value) {
        if (multiplier.equalsIgnoreCase("mode")) {
            double oldvalue = modeMult;
            modeMult = value;
            totalPoints = getTotal();
            return "Mode has been set from " + oldvalue + " to " + value + "\n";
        } else if (multiplier.equalsIgnoreCase("house")) {
            double oldvalue = houseMult;
            houseMult = value;
            totalPoints = getTotal();
            return "House has been set from " + oldvalue + " to " + value + "\n";
        } else if (multiplier.equalsIgnoreCase("element")) {
            double oldValue = elementMult;
            elementMult = value;
            totalPoints = getTotal();
            return "Element has been set from " + oldValue + " to " + value + "\n";
        } else if (multiplier.equalsIgnoreCase("zodiac")) {
            double oldValue = zodiacMult;
            zodiacMult = value;
            totalPoints = getTotal();
            return "Zodiac has been set from " + oldValue + " to " + value + "\n";
        } else {
            return "invalid input string must be [mode, house, element, zodiac] \n";
        }
    }

    public static String getMult() {
        List<Node> list = new ArrayList<>();
        list.add(new Node("Zodiac", zodiacMult));
        list.add(new Node("Mode", modeMult));
        list.add(new Node("Element", elementMult));
        list.add(new Node("House", houseMult));
        
        Collections.sort(list, (n2, n1) -> n1.value.compareTo(n2.value));
        StringBuilder retStr = new StringBuilder("");
        
        for (Node n : list) {
            StringBuilder line = new StringBuilder("");
            line.append(n.node);
            int sbLen = line.length();
            line.append(" ".repeat(GlobalConst.MULT_COL_LEN - sbLen));
            line.append(n.value + "\n");
            retStr.append(line);
        }

        return retStr.toString();
    }

    public static double getTotal() {
        double temp = 0;
        double componenetMult = Math.max(zodiacMult, Math.max(elementMult, modeMult));
        for (Map.Entry<Planet, Float> val : planetMult.entrySet()) {
            temp += val.getValue() * componenetMult * houseMult;
        }
        return temp;
    }

    public int getSessionID() throws SQLException {
        getSessionStatement.clearParameters();
        ResultSet results = getSessionStatement.executeQuery();
        int maxid = 0;
        if (results.next()) {
            maxid = results.getInt("maxid");
        }
        results.close();
        return maxid + 1;
    }

    public void removePlanetSession() throws SQLException {
        removePlanetStatement.clearParameters();
        removePlanetStatement.setInt(1, sessionID);
        removePlanetStatement.execute();
        conn.commit();
    }

    public String getUsers() {
        StringBuilder sb = new StringBuilder("");
        if (usersCreated.isEmpty()) {
            return "No users have been created in this session yet";
        }
        sb.append("ID \t Name \n");
        for (Map.Entry<Integer, Person> entry : usersCreated.entrySet()) {
            sb.append(entry.getKey() + " " + entry.getValue().name + "\n");
        } 
        return sb.toString();
    }
    /**
        * Throw IllegalStateException if transaction not completely complete, rollback.
        * 
    */
    private void checkDanglingTransaction() {
        try {
            try (ResultSet rs = tranCountStatement.executeQuery()) {
                rs.next();
                int count = rs.getInt("tran_count");
                if (count > 0) {
                    throw new IllegalStateException(
                    "Transaction not fully commit/rollback. Number of transaction in process: " + count);
                }
            } finally {
                conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(DB_ERROR, e);
        }
    }

    private static boolean isDeadLock(SQLException ex) {
        return ex.getErrorCode() == 1205;
    }
}
