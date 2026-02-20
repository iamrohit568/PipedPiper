package com.example.yt_scrapper.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Custom exception classes (static inner classes for simplicity)
    public static class TranscriptNotAvailableException extends RuntimeException {
        public TranscriptNotAvailableException(String message) {
            super(message);
        }
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }

    public static class InvalidVideoUrlException extends RuntimeException {
        public InvalidVideoUrlException(String message) {
            super(message);
        }
    }

    public static class GeminiApiException extends RuntimeException {
        public GeminiApiException(String message) {
            super(message);
        }

        public GeminiApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @ExceptionHandler(TranscriptNotAvailableException.class)
    public String handleTranscriptNotAvailable(TranscriptNotAvailableException ex, Model model) {
        logger.warn("Transcript not available: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Transcript Not Available");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorType", "transcript");
        return "transcript-analyzer";
    }

    @ExceptionHandler(InvalidVideoUrlException.class)
    public String handleInvalidUrl(InvalidVideoUrlException ex, Model model) {
        logger.warn("Invalid video URL: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Invalid URL");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorType", "validation");
        return "transcript-analyzer";
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, String> handleRateLimit(RateLimitExceededException ex) {
        logger.warn("Rate limit exceeded: {}", ex.getMessage());
        return Map.of(
                "error", "Rate limit exceeded",
                "message", ex.getMessage());
    }

    @ExceptionHandler(GeminiApiException.class)
    public String handleGeminiError(GeminiApiException ex, Model model) {
        logger.error("Gemini API error: {}", ex.getMessage(), ex);
        model.addAttribute("errorTitle", "AI Service Error");
        model.addAttribute("errorMessage",
                "The AI summarization service is temporarily unavailable. Please try again in a moment.");
        model.addAttribute("errorType", "ai");
        return "transcript-analyzer";
    }
}
