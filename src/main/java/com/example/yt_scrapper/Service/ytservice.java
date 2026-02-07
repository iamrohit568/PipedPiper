// package com.example.yt_scrapper.Service;

// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;

// import com.example.yt_scrapper.Config.ytConfig;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;

// @Service
// public class ytservice {

//     @Autowired
//     private ytConfig youtubeConfig;
//     public String extractVideoId(String videoLink){
//         Pattern pattern1 = Pattern.compile("(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed\\.|youtu\\.be\\/|\\/v\\/|\\/e\\/|watch\\?v=|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed\\.)([^\"&?\\/\\s]{11})", Pattern.CASE_INSENSITIVE);
//         Matcher matcher1 = pattern1.matcher(videoLink);

//         // Pattern for short YouTube URL
//         Pattern pattern2 = Pattern.compile("youtu.be\\/(.{11})", Pattern.CASE_INSENSITIVE);
//         Matcher matcher2 = pattern2.matcher(videoLink);

//         if(matcher1.find()){
//             return matcher1.group(1);
//         }
//         else if(matcher2.find()){
//             return matcher2.group(1);
//         }

//         return null;
//     }

//     public JsonNode getVideoDetails(String videoId) throws Exception {
//         // API integration using rest Template
//         String apiUrl=youtubeConfig.getApiUrl();
//         String apiKey="key="+ youtubeConfig.getApiKey();
//         String partParam = "part=snippet";
//         String idParam = "id=" + videoId;

//         String url = apiUrl + "?" + apiKey + "&" + partParam + "&" + idParam;
//         RestTemplate restTemplate = new RestTemplate();
//         String response = restTemplate.getForObject(url, String.class);
//         // System.out.println(response);

//         ObjectMapper objectMapper = new ObjectMapper();
//         return objectMapper.readTree(response).path("items").get(0).path("snippet");

//     }
// }


package com.example.yt_scrapper.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.yt_scrapper.Config.ytConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ytservice {

    @Autowired
    private ytConfig youtubeConfig;

    public String extractVideoId(String videoLink) {
        Pattern pattern1 = Pattern.compile("(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed\\.|youtu\\.be\\/|\\/v\\/|\\/e\\/|watch\\?v=|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed\\.)([^\"&?\\/\\s]{11})", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(videoLink);

        Pattern pattern2 = Pattern.compile("youtu.be\\/(.{11})", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(videoLink);

        if (matcher1.find()) {
            return matcher1.group(1);
        } else if (matcher2.find()) {
            return matcher2.group(1);
        }
        return null;
    }

    public JsonNode getVideoDetails(String videoId) throws Exception {
        String apiUrl = youtubeConfig.getApiUrl();
        String apiKey = "key=" + youtubeConfig.getApiKey();
        String partParam = "part=snippet";
        String idParam = "id=" + videoId;

        String url = apiUrl + "?" + apiKey + "&" + partParam + "&" + idParam;
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response).path("items").get(0).path("snippet");
    }

    // NEW METHOD: Get video details with statistics
    public JsonNode getVideoDetailsWithStats(String videoId) throws Exception {
        String apiUrl = youtubeConfig.getApiUrl();
        String url = String.format("%s?part=snippet,statistics&id=%s&key=%s",
                apiUrl, videoId, youtubeConfig.getApiKey());

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response).path("items").get(0);
    }

    // NEW METHOD: Get channel details
    public JsonNode getChannelDetails(String channelId) throws Exception {
        String channelUrl = "https://www.googleapis.com/youtube/v3/channels";
        String url = String.format("%s?part=snippet,statistics&id=%s&key=%s",
                channelUrl, channelId, youtubeConfig.getApiKey());

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response).path("items").get(0);
    }

    public JsonNode searchVideos(String keywords, int maxResults, String sortBy, String pageToken) throws Exception {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append(String.format("%s?part=snippet&q=%s&maxResults=%d&type=video&order=%s&key=%s",
            youtubeConfig.getSearchApiUrl(),
            URLEncoder.encode(keywords, StandardCharsets.UTF_8),
            maxResults,
            sortBy,
            youtubeConfig.getApiKey()));
    
    if (pageToken != null && !pageToken.isEmpty()) {
        urlBuilder.append("&pageToken=").append(pageToken);
    }

    RestTemplate restTemplate = new RestTemplate();
    String response = restTemplate.getForObject(urlBuilder.toString(), String.class);

    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readTree(response);
}

    public List<JsonNode> getVideoDetailsBatch(List<String> videoIds) throws Exception {
        String joinedIds = String.join(",", videoIds);
        String url = String.format("%s?part=snippet,statistics&id=%s&key=%s",
                youtubeConfig.getApiUrl(),
                joinedIds,
                youtubeConfig.getApiKey());

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode items = objectMapper.readTree(response).path("items");

        List<JsonNode> results = new ArrayList<>();
        for (JsonNode item : items) {
            results.add(item);
        }
        return results;
    }

    // Add these methods to your ytservice class

public List<Map<String, String>> getUserSubscriptions(String username) throws Exception {
    // In a real implementation, you would:
    // 1. Get the user's YouTube access token from your database
    // 2. Call YouTube API to get subscriptions
    // 3. Process and return the data
    
    // Mock data for demonstration
    List<Map<String, String>> subscriptions = new ArrayList<>();
    
    subscriptions.add(Map.of(
        "id", "UC123456789",
        "name", "Tech Reviews",
        "thumbnail", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
    ));
    
    subscriptions.add(Map.of(
        "id", "UC987654321",
        "name", "Cooking Channel",
        "thumbnail", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
    ));
    
    return subscriptions;
}

public List<Map<String, String>> getSubscribedVideos(String username) throws Exception {
    // In a real implementation, you would:
    // 1. Get the user's subscriptions
    // 2. Fetch videos from those channels
    // 3. Process and return the data
    
    // Mock data for demonstration
    List<Map<String, String>> videos = new ArrayList<>();
    
    videos.add(Map.of(
        "id", "video1",
        "title", "The Ultimate Tech Review 2025",
        "thumbnailUrl", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
        "duration", "12:34",
        "viewCount", "1.2M",
        "publishedAt", "2 days ago",
        "channelName", "Tech Reviews",
        "channelThumbnail", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
    ));
    
    videos.add(Map.of(
        "id", "video2",
        "title", "How to Cook Perfect Pasta",
        "thumbnailUrl", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
        "duration", "8:45",
        "viewCount", "456K",
        "publishedAt", "1 week ago",
        "channelName", "Cooking Channel",
        "channelThumbnail", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
    ));
    
    return videos;
}

public Map<String, String> getUserChannelInfo(String username) throws Exception {
    // In a real implementation, you would:
    // 1. Get the user's YouTube channel info
    // 2. Process and return the data
    
    // Mock data for demonstration
    return Map.of(
        "name", username + "'s Channel",
        "thumbnail", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
        "subscriberCount", "10K"
    );
}

/**
 * Get trending/popular videos from YouTube - Mixed from different categories for variety
 * This simulates the YouTube homepage experience with different videos on each refresh
 * @param regionCode Country code (e.g., "US", "IN", "GB")
 * @param maxResults Number of results to return
 * @return List of video information maps
 */
public List<Map<String, String>> getTrendingVideos(String regionCode, int maxResults) throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();
    
    // YouTube category IDs for variety
    String[] categories = {"10", "20", "24", "25", "28", "17", "22", "23", "1", "2"}; // Music, Gaming, Entertainment, News, Science, Sports, People, Comedy, Film, Autos
    
    // Randomly select a few categories to fetch from
    java.util.Random random = new java.util.Random();
    java.util.List<String> shuffledCategories = new java.util.ArrayList<>(java.util.Arrays.asList(categories));
    java.util.Collections.shuffle(shuffledCategories);
    
    List<Map<String, String>> allVideos = new ArrayList<>();
    
    // Fetch from trending (general)
    try {
        String trendingUrl = String.format(
            "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics,contentDetails&chart=mostPopular&regionCode=%s&maxResults=%d&key=%s",
            regionCode, maxResults / 2, youtubeConfig.getApiKey()
        );
        String response = restTemplate.getForObject(trendingUrl, String.class);
        JsonNode items = objectMapper.readTree(response).path("items");
        for (JsonNode item : items) {
            allVideos.add(parseVideoItem(item));
        }
    } catch (Exception e) {
        // Continue with other sources
    }
    
    // Fetch from 2-3 random categories
    int categoriesToFetch = Math.min(3, shuffledCategories.size());
    for (int i = 0; i < categoriesToFetch && allVideos.size() < maxResults; i++) {
        try {
            String categoryUrl = String.format(
                "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics,contentDetails&chart=mostPopular&videoCategoryId=%s&regionCode=%s&maxResults=%d&key=%s",
                shuffledCategories.get(i), regionCode, 8, youtubeConfig.getApiKey()
            );
            String response = restTemplate.getForObject(categoryUrl, String.class);
            JsonNode items = objectMapper.readTree(response).path("items");
            for (JsonNode item : items) {
                Map<String, String> video = parseVideoItem(item);
                // Avoid duplicates
                boolean isDuplicate = allVideos.stream().anyMatch(v -> v.get("videoId").equals(video.get("videoId")));
                if (!isDuplicate) {
                    allVideos.add(video);
                }
            }
        } catch (Exception e) {
            // Continue with other categories
        }
    }
    
    // Shuffle the results for variety on each request
    java.util.Collections.shuffle(allVideos);
    
    // Return only the requested number
    return allVideos.subList(0, Math.min(maxResults, allVideos.size()));
}

