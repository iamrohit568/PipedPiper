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
}