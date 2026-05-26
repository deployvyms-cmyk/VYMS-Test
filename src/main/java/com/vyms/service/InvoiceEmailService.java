package com.vyms.service;

import com.vyms.entity.Sale;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

/**
 * Sends invoice emails using a dedicated HTML email template.
 */
@Service
public class InvoiceEmailService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InvoiceEmailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from:}")
    private String mailFrom;

    public InvoiceEmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public InvoiceSendResult sendInvoiceEmail(Sale sale) {
        if (sale == null || sale.getEmail() == null || sale.getEmail().isBlank()) {
            return InvoiceSendResult.failure("missing_email");
        }

        try {
            String subject = "Vehicle Yard Invoice INV-" + sale.getId();
            Context context = new Context();
            context.setVariable("sale", sale);
            context.setVariable("invoiceNumber", "INV-" + sale.getId());

            String html = templateEngine.process("email/invoice-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setTo(sale.getEmail());
            helper.setSubject(subject);
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            helper.setText(html, true);

            mailSender.send(message);
            return InvoiceSendResult.success(sale.getEmail());
        } catch (MessagingException | RuntimeException ex) {
            logger.error("Invoice email send failed for saleId={} to={}.",
                    sale != null ? sale.getId() : null,
                    sale != null ? sale.getEmail() : null,
                    ex);
            return InvoiceSendResult.failure("mail_error");
        }
    }
}
