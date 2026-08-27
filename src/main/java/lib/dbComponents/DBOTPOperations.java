package lib.dbComponents;

import java.sql.*;
import java.time.LocalDateTime;

/******** CLASS FOR DATABASE OTP RELATED OPERATIONS ********/
public class DBOTPOperations implements DBOTPOperationsInterface {

    /**** Fields ****/
    // Variables
    private final DBConnectionClassInterface connector;         // Connector object
    private final Connection directConnection;                  // Connector for Spring DataSource


    /**** Constructors ****/
    public DBOTPOperations(final DBConnectionClassInterface connector) {
        this.connector = connector;
        this.directConnection = null;
    }

    // Constructor for Spring Boot / HikariCP Pool
    public DBOTPOperations(final Connection connection) {
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

    // Method to create the OTP code made by 6 numerical digits
    public String generateOTP() {

        int code = (int)(Math.random() * 900000) + 100000; // Between 100000 & 999999
        return String.valueOf(code);
    }

    // Method to save the OTP, checking if it exists already and in case switch it and update its duration
    public boolean saveOTP(String email, String code) {

        Connection conn = this.getConnection();
        if (conn == null) {
            return false;
        }

        // Code expires in 5 minutes
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);
        Timestamp otpExpiry = Timestamp.valueOf(expiry);

        // On duplicate key update to do both insert and update in one go
        String sql = """
            INSERT INTO otp_codes (email, otp_code, expiry_time)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE otp_code = VALUES(otp_code), expiry_time = VALUES(expiry_time)
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, code);
            pstmt.setTimestamp(3, otpExpiry);
            pstmt.executeUpdate();
            return true;
        }
        catch (SQLException e) {
            System.err.println("Could not send the OTP");
            e.printStackTrace();
            return false;
        }
    }

    // Method to verify the OTP and return if the verification turned out correct or not or if it's expired
    public boolean verifyOTP(String email, String inputCode) {

        Connection conn = this.getConnection();
        if (conn == null) {
            return false;
        }

        String sql = "SELECT otp_code, expiry_time FROM otp_codes WHERE email = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbCode = rs.getString("otp_code");
                Timestamp expiry = rs.getTimestamp("expiry_time");


                // If code is correct and is not expired
                if (dbCode.equals(inputCode) && expiry.after(new Timestamp(System.currentTimeMillis()))) {
                    deleteOTP(email);   // Delete the code (one use only)
                    return true;
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Method to clean the expired OTPs from the DB
    public void cleanExpiredOTPs() {

        Connection conn = this.getConnection();
        if (conn == null) {
            return;
        }

        String sql = "DELETE FROM otp_codes WHERE expiry_time < CURRENT_TIMESTAMP";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                System.out.println("Cleaned " + deleted + " expired OTP codes from database.");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Sub method to delete the OTPs linked to the chosen email
    private void deleteOTP(String email) {

        Connection conn = this.getConnection();
        if (conn == null) {
            return;
        }

        String sql = "DELETE FROM otp_codes WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}