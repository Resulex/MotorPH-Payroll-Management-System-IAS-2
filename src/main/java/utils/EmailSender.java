/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

/**
 *
 * @author Alex Resurreccion
 */
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailSender {
    private static String from = "alexmotorph.noreply@gmail.com";
    private static String password = System.getenv("MOTORPH_EMAIL_PASSWORD");
    
    public EmailSender() {}
    
    public EmailSender(String from, String password){
    }
    
    public static boolean sendEmail(String to, String subject, String body) {
        if (password == null || password.isBlank()) {
            System.err.println("Email sending is not configured. Set MOTORPH_EMAIL_PASSWORD.");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email sent successfully");
            return true;
        } catch (MessagingException e) {
            System.err.println("Something went wrong while submitting email");
            e.printStackTrace();
        }
        return false;
    }
}

