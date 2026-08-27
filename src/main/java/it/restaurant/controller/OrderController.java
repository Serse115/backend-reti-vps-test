package it.restaurant.controller;

import it.restaurant.dto.*;
import lib.dbComponents.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**** Class for the controller of the web server events for the orders ****/
@RestController
@RequestMapping("/order_management")
public class OrderController {

    // Fetch the data from the properties file
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    // Injecting the email service
    @Autowired
    private EmailService emailService;

    @Autowired
    private DataSource dataSource;      // Auto handling the connection pool with HikariCP

    // "Waiting room" for the orders that are waiting for the OTP confirmation to be confirmed and saved
    private final Map<String, OrderRequestDTO> pendingOrders = new ConcurrentHashMap<>();


    /**** FROM FRONTEND TO BACKEND ****/
    // Method to request the OTP
    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody OrderRequestDTO request) {

        try (Connection conn = this.dataSource.getConnection()) {
            DBOTPOperationsInterface otpService = new DBOTPOperations(conn);

            // Clear all the previously expired OTPs (Self-cleaning of expired items everytime a new OTP is requested from the frontend)
            otpService.cleanExpiredOTPs();

            // Generate the otp
            String code = otpService.generateOTP();

            // Save the OTP in the DB waiting for the confirmation
            if (otpService.saveOTP(request.getEmail(), code)) {
                // Keep the order awaiting using the email as the key
                this.pendingOrders.put(request.getEmail(), request);

                // Using the email service
                try {
                    this.emailService.sendOtpEmail(request.getEmail(), code);
                    System.out.println(">>> OTP inviato a " + request.getEmail() + ": " + code);
                    return ResponseEntity.ok("OTP inviato all'email: " + request.getEmail());
                } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseEntity.status(500).body("Errore nell'invio dell'email.");
                }
            }
            return ResponseEntity.status(500).body("Errore nella generazione dell'OTP.");

        } catch (SQLException e) {
            System.err.println("Errore DB full-cards-data: " + e.getMessage());
            return ResponseEntity.status(503).build();
        }
    }


    // Method to submit the order from the front end call to the database
    @PostMapping("/verify-otp-and-submit")
    public ResponseEntity<?> verifyOtpAndSubmit(@RequestParam("email") String email, @RequestParam("otp") String otp) {

        try (Connection conn = this.dataSource.getConnection()) {
            DBOTPOperationsInterface otpService = new DBOTPOperations(conn);

            // Verify the OTP through its otpService class
            if (otpService.verifyOTP(email, otp)) {

                // Recover the order from the "parked" order from the "waiting room"
                OrderRequestDTO originalRequest = this.pendingOrders.get(email);

                if (originalRequest != null) {      // If the order exists and is not null
                    String orderCode = this.executeFinalSubmit(originalRequest);            // Execute the final submit to save it

                    if (orderCode != null) {
                        this.pendingOrders.remove(email);      // Remove the order from the "waiting room" for pending orders
                        return ResponseEntity.ok(orderCode);
                    }
                    return ResponseEntity.status(500).body("Errore durante il salvataggio dell'ordine nel database.");
                }
                return ResponseEntity.status(400).body("Sessione ordine scaduta. Ricomincia il processo.");
            }
            return ResponseEntity.status(401).body("Codice OTP errato o scaduto.");

        } catch (SQLException e) {
            return ResponseEntity.status(503).body("Database non raggiungibile.");
        }
    }


    // Method to send the resume email after the order confirmation
    @PostMapping("/send-order-resume-email")
    public ResponseEntity<?> sendOrderResume(@RequestBody OrderRequestDTO request) {

        try (Connection conn = this.dataSource.getConnection()) {

            try {
                this.emailService.sendOrderConfirmationEmail(request.getEmail(), request);
                System.out.println(">>> Email di conferma e riepilogo inviata a " + request.getEmail());
                return ResponseEntity.ok("Riepilogo di conferma inviato all'email: " + request.getEmail());
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body("Errore nell'invio dell'email.");
            }
        }
        catch (SQLException e) {
            return ResponseEntity.status(503).body("Database non raggiungibile.");
        }
    }


    /**** FROM BACKEND TO FRONTEND ****/
    // Method to retrieve the menu data from the DB and send it to the frontend to display it
    @GetMapping("/menu")
    public ResponseEntity<?> getPublicMenu() {
        final String sqlCommand = """
            SELECT meal_code, name, category, price, description, availability 
            FROM menu
            ORDER BY FIELD(category, 'Antipasti', 'Primi', 'Secondi', 'Dolci', 'Bevande'),
                     position ASC
            """;

        List<Map<String, Object>> menuList = new ArrayList<>();

        try (Connection conn = this.dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlCommand)) {

            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("mealCode", rs.getString("meal_code"));
                item.put("name", rs.getString("name"));
                item.put("category", rs.getString("category"));
                item.put("price", rs.getDouble("price"));
                item.put("description", rs.getString("description"));
                item.put("availability", rs.getInt("availability"));
                menuList.add(item);
            }

            return ResponseEntity.ok(menuList);

        } catch (SQLException e) {
            System.err.println("Errore query menu: " + e.getMessage());
            return ResponseEntity.status(503).body("Database non raggiungibile");
        }
    }


    // Method to retrieve the delivery orders list form the DB and send it to the frontend to display it
    @GetMapping("/full-delivery-orders")
    public ResponseEntity<List<OrderRequestDTO>> getDeliveryOrdersFullList() {

        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            List<OrderRequestDTO> orderRequest = dbDelivery.getTheDeliveryOrderCodesAndTheirInfo(1);
            return ResponseEntity.ok(orderRequest);
        }
        catch (SQLException e) {
            System.out.println("DB NON CONNESSO!");
            return null;
        }
    }


    /**** HELPERS ****/
    // Helper method to save the info data of the order, used in the endpoint shown previously
    private String executeFinalSubmit(OrderRequestDTO request) {

        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            final String orderCode = this.createTheOrderCode();

            // Saving the data through the DTO field object
            boolean infoSaved = dbDelivery.insertDeliveryOrderDataInfo(
                    orderCode,
                    request.getName(),
                    request.getAddress(),
                    request.getCap(),
                    request.getCity(),
                    request.getEmail(),
                    request.getPhone(),
                    "awaiting"
            );

            if (infoSaved) {                                        // If the info is saved successfully
                List<Object[]> itemsForDb = new ArrayList<>();      // Convert the list of DTOs in the format for the insertion method
                for (OrderItemDTO item : request.getItems()) {      // For each item
                    itemsForDb.add(new Object[]{item.getMealCode(), item.getQuantity()});   // Add it to the data structure
                }
                if (dbDelivery.insertDeliveryOrderList(orderCode, itemsForDb)) {            // And then insert the data structure with the method
                    return orderCode;
                }
            }
            return null;
        }
        catch (SQLException e) {
            return null;
        }
    }

    // Sub method to create the order code to insert into the DB with the order data taken from the frontend
    private String createTheOrderCode() {

        // Using the java security random class to generate random numbers
        java.security.SecureRandom random = new java.security.SecureRandom();

        // List of characters that can be used for the generation of the order code
        final String charactersToUse = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);                    // String builder object to form the string with the characters

        // Loop to choose from the list of characters to use and form an array of 8 characters
        for (int i = 0; i < 8; i++) {
            // Append the char obtained randomly from the random choice from the list of chars to pick
            sb.append(charactersToUse.charAt(random.nextInt(charactersToUse.length())));
        }

        return sb.toString();       // Return the string obtained as code
    }



    /**** FROM BACKEND TO CLIENT SWING (DELIVERY SERVICE) ****/
    // Method to count the delivery orders available in the VPS db
    @GetMapping("/delivery/count")
    public ResponseEntity<Map<String, Integer>> getDeliveryOrdersCount() {

        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            int count = dbDelivery.countOrders();
            return ResponseEntity.ok(Map.of("count", count));
        }
        catch (SQLException e) {
            return ResponseEntity.status(503).body(Map.of("count", 0));
        }
    }

    // Method for the summary of all the orders (order code and status)
    @GetMapping("/delivery/orders-summary")
    public ResponseEntity<List<OrderSummaryDTO>> getOrdersSummary() {

        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            String[][] rawData = dbDelivery.getTheDeliveryOrderCodesAndStatus();
            List<OrderSummaryDTO> summaryList = new ArrayList<>();
            if (rawData != null) {
                for (String[] row : rawData) {
                    if (row.length >= 2) {
                        summaryList.add(new OrderSummaryDTO(row[0], row[1]));
                    }
                }
            }
            return ResponseEntity.ok(summaryList);
        }
        catch (SQLException e) {
            return ResponseEntity.status(503).build();
        }
    }

    // Method to retrieve the data of a single specified delivery order
    @GetMapping("/delivery/order-details/{orderCode}")
    public ResponseEntity<List<OrderItemDTO>> getOrderDetails(@PathVariable("orderCode") String orderCode) {

        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            Object[][] rawData = dbDelivery.getTheDeliveryOrderData(orderCode);
            List<OrderItemDTO> itemsList = new ArrayList<>();
            if (rawData != null) {
                for (Object[] row : rawData) {
                    String mealCode = row[0] != null ? row[0].toString() : "";
                    String mealName = row[1] != null ? row[1].toString() : "";
                    int quantity = 1;
                    if (row[2] instanceof Number) {
                        quantity = ((Number) row[2]).intValue();
                    } else if (row[2] != null) {
                        try {
                            quantity = Integer.parseInt(row[2].toString());
                        } catch (NumberFormatException ignored) {}
                    }
                    String specialReq = row.length > 3 && row[3] != null ? row[3].toString() : "";

                    itemsList.add(new OrderItemDTO(mealCode, mealName, quantity, specialReq));
                }
            }
            return ResponseEntity.ok(itemsList);
        }
        catch (SQLException e) {
            return ResponseEntity.status(503).build();
        }
    }

    // Method to update the delivery status of an order (awaiting - ongoing - ready)
    @PutMapping("/delivery/status")
    public ResponseEntity<?> updateOrderStatus(@RequestBody UpdateStatusRequestDTO body) {

        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            dbDelivery.updateStatusService(body.getOrderCode(), body.getStatus());
            return ResponseEntity.ok().build();
        }
        catch (SQLException e) {
            return ResponseEntity.status(503).body("Database non raggiungibile.");
        }
    }

    // Retrieve the full data for the delivery orders (in the client case to save them in the local cache)
    @GetMapping("/delivery/full-cards-data")
    public ResponseEntity<List<DeliveryOrdersFullListDTO>> getAllDeliveryCardsData() {

        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);

            // Fetch the list of orders and their status
            String[][] orders = dbDelivery.getTheDeliveryOrderCodesAndStatus();
            List<DeliveryOrdersFullListDTO> responseList = new ArrayList<>();

            if (orders != null) {                   // If the list is not null
                for (String[] order : orders) {
                    String orderCode = order[0];        // Order code
                    String orderStatus = order[1];      // Order status

                    // Fetch the details of the order data of the specified order code
                    Object[][] rawDetails = dbDelivery.getTheDeliveryOrderData(orderCode);
                    List<OrderItemDTO> items = new ArrayList<>();

                    if (rawDetails != null) {
                        for (Object[] row : rawDetails) {
                            String mealCode = row[0] != null ? row[0].toString() : "";
                            String mealName = row[1] != null ? row[1].toString() : "";
                            int quantity = 1;
                            if (row[2] instanceof Number) {
                                quantity = ((Number) row[2]).intValue();
                            } else if (row[2] != null) {
                                try {
                                    quantity = Integer.parseInt(row[2].toString());
                                } catch (NumberFormatException ignored) {}
                            }
                            String specialReq = row.length > 3 && row[3] != null ? row[3].toString() : "";
                            items.add(new OrderItemDTO(mealCode, mealName, quantity, specialReq));
                        }
                    }
                    responseList.add(new DeliveryOrdersFullListDTO(orderCode, orderStatus, items));
                }
            }
            return ResponseEntity.ok(responseList);
        }
        catch (SQLException e) {
            return ResponseEntity.status(503).build();
        }
    }

    // To get the orders complete info, so they can be shown in the all info table for the orders
    @GetMapping("/delivery/orders-info")
    public ResponseEntity<Object[][]> getDeliveryOrdersFullInfo() {
        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            Object[][] data = dbDelivery.getTheDeliveryOrderCodesAndTheirInfo();
            return ResponseEntity.ok(data != null ? data : new Object[0][0]);
        }
        catch (SQLException e) {
            System.err.println("Errore DB orders-info: " + e.getMessage());
            return ResponseEntity.status(503).body(new Object[0][0]);
        }
    }

    // To get the full info orders for the table show all delivery orders info from the show 'all delivery orders' button in the delivery service panel
    @GetMapping("/delivery/items-status")
    public ResponseEntity<Object[][]> getAllDeliveryOrderDataWithStatus() {
        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            Object[][] data = dbDelivery.getAllTheDeliveryOrderDataWithStatus();
            return ResponseEntity.ok(data != null ? data : new Object[0][0]);
        }
        catch (SQLException e) {
            System.err.println("Errore DB items-status: " + e.getMessage());
            return ResponseEntity.status(503).body(new Object[0][0]);
        }
    }

    // Endpoint method to delete an order
    @DeleteMapping("/delivery/delete-order/{orderCode}")
    public ResponseEntity<?> deleteDeliveryOrder(@PathVariable("orderCode") String orderCode) {
        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            dbDelivery.deleteDeliveryOrder(orderCode);
            return ResponseEntity.ok().build();
        }
        catch (SQLException e) {
            System.err.println("Errore DB delete delivery order: " + e.getMessage());
            return ResponseEntity.status(503).body("Database non raggiungibile.");
        }
    }
}