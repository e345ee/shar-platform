package com.course.dto.lesson;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class LessonUpsertForm {

    
    
    
    @Min(value = 1, message = "orderIndex must be >= 1")
    private Integer orderIndex;

    @NotBlank(message = "title is required")
    @Size(min = 1, max = 127, message = "title must be between 1 and 127 characters")
    private String title;

    @Pattern(regexp = "^(?!\\s*$).+", message = "description must not be blank")
    @Size(max = 2048, message = "description must be at most 2048 characters")
    private String description;

    @NotNull(message = "presentation file is required")
    private MultipartFile presentation;

    

    public Integer getOrderIndex() {
        return this.orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile getPresentation() {
        return this.presentation;
    }

    public void setPresentation(MultipartFile presentation) {
        this.presentation = presentation;
    }

}