/**
 * Parse a video item from YouTube API response
 */
private Map<String, String> parseVideoItem(JsonNode item) {
    Map<String, String> video = new HashMap<>();
    JsonNode snippet = item.path("snippet");
    JsonNode statistics = item.path("statistics");
    JsonNode contentDetails = item.path("contentDetails");
    
    String channelTitle = snippet.path("channelTitle").asText();
    
    video.put("videoId", item.path("id").asText());
    video.put("title", snippet.path("title").asText());
    video.put("description", snippet.path("description").asText());
    video.put("thumbnailUrl", snippet.path("thumbnails").path("high").path("url").asText());
    video.put("channelTitle", channelTitle);
    video.put("channelName", channelTitle);
    video.put("channelId", snippet.path("channelId").asText());
    video.put("channelThumbnail", "https://ui-avatars.com/api/?name=" + channelTitle.replace(" ", "+") + "&background=random&size=64");
    video.put("publishedAt", formatPublishedDate(snippet.path("publishedAt").asText()));
    video.put("viewCount", formatViewCount(statistics.path("viewCount").asText()));
    video.put("likeCount", statistics.path("likeCount").asText());
    video.put("duration", formatDuration(contentDetails.path("duration").asText()));
    video.put("categoryId", snippet.path("categoryId").asText());
    
    return video;
}

/**
 * Format view count to human readable format (e.g., 1234567 -> 1.2M)
 */
private String formatViewCount(String viewCount) {
    try {
        long count = Long.parseLong(viewCount);
        if (count >= 1_000_000_000) {
            return String.format("%.1fB", count / 1_000_000_000.0);
        } else if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return viewCount;
    } catch (NumberFormatException e) {
        return viewCount;
    }
}

/**
 * Format published date to relative time (e.g., "2 days ago")
 */
private String formatPublishedDate(String isoDate) {
    try {
        java.time.Instant publishedInstant = java.time.Instant.parse(isoDate);
        java.time.Instant now = java.time.Instant.now();
        long seconds = java.time.Duration.between(publishedInstant, now).getSeconds();
        
        if (seconds < 60) return "just now";
        if (seconds < 3600) return (seconds / 60) + " minutes ago";
        if (seconds < 86400) return (seconds / 3600) + " hours ago";
        if (seconds < 604800) return (seconds / 86400) + " days ago";
        if (seconds < 2592000) return (seconds / 604800) + " weeks ago";
        if (seconds < 31536000) return (seconds / 2592000) + " months ago";
        return (seconds / 31536000) + " years ago";
    } catch (Exception e) {
        return isoDate;
    }
}

/**
 * Format ISO 8601 duration to readable format (e.g., PT1H2M3S -> 1:02:03)
 */
private String formatDuration(String isoDuration) {
    if (isoDuration == null || isoDuration.isEmpty()) {
        return "0:00";
    }
    
    try {
        java.time.Duration duration = java.time.Duration.parse(isoDuration);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    } catch (Exception e) {
        return "0:00";
    }
}

