package astrotwin;

import java.io.*;
import java.sql.*;
import java.util.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

public class Query {
    // Database Connection
    private Connection conn;
    Map<Integer, Person> usersCreated;
    Map<Integer, Person> celebCache;
    int sessionID;
    Set<String> celebsInDatabase;
    
    // multipliers we can change
    private static EnumMap<Variable, Double> variableMap;
    

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
 
    private static final String CALC_MATCHES = "WITH CompareCelebs AS (SELECT c1.id AS id1, c2.id AS id2, c1.planet AS planet, pm.val AS planetVal," +
                                               " CASE WHEN c1.zodiac = c2.zodiac THEN ? WHEN c1. element = c2.element THEN ? WHEN c1.mode = c2.mode THEN ? ELSE 0 END AS matchMult," +
                                               " CASE WHEN c1.house = c2.house THEN ? ELSE 1 END AS houseMult" +
                                               " FROM User_Charts AS c1, Celeb_Charts AS c2, PlanetMultiplier AS pm" +
                                               " WHERE c1.planet = c2.planet AND c1.planet = pm.planet AND c1.id = ? AND pm.sessionID = ?)" +
                                               " SELECT cc.id2, c.name AS name, sum(cc.planetVal * cc.matchMult * cc.houseMult) AS total" +
                                               " FROM CompareCelebs AS cc, Celebs AS c" +
                                               " WHERE cc.id2 = c.cid" +
                                               " GROUP BY cc.id2, c.name" +
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

    private static final String GET_ALL_CELEBS = "SELECT * FROM [dbo].[Celebs]";
    private PreparedStatement getAllCelebsStatement;
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
        celebCache = new HashMap<>();
        celebsInDatabase = new HashSet<>();
        variableMap = new EnumMap<>(Variable.class);
        for (Variable v : Variable.values()) {
            variableMap.put(v, v.initValue);
        }
        
