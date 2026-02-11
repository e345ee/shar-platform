package com.course.service;

import com.course.dto.lesson.LessonPresentationInfoResponse;
import com.course.entity.Lesson;
import com.course.exception.LessonPresentationNotFoundException;
import com.course.exception.LessonPresentationValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonPresentationSlideService {

    private static final int DEFAULT_DPI = 144;
    private static final int MIN_DPI = 72;
    private static final int MAX_DPI = 300;

    private final LessonService lessonService;
    private final LessonPresentationStorageService storageService;

    public LessonPresentationInfoResponse getPresentationInfo(Integer lessonId) {
        Lesson lesson = lessonService.getEntityByIdForCurrentUser(lessonId);

        if (!StringUtils.hasText(lesson.getPresentationUrl())) {
            return new LessonPresentationInfoResponse(false, 0);
        }

        int pages = getPageCount(lesson);
        return new LessonPresentationInfoResponse(true, pages);
    }

    public ResponseEntity<byte[]> renderPageAsPng(Integer lessonId, int pageNumber, Integer dpi) {
        Lesson lesson = lessonService.getEntityByIdForCurrentUser(lessonId);

        if (!StringUtils.hasText(lesson.getPresentationUrl())) {
            throw new LessonPresentationNotFoundException("Lesson presentation not found");
        }

        int actualDpi = dpi == null ? DEFAULT_DPI : dpi;
        if (actualDpi < MIN_DPI || actualDpi > MAX_DPI) {
            throw new LessonPresentationValidationException("Invalid dpi. Allowed range: " + MIN_DPI + ".." + MAX_DPI);
        }

        byte[] pdfBytes = storageService.downloadByPublicUrl(lesson.getPresentationUrl());
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new LessonPresentationNotFoundException("Lesson presentation not found");
        }

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int pageCount = doc.getNumberOfPages();
            if (pageCount <= 0) {
                throw new LessonPresentationValidationException("Presentation has no pages");
            }
            if (pageNumber < 1 || pageNumber > pageCount) {
                throw new LessonPresentationValidationException("Page number is out of range (1.." + pageCount + ")");
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, actualDpi, ImageType.RGB);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            byte[] bytes = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);
        } catch (IOException e) {
            throw new LessonPresentationValidationException("Failed to process PDF presentation");
        }
    }

    private int getPageCount(Lesson lesson) {
        byte[] pdfBytes = storageService.downloadByPublicUrl(lesson.getPresentationUrl());
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new LessonPresentationNotFoundException("Lesson presentation not found");
        }

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return Math.max(doc.getNumberOfPages(), 0);
        } catch (IOException e) {
            throw new LessonPresentationValidationException("Failed to process PDF presentation");
        }
    }
}
