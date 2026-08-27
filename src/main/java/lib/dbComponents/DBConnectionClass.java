package lib.dbComponents;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

/******** Class for the connection ********/
public class DBConnectionClass implements DBConnectionClassInterface {

    /**** Fields ****/
    private String currentDbUrl = "jdbc:mysql://127.0.0.1:3306/restaurant";
    private final String currentServerUrl = "jdbc:mysql://127.0.0.1:3306";

    // Variables
    private Connection conn;                 // Connection field



    /**** Constructors ****/
    // Empty constructor
    public DBConnectionClass() {

    }



    /**** Methods ****/
    // Connect to the DB method
    public boolean connectToDB(final String user, final String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");                                // For the spring boot
            this.conn = DriverManager.getConnection(currentDbUrl, user, password);    // Getting the connection

            // CONTROLLO HEADLESS: Mostra il pop-up solo se non siamo su un server
            System.out.println("Database connesso con successo (Web Mode)");
            return true;

        } catch (Exception e) {
            System.err.println("Errore connessione: " + e.getMessage());
            return false;
        }
    }

    // Method to set the db url to the chosen one
    public void setDbUrl(String url) {
        this.currentDbUrl = url;
    }

    // Get the current connection status method
    public Connection returnConnection() {
        return this.conn;
    }

    // Close the connection to the DB method
    public void closeConnectionToDB() {
        try {
            if (this.conn != null && !this.conn.isClosed()) {                                       // If the connection is not null and is not already closed
                this.conn.close();                                                                  // close it
                System.out.println("Database disconnected successfully!");
            }
        } catch (SQLException e) {
            System.err.println("Could not close the connection to the Database!");
        }
    }

    // Connect to the mysql server to check the DB existence
    public boolean checkServerConnection(final String user, final String password) {
        try {
            this.conn = DriverManager.getConnection(currentServerUrl, user, password);     // Connecting to the expected MySQL server
            System.out.println("Server connected successfully! Now checking the DB existence...");

            // If it exists, the check returns true
            // Otherwise it returns false and closes the application
            return checkDBExistence(conn);

        } catch (SQLException e) {
            System.err.println("Could not connect to the server! Check your user and password!");
            System.exit(0);
        }
        return false;
    }

    // Checking the DB existence into the MySQL server
    private boolean checkDBExistence(final Connection conn) {

        // Checking the existence of the DB
        try {
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet resultSet = metaData.getCatalogs();
        while (resultSet.next()) {
            String dbName = resultSet.getString(1);
            if (dbName.equalsIgnoreCase("restaurant")) {     // Check if a DB called "restaurant" exists
                return true;                                            // Database exists so returns true
            }
        }
        } catch (SQLException e) {
            return false;                                               // Database does not exist so returns false
        }
        return false;
    }
}