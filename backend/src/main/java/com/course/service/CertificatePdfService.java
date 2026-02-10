package com.course.service;

import com.course.entity.Course;
import com.course.entity.User;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


@Service
@RequiredArgsConstructor
public class CertificatePdfService {

    // Шапка и шрифт
    private static final String ORG_NAME = "ШАРИК";
    private static final String FONT_PATH = "fonts/DejaVuSans.ttf";

    // Входные данные
    public byte[] generateCourseCertificate(Course course,
                                            String teacherName,
                                            User student,
                                            int earnedPoints,
                                            int maxPoints) {
        // Название курса
        String courseName = (course != null && course.getName() != null && !course.getName().isBlank())
                ? course.getName()
                : "Курс";
        // Имя ученика
        String studentName = (student != null && student.getName() != null && !student.getName().isBlank())
                ? student.getName()
                : "Ученик";
        // Имя учителя
        String teacher = (teacherName == null || teacherName.isBlank()) ? "—" : teacherName;

        // создаем pdf
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             InputStream fontStream = new ClassPathResource(FONT_PATH).getInputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType0Font font = PDType0Font.load(doc, fontStream, true);
            PDRectangle mediaBox = page.getMediaBox();
            float width = mediaBox.getWidth();
            float height = mediaBox.getHeight();

            // контент в пдф документе
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 48f;
                cs.setLineWidth(2.5f);
                cs.addRect(margin, margin, width - 2 * margin, height - 2 * margin);
                cs.stroke();

                float centerX = width / 2f;
                float contentTop = height - margin - 100f;
                float maxTextWidth = width - 2 * margin - 80f;

                float y = contentTop;

                y = writeCenteredLine(cs, font, 22f, ORG_NAME, centerX, y);
                y -= 20f;

                y = writeCenteredLine(cs, font, 48f, "СЕРТИФИКАТ", centerX, y);
                y -= 8f;

                y = writeCenteredLine(cs, font, 18f, "о прохождении курса", centerX, y);

                y -= 20f;
                cs.moveTo(margin + 80f, y);
                cs.lineTo(width - margin - 80f, y);
                cs.stroke();
                y -= 30f;

                String courseLine = "«" + courseName + "»";
                float courseFont = fitFontSize(font, courseLine, maxTextWidth, 32f, 18f);
                y = writeCenteredLine(cs, font, courseFont, courseLine, centerX, y);
                y -= 20f;

                String studentLine = "Выдан: " + studentName;
                float studentFont = fitFontSize(font, studentLine, maxTextWidth, 18f, 12f);
                y = writeCenteredLine(cs, font, studentFont, studentLine, centerX, y);
                y -= 8f;

                String teacherLine = "Преподаватель: " + teacher;
                float teacherFont = fitFontSize(font, teacherLine, maxTextWidth, 16f, 10f);
                y = writeCenteredLine(cs, font, teacherFont, teacherLine, centerX, y);
                y -= 8f;

                String scoreLine = "Баллы за курс: " + earnedPoints + " из " + maxPoints;
                y = writeCenteredLine(cs, font, 16f, scoreLine, centerX, y);
                y -= 8f;

                // дата выдачи
                String date = LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru")));
                y = writeCenteredLine(cs, font, 14f, "Дата выдачи: " + date, centerX, y);

                float footerY = margin + 26f;
                writeCentered(cs, font, 10f,
                        "Этот сертификат сформирован автоматически в системе обучения.",
                        width / 2f,
                        footerY);
            }

            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка генерации PDF!", e);
        }
    }

    // Центрирование текста

    private float writeCentered(PDPageContentStream cs,
                                PDType0Font font,
                                float fontSize,
                                String text,
                                float centerX,
                                float baselineY) throws IOException {
        if (text == null) text = "";
        float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
        float x = centerX - (textWidth / 2f);

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, baselineY);
        cs.showText(text);
        cs.endText();
        return baselineY;
    }

    private float writeCenteredLine(PDPageContentStream cs,
                                    PDType0Font font,
                                    float fontSize,
                                    String text,
                                    float centerX,
                                    float baselineY) throws IOException {
        writeCentered(cs, font, fontSize, text, centerX, baselineY);
        float leading = Math.max(16f, fontSize * 1.4f);
        return baselineY - leading;
    }

    // размер шрифта
    private float fitFontSize(PDType0Font font,
                              String text,
                              float maxWidth,
                              float desiredFont,
                              float minFont) throws IOException {
        if (text == null) return desiredFont;
        float size = desiredFont;
        while (size > minFont) {
            float w = (font.getStringWidth(text) / 1000f) * size;
            if (w <= maxWidth) return size;
            size -= 1f;
        }
        return minFont;
    }
}
