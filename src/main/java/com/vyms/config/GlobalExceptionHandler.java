package com.vyms.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Central place to handle web exceptions from all controllers.
 *
 * {@code @ControllerAdvice} lets Spring apply this handler globally,
 * so we do not repeat the same try/catch in many controllers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles file uploads that exceed the configured maximum size.
     *
     * We redirect back to the vehicle page with a flash message so the user can
     * immediately understand what went wrong and retry with a smaller file.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("uploadError", "Image file is too large. Please upload a file under 10 MB.");
        return "redirect:/inventory/vehicles";
    }
}