/**
 * Get YouTube home feed videos (mix of popular videos from different categories)
 * Simulates the YouTube homepage experience with variety on each request
 * @param regionCode Country code (e.g., "US", "IN")
 * @param pageToken Token for pagination (null for first page)
 * @param maxResults Number of results per page
 * @return Map containing videos list and nextPageToken
 */
public Map<String, Object> getHomeFeedVideos(String regionCode, String pageToken, int maxResults) throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();
    
    // YouTube category IDs for variety
    String[] categories = {"10", "20", "24", "25", "28", "17", "22", "23", "1", "2", "26", "27", "29"};
    java.util.Random random = new java.util.Random();
    
    List<Map<String, String>> allVideos = new ArrayList<>();
    String nextToken = null;
    
    // If we have a pageToken, use it for pagination on trending
    if (pageToken != null && !pageToken.isEmpty()) {
        // Use pageToken for trending videos
        String trendingUrl = String.format(
            "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics,contentDetails&chart=mostPopular&regionCode=%s&maxResults=%d&pageToken=%s&key=%s",
            regionCode, maxResults, pageToken, youtubeConfig.getApiKey()
        );
        String response = restTemplate.getForObject(trendingUrl, String.class);
        JsonNode root = objectMapper.readTree(response);
        JsonNode items = root.path("items");
        
        for (JsonNode item : items) {
            allVideos.add(parseVideoItem(item));
        }
        nextToken = root.path("nextPageToken").asText(null);
    } else {
        // First page - mix from different sources
        java.util.List<String> shuffledCategories = new java.util.ArrayList<>(java.util.Arrays.asList(categories));
        java.util.Collections.shuffle(shuffledCategories);
        
        // Fetch trending videos (general)
        try {
            String trendingUrl = String.format(
                "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics,contentDetails&chart=mostPopular&regionCode=%s&maxResults=%d&key=%s",
                regionCode, maxResults / 2, youtubeConfig.getApiKey()
            );
            String response = restTemplate.getForObject(trendingUrl, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");
            
            for (JsonNode item : items) {
                allVideos.add(parseVideoItem(item));
            }
            nextToken = root.path("nextPageToken").asText(null);
        } catch (Exception e) {
            // Continue
        }
        
        // Fetch from random categories to add variety
        int categoriesToFetch = 3;
        for (int i = 0; i < categoriesToFetch && allVideos.size() < maxResults * 2; i++) {
            try {
                String categoryUrl = String.format(
                    "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics,contentDetails&chart=mostPopular&videoCategoryId=%s&regionCode=%s&maxResults=%d&key=%s",
                    shuffledCategories.get(i), regionCode, 8, youtubeConfig.getApiKey()
                );
                String response = restTemplate.getForObject(categoryUrl, String.class);
                JsonNode items = objectMapper.readTree(response).path("items");
                
                for (JsonNode item : items) {
                    Map<String, String> video = parseVideoItem(item);
                    boolean isDuplicate = allVideos.stream().anyMatch(v -> v.get("videoId").equals(video.get("videoId")));
                    if (!isDuplicate) {
                        allVideos.add(video);
                    }
                }
            } catch (Exception e) {
                // Continue with other categories
            }
        }
        
        // Shuffle for variety
        java.util.Collections.shuffle(allVideos);
    }
    
    // Limit to maxResults
    List<Map<String, String>> resultVideos = allVideos.subList(0, Math.min(maxResults, allVideos.size()));
    
    Map<String, Object> result = new HashMap<>();
    result.put("videos", resultVideos);
    result.put("nextPageToken", nextToken);
    result.put("totalResults", allVideos.size());
    
    return result;
}

/**
 * Get videos by category with pagination
 * @param categoryId YouTube category ID (e.g., "10" for Music, "20" for Gaming)
 * @param regionCode Country code
 * @param pageToken Token for pagination
 * @param maxResults Number of results per page
 * @return Map containing videos list and nextPageToken
 */
public Map<String, Object> getVideosByCategory(String categoryId, String regionCode, String pageToken, int maxResults) throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();
    
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("https://www.googleapis.com/youtube/v3/videos")
              .append("?part=snippet,statistics,contentDetails")
              .append("&chart=mostPopular")
              .append("&videoCategoryId=").append(categoryId)
              .append("&regionCode=").append(regionCode)
              .append("&maxResults=").append(maxResults)
              .append("&key=").append(youtubeConfig.getApiKey());
    
    if (pageToken != null && !pageToken.isEmpty()) {
        urlBuilder.append("&pageToken=").append(pageToken);
    }
    
    String response = restTemplate.getForObject(urlBuilder.toString(), String.class);
    JsonNode root = objectMapper.readTree(response);
    JsonNode items = root.path("items");
    
    List<Map<String, String>> videos = new ArrayList<>();
    
    for (JsonNode item : items) {
        Map<String, String> video = parseVideoItem(item);
        video.put("categoryId", categoryId);
        videos.add(video);
    }
    
    // Shuffle for variety
    java.util.Collections.shuffle(videos);
    
    Map<String, Object> result = new HashMap<>();
    result.put("videos", videos);
    result.put("nextPageToken", root.path("nextPageToken").asText(null));
    
    return result;
}

/**
 * Search videos with pagination support
 * @param query Search query
 * @param pageToken Token for pagination
 * @param maxResults Number of results per page
 * @return Map containing videos list and nextPageToken
 */
public Map<String, Object> searchVideosWithPagination(String query, String pageToken, int maxResults) throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();
    
    // First, search for video IDs
    StringBuilder searchUrlBuilder = new StringBuilder();
    searchUrlBuilder.append("https://www.googleapis.com/youtube/v3/search")
                    .append("?part=snippet")
                    .append("&q=").append(java.net.URLEncoder.encode(query, "UTF-8"))
                    .append("&type=video")
                    .append("&maxResults=").append(maxResults)
                    .append("&key=").append(youtubeConfig.getApiKey());
    
    if (pageToken != null && !pageToken.isEmpty()) {
        searchUrlBuilder.append("&pageToken=").append(pageToken);
    }
    
    String searchResponse = restTemplate.getForObject(searchUrlBuilder.toString(), String.class);
    JsonNode searchRoot = objectMapper.readTree(searchResponse);
    JsonNode searchItems = searchRoot.path("items");
    
    // Collect video IDs for detailed info
    List<String> videoIds = new ArrayList<>();
    for (JsonNode item : searchItems) {
        String videoId = item.path("id").path("videoId").asText();
        if (!videoId.isEmpty()) {
            videoIds.add(videoId);
        }
    }
    
    List<Map<String, String>> videos = new ArrayList<>();
    
    if (!videoIds.isEmpty()) {
        // Get detailed video info
        String videoDetailsUrl = "https://www.googleapis.com/youtube/v3/videos"
                + "?part=snippet,statistics,contentDetails"
                + "&id=" + String.join(",", videoIds)
                + "&key=" + youtubeConfig.getApiKey();
        
        String detailsResponse = restTemplate.getForObject(videoDetailsUrl, String.class);
        JsonNode detailsItems = objectMapper.readTree(detailsResponse).path("items");
        
        for (JsonNode item : detailsItems) {
            Map<String, String> video = new HashMap<>();
            JsonNode snippet = item.path("snippet");
            JsonNode statistics = item.path("statistics");
            JsonNode contentDetails = item.path("contentDetails");
            
            String channelTitle = snippet.path("channelTitle").asText();
            
            video.put("videoId", item.path("id").asText());
            video.put("title", snippet.path("title").asText());
            video.put("description", snippet.path("description").asText());
            video.put("thumbnailUrl", snippet.path("thumbnails").path("high").path("url").asText());
            video.put("channelTitle", channelTitle);
            video.put("channelId", snippet.path("channelId").asText());
            video.put("channelThumbnail", "https://ui-avatars.com/api/?name=" + channelTitle.replace(" ", "+") + "&background=random&size=64");
            video.put("publishedAt", formatPublishedDate(snippet.path("publishedAt").asText()));
            video.put("viewCount", formatViewCount(statistics.path("viewCount").asText()));
            video.put("likeCount", statistics.path("likeCount").asText());
            video.put("duration", formatDuration(contentDetails.path("duration").asText()));
            
            videos.add(video);
        }
    }
    
    Map<String, Object> result = new HashMap<>();
    result.put("videos", videos);
    result.put("nextPageToken", searchRoot.path("nextPageToken").asText(null));
    
    return result;
}
}