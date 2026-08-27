package lib.dbComponents;

import it.restaurant.dto.OrderRequestDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/******** CLASS FOR DATABASE DELIVERY OPERATIONS ********/
public class DBDeliveryOperations implements DBDeliveryOperationsInterface {

    /**** Fields ****/
    // Variables
    private final DBConnectionClassInterface connector;         // Connector object
    private final Connection directConnection;                  // Connector for Spring DataSource


    /**** Constructors ****/
    public DBDeliveryOperations(final DBConnectionClassInterface connector) {
        this.connector = connector;
        this.directConnection = null;
    }

    // Constructor for Spring Boot / HikariCP Pool
    public DBDeliveryOperations(final Connection connection) {
        this.directConnection = connection;
        this.connector = null;
    }



    /**** Methods ****/
    // Helper to switch to the right connection in between the classic connector object and the connector for the spring datasource
    private Connection getConnection() {
        if (this.directConnection != null) {
            return this.directConnection;
        }
        return this.connector != null ? this.connector.returnConnection() : null;
    }

    // Method to retrieve the full delivery order codes and their infos
    public Object[][] getTheDeliveryOrderCodesAndTheirInfo() {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn == null) {
            System.err.println("Please connect to the database first!");
            return new Object[0][0];
        }

        // Sql command for the method
        final String sqlCommand = "SELECT * FROM delivery_order_details;";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sqlCommand);        // Prepared statement with the sql command chosen
            ResultSet rs = pstmt.executeQuery();                                // Execute the query and get the result set

            // List to hold the orders data before organizing it in a 2d Array
            ArrayList<Object[]> data = new ArrayList<>();

            // "Order Code", "Order Name", "Address", "CAP", "City", "Email", "Phone", "Order Status"
            while (rs.next()) {
                String orderCode = rs.getString("order_code");
                String orderName = rs.getString("order_name");
                String orderAddress = rs.getString("address");
                String orderCap = rs.getString("cap");
                String orderCity = rs.getString("city");
                String orderEmail = rs.getString("email");
                String orderPhone = rs.getString("phone");
                String orderStatus = rs.getString("order_status");

                // Create an array of objects representing a row of data
                Object[] rowData = {orderCode, orderName, orderAddress, orderCap, orderCity, orderEmail, orderPhone, orderStatus};
                data.add(rowData);
            }

            // Convert the list of data to a 2D array and return it
            Object[][] dataArray = new Object[data.size()][];
            for (int i = 0; i < data.size(); i++) {
                dataArray[i] = data.get(i);
            }
            return dataArray;


        } catch (SQLException e) {
            System.err.println("The Database is not connected or the data requested is not available!");
        }
        return new Object[0][0];
    }

    // VARIANT: To retrieve all the delivery orders info and codes but returning an OrderRequestDTO from the springboot
    // Method to retrieve the full delivery order codes and their infos
    public List<OrderRequestDTO> getTheDeliveryOrderCodesAndTheirInfo(int tag) {

        // List of data to return
        List<OrderRequestDTO> ordersList = new ArrayList<>();

        // Connection status check
        Connection conn = this.getConnection();

        if (conn == null) {
            // Headless (without graphics handling)
            System.err.println("The DB is not connected for getTheDeliveryOrderCodesAndTheirInfo");
            return ordersList;
        }

        // SQL command for the method
        final String sqlCommand = "SELECT * FROM delivery_order_details";

        try (PreparedStatement pstmt = conn.prepareStatement(sqlCommand);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {

                // Object OrderRequestDTO to hold the data
                OrderRequestDTO data = new OrderRequestDTO();
                data.setOrderCode(rs.getString("order_code"));
                data.setName(rs.getString("order_name"));
                data.setAddress(rs.getString("address"));
                data.setCap(rs.getString("cap"));
                data.setCity(rs.getString("city"));
                data.setEmail(rs.getString("email"));
                data.setPhone(rs.getString("phone"));

                // Adding the retrieved data to the list
                ordersList.add(data);
            }
            return ordersList;
        }
        catch (SQLException e) {
            System.err.println("The Database is not connected or the data requested is not available!");
            e.printStackTrace();
        }
        return new ArrayList<OrderRequestDTO>();
    }

    // Method to retrieve and return all the delivery order data of all order codes plus their status through the all orders info table
    public Object[][] getAllTheDeliveryOrderDataWithStatus() {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn == null) {
            System.err.println("Please connect to the database first!");
            return new Object[0][0];
        }

        // Sql command for the method
        final String sqlCommand = """
                SELECT L.order_code, L.meal_code, L.quantity, D.order_status, G.name\r
                FROM delivery_orders_list AS L, delivery_order_details AS D, menu AS G\r
                WHERE L.order_code = D.order_code AND L.meal_code = G.meal_code\r
                ORDER BY L.index_code ASC;""";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sqlCommand);        // Prepared statement with the sql command chosen
            ResultSet rs = pstmt.executeQuery();                                // Execute the query and get the result set

            // List to hold the orders data before organizing it in a 2d Array
            ArrayList<Object[]> data = new ArrayList<>();

            while (rs.next()) {
                String orderCode = rs.getString("order_code");
                String mealCode = rs.getString("meal_code");
                String mealName = rs.getString("name");
                int quantity = rs.getInt("quantity");
                String orderStatus = rs.getString("order_status");

                // Create an array of objects representing a row of data
                Object[] rowData = {orderCode, mealCode, mealName, quantity, orderStatus};
                data.add(rowData);
            }

            // Convert the list of data to a 2D array and return it
            Object[][] dataArray = new Object[data.size()][];
            for (int i = 0; i < data.size(); i++) {
                dataArray[i] = data.get(i);
            }
            return dataArray;


        } catch (SQLException e) {
            System.err.println("The Database is not connected or the data requested is not available!");
        }

        return new Object[0][0];                    // Returns an empty 2D array if there was an error
    }

    // Method overloading
    // Method to retrieve and return the delivery order data of a specific order code (in the main panel to display the orders)
    public Object[][] getTheDeliveryOrderData(String order_code) {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn == null) {
            System.err.println("Please connect to the database first!");
            return new Object[0][0];
        }

        // Sql command for the method
        final String sqlCommand = """
                SELECT L.order_code, L.meal_code, D.name, L.quantity
                FROM delivery_orders_list AS L, menu AS D
                WHERE L.order_code = ? AND L.meal_code = D.meal_code
                ORDER BY L.index_code ASC;""";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sqlCommand);        // Prepared statement with the sql command chosen
            pstmt.setString(1, order_code);
            ResultSet rs = pstmt.executeQuery();                                // Execute the query and get the result set

            // List to hold the orders data before organizing it in a 2d Array
            ArrayList<Object[]> data = new ArrayList<>();

            while (rs.next()) {
                String mealCode = rs.getString("meal_code");
                String mealName = rs.getString("name");
                int quantity = rs.getInt("quantity");

                // Create an array of objects representing a row of data
                Object[] rowData = {mealCode, mealName, quantity};
                data.add(rowData);
            }

            // Convert the list of data to a 2D array and return it
            Object[][] dataArray = new Object[data.size()][];
            for (int i = 0; i < data.size(); i++) {
                dataArray[i] = data.get(i);
            }
            return dataArray;


        } catch (SQLException e) {
            System.err.println("The Database is not connected or the data requested is not available!");
        }

        return new Object[0][0];                    // Returns an empty 2D array if there was an error
    }

    // Method to retrieve all the delivery order codes
    public String[][] getTheDeliveryOrderCodesAndStatus() {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn == null) {
            System.err.println("Please connect to the database first!");
            return new String[0][0];
        }

        // Sql statement
        final String sqlCommand = "SELECT order_code, order_status FROM delivery_order_details;";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sqlCommand);
            ResultSet rs = pstmt.executeQuery();

            // List to hold the orders' data before organizing it in an Array
            ArrayList<String[]> data = new ArrayList<>();

            // Getting the data
            while (rs.next()) {
                String orderCode = rs.getString("order_code");
                String orderStatus = rs.getString("order_status");

                // Create an array of objects representing a row of data
                String[] rowData = {orderCode, orderStatus};
                data.add(rowData);
            }

            // Convert the list of data to a 2D array and return it
            String[][] dataArray = new String[data.size()][];
            for (int i = 0; i < data.size(); i++) {
                dataArray[i] = data.get(i);
            }
            return dataArray;

        } catch (SQLException e) {
            System.err.println("The Database is not connected or the data requested is not available!");
        }

        // Return nothing if the process fails
        return new String[0][0];
    }

    // Method to get all the order codes for the order status change
    public String[] getTheDeliveryOrderCodes() {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn == null) {
            System.err.println("Please connect to the database first!");
            return new String[0];
        }

        // Sql statement
        final String sqlCommand = "SELECT order_code FROM delivery_order_details;";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sqlCommand);
            ResultSet rs = pstmt.executeQuery();

            // List to hold the orders' data before organizing it in an Array
            ArrayList<String> data = new ArrayList<>();

            // Getting the data
            while (rs.next()) {
                // Get the string and add it to the list
                data.add(rs.getString("order_code"));
            }

            // Convert it into a String array
            return data.toArray(new String[0]);

        } catch (SQLException e) {
            System.err.println("The Database is not connected or the data requested is not available!");
        }

        return new String[0];
    }

    // Method to update the status of the service for the selected order
    public void updateStatusService(final String orderCode, final String updatedStatus) {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn == null) {
            System.err.println("Please connect to the database first!");
        }

        // Sql command for the method
        final String sqlCommand = "UPDATE delivery_order_details SET order_status = ? WHERE order_code = ?";

        // Using the established connection to update the service status
        try {
            assert conn != null;
            PreparedStatement pstmt = conn.prepareStatement(sqlCommand);
            pstmt.setString(1, updatedStatus);
            pstmt.setString(2, orderCode);
            pstmt.executeUpdate();
            System.out.println("Service status updated successfully");
        } catch (SQLException e) {
            System.err.println("The Database is not connected or the data requested is not available!");
        }
    }

    // Method to delete a delivery order from the database based on its order code
    public void deleteDeliveryOrder(final String deliveryOrderCode) {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn == null) {
            System.err.println("Please connect to the database first!");
        }

        // Sql command for the method
        final String sqlCommand = "DELETE FROM delivery_order_details WHERE order_code = ?";

        // Using the established connection to delete the delivery order
        try {
            assert conn != null;
            PreparedStatement pstmt1 = conn.prepareStatement(sqlCommand);
            pstmt1.setString(1, deliveryOrderCode);
            pstmt1.executeUpdate();
            System.out.println("Delivery order deleted successfully!");
        } catch (SQLException e) {
            System.err.println("The Database is not connected or the data requested is not available!");
        }
    }

    // Method to insert the delivery order data info
    public boolean insertDeliveryOrderDataInfo(final String orderCode, final String orderName, final String address,
                                               final String cap, final String city, final String email, final String phone,
                                               final String orderStatus) {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn == null) {
            return false;
        }

        final String sqlCommand = """
            INSERT INTO delivery_order_details
            (order_code, order_name, address, cap, city, email, phone, order_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";

        try (PreparedStatement pstmt = conn.prepareStatement(sqlCommand)) {
            pstmt.setString(1, orderCode);
            pstmt.setString(2, orderName);
            pstmt.setString(3, address);
            pstmt.setString(4, cap);
            pstmt.setString(5, city);
            pstmt.setString(6, email);
            pstmt.setString(7, phone);
            pstmt.setString(8, orderStatus);

            pstmt.executeUpdate();
            System.out.println("Delivery Order inserito con successo (From DB)");
            return true;
        } catch (SQLException e) {
            System.err.println("Errore inserimento delivery_order_details: " + e.getMessage());
            return false;
        }
    }

    // Method to insert the orders list
    public boolean insertDeliveryOrderList(String orderCode, List<Object[]> items) {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn == null) {
            System.err.println("Connect to the DB first!");
            return false;
        }

        // Sql command for the method
        String sql = "INSERT INTO delivery_orders_list (order_code, meal_code, quantity) VALUES (?, ?, ?)";

        // Using the established connection to delete the delivery order
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // No auto commit to handle everything as one single transaction
            conn.setAutoCommit(false);

            for (Object[] item : items) {
                pstmt.setString(1, orderCode);          // Order code
                pstmt.setString(2, (String) item[0]);   // meal_code
                pstmt.setInt(3, (Integer) item[1]);     // quantity
                pstmt.addBatch();                                    // Add everything
            }

            pstmt.executeBatch(); // Send everything to the DB in one piece
            conn.commit();        // And commit

            System.out.println("DEBUG: Lista piatti inserita per l'ordine " + orderCode);
            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;

        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Method to count the number of orders
    public int countOrders() {

        // Connection status check
        Connection conn = this.getConnection();
        if (conn != null) {

            // Sql command for the method
            String sqlCommand = "SELECT COUNT(*) AS total FROM delivery_order_details";

            // The try-with-resources closes automatically pstmt and rs
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCommand);
                 ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("total");
                }
            } catch (SQLException e) {
                System.err.println("Errore nel conteggio ordini: " + e.getMessage());
            }
        }
        return 0;
    }
}