package com.course.service;

import com.course.config.MailProperties;
import com.course.entity.User;
import com.course.exception.InvalidEmailException;
import com.course.exception.MailNotConfiguredException;
import com.course.exception.MailSendingException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Validated
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final MailProperties mailProperties;

    
    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override
    public void sendToUser(Integer userId,
                           String subject,
                           String text) {
        sendToUserWithAttachment(userId, subject, text, null, null, null);
    }

    @Override
    public void sendToUserWithAttachment(Integer userId,
                                         String subject,
                                         String text,
                                         String attachmentFilename,
                                         byte[] attachmentBytes,
                                         String contentType) {
        if (!mailProperties.isEnabled()) {
            throw new MailNotConfiguredException("Email sending is disabled. Set APP_MAIL_ENABLED=true");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        String to = user.getEmail();
        if (to == null || to.isBlank()) {
            throw new InvalidEmailException("User has no email");
        }
        if (!SIMPLE_EMAIL.matcher(to).matches()) {
            throw new InvalidEmailException("User email has invalid format");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            boolean multipart = attachmentBytes != null && attachmentBytes.length > 0;
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            if (multipart) {
                String safeFilename = (attachmentFilename == null || attachmentFilename.isBlank())
                        ? "attachment" : attachmentFilename;
                String safeContentType = (contentType == null || contentType.isBlank())
                        ? "application/octet-stream" : contentType;
                helper.addAttachment(safeFilename, new org.springframework.core.io.ByteArrayResource(attachmentBytes), safeContentType);
            }

            mailSender.send(mimeMessage);
        } catch (MailException | MessagingException ex) {
            throw new MailSendingException("Failed to send email", ex);
        }
    }
}
