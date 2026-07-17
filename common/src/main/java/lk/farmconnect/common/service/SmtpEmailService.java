package lk.farmconnect.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public SmtpEmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> context) {
        try {
            Context thymeleafContext = new Context();
            if (context != null) {
                context.forEach(thymeleafContext::setVariable);
            }

            String htmlContent = templateEngine.process(templateName, thymeleafContext);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@farmconnect.lk");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent to {} with subject: {}", to, subject);

        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}