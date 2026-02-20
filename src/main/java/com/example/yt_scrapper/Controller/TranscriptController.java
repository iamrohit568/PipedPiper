package com.example.yt_scrapper.Controller;

import com.example.yt_scrapper.Config.RateLimiter;
import com.example.yt_scrapper.Config.GlobalExceptionHandler.*;
import com.example.yt_scrapper.Service.TranscriptService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
public class TranscriptController {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptController.class);

    @Autowired
    private TranscriptService transcriptService;

    @Autowired
    private RateLimiter rateLimiter;

    /**
     * Show the transcript analyzer page.
     */
    @GetMapping("/transcript")
    public String showTranscriptPage() {
        return "transcript-analyzer";
    }

    /**
     * Analyze a YouTube video — extract transcript and generate AI summary.
     */
    @PostMapping("/transcript/analyze")
    public String analyzeVideo(
            @RequestParam String videoUrl,
            Model model,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        // Rate limit check
        if (!rateLimiter.isAnalyzeAllowed(clientIp)) {
            model.addAttribute("errorTitle", "Rate Limit Exceeded");
            model.addAttribute("errorMessage", "You've made too many requests. Please wait a moment and try again.");
            model.addAttribute("errorType", "ratelimit");
            return "transcript-analyzer";
        }

        try {
            Map<String, Object> result = transcriptService.analyzeVideo(videoUrl);

            model.addAttribute("videoId", result.get("videoId"));
            model.addAttribute("videoTitle", result.get("videoTitle"));
            model.addAttribute("summary", result.get("summary"));
            model.addAttribute("transcript", result.get("transcript"));
            model.addAttribute("transcriptLength", result.get("transcriptLength"));
            model.addAttribute("cached", result.get("cached"));
            model.addAttribute("analyzed", true);

            return "transcript-analyzer";

        } catch (InvalidVideoUrlException e) {
            model.addAttribute("errorTitle", "Invalid URL");
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("errorType", "validation");
            model.addAttribute("videoUrl", videoUrl);
            return "transcript-analyzer";
        } catch (TranscriptNotAvailableException e) {
            model.addAttribute("errorTitle", "Transcript Not Available");
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("errorType", "transcript");
            model.addAttribute("videoUrl", videoUrl);
            return "transcript-analyzer";
        } catch (GeminiApiException e) {
            model.addAttribute("errorTitle", "AI Service Error");
            model.addAttribute("errorMessage",
                    "The AI summarization service is temporarily unavailable. Please try again.");
            model.addAttribute("errorType", "ai");
            model.addAttribute("videoUrl", videoUrl);
            return "transcript-analyzer";
        } catch (Exception e) {
            logger.error("Unexpected error analyzing video: {}", videoUrl, e);
            model.addAttribute("errorTitle", "Something Went Wrong");
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            model.addAttribute("errorType", "generic");
            model.addAttribute("videoUrl", videoUrl);
            return "transcript-analyzer";
        }
    }

    /**
     * Chat with the video transcript — AJAX endpoint.
     */
    @PostMapping("/api/transcript/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chatWithVideo(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        // Rate limit check
        if (!rateLimiter.isChatAllowed(clientIp)) {
            return ResponseEntity.status(429).body(Map.of(
                    "error", true,
                    "message", "Too many requests. Please wait a moment before sending another message."));
        }

        try {
            String videoId = (String) body.get("videoId");
            String question = (String) body.get("question");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> chatHistory = (List<Map<String, String>>) body.getOrDefault("chatHistory",
                    new ArrayList<>());

            if (videoId == null || videoId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", true,
                        "message", "Video ID is required."));
            }

            if (question == null || question.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", true,
                        "message", "Please enter a question."));
            }

            // Limit question length
            if (question.length() > 1000) {
                question = question.substring(0, 1000);
            }

            String answer = transcriptService.chatWithVideo(videoId, question, chatHistory);

            Map<String, Object> response = new HashMap<>();
            response.put("error", false);
            response.put("answer", answer);
            return ResponseEntity.ok(response);

        } catch (TranscriptNotAvailableException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()));
        } catch (GeminiApiException e) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", true,
                    "message", "AI service is temporarily unavailable. Please try again."));
        } catch (Exception e) {
            logger.error("Chat error", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", true,
                    "message", "An error occurred. Please try again."));
        }
    }

    /**
     * Get client IP address, handling proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
