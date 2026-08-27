package lib.dbComponents;

import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/******** CLASS FOR DATABASE CREATION OPERATIONS ********/
public final class DBCreationOperations implements DBCreationOperationsInterface {

    /**** Fields ****/
    // Constants
    //private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant";
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/restaurant";

    // Variables
    private final DBConnectionClassInterface connector;       // Connection field for the first connection (to the MySQL server to create the DB)
    private Connection connection;                            // Connection field for the second connection (to the DB to create the tables, triggers and populate them)


    /**** Constructors ****/
    public DBCreationOperations(final DBConnectionClassInterface connector) {
        this.connector = connector;
    }


    /**** Methods ****/
    /********************************* DB creating operations *********************************/
    /**** It is first needed to connect to the server to create the DB, to then connect to the DB to create, populate and handle the tables ****/
    // Create the DB method
    private void createDB() {

        // Sql command for the method
        final String sqlCommand = "CREATE DATABASE restaurant";

        // Connecting to the server and creating the database
        try {                                                             // The type of connection needed is the one to the server, not to the DB
            Connection conn = this.connector.returnConnection();          // Gets the previously established connection to the server to create the DB in it
            Statement statement = conn.createStatement();                 // Creates the statement
            statement.executeUpdate(sqlCommand);                          // And executes it
        }
        catch (SQLException e) {
            System.err.println("Could not create the Database in the chosen mysql server");
        }
    }

    /**** Now that the DB has been created, it is necessary to connect to it to create the tables and the triggers ****/
    // Create menu table method
    private void createMenuTable() {

        // Sql strings for the method
        final String sqlCommand = """
                CREATE TABLE menu (\r
                position tinyint unsigned auto_increment,\r
                meal_code char(4),\r
                category varchar(30) not null,
                name varchar(45) not null,\r
                price decimal(7,2) not null default 0.0 check (price >= 0.0),\r
                description varchar(100) not null,\r
                availability tinyint unsigned not null default 0 check (availability>= 0),\r
                primary key (position, meal_code));""";

        // Index for the foreign key
        final String sqlCommand1 = "CREATE INDEX order_management_ibfk_2\r\n" +
                                   "ON menu (meal_code);";

        // Creating the menu table
        try {
            Statement statement = this.connection.createStatement();       // Using the usual "connection" field to create the statement
            statement.executeUpdate(sqlCommand);                           // Executing the first command
            statement.executeUpdate(sqlCommand1);                          // And the second command
        }
        catch (SQLException e) {
            System.err.println("Could not create the menu table in the DB!");
        }
    }

    // Method to create the table for the orders details info table
    private void createDeliveryOrderDetailsInfoTable() {

        // Sql string
        final String sqlCommand = """
                              create table delivery_order_details (
                              order_code char(8) not null primary key,
                              order_name char(20) not null,
                              address char(30) not null,
                              cap char(5) not null,
                              city char(20) not null,
                              email char(50) not null,
                              phone char(10) not null,
                              order_status char(8) not null default 'awaiting'
                              );
                              """;

        // Creating the table for the orders details info table
        try {
            Statement statement = this.connection.createStatement();       // Using the usual "connection" field to create the statement
            statement.executeUpdate(sqlCommand);                           // Executing the command
        }
        catch (SQLException e) {
            System.err.println("Could not create the menu table in the DB!");
        }
    }

    // Method to create the table for the orders details info table
    private void createDeliveryOrdersListTable() {

        // Sql string
        final String sqlCommand = """
                            create table delivery_orders_list (
                            index_code tinyint unsigned not null primary key auto_increment,
                            order_code char(8) not null,
                            meal_code char(4) not null,
                            quantity tinyint unsigned not null check (quantity > 0),
                            foreign key (order_code) references delivery_order_details(order_code) on delete cascade
                            );
                            """;

        // Creating the table for the orders details info table
        try {
            Statement statement = this.connection.createStatement();       // Using the usual "connection" field to create the statement
            statement.executeUpdate(sqlCommand);                           // Executing the command
        }
        catch (SQLException e) {
            System.err.println("Could not create the menu table in the DB!");
        }
    }

    // Create the OTP data table
    private void createOTPDataTable() {

        // Sql String
        final String sqlCommand = """
                CREATE TABLE otp_codes (
                email VARCHAR(75) PRIMARY KEY,
                otp_code CHAR(6) NOT NULL,
                expiry_time DATETIME NOT NULL
                );""";

        // Creating the table for the orders details info table
        try {
            Statement statement = this.connection.createStatement();       // Using the usual "connection" field to create the statement
            statement.executeUpdate(sqlCommand);                           // Executing the command
        }
        catch (SQLException e) {
            System.err.println("Could not create the menu table in the DB!");
        }
    }

    // Create triggers method
    // Trigger 1
    private void createTrigger1() {

        // Sql strings for the method
        // Trigger to update the menu's availability after the insertion in the delivery orders list table
        final String sqlCommand = """
                create trigger update_menu1\r
                after insert on delivery_orders_list\r
                for each row\r
                begin\r
                 update menu \r
                 set availability = availability - NEW.quantity\r
                 where meal_code = NEW.meal_code;\r
                end;""";

        // Creating the trigger
        try {
            Statement statement = this.connection.createStatement();       // Using the usual "connection" field to create the statement
            statement.executeUpdate(sqlCommand);
        }
        catch (SQLException e) {
            System.err.println("Could not create the trigger 3 in the DB!");
        }
    }

