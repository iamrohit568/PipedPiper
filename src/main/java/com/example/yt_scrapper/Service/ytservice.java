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
}