package com.course.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


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