    // Populating the tables methods
    // Populating the menu table
    private void populateMenuTable() {

        // Sql string for the method
        final String sqlCommand = """
                INSERT INTO menu (meal_code, category, name, price, description, availability)\r
                VALUES ('ST01', 'Primi', 'Spaghetti al baioioiab', 12.99, 'spaghetti al saas', 20),\r
                 ('ST02', 'Antipasti', 'Involtini al siis', 10.99, 'involtini con carne di siis', 20),\r
                 ('FC01', 'Primi', 'Carbonara al carbone', 12.99, 'carbonara cotta al carbone', 20),\r
                 ('SC01', 'Secondi', 'Arrosto di soos', 15.99, 'carne di soos arrosto', 20),\r
                 ('DR01', 'Bevande', 'Acqua lezza', 1.99, 'acqua lezza direttamente dalle fonti sudicie', 20),\r
                 ('DE01', 'Dolci', 'Torta al leel', 9.99, 'torta al leel con zuuz', 20),\r
                 ('DE02', 'Dolci', 'Crema jamaicana', 11.99, 'crema jamaicana con paap', 20);""";

        // Populating the table
        try {
            Statement statement = this.connection.createStatement();       // Using the usual "connection" field to create the statement
            statement.executeUpdate(sqlCommand);
        }
        catch (SQLException e) {
            System.err.println("Could not populate the menu table in the DB!");
        }
    }

    // Method to populate the delivery orders data info table
    private void populateTheDeliveryOrderDetailInfoTable() {

        // Sql string for the method
        final String sqlCommand = """
                INSERT INTO delivery_order_details (order_code, order_name, address, cap, city, email, phone, order_status)
                VALUES ('HKRPK115', 'Mario Rossi', 'Via Roma 10', '20100', 'Milano', 'mario@email.it', '0123456789', 'awaiting'),
                 ('SVUAS987', 'Sandro Sterchi', 'Via Culo 22', '36187', 'Padova', 'sandro85@email.it', '3348516504', 'ongoing'),
                 ('ADDRD500', 'Fabio Fabigli', 'Via Molise 12', '12122', 'Saponarola', 'faby@email.it', '8745126532', 'ongoing'),
                 ('PANED832', 'Sergio Raschioni', 'Via Tonno 15', '88152', 'Poggibonzi', 'sergio@email.it', '1135726142', 'ongoing'),
                 ('TARTD832', 'Enrico Feci', 'Via Mulo 15', '99122', 'Poggibonzi', 'enri@email.it', '3135582142', 'awaiting');
                 """;

        // Populating the table
        try {
            Statement statement = this.connection.createStatement();       // Using the usual "connection" field to create the statement
            statement.executeUpdate(sqlCommand);
        }
        catch (SQLException e) {
            System.err.println("Could not populate the menu table in the DB!");
        }
    }

    // Method to populate the delivery orders list table
    private void populateTheDeliveryOrderListTable() {

        // Sql string for the method
        final String sqlCommand = """
                INSERT INTO delivery_orders_list (order_code, meal_code, quantity)\r
                VALUES ('HKRPK115', 'ST02', 2),
                 ('HKRPK115', 'SC01', 1),
                 ('SVUAS987', 'ST01', 3),
                 ('SVUAS987', 'DE01', 5),
                 ('ADDRD500', 'ST02', 4),
                 ('ADDRD500', 'DE01', 2),
                 ('ADDRD500', 'DE02', 3),
                 ('PANED832', 'ST01', 4),
                 ('PANED832', 'FC01', 2),
                 ('PANED832', 'SC01', 3),
                 ('TARTD832', 'ST02', 4),
                 ('TARTD832', 'FC01', 2),
                 ('TARTD832', 'DE02', 3);
                 """;

        // Populating the table
        try {
            Statement statement = this.connection.createStatement();       // Using the usual "connection" field to create the statement
            statement.executeUpdate(sqlCommand);
            System.out.println("Database created successfully!");
        }
        catch (SQLException e) {
            System.err.println("Could not populate the menu table in the DB!");
        }
    }

    // Only public method, used for the entire creation of the DB
    // Full DB creation method
    public void fullDBCreation(final String user, final String password) {

        // Creating the DB
        this.createDB();

        // Switching the connection from the server to the freshly-created DB and creating the tables
        this.createMenuTable();                           // Creating the menu table
        this.createDeliveryOrderDetailsInfoTable();       // Create the delivery order details info table
        this.createDeliveryOrdersListTable();             // Create the delivery orders list table
        this.createOTPDataTable();                        // Create the OTP data table for the OTP verification

        // Adding the triggers
        this.createTrigger1();                            // Creating the trigger to update the menu's availability

        // Populating the tables
        this.populateMenuTable();                         // Populating the menu table with default values
        this.populateTheDeliveryOrderDetailInfoTable();   // Populating the delivery orders details info table
        this.populateTheDeliveryOrderListTable();         // Populating the delivery orders list table
    }
}