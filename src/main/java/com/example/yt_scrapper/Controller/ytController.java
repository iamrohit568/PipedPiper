// package com.example.yt_scrapper.Controller;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestParam;

// import com.example.yt_scrapper.Service.ytservice;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;


// @Controller
// public class ytController {
//     @Autowired
//     private ytservice youtubeservice;
//     @GetMapping("/")
//     public String getHome() {
//         return "index";
//     }
    
//     @PostMapping("/processing")
//     public String getData(@RequestParam String videoLink, Model model) {
//         String videoId=youtubeservice.extractVideoId(videoLink);
//         // System.out.println("Video ID: " + videoId);

//         if(videoId != null) {
//             try {
//                 JsonNode videoDetails=youtubeservice.getVideoDetails(videoId);
//                 // System.out.println(videoDetails);
//                 String title=videoDetails.path("title").asText();
//                 String description=videoDetails.path("description").asText();
//                 String thmurl=videoDetails.path("thumbnails").path("standard").path("url").asText();
//                 String[] tags = new ObjectMapper().treeToValue(videoDetails.path("tags"), String[].class);

//                     model.addAttribute("videoDetails", "test");
//                     model.addAttribute("videoId", videoId);
//                     model.addAttribute("title", title);
//                     model.addAttribute("thumbnailUrl", thmurl);
//                     model.addAttribute("tags", tags);
//                     model.addAttribute("description", description);
//                     // System.out.println(model);

//                 return "details";
//             } catch (Exception e) {
//                 e.printStackTrace();
//             }
//         }
//         return "error";
//     }
// }


package com.example.yt_scrapper.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.yt_scrapper.Service.ytservice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Controller
public class ytController {
    @Autowired
    private ytservice youtubeservice;

    @GetMapping("/")
    public String getHome() {
        return "index";
    }

    @PostMapping("/processing")
    public String getData(@RequestParam String videoLink, Model model) {
        String videoId = youtubeservice.extractVideoId(videoLink);
        if (videoId != null) {
            try {
                JsonNode videoDetails = youtubeservice.getVideoDetails(videoId);
                String title = videoDetails.path("title").asText();
                String description = videoDetails.path("description").asText();
                String thmurl = videoDetails.path("thumbnails").path("standard").path("url").asText();
                String[] tags = new ObjectMapper().treeToValue(videoDetails.path("tags"), String[].class);

                model.addAttribute("videoDetails", "test");
                model.addAttribute("videoId", videoId);
                model.addAttribute("title", title);
                model.addAttribute("thumbnailUrl", thmurl);
                model.addAttribute("tags", tags);
                model.addAttribute("description", description);

                return "details";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "error";
    }

    @PostMapping("/search")
    public String handleSearch(
            @RequestParam String keywords,
            @RequestParam(defaultValue = "10") int maxResults,
            @RequestParam(defaultValue = "viewCount") String sortBy,
            Model model) {

        try {
            JsonNode searchResults = youtubeservice.searchVideos(keywords, maxResults, sortBy);
            
            List<String> videoIds = new ArrayList<>();
            for (JsonNode item : searchResults.path("items")) {
                videoIds.add(item.path("id").path("videoId").asText());
            }

            List<JsonNode> videoDetails = youtubeservice.getVideoDetailsBatch(videoIds);
            
            List<Map<String, String>> videos = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            
            for (int i = 0; i < searchResults.path("items").size(); i++) {
                JsonNode searchItem = searchResults.path("items").get(i);
                JsonNode details = videoDetails.get(i);

                Map<String, String> videoData = new HashMap<>();
                videoData.put("videoId", searchItem.path("id").path("videoId").asText());
                videoData.put("title", searchItem.path("snippet").path("title").asText());
                videoData.put("thumbnailUrl", searchItem.path("snippet").path("thumbnails").path("high").path("url").asText());
                
                // Format the date properly
                try {
                    Instant instant = Instant.parse(searchItem.path("snippet").path("publishedAt").asText());
                    videoData.put("publishedAt", formatter.format(instant.atZone(java.time.ZoneId.systemDefault())));
                } catch (DateTimeParseException e) {
                    videoData.put("publishedAt", searchItem.path("snippet").path("publishedAt").asText());
                }
                
                videoData.put("viewCount", details.path("statistics").path("viewCount").asText());
                videoData.put("url", "https://www.youtube.com/watch?v=" + searchItem.path("id").path("videoId").asText());

                videos.add(videoData);
            }

            model.addAttribute("keywords", keywords);
            model.addAttribute("videos", videos);
            return "search-results";

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    // NEW METHOD: Handle analyze button from search results
    @GetMapping("/analyze")
    public String analyzeVideo(@RequestParam String videoId, Model model) {
        try {
            // Get comprehensive video details including statistics
            JsonNode videoDetails = youtubeservice.getVideoDetailsWithStats(videoId);
            JsonNode channelDetails = youtubeservice.getChannelDetails(
                videoDetails.path("snippet").path("channelId").asText()
            );

            // Extract video information
            JsonNode snippet = videoDetails.path("snippet");
            JsonNode statistics = videoDetails.path("statistics");
            
            // Extract channel information
            JsonNode channelSnippet = channelDetails.path("snippet");
            JsonNode channelStatistics = channelDetails.path("statistics");

            // Video details
            model.addAttribute("videoId", videoId);
            model.addAttribute("title", snippet.path("title").asText());
            model.addAttribute("description", snippet.path("description").asText());
            model.addAttribute("thumbnailUrl", snippet.path("thumbnails").path("maxres").path("url").asText());
            model.addAttribute("publishedAt", snippet.path("publishedAt").asText());
            
            // Video statistics
            model.addAttribute("viewCount", statistics.path("viewCount").asText());
            model.addAttribute("likeCount", statistics.path("likeCount").asText());
            model.addAttribute("commentCount", statistics.path("commentCount").asText());
            
            // Channel details
            model.addAttribute("channelTitle", snippet.path("channelTitle").asText());
            model.addAttribute("channelId", snippet.path("channelId").asText());
            model.addAttribute("channelThumbnail", channelSnippet.path("thumbnails").path("high").path("url").asText());
            model.addAttribute("channelDescription", channelSnippet.path("description").asText());
            model.addAttribute("subscriberCount", channelStatistics.path("subscriberCount").asText());
            model.addAttribute("videoCount", channelStatistics.path("videoCount").asText());
            
            // YouTube URL
            model.addAttribute("youtubeUrl", "https://www.youtube.com/watch?v=" + videoId);

            return "video-analytics";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
}