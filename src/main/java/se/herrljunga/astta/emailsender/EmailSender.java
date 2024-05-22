package se.herrljunga.astta.emailsender;

import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.ConfigLoader;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailSender {
    Config config = ConfigLoader.loadConfig();

    private String smtpHost;
    private String smtpPort;
    private String fromAddress; // Server name

    public EmailSender(String smtpHost, String smtpPort, String fromAddress) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.fromAddress = fromAddress;
    }

    public void sendEmail(String toAddress, String subject, String message) throws MessagingException {
        // Set up the SMTP server properties
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true"); // TLS

        // Create a session with an authenticator
        Authenticator auth = new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromAddress, "");
            }
        };
        Session session = Session.getInstance(properties, auth);

        // Create the email message
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(fromAddress));
        InternetAddress[] toAddresses = {new InternetAddress(toAddress)};
        msg.setRecipients(Message.RecipientType.TO, toAddresses);
        msg.setSubject(subject);
        msg.setSentDate(new java.util.Date());
        msg.setText(message);

        // Send the email
        Transport.send(msg);
    }
}
