package com.course.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Email sending abstraction.
 *
 * Intentionally minimal: no DTOs, only primitives.
 * Attachments are supported for future PDF sending.
 */
public interface MailService {

    void sendToUser(@NotNull Integer userId,
                    @NotBlank String subject,
                    @NotBlank String text);

    void sendToUserWithAttachment(@NotNull Integer userId,
                                  @NotBlank String subject,
                                  @NotBlank String text,
                                  String attachmentFilename,
                                  byte[] attachmentBytes,
                                  String contentType);
}
