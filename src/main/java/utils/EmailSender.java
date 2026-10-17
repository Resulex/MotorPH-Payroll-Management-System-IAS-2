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
    private final String from;
    private final String password;
    private final String host;
    private final String port;

    public EmailSender() {
        this(AppConfig.mailFrom(), AppConfig.mailPassword());
    }

    public EmailSender(String from, String password) {
        this.from = from;
        this.password = password;
        this.host = AppConfig.mailHost();
        this.port = AppConfig.mailPort();
    }

    public boolean sendEmail(String to, String subject, String body) {
        if (from == null || password == null) {
            System.err.println("Email not sent: mail.from and mail.password are not configured. "
                    + "Set MOTORPH_MAIL_USERNAME and MOTORPH_MAIL_PASSWORD, or add them to "
                    + AppConfig.CONFIG_FILE_NAME + ".");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
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

