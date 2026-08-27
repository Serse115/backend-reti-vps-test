package it.restaurant.controller;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import it.restaurant.dto.OrderItemDTO;
import it.restaurant.dto.OrderRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String apiKey;

    @Value("${app.mail.from:Progretiurb <ordini@progretiurb.com>}")
    private String fromEmail;

    public void sendOtpEmail(String to, String otp) {
        Resend resend = new Resend(apiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject("Il tuo codice OTP per confermare l'ordine")
                .html("<strong>Il tuo codice di verifica è: " + otp + "</strong> <br>Usalo per confermare sulla pagina dell'ordine. <br>Se non sei stato tu a richiederlo, <strong>ignora</strong> questa email.")
                .build();

        try {
            resend.emails().send(params);
            System.out.println("Email inviata con successo tramite Resend API!");
        } catch (Exception e) {
            System.err.println("Errore Resend: " + e.getMessage());
            throw new RuntimeException("Fallimento invio email API");
        }
    }

    public void sendOrderConfirmationEmail(String to, OrderRequestDTO request) {
        Resend resend = new Resend(apiKey);
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<h2>Grazie per aver effettuato l'ordine!</h2>");
        htmlBuilder.append("<p>Riepilogo dati personali:</p>");

        htmlBuilder.append("<p>Nominativo ordine: <strong>")
                .append(request.getName())
                .append("</strong></p>");

        htmlBuilder.append("<p>Indirizzo: <strong>")
                .append(request.getAddress())
                .append("</strong></p>");

        htmlBuilder.append("<p>Città: <strong>")
                .append(request.getCity())
                .append("</strong></p>");

        htmlBuilder.append("<p>CAP: <strong>")
                .append(request.getCap())
                .append("</strong></p>");

        htmlBuilder.append("<p>Telefono: <strong>")
                .append(request.getPhone())
                .append("</strong></p>");

        htmlBuilder.append("<p>Riepilogo dei piatti scelti:</p>");
        htmlBuilder.append("<ul>");

        for (OrderItemDTO item : request.getItems()) {
            htmlBuilder.append("<li>")
                    .append("Codice Piatto: <strong>")
                    .append(item.getMealCode())
                    .append("</strong> ")
                    .append("- Nome piatto: <strong>")
                    .append(item.getMealName())
                    .append("</strong> ")
                    .append("- Quantità: <strong>")
                    .append(item.getQuantity())
                    .append("</strong>")
                    .append("</li>");
        }
        htmlBuilder.append("</ul>");

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject("Riepilogo ordine")
                .html(htmlBuilder.toString())
                .build();

        try {
            resend.emails().send(params);
            System.out.println("Email inviata con successo tramite Resend API!");
        } catch (Exception e) {
            System.err.println("Errore Resend: " + e.getMessage());
            throw new RuntimeException("Fallimento invio email API");
        }
    }
}