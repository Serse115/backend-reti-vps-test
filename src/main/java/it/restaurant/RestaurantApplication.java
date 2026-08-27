package it.restaurant;

import lib.dbComponents.DBConnectionClass;
import lib.dbComponents.DBConnectionClassInterface;
import lib.dbComponents.DBCreationOperations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// SPRING BOOT APPLICATION CLASS
@SpringBootApplication
public class RestaurantApplication {

    // Importing the credentials from the Spring's environment variables
    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public static void main(String[] args) {
        SpringApplication.run(RestaurantApplication.class, args);
    }

    // Check to control the database existence and initialize it
    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            System.out.println(">>> [INIT] Avvio controllo consistenza Database...");

            DBConnectionClassInterface connector = new DBConnectionClass();
            String fullDbUrl = "jdbc:mysql://127.0.0.1:3306/restaurant?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&protocol=tcp";
            String serverUrl = "jdbc:mysql://127.0.0.1:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&protocol=tcp";

            // Direct connection attempt through the full db url
            connector.setDbUrl(fullDbUrl);

            if (connector.connectToDB(dbUser, dbPassword)) {        // If it works give a positive feedback and consider it ready
                System.out.println(">>> [INIT] Connessione riuscita. Il database 'restaurant' è pronto e operativo.");
                connector.closeConnectionToDB();
            } else {                                                // If it doesn't, start the initalization process of the DB
                System.out.println(">>> [INIT] Il database non esiste. Procedo con l'installazione automatica (Web Mode)...");

                // DB doesn't exist so the connection is to the server itself and not the db itself
                connector.setDbUrl(serverUrl);
                if (connector.connectToDB(dbUser, dbPassword)) {
                    System.out.println(">>> [INIT] Connesso al server MySQL base. Avvio script di creazione...");

                    try {
                        // DB creation operations comes in action to create and initialize the db to use
                        DBCreationOperations creationOperations = new DBCreationOperations(connector);
                        creationOperations.fullDBCreation(dbUser, dbPassword);

                        System.out.println(">>> [INIT] Setup completato! Database 'restaurant', tabelle e dati generati con successo.");
                    }
                    catch (Exception e) {
                        System.err.println(">>> [INIT] Errore critico durante l'esecuzione di fullDBCreation: " + e.getMessage());
                        System.err.println(">>> [INIT] CONTROLLA LA PRESENZA DELLE TABELLE, L'ERRORE POTREBBE ESSERE ANCHE SOLO DI TIMING " + e.getMessage());
                    }
                    finally {
                        connector.closeConnectionToDB();
                    }
                }
                else {
                    System.err.println(">>> [INIT] ERRORE CRITICO: Impossibile connettersi al server MySQL base. Controlla che le credenziali siano corrette e che MySQL sia attivo.");
                }
            }
        };
    }
}