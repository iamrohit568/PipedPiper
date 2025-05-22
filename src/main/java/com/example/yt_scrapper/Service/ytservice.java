package com.example.yt_scrapper.Service;

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
    public String extractVideoId(String videoLink){
        Pattern pattern1 = Pattern.compile("(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed\\.|youtu\\.be\\/|\\/v\\/|\\/e\\/|watch\\?v=|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed\\.)([^\"&?\\/\\s]{11})", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(videoLink);

        // Pattern for short YouTube URL
        Pattern pattern2 = Pattern.compile("youtu.be\\/(.{11})", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(videoLink);

        if(matcher1.find()){
            return matcher1.group(1);
        }
        else if(matcher2.find()){
            return matcher2.group(1);
        }

        return null;
    }

    public JsonNode getVideoDetails(String videoId) throws Exception {
        // API integration using rest Template
        String apiUrl=youtubeConfig.getApiUrl();
        String apiKey="key="+ youtubeConfig.getApiKey();
        String partParam = "part=snippet";
        String idParam = "id=" + videoId;

        String url = apiUrl + "?" + apiKey + "&" + partParam + "&" + idParam;
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        // System.out.println(response);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response).path("items").get(0).path("snippet");

    }
}
