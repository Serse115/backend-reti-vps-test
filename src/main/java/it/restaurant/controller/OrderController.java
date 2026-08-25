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

    // Recuperiamo le credenziali dal file properties
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    //private final lib.dbComponents.DBConnectionClass connector = new lib.dbComponents.DBConnectionClass();
    // Iniettiamo il servizio email
    @Autowired
    private EmailService emailService;

    @Autowired
    private DataSource dataSource; // Gestisce automaticamente il pool di connessioni HikariCP

    // "Sala d'attesa" per gli ordini che aspettano la conferma OTP
    private final Map<String, OrderRequestDTO> pendingOrders = new ConcurrentHashMap<>();


    /**** FROM FRONTEND TO BACKEND ****/
    // Method to request the OTP
    @PostMapping("/request-otp")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> requestOtp(@RequestBody OrderRequestDTO request) {

        //this.connector.setDbUrl(this.dbUrl);

        try (Connection conn = this.dataSource.getConnection()) {
            //DBOTPOperationsInterface otpService = new DBOTPOperations(this.connector);
            DBOTPOperationsInterface otpService = new DBOTPOperations(conn);

            // Clear all the previously expired OTPs (Self-cleaning of expired items everytime a new OTP is requested from the frontend)
            otpService.cleanExpiredOTPs();

            // 1. Genera l'OTP
            String code = otpService.generateOTP();

            // 2. Salva l'OTP nel DB (per la scadenza e verifica)
            if (otpService.saveOTP(request.email, code)) {
                // 3. Parcheggia l'ordine in memoria usando l'email come chiave
                this.pendingOrders.put(request.email, request);

                // 4. TODO: Invia Email reale qui. Per ora stampiamo in console
                // --- CHIAMATA AL SERVIZIO EMAIL ---
                try {
                    //this.emailService.sendOtpEmail(request.email, code);
                    System.out.println(">>> OTP inviato a " + request.email + ": " + code);
                    return ResponseEntity.ok("OTP inviato all'email: " + request.email);
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
        //return ResponseEntity.status(503).body("Database non raggiungibile.");
    }


    // Method to submit the order from the front end call to the database
    @PostMapping("/verify-otp-and-submit")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> verifyOtpAndSubmit(@RequestParam("email") String email, @RequestParam("otp") String otp) {

        //this.connector.setDbUrl(dbUrl);

        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {
            //DBOTPOperationsInterface otpService = new DBOTPOperations(this.connector);
            DBOTPOperationsInterface otpService = new DBOTPOperations(conn);

            // 1. Verifica l'OTP tramite la tua classe (che lo cancella se valido)
            if (otpService.verifyOTP(email, otp)) {

                // 2. Recupera l'ordine parcheggiato
                OrderRequestDTO originalRequest = this.pendingOrders.get(email);

                if (originalRequest != null) {
                    // 3. Salva l'ordine definitivamente
                    String orderCode = this.executeFinalSubmit(originalRequest);

                    if (orderCode != null) {
                        this.pendingOrders.remove(email);      // Pulizia
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


    // Method send the resume email after the order confirmation
    @PostMapping("/send-order-resume-email")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> sendOrderResume(@RequestBody OrderRequestDTO request) {

        //this.connector.setDbUrl(dbUrl);

        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {

            try {
                //this.emailService.sendOrderConfirmationEmail(request.email, request);
                System.out.println(">>> Email di conferma e riepilogo inviata a " + request.email);
                return ResponseEntity.ok("Riepilogo di conferma inviato all'email: " + request.email);
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
    @CrossOrigin(origins = "*")
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
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OrderRequestDTO>> getDeliveryOrdersFullList() {

        //this.connector.setDbUrl(this.dbUrl);

        // Instanziare la classe di operazioni DB
        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {
            //DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(this.connector);
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
    // Metodo helper privato che contiene la tua vecchia logica di salvataggio
    private String executeFinalSubmit(OrderRequestDTO request) {

        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            final String orderCode = this.createTheOrderCode();

            // 1. Salviamo l'anagrafica usando i campi dell'oggetto DTO
            boolean infoSaved = dbDelivery.insertDeliveryOrderDataInfo(
                    orderCode,
                    request.name,
                    request.address,
                    request.cap,
                    request.city,
                    request.email,
                    request.phone,
                    "awaiting"
            );

            if (infoSaved) {
                // 2. Convertiamo la lista di DTO nel formato richiesto dal tuo metodo DB (List<Object[]>)
                List<Object[]> itemsForDb = new ArrayList<>();
                for (OrderItemDTO item : request.items) {
                    itemsForDb.add(new Object[]{item.getMealCode(), item.getQuantity()});
                }
                if (dbDelivery.insertDeliveryOrderList(orderCode, itemsForDb)) {
                    return orderCode;
                }
            }
            return null;
        } catch (SQLException e) {
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



















    @GetMapping("/today")
    public ResponseEntity<?> getTodayOrders() {

        //lib.dbComponents.DBConnectionClass connector = new lib.dbComponents.DBConnectionClass();

        // Inserisci qui le tue credenziali reali
        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
            //try (Connection conn = this.connector.returnConnection();
        try (Connection conn = this.dataSource.getConnection()) {
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT meal_code FROM order_management");

            List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString("meal_code"));
            }
            return ResponseEntity.ok(results);
        } catch (SQLException e) {
            return ResponseEntity.status(401).body("Impossibile stabilire una connessione al DB");
        }
    }

    @PostMapping("/submit")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> submitOrder(@RequestBody OrderRequestDTO request) {

        //if (this.connector.connectToDB(dbUser, dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {

            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            final String orderCode = this.createTheOrderCode();

            // 1. Salviamo l'anagrafica usando i campi dell'oggetto DTO
            boolean infoSaved = dbDelivery.insertDeliveryOrderDataInfo(
                    orderCode,
                    request.name,
                    request.address,
                    request.cap,
                    request.city,
                    request.email,
                    request.phone,
                    "awaiting" // Stato iniziale fisso
            );

            if (infoSaved) {
                // 2. Convertiamo la lista di DTO nel formato richiesto dal tuo metodo DB (List<Object[]>)
                // Oppure potresti aggiornare il metodo DB per accettare direttamente la lista di DTO!
                List<Object[]> itemsForDb = new ArrayList<>();
                for (OrderItemDTO item : request.items) {
                    itemsForDb.add(new Object[]{item.getMealCode(), item.getQuantity()});
                }

                boolean listSaved = dbDelivery.insertDeliveryOrderList(orderCode, itemsForDb);

                if (listSaved) {
                    return ResponseEntity.ok(orderCode);
                }
            }

            return ResponseEntity.status(500).body("Errore nel salvataggio dell'ordine");

        } catch (SQLException e) {
            System.out.println("DB NON CONNESSO!");
            return null;
        }
    }



    /**** FROM BACKEND TO CLIENT SWING (DELIVERY SERVICE) ****/
    // 1. Endpoint per il conteggio ordini (Timer 5s)
    @GetMapping("/delivery/count")
    @CrossOrigin(origins = "*")
    public ResponseEntity<Map<String, Integer>> getDeliveryOrdersCount() {
        //this.connector.setDbUrl(this.dbUrl);

        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {
            //DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(this.connector);
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            int count = dbDelivery.countOrders();
            return ResponseEntity.ok(Map.of("count", count));
        }
        catch (SQLException e) {
            return ResponseEntity.status(503).body(Map.of("count", 0));
        }
    }

    // 2. Endpoint per il riepilogo ordini (Codice + Stato per creare le card)
    @GetMapping("/delivery/orders-summary")
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OrderSummaryDTO>> getOrdersSummary() {
        //this.connector.setDbUrl(this.dbUrl);

        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {
            //DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(this.connector);
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

    // 3. Endpoint per i dettagli dei piatti di un singolo ordine
    @GetMapping("/delivery/order-details/{orderCode}")
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OrderItemDTO>> getOrderDetails(@PathVariable("orderCode") String orderCode) {
        //this.connector.setDbUrl(this.dbUrl);

        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {
            //DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(this.connector);
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

    // 4. Endpoint per aggiornare lo stato di un ordine (es. Ongoing -> Ready)
    @PutMapping("/delivery/status")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> updateOrderStatus(@RequestBody UpdateStatusRequestDTO body) {
        //this.connector.setDbUrl(this.dbUrl);

        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {
            //DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(this.connector);
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            dbDelivery.updateStatusService(body.getOrderCode(), body.getStatus());
            return ResponseEntity.ok().build();
        }
        catch (SQLException e) {
            return ResponseEntity.status(503).body("Database non raggiungibile.");
        }
    }

    // Retrieve the full data for the delivery orders (in the client case to save them in the local cache)
    // GET /order_management/delivery/full-cards-data
    @GetMapping("/delivery/full-cards-data")
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<DeliveryOrdersFullListDTO>> getAllDeliveryCardsData() {
        //this.connector.setDbUrl(this.dbUrl);

        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {
            //DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(this.connector);
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);

            // 1. Recupera la lista degli ordini e stati
            String[][] orders = dbDelivery.getTheDeliveryOrderCodesAndStatus();
            List<DeliveryOrdersFullListDTO> responseList = new ArrayList<>();

            if (orders != null) {
                for (String[] order : orders) {
                    String orderCode = order[0];
                    String orderStatus = order[1];

                    // 2. Recupera i piatti (avviene localmente via socket MySQL sulla VPS a latenza zero)
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

    // To get the orders complete info so they can be shown in the all info table for the orders
    @GetMapping("/delivery/orders-info")
    @CrossOrigin(origins = "*")
    public ResponseEntity<Object[][]> getDeliveryOrdersFullInfo() {
        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            Object[][] data = dbDelivery.getTheDeliveryOrderCodesAndTheirInfo();
            return ResponseEntity.ok(data != null ? data : new Object[0][0]);
        } catch (SQLException e) {
            System.err.println("Errore DB orders-info: " + e.getMessage());
            return ResponseEntity.status(503).body(new Object[0][0]);
        }
    }

    // To get the full info orders for the table show all delivery orders info from the show 'all delivery orders' button in the delivery service panel
    @GetMapping("/delivery/items-status")
    @CrossOrigin(origins = "*")
    public ResponseEntity<Object[][]> getAllDeliveryOrderDataWithStatus() {
        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            Object[][] data = dbDelivery.getAllTheDeliveryOrderDataWithStatus();
            return ResponseEntity.ok(data != null ? data : new Object[0][0]);
        } catch (SQLException e) {
            System.err.println("Errore DB items-status: " + e.getMessage());
            return ResponseEntity.status(503).body(new Object[0][0]);
        }
    }

    // Endpoint method to delete an order
    @DeleteMapping("/delivery/delete-order/{orderCode}")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> deleteDeliveryOrder(@PathVariable("orderCode") String orderCode) {
        try (Connection conn = this.dataSource.getConnection()) {
            DBDeliveryOperationsInterface dbDelivery = new DBDeliveryOperations(conn);
            dbDelivery.deleteDeliveryOrder(orderCode);
            return ResponseEntity.ok().build();
        } catch (SQLException e) {
            System.err.println("Errore DB delete delivery order: " + e.getMessage());
            return ResponseEntity.status(503).body("Database non raggiungibile.");
        }
    }
}

/*

@GetMapping("/menu")
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<MealDTO>> getFullMenu() {

        //this.connector.setDbUrl(this.dbUrl);

        // Instanziare la classe di operazioni DB
        //if (this.connector.connectToDB(this.dbUser, this.dbPassword)) {
        try (Connection conn = this.dataSource.getConnection()) {
            //DBMenuOperationsInterface dbMenu = new DBMenuOperations(this.connector);
            DBMenuOperationsInterface dbMenu = new DBMenuOperations(conn);
            List<MealDTO> menu = dbMenu.getMenuData(1);

            return ResponseEntity.ok(menu);

        } catch (SQLException e) {
            System.out.println("DB NON CONNESSO!");
            return null;
        }
    }


 */