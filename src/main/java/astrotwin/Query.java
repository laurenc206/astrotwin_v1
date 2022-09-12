package astrotwin;

import java.io.*;
import java.sql.*;
import java.util.*;

import com.mysql.cj.x.protobuf.MysqlxPrepare.Prepare;
import com.mysql.cj.xdevapi.Result;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class Query {
    // Database Connection
    private Connection conn;
    int userID;
    boolean insertedUser = false;
    Person user;
    
    // multipliers we can change
    private static Double zodiacMult = GlobalConst.ZODIAC_MULT;
    private static Double elementMult = GlobalConst.ELEMENT_MULT;
    private static Double modeMult = GlobalConst.MODE_MULT;
    private static Double houseMult = GlobalConst.HOUSE_MULT;


    // Canned queries
    // insert user
    private static final String INSERT_PERSON = "INSERT INTO People(pid, username, bday, bplace) VALUES(?, ?, ?, ?)";
    private PreparedStatement insertPersonStatement;
    private static final String REMOVE_PERSON = " DELETE FROM Chart WHERE id = ?" +
                                                " DELETE FROM People WHERE pid = ?;";
    private PreparedStatement removePersonStatement;

    private static final String LARGEST_UNIQUE_ID = "SELECT MAX(pid) AS maxid FROM People;";
    private PreparedStatement largestUniqueIDStatement;
  
    private static final String INSERT_CHART = "INSERT INTO Chart(id, planet, zodiac, element, mode, house) VAlUES (?, ?, ?, ?, ?, ?)";
    private PreparedStatement insertChartStatement;
    
    private static final String CALC_MATCHES = "WITH Multiplier AS (SELECT c1.id AS id1, c2.id AS id2, c1.planet AS planet," +
                                               " CASE WHEN c1.zodiac = c2.zodiac THEN " + zodiacMult +
                                               " WHEN c1.element = c2.element THEN "+ elementMult +
                                               " WHEN c1.mode = c2.mode THEN " + modeMult + " ELSE 0 END AS zodiacMult," +
                                               " CASE WHEN c1.house = c2.house THEN "+ houseMult + " ELSE 1 END AS houseMult" +
                                               " FROM [dbo].[Chart] AS c1, [dbo].[Chart] AS c2" +
                                               " WHERE c1.id = ? AND c1.planet = c2.planet AND (c1.zodiac = c2.zodiac OR c1.element = c2.element OR c1.mode = c2.mode))" +
                                               " SELECT id2, SUM(pm.val * zodiacMult * houseMult) AS total" +
                                               " FROM Multiplier AS m, [dbo].[PlanetMultiplier] AS pm" +
                                               " WHERE m.planet = pm.planet" +
                                               " GROUP BY id2" +
                                               " ORDER BY total DESC;";
    private PreparedStatement calcMatchesStatement;

    private static final String GET_PLANET_MULT = "SELECT * FROM [dbo].[PlanetMultiplier];";
    private PreparedStatement getPlanetMultStatement;
    private static final String SET_PLANET_MULT = "UPDATE [dbo].[PlanetMultiplier] SET val = ? WHERE planet = ?;";
    private PreparedStatement setPlanetMultStatement;

    private static final String GET_PERSON = "SELECT * FROM [dbo].[People] WHERE pid = ?;";
    private PreparedStatement getPersonStatement;
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

        prepareStatements();
        userID = getUniqueID();
        initializePlanetMultipliers();
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
            String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s", serverURL,
                dbName, adminName, password);
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
        if (insertedUser) {
            removePerson(userID);
        }
        
        conn.close();
    }

    /*
     * prepare all the SQL statements in this method.
     */
    private void prepareStatements() throws SQLException {
        tranCountStatement = conn.prepareStatement(TRANCOUNT_SQL);
        insertPersonStatement = conn.prepareStatement(INSERT_PERSON);
        removePersonStatement = conn.prepareStatement(REMOVE_PERSON);
        largestUniqueIDStatement = conn.prepareStatement(LARGEST_UNIQUE_ID);
        insertChartStatement = conn.prepareStatement(INSERT_CHART);

        calcMatchesStatement = conn.prepareStatement(CALC_MATCHES);
        getPlanetMultStatement = conn.prepareStatement(GET_PLANET_MULT);
        setPlanetMultStatement = conn.prepareStatement(SET_PLANET_MULT);
        getPersonStatement = conn.prepareStatement(GET_PERSON);
    }

    private void initializePlanetMultipliers() {
        for (Planet p : Planet.values()) {
            setPlanetMult(p, p.getMult());
        }
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
            id = getUniqueID();
            insertPersonStatement.clearParameters();
            insertPersonStatement.setInt(1, id);// get size of user table
            insertPersonStatement.setString(2, user.name);
            insertPersonStatement.setString(3, user.getBirthday());
            insertPersonStatement.setString(4, user.getBirthLocation());
            insertPersonStatement.execute();

            insertChart(user, id);

            conn.commit();
            if (insertedUser) {
                // a user has been previously inserted so we remove that first so we're only searching celebs
                removePerson(userID);
            } else {
                insertedUser = true;
            }
            userID = id;
            this.user = user;
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
            id = getUniqueID();
            insertPersonStatement.clearParameters();
            insertPersonStatement.setInt(1, id);// get size of user table
            insertPersonStatement.setString(2, celeb.name);
            insertPersonStatement.setString(3, celeb.getBirthday());
            insertPersonStatement.setString(4, celeb.getBirthLocation());
            insertPersonStatement.execute();
            insertChart(celeb, id);
            conn.commit();
            return "Created celeb " + celeb.name + " with id: " + id + "\n";
        } catch (SQLException e) {
            if (isDeadLock(e)) {
                return insertUser(celeb);
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

    private void insertChart(Person p, int id) throws SQLException {
        for (Planet planet : Planet.values()) {
            Zodiac sign = p.chart.signMap.get(planet);
            int house = p.chart.houseMap.get(planet);
            insertChartStatement.clearParameters();
            insertChartStatement.setInt(1, id);
            insertChartStatement.setString(2, planet.toString());
            insertChartStatement.setString(3, sign.toString());
            insertChartStatement.setString(4, sign.getElement());
            insertChartStatement.setString(5, sign.getMode());
            insertChartStatement.setInt(6, house);

            insertChartStatement.execute();
        }
    }

    private int getUniqueID() throws SQLException {
        largestUniqueIDStatement.clearParameters();
        ResultSet results = largestUniqueIDStatement.executeQuery();
        results.next();
        int maxid = results.getInt("maxid");
        results.close();
        return maxid + 1;
    }

    public String calculateMatches() throws SQLException {
        if (!insertedUser) {
            return "User hasn't been inserted yet";
        }
        
        StringBuilder sb = new StringBuilder("");
        calcMatchesStatement.clearParameters();
        calcMatchesStatement.setInt(1, userID);
        ResultSet results = calcMatchesStatement.executeQuery();
        double total = 1;
        while(results.next()) {
            int id = results.getInt("id2");
            if (id == this.userID) {
                total = results.getDouble("total");
            } else {
                double sum = results.getDouble("total");
                double percent = (sum / total) * 100;
                sb.append("id: " + id + "\t sum: " + sum + "\t percent match: " + percent + "%\n");
            }
        }
        results.close();
        return sb.toString();
    }

    public String displayMatch(int celebID) throws SQLException, InterruptedException, IOException {
        Person celeb = getCeleb(celebID);
        if (celeb == null) {
            return "Error creating celeb with " + celebID;
        }
        if (insertedUser) {
            return user.compareCharts(celeb);
        } 
        return "User hasn't been inserted into database yet";
    }

    private Person getCeleb(int celebID) throws SQLException, InterruptedException, IOException {
        Person celeb = null;
        getPersonStatement.clearParameters();
        getPersonStatement.setInt(1, celebID);
        ResultSet results = getPersonStatement.executeQuery();
        while(results.next() && celeb == null) {
            String bdayStr = results.getString("bday");
            String bplaceStr = results.getString("bplace");
            String name = results.getString("username");
            System.out.println(bdayStr);
            System.out.println(bplaceStr);
            System.out.println(name);
            LocalDateTime date = LocalDateTime.parse(bdayStr);
            String[] tokens = bplaceStr.split(",");
            String town = tokens[0].trim();
            String country = tokens[1].trim();
            String latitude = tokens[2].trim();
            String longitude = tokens[3].trim();
            String timeZone = tokens[4].trim();
            AtlasModel location = new AtlasModel(latitude, longitude, timeZone, town, country);
            System.out.println(date);
            System.out.println(town);
            System.out.println(country);
            System.out.println(latitude);
            System.out.println(longitude);
            System.out.println(timeZone);
            System.out.println(location.toString());
            celeb = new Person(name, date, location);
        }
        results.close();
        return celeb;
    }

    public String removePerson(int personID) {
        try {
            removePersonStatement.clearParameters();
            removePersonStatement.setInt(1, personID);
            removePersonStatement.setInt(2, personID);
            removePersonStatement.setInt(3, personID);
            removePersonStatement.setInt(4, personID);
            removePersonStatement.setInt(5, personID);
            removePersonStatement.setInt(6, personID);
            removePersonStatement.execute();
            conn.commit();
            return "Successfully removed user " + String.valueOf(personID);  
        } catch (SQLException e) {
            if (isDeadLock(e)) {
                return removePerson(personID);
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
        StringBuilder sb = new StringBuilder("");
        getPlanetMultStatement.clearParameters();
        ResultSet results = getPlanetMultStatement.executeQuery();
        EnumMap<Planet, Float> multMap = new EnumMap<>(Planet.class);
        while (results.next()) {
            String planet = results.getString("planet");
            float value = results.getFloat("val");
            multMap.put(Planet.valueOf(planet.toUpperCase()), value);
        }
        for (Planet planet : Planet.values()) {
            sb.append(planet.toString() + " " + multMap.get(planet) + "\n");
        }
        results.close();
        return sb.toString();   
    }

    public String setPlanetMult(Planet planet, Float value) {
        try {
            setPlanetMultStatement.clearParameters();
            setPlanetMultStatement.setFloat(1, value);
            setPlanetMultStatement.setString(2, planet.toString());
            setPlanetMultStatement.execute();
            conn.commit();
            planet.setMult(value);
            return "Success updating planet " + planet.toString() + " to value " + value.toString();
        } catch (SQLException e) {
            if (isDeadLock(e)) {
                return setPlanetMult(planet, value);
            } else {
                try {
                  conn.rollback();
                } catch (SQLException e1) {
                  e1.printStackTrace();
                }
                return "Error modifying planet " + planet.toString() + " to value " + value.toString();
            }
        } finally {
            checkDanglingTransaction();
        } 
    }

    public static String setMult(String multiplier, double value) {
        if (multiplier.equals("mode")) {
            double oldvalue = modeMult;
            modeMult = value;
            return "mode has been set from " + oldvalue + " to " + value;
        } else if (multiplier.equals("house")) {
            double oldvalue = houseMult;
            houseMult = value;
            return "house has been set from " + oldvalue + " to " + value;
        } else if (multiplier.equals("element")) {
            double oldValue = elementMult;
            elementMult = value;
            return "element has been set from " + oldValue + " to " + value;
        } else {
            return "invalid input string must be [mode, house, element]";
        }
    }

    public static String getMult() {
        StringBuilder sb = new StringBuilder("");
        sb.append("mode = " + modeMult);
        sb.append("house = " + houseMult);
        sb.append("element = " + elementMult);
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
