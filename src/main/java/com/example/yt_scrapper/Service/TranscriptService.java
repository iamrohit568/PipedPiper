package com.example.yt_scrapper.Service;

import com.example.yt_scrapper.Config.GlobalExceptionHandler.*;
import com.example.yt_scrapper.Model.TranscriptCache;
import com.example.yt_scrapper.Repository.TranscriptCacheRepository;

import io.github.thoroldvix.api.TranscriptApiFactory;
import io.github.thoroldvix.api.YoutubeTranscriptApi;
import io.github.thoroldvix.api.TranscriptContent;
import io.github.thoroldvix.api.TranscriptFormatters;
import io.github.thoroldvix.api.TranscriptFormatter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TranscriptService {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptService.class);

    // AI Provider config — "gemini" or "groq"
    @Value("${ai.provider:groq}")
    private String aiProvider;

    // Gemini config
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    // Groq config
    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    // Shared config
    @Value("${ai.temperature:0.3}")
    private double aiTemperature;

    @Value("${ai.max.tokens:4096}")
    private int aiMaxTokens;

    @Autowired
    private TranscriptCacheRepository cacheRepository;

    @Autowired
    private ytservice youtubeService;

    // Proxy config
    @Value("${proxy.host:}")
    private String proxyHost;

    @Value("${proxy.port:}")
    private String proxyPort;

    @Value("${proxy.user:}")
    private String proxyUser;

    @Value("${proxy.password:}")
    private String proxyPassword;

    // Transcript API config
    @Value("${transcript.api.provider:local}")
    private String transcriptApiProvider;

    @Value("${transcript.api.key:}")
    private String transcriptApiKey;

    private YoutubeTranscriptApi transcriptApi;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("TranscriptService initializing with provider: {}", transcriptApiProvider);
        this.transcriptApi = initializeTranscriptApi();
    }

    private YoutubeTranscriptApi initializeTranscriptApi() {
        // ONLY configure system-level proxies if we are using the local scraper.
        if ("local".equalsIgnoreCase(transcriptApiProvider) && proxyHost != null && !proxyHost.isBlank()) {
            logger.info("Initializing Local Transcript API with custom ProxyYoutubeClient: {}:{}", proxyHost,
                    proxyPort);
            try {
                int port = Integer.parseInt(proxyPort != null && !proxyHost.isBlank() ? proxyPort : "8080");

                // Enable Basic auth for proxying and tunneling in Java 11+
                System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
                System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

                // Set system properties for RestTemplate/HttpURLConnection to use proxy
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", String.valueOf(port));
                System.setProperty("https.proxyHost", proxyHost);
                System.setProperty("https.proxyPort", String.valueOf(port));

                io.github.thoroldvix.api.YoutubeClient customClient = new ProxyYoutubeClient(proxyHost, port, proxyUser,
                        proxyPassword);
                return TranscriptApiFactory.createWithClient(customClient);
            } catch (Exception e) {
                logger.error("Failed to configure custom proxy client for Transcript API", e);
            }
        } else if (!"local".equalsIgnoreCase(transcriptApiProvider)) {
            logger.info("Bypassing local proxy initialization (using provider: {})", transcriptApiProvider);
        }

        // Set a realistic User-Agent for all requests (helps with bot detection)
        System.setProperty("http.agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");

        return TranscriptApiFactory.createDefault();
    }

    /**
     * Custom implementation of YoutubeClient to handle proxy authentication
     * reliably.
     */
    private class ProxyYoutubeClient implements io.github.thoroldvix.api.YoutubeClient {
        private final java.net.http.HttpClient httpClient;
        private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";

        public ProxyYoutubeClient(String host, int port, String user, String pass) {
            java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder()
                    .proxy(java.net.ProxySelector.of(new java.net.InetSocketAddress(host, port)))
                    .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS);

            if (user != null && !user.isBlank()) {
                builder.authenticator(new java.net.Authenticator() {
                    @Override
                    protected java.net.PasswordAuthentication getPasswordAuthentication() {
                        return new java.net.PasswordAuthentication(user, pass.toCharArray());
                    }
                });
            }
            this.httpClient = builder.build();
        }

        @Override
        public String get(String url, Map<String, String> params)
                throws io.github.thoroldvix.api.TranscriptRetrievalException {
            try {
                StringBuilder urlBuilder = new StringBuilder(url);
                if (params != null && !params.isEmpty()) {
                    boolean first = !url.contains("?");
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        urlBuilder.append(first ? "?" : "&")
                                .append(java.net.URLEncoder.encode(entry.getKey(),
                                        java.nio.charset.StandardCharsets.UTF_8))
                                .append("=")
                                .append(java.net.URLEncoder.encode(entry.getValue(),
                                        java.nio.charset.StandardCharsets.UTF_8));
                        first = false;
                    }
                }

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(urlBuilder.toString()))
                        .header("User-Agent", userAgent)
                        .GET()
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    throw new io.github.thoroldvix.api.TranscriptRetrievalException(
                            "YouTube request failed with status " + response.statusCode(),
                            String.valueOf(response.statusCode()));
                }
                return response.body();
            } catch (Exception e) {
                if (e instanceof io.github.thoroldvix.api.TranscriptRetrievalException)
                    throw (io.github.thoroldvix.api.TranscriptRetrievalException) e;
                throw new io.github.thoroldvix.api.TranscriptRetrievalException("Failed to fetch: " + e.getMessage(),
                        e);
            }
        }

        @Override
        public String post(String url, String body) throws io.github.thoroldvix.api.TranscriptRetrievalException {
            try {
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", userAgent)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    throw new io.github.thoroldvix.api.TranscriptRetrievalException(
                            "YouTube POST fail: " + response.statusCode(), String.valueOf(response.statusCode()));
                }
                return response.body();
            } catch (Exception e) {
                if (e instanceof io.github.thoroldvix.api.TranscriptRetrievalException)
                    throw (io.github.thoroldvix.api.TranscriptRetrievalException) e;
                throw new io.github.thoroldvix.api.TranscriptRetrievalException("Failed to post: " + e.getMessage(), e);
            }
        }
    }

    // ========================
    // SYSTEM PROMPTS
    // ========================

    private static final String SUMMARY_SYSTEM_PROMPT = """
            You are an expert Video Content Analyst. Your goal is to provide a comprehensive, high-signal summary of the provided transcript so the user doesn't have to watch the video.

            Analyze the following transcript and respond with EXACTLY these sections using markdown formatting:

            ## The 30-Second Pitch (TL;DR)
            A 2-3 sentence high-level summary of the video's core message.

            ## Key Takeaways & Insights
            5-7 bullet points of the most important facts, arguments, or lessons. Use **bold** for emphasis on key terms.

            ## Actionable Steps
            If the video is a tutorial or "how-to," list the specific numbered steps to follow. If not applicable, write "Not applicable for this video type."

            ## Code & Technical Snippets
            Extract any specific code logic, formulas, commands, or technical stacks mentioned. Use code blocks for code. If none, write "No technical content in this video."

            ## Critical Timestamps
            Identify 3-4 pivotal moments in the video (based on the transcript flow) and describe why they are important. Format as bullet points with approximate time references.

            ## The "Missing Context"
            What does the creator assume the audience already knows? List 2-3 concepts or background knowledge that would help a viewer better understand this content.

            IMPORTANT RULES:
            - You MUST include ALL six sections above, in the exact order shown
            - Use markdown formatting with ## headers
            - Be concise but comprehensive
            - Do NOT add any sections beyond these six
            - Do NOT include any preamble before the first section
            """;

    private static final String CHAT_SYSTEM_PROMPT = """
            You are an AI assistant that helps users understand video content. You have been given the transcript of a YouTube video.

            Answer the user's question based ONLY on the information in the transcript. If the answer cannot be found in the transcript, say so clearly.

            Be concise, accurate, and helpful. Use markdown formatting when appropriate (bold, lists, code blocks).
            """;

    // ========================
    // PUBLIC METHODS
    // ========================

    /**
     * Analyze a YouTube video: fetch transcript, summarize with AI, cache result.
     * Returns cached result if available. Auto-refreshes stale fallback summaries.
     */
    public Map<String, Object> analyzeVideo(String videoUrl) {
        // Validate URL
        String videoId = extractAndValidateVideoId(videoUrl);

        // Check cache first
        Optional<TranscriptCache> cached = cacheRepository.findByVideoId(videoId);
        if (cached.isPresent()) {
            TranscriptCache cache = cached.get();

            // Auto-refresh if cached summary is a fallback AND we now have a valid API key
            boolean isFallbackSummary = cache.getSummary() != null
                    && cache.getSummary().contains("AI summarization is not available");

            if (isFallbackSummary && hasValidApiKey()) {
                logger.info("Stale fallback cache for videoId: {} — re-analyzing with API key", videoId);
                try {
                    String newSummary = summarizeWithAI(cache.getTranscript(), cache.getVideoTitle());
                    cache.setSummary(newSummary);
                    cacheRepository.save(cache);
                    logger.info("Updated cached summary for videoId: {}", videoId);
                } catch (GeminiApiException e) {
                    logger.warn("Re-analysis failed, keeping fallback: {}", e.getMessage());
                }
            } else {
                logger.info("Cache hit for videoId: {}", videoId);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("videoId", videoId);
            result.put("videoTitle", cache.getVideoTitle());
            result.put("summary", cache.getSummary());
            result.put("transcript", cache.getTranscript());
            result.put("transcriptLength", cache.getTranscriptLength());
            result.put("cached", true);
            return result;
        }

        // Fetch transcript
        String transcript = fetchTranscript(videoId);

        // Get video title
        String videoTitle = getVideoTitle(videoId);

        // Summarize with AI — gracefully degrade on failure
        String summary;
        try {
            summary = summarizeWithAI(transcript, videoTitle);
        } catch (GeminiApiException e) {
            logger.warn("AI summarization failed, using fallback: {}", e.getMessage());
            summary = generateFallbackSummary(transcript, videoTitle);
        }

        // Cache the result
        TranscriptCache cache = new TranscriptCache(videoId, videoTitle, transcript, summary);
        cacheRepository.save(cache);
        logger.info("Cached transcript and summary for videoId: {}", videoId);

        Map<String, Object> result = new HashMap<>();
        result.put("videoId", videoId);
        result.put("videoTitle", videoTitle);
        result.put("summary", summary);
        result.put("transcript", transcript);
        result.put("transcriptLength", transcript.length());
        result.put("cached", false);
        return result;
    }

    /**
     * Chat with the video transcript — answer follow-up questions.
     * Degrades gracefully if AI is unavailable.
     */
    public String chatWithVideo(String videoId, String question, List<Map<String, String>> chatHistory) {
        Optional<TranscriptCache> cached = cacheRepository.findByVideoId(videoId);
        if (cached.isEmpty()) {
            throw new TranscriptNotAvailableException("Video transcript not found. Please analyze the video first.");
        }

        String transcript = cached.get().getTranscript();

        if (transcript.length() > 500000) {
            transcript = transcript.substring(0, 500000) + "\n[Transcript truncated for chat context]";
        }

        try {
            return chatWithAI(transcript, question, chatHistory);
        } catch (GeminiApiException e) {
            logger.warn("Chat AI failed: {}", e.getMessage());
            return "⚠️ AI chat is currently unavailable: " + e.getMessage()
                    + "\n\nPlease verify your API key in application.properties.";
        }
    }

    // ========================
    // PRIVATE METHODS
    // ========================

    /**
     * Extract and validate video ID from YouTube URL.
     */
    private String extractAndValidateVideoId(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new InvalidVideoUrlException("Please enter a YouTube URL.");
        }

        String trimmed = videoUrl.trim();

        // Reject playlists
        if (trimmed.contains("/playlist") || trimmed.contains("list=")) {
            throw new InvalidVideoUrlException("Playlists are not supported. Please enter a single video URL.");
        }

        // Reject channels
        if (trimmed.contains("/channel/") || trimmed.contains("/@") || trimmed.contains("/c/")) {
            throw new InvalidVideoUrlException("Channel URLs are not supported. Please enter a single video URL.");
        }

        // Reject shorts
        if (trimmed.contains("/shorts/")) {
            throw new InvalidVideoUrlException("YouTube Shorts are not supported. Please enter a regular video URL.");
        }

        // Validate YouTube domain
        if (!trimmed.contains("youtube.com") && !trimmed.contains("youtu.be")) {
            throw new InvalidVideoUrlException(
                    "Please enter a valid YouTube URL (e.g., https://www.youtube.com/watch?v=VIDEO_ID).");
        }

        String videoId = youtubeService.extractVideoId(trimmed);
        if (videoId == null || videoId.isBlank()) {
            throw new InvalidVideoUrlException(
                    "Could not extract video ID from the URL. Please check the URL and try again.");
        }

        return videoId;
    }

    /**
     * Fetch transcript using the configured provider.
     */
    private String fetchTranscript(String videoId) {
        logger.info("Fetch attempt for videoId: {} using provider: {}", videoId, transcriptApiProvider);

        if ("supadata".equalsIgnoreCase(transcriptApiProvider)) {
            if (transcriptApiKey == null || transcriptApiKey.isBlank()) {
                throw new TranscriptNotAvailableException(
                        "Supadata provider selected but TRANSCRIPT_API_KEY is missing.");
            }
            try {
                return fetchTranscriptFromSupadata(videoId);
            } catch (Exception e) {
                logger.error("Supadata fetch failed: {}", e.getMessage());
                throw new TranscriptNotAvailableException(
                        "Supadata API failed: " + e.getMessage() + ". Check your API key and quota.");
            }
        }

        return fetchTranscriptLocally(videoId);
    }

    /**
     * Fetch transcript from Supadata API.
     */
    private String fetchTranscriptFromSupadata(String videoId) throws Exception {
        logger.info("Requesting transcript from Supadata.ai for: {}", videoId);
        try {
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

            // Use URI to prevent RestTemplate from double-encoding the query parameters
            java.net.URI uri = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl("https://api.supadata.ai/v1/transcript")
                    .queryParam("url", videoUrl)
                    .queryParam("text", "true")
                    .build()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", transcriptApiKey.trim()); // Trim to avoid accidental spaces
            headers.set("User-Agent", "PipedPiper/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

            logger.info("Supadata Response Code: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("content").asText();
                if (content != null && !content.isBlank()) {
                    logger.info("Successfully received transcript from Supadata ({} chars)", content.length());
                    return content;
                }
            }

            throw new Exception(
                    "Supadata returned status " + response.getStatusCode() + " with body: " + response.getBody());
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            logger.error("Supadata API Error ({}): {}", e.getStatusCode(), errorBody);
            throw new Exception("Supadata Error " + e.getStatusCode() + ": " + errorBody);
        } catch (Exception e) {
            logger.error("Supadata communication error: {}", e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Original logic using the local library/scraper.
     */
    private String fetchTranscriptLocally(String videoId) {
        try {
            // Try to get English transcript first, then any available language
            TranscriptContent content = transcriptApi.getTranscript(videoId, "en");

            // Format as plain text
            TranscriptFormatter textFormatter = TranscriptFormatters.textFormatter();
            String transcript = textFormatter.format(content);

            if (transcript == null || transcript.isBlank()) {
                throw new TranscriptNotAvailableException(
                        "The transcript for this video is empty. The video may not have spoken content.");
            }

            logger.info("Successfully fetched transcript for videoId: {} ({} chars)", videoId, transcript.length());
            return transcript;

        } catch (TranscriptNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch transcript for videoId: {}", videoId, e);

            // Try with auto-generated transcript as fallback
            try {
                TranscriptContent content = transcriptApi.listTranscripts(videoId)
                        .findGeneratedTranscript("en")
                        .fetch();
                TranscriptFormatter textFormatter = TranscriptFormatters.textFormatter();
                String transcript = textFormatter.format(content);
                if (transcript != null && !transcript.isBlank()) {
                    return transcript;
                }
            } catch (Exception fallbackEx) {
                logger.error("Fallback transcript fetch also failed for videoId: {}", videoId, fallbackEx);
            }

            throw new TranscriptNotAvailableException(
                    "No captions/transcript available for this video. This video may not have captions enabled, " +
                            "or the captions may be restricted. Error details: " + e.getMessage()
                            + ". Try a different video.");
        }
    }

    /**
     * Get video title using existing YouTube Data API integration.
     */
    private String getVideoTitle(String videoId) {
        try {
            JsonNode details = youtubeService.getVideoDetails(videoId);
            return details.path("title").asText("Unknown Video");
        } catch (Exception e) {
            logger.warn("Could not fetch video title for videoId: {}", videoId, e);
            return "YouTube Video";
        }
    }

    /**
     * Summarize transcript using AI.
     */
    private String summarizeWithAI(String transcript, String videoTitle) {
        if (!hasValidApiKey()) {
            logger.warn("No AI API key configured — returning fallback summary");
            return generateFallbackSummary(transcript, videoTitle);
        }

        // Truncate transcript if extremely long (safety limit at 800K chars ≈ ~200K
        // tokens)
        String transcriptForAI = transcript;
        if (transcript.length() > 800000) {
            transcriptForAI = transcript.substring(0, 800000) + "\n\n[Transcript truncated at 800,000 characters]";
        }

        String userPrompt = String.format(
                "Video Title: \"%s\"\n\nTranscript:\n%s",
                videoTitle, transcriptForAI);

        return callAiApi(SUMMARY_SYSTEM_PROMPT, userPrompt);
    }

    /**
     * Chat using AI with transcript context.
     */
    private String chatWithAI(String transcript, String question, List<Map<String, String>> chatHistory) {
        if (!hasValidApiKey()) {
            return "⚠️ AI chat is not available. Please configure an API key in application.properties.";
        }

        StringBuilder conversationContext = new StringBuilder();
        conversationContext.append("Video Transcript:\n").append(transcript).append("\n\n");

        // Include recent chat history (last 10 messages to manage context)
        if (chatHistory != null && !chatHistory.isEmpty()) {
            conversationContext.append("Previous conversation:\n");
            int startIdx = Math.max(0, chatHistory.size() - 10);
            for (int i = startIdx; i < chatHistory.size(); i++) {
                Map<String, String> msg = chatHistory.get(i);
                String role = msg.getOrDefault("role", "user");
                String content = msg.getOrDefault("content", "");
                conversationContext.append(role.equals("user") ? "User: " : "Assistant: ")
                        .append(content).append("\n");
            }
            conversationContext.append("\n");
        }

        conversationContext.append("Current question: ").append(question);

        return callAiApi(CHAT_SYSTEM_PROMPT, conversationContext.toString());
    }

    /**
     * Make a REST call to Gemini API.
     */
    private String callGeminiApi(String systemPrompt, String userPrompt) {
        try {
            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    geminiModel, geminiApiKey);

            Map<String, Object> requestBody = new HashMap<>();

            Map<String, Object> systemInstruction = new HashMap<>();
            systemInstruction.put("parts", List.of(Map.of("text", systemPrompt)));
            requestBody.put("systemInstruction", systemInstruction);

            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", List.of(Map.of("text", userPrompt)));
            contents.add(userContent);
            requestBody.put("contents", contents);

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", aiTemperature);
            generationConfig.put("maxOutputTokens", aiMaxTokens);
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    return candidates.get(0).path("content").path("parts").get(0)
                            .path("text").asText("No response generated.");
                }
                JsonNode error = root.path("error");
                if (!error.isMissingNode()) {
                    String errorMsg = error.path("message").asText("Unknown error");
                    throw new GeminiApiException("Gemini: " + errorMsg);
                }
            }
            throw new GeminiApiException("Empty response from Gemini API");

        } catch (GeminiApiException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Gemini API HTTP {} error: {}", e.getStatusCode().value(), body);
            try {
                String errorMsg = objectMapper.readTree(body).path("error").path("message").asText(body);
                throw new GeminiApiException("Gemini: " + errorMsg);
            } catch (GeminiApiException ge) {
                throw ge;
            } catch (Exception parseEx) {
                throw new GeminiApiException("Gemini HTTP " + e.getStatusCode().value() + ": " + body);
            }
        } catch (Exception e) {
            logger.error("Gemini API call failed", e);
            throw new GeminiApiException("Gemini failed: " + e.getMessage(), e);
        }
    }

    /**
     * Make a REST call to Groq API (OpenAI-compatible endpoint).
     */
    private String callGroqApi(String systemPrompt, String userPrompt) {
        try {
            String url = "https://api.groq.com/openai/v1/chat/completions";

            // Build OpenAI-compatible request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", groqModel);
            requestBody.put("temperature", aiTemperature);
            requestBody.put("max_tokens", aiMaxTokens);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    return choices.get(0).path("message").path("content").asText("No response generated.");
                }
            }
            throw new GeminiApiException("Empty response from Groq API");

        } catch (GeminiApiException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Groq API HTTP {} error: {}", e.getStatusCode().value(), body);
            try {
                String errorMsg = objectMapper.readTree(body).path("error").path("message").asText(body);
                throw new GeminiApiException("Groq: " + errorMsg);
            } catch (GeminiApiException ge) {
                throw ge;
            } catch (Exception parseEx) {
                throw new GeminiApiException("Groq HTTP " + e.getStatusCode().value() + ": " + body);
            }
        } catch (Exception e) {
            logger.error("Groq API call failed", e);
            throw new GeminiApiException("Groq failed: " + e.getMessage(), e);
        }
    }

    /**
     * Dispatch AI call to the configured provider. Tries Groq by default; falls
     * back between providers.
     */
    private String callAiApi(String systemPrompt, String userPrompt) {
        if ("gemini".equalsIgnoreCase(aiProvider)) {
            // Try Gemini first, fall back to Groq
            try {
                return callGeminiApi(systemPrompt, userPrompt);
            } catch (GeminiApiException e) {
                if (groqApiKey != null && !groqApiKey.isBlank()) {
                    logger.warn("Gemini failed, falling back to Groq: {}", e.getMessage());
                    return callGroqApi(systemPrompt, userPrompt);
                }
                throw e;
            }
        } else {
            // Try Groq first (default), fall back to Gemini
            try {
                return callGroqApi(systemPrompt, userPrompt);
            } catch (GeminiApiException e) {
                if (geminiApiKey != null && !geminiApiKey.isBlank()) {
                    logger.warn("Groq failed, falling back to Gemini: {}", e.getMessage());
                    return callGeminiApi(systemPrompt, userPrompt);
                }
                throw e;
            }
        }
    }

    /**
     * Check if at least one AI provider has a valid API key.
     */
    private boolean hasValidApiKey() {
        boolean hasGemini = geminiApiKey != null && !geminiApiKey.isBlank()
                && !geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE");
        boolean hasGroq = groqApiKey != null && !groqApiKey.isBlank()
                && !groqApiKey.equals("YOUR_GROQ_API_KEY_HERE");
        return hasGemini || hasGroq;
    }

    /**
     * Fallback summary when no AI API key is configured.
     */
    private String generateFallbackSummary(String transcript, String videoTitle) {
        StringBuilder summary = new StringBuilder();
        summary.append("## The 30-Second Pitch (TL;DR)\n");
        summary.append(String.format("This video titled \"%s\" covers the following content. ", videoTitle));
        summary.append("AI summarization is not available — please configure an API key for full analysis.\n\n");

        summary.append("## Key Takeaways & Insights\n");
        summary.append("- Full AI-powered analysis requires an API key (Groq recommended — free tier)\n");
        summary.append("- The transcript has been extracted successfully (")
                .append(transcript.length()).append(" characters)\n");
        summary.append("- You can read the full transcript below\n\n");

        summary.append("## Actionable Steps\n");
        summary.append("1. Get a free Groq API key from [console.groq.com](https://console.groq.com/keys)\n");
        summary.append("2. Add it to `application.properties` as `groq.api.key=YOUR_KEY`\n");
        summary.append("3. Restart the application and try again\n\n");

        summary.append("## Code & Technical Snippets\n");
        summary.append("No AI analysis available without API key.\n\n");

        summary.append("## Critical Timestamps\n");
        summary.append("No AI analysis available without API key.\n\n");

        summary.append("## The \"Missing Context\"\n");
        summary.append("No AI analysis available without API key.\n");

        return summary.toString();
    }
}