        prepareStatements();
        sessionID = getSessionID();
        initializePlanetMultipliers();
        initalizeCelebList();
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
        getAllCelebsStatement = conn.prepareStatement(GET_ALL_CELEBS);
    }

    private void initializePlanetMultipliers() throws SQLException {
        for (Planet p : Planet.values()) {
            Variable v = Variable.valueOf(p.name());
            Double value = variableMap.get(v);
            insertPlanetMultStatement.clearParameters();
            insertPlanetMultStatement.setString(1, p.toString());
            insertPlanetMultStatement.setDouble(2, value);
            insertPlanetMultStatement.setInt(3, sessionID);
            insertPlanetMultStatement.execute();
        }
        conn.commit();
    }

    private void initalizeCelebList() throws SQLException {
        getAllCelebsStatement.clearParameters();
        ResultSet results = getAllCelebsStatement.executeQuery();
        while(results.next()) {
            String name = results.getString("name");
            celebsInDatabase.add(name);
        }
        results.close();
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
            StringBuilder response = new StringBuilder("");
            response.append("\nUser created " + user.name + "\n");
            response.append("\tBirthday: " + user.getBirthday() + "\n");
            response.append("\tBirthplace: " + user.getBirthLocation() + "\n");
            response.append("\tuserID: " + id + "\n");
            return response.toString();
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

    //public String insertCeleb(Person celeb) {
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
            StringBuilder response = new StringBuilder("");
            response.append("\n" + celeb.name + " inserted into database: \n");
            response.append("\tBirthday: " + celeb.getBirthday() + "\n");
            response.append("\tBirthplace: " + celeb.getBirthLocation() + "\n");
            return response.toString();

            //return "Created celeb " + celeb.name + " with id: " + id + "\n";
        } catch (SQLException e) {
            if (isDeadLock(e)) {
                return insertCeleb(celeb);
            } else {
                try {
                  conn.rollback();
                } catch (SQLException e1) {
                  e1.printStackTrace();
                }
                //e.printStackTrace();
                return "Unable to insert " + celeb.name + " into database";
                //return "Inserting celeb failed\n";
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
        calcMatchesStatement.setDouble(1, variableMap.get(Variable.valueOf("ZODIAC")));
        calcMatchesStatement.setDouble(2, variableMap.get(Variable.valueOf("ELEMENT")));
        calcMatchesStatement.setDouble(3, variableMap.get(Variable.valueOf("MODE")));
        calcMatchesStatement.setDouble(4, variableMap.get(Variable.valueOf("HOUSE")));
        calcMatchesStatement.setInt(5, userID);
        calcMatchesStatement.setInt(6, sessionID);
        ResultSet results = calcMatchesStatement.executeQuery();
        double totalPoints = getTotal();
        DecimalFormat df = new DecimalFormat("#.##");
        while(results.next()) {
            int id = results.getInt("id2");
            double sum = results.getDouble("total");
            String name = results.getString("name");         
            double total = (sum / totalPoints) * 100;
            //System.out.println("total points: " + totalPoints);
            List<String> colVals = new ArrayList<>();
            colVals.add("celebID: " + id);
            colVals.add("name: " + name);
            colVals.add("sum: " + df.format(sum));
            colVals.add("percent: " + df.format(total));
            sb.append(getLine(colVals));
            //sb.append("celebID: " + id + "\t name: " + name + "\t sum: " + sum + "\t percent match: " + total + " \n");  
        }
        results.close();
        return sb.toString();
    }

    public String displayMatch(int personID, int celebID) throws SQLException, InterruptedException, IOException {   
        if (!usersCreated.containsKey(personID)) {
            return "User hasn't been inserted into database yet";
        }
        Person user = usersCreated.get(personID);

        if (!celebCache.containsKey(celebID)) {
            Person celeb = getCeleb(celebID);
            if (celeb == null) {
                return "Error creating celeb with " + celebID;
            } else {
                celebCache.put(celebID, celeb);
                return user.compareCharts(celeb, variableMap);
            }
        } else {
            //System.out.println("celeb from cache");
            return user.compareCharts(celebCache.get(celebID), variableMap);
        }
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

    public boolean containsCeleb(String name) {
        return celebsInDatabase.contains(name);
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

    public String getVars() {
        StringBuilder retStr = new StringBuilder("");
        retStr.append("Planet Variables: \n");
        retStr.append(getPlanetVars());
        retStr.append("\nMatch Variables: \n");
        retStr.append(getMatchVars());
        retStr.append("\nHouse Variable: \n");
        retStr.append(getHouseVar());
        return retStr.toString();
    }

    private String getPlanetVars() {
        List<VarNode> planetVars = new ArrayList<>();
        StringBuilder retStr = new StringBuilder("");

        // Build list of planet variables so we can sort
        for (Planet p : Planet.values()) {
            Double pValue = variableMap.get(Variable.valueOf(p.name()));
            VarNode node = new VarNode(p.toString(), pValue);
            planetVars.add(node);
        }
        Collections.sort(planetVars, (n2, n1) -> n1.value.compareTo(n2.value));
        
        // Create string
        for (VarNode n : planetVars) {
            List<String> col = new ArrayList<>();
            col.add(n.node);
            col.add(n.value.toString());
            retStr.append(getLine(col));
        }
        return retStr.toString(); 
    }

    private String getMatchVars() {
        List<VarNode> matchList = new ArrayList<>();

        for (String mString: GlobalConst.MATCH_TYPES) {
            Double matchValue = variableMap.get(Variable.valueOf(mString.toUpperCase()));
            matchList.add(new VarNode(mString, matchValue));
        }
        
        Collections.sort(matchList, (n2, n1) -> n1.value.compareTo(n2.value));
        StringBuilder retStr = new StringBuilder("");
        
        for (VarNode n : matchList) {
            List<String> col = new ArrayList<>();
            col.add(n.node);
            col.add(n.value.toString());
            retStr.append(getLine(col));
        }

        return retStr.toString();
    }

    private String getHouseVar() {
        Double houseVal = variableMap.get(Variable.valueOf("HOUSE"));
        List<String> col = new ArrayList<>(Arrays.asList("House", houseVal.toString()));
        return getLine(col);
    }

    public String setVar(String var, Double value) {
        Double oldValue = variableMap.get(Variable.valueOf(var.toUpperCase()));

        if (!GlobalConst.MATCH_TYPES.contains(var) || !var.equalsIgnoreCase("House")) {
            // var is a planet so we update it in the database
            if (!setPlanetVar(var, oldValue, value)) {
                return "Error modifying planet " + var + " to value " + value.toString() + "\n";
            }
        }

        variableMap.put(Variable.valueOf(var.toUpperCase()), value);
        return var + " has been set from " + oldValue + " to value " + value.toString() + "\n";
    }

    public Boolean setPlanetVar(String planet, Double oldValue, Double newValue) { 
        try {
            setPlanetMultStatement.clearParameters();
            setPlanetMultStatement.setDouble(1, newValue);
            setPlanetMultStatement.setString(2, planet);
            setPlanetMultStatement.setInt(3, sessionID);
            setPlanetMultStatement.execute();
            conn.commit();
            return true;
            //return "Success updating planet " + planet.toString() + " from " + oldValue + " to value " + newValue.toString() + "\n";
        } catch (SQLException e) {
            if (isDeadLock(e)) {
                return setPlanetVar(planet, oldValue, newValue);
            } else {
                try {
                  conn.rollback();
                } catch (SQLException e1) {
                  e1.printStackTrace();
                }
                return false;
                //return "Error modifying planet " + planet.toString() + " to value " + newValue.toString() + "\n";
            }
        } finally {
            checkDanglingTransaction();
        }
    }

    public boolean lessThanZodiac(double value) {
        return Double.compare(value, variableMap.get(Variable.ZODIAC)) > 0;
    }

    private static double getTotal() {
        double planetSum = 0;
        double matchMax = Double.MIN_VALUE;
        double houseVal = variableMap.get(Variable.valueOf("HOUSE"));
        for (String m : GlobalConst.MATCH_TYPES) {
            Double value = variableMap.get(Variable.valueOf(m.toUpperCase()));
            matchMax = Math.max(matchMax, value);
        }

        for (Planet p : Planet.values()) {
            Double value = variableMap.get(Variable.valueOf(p.toString().toUpperCase()));
            planetSum += value;
        }
        return planetSum * matchMax * houseVal;
    }

    private int getSessionID() throws SQLException {
        getSessionStatement.clearParameters();
        ResultSet results = getSessionStatement.executeQuery();
        int maxid = 0;
        if (results.next()) {
            maxid = results.getInt("maxid");
        }
        results.close();
        return maxid + 1;
    }

    private void removePlanetSession() throws SQLException {
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
        List<String> header = new ArrayList<>(Arrays.asList("userID", "Name"));
        sb.append(getLine(header));
        
        for (Map.Entry<Integer, Person> entry : usersCreated.entrySet()) {
            List<String> col = new ArrayList<>();
            col.add(entry.getKey().toString());
            col.add(entry.getValue().name);
            sb.append(getLine(col));
        } 
        return sb.toString();
    }

    private String getLine(List<String> columnVals) {
        StringBuilder retStr = new StringBuilder();
        for(String col : columnVals) {
            StringBuilder cell = new StringBuilder("");
            cell.append(col);
            int len = cell.length();
            cell.append(" ".repeat(GlobalConst.ALL_MATCH_LEN - len));
            retStr.append(cell);
        }
        retStr.append("\n");
        return retStr.toString();
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
