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
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Arrays;

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

    @GetMapping("/search")
public String handleSearch(
        @RequestParam String keywords,
        @RequestParam(defaultValue = "10") int maxResults,
        @RequestParam(defaultValue = "viewCount") String sortBy,
        @RequestParam(defaultValue = "") String pageToken,
        Model model) {

    try {
        JsonNode searchResults = youtubeservice.searchVideos(keywords, maxResults, sortBy, pageToken);
        
        List<String> videoIds = new ArrayList<>();
        for (JsonNode item : searchResults.path("items")) {
            videoIds.add(item.path("id").path("videoId").asText());
        }

        List<JsonNode> videoDetails = youtubeservice.getVideoDetailsBatch(videoIds);
        
        List<Map<String, String>> videos = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d,yyyy");
        
        for (int i = 0; i < searchResults.path("items").size(); i++) {
            JsonNode searchItem = searchResults.path("items").get(i);
            JsonNode details = videoDetails.get(i);

            Map<String, String> videoData = new HashMap<>();
            videoData.put("videoId", searchItem.path("id").path("videoId").asText());
            videoData.put("title", searchItem.path("snippet").path("title").asText());
            videoData.put("thumbnailUrl", searchItem.path("snippet").path("thumbnails").path("high").path("url").asText());
            
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
        model.addAttribute("maxResults", maxResults);
        model.addAttribute("sortBy", sortBy);
        
        // Add nextPageToken if available
        String nextPageToken = searchResults.path("nextPageToken").asText(null);
        if (nextPageToken != null) {
            model.addAttribute("nextPageToken", nextPageToken);
        }
        
        return "search-results";

    } catch (Exception e) {
        e.printStackTrace();
        return "error";
    }
}

    @GetMapping("/analyze")
    public String analyzeVideo(@RequestParam String videoId, Model model) {
        try {
            JsonNode videoDetails = youtubeservice.getVideoDetailsWithStats(videoId);
            
            JsonNode channelDetails = youtubeservice.getChannelDetails(
                videoDetails.path("snippet").path("channelId").asText()
            );

            JsonNode snippet = videoDetails.path("snippet");
            JsonNode statistics = videoDetails.path("statistics");
            
            JsonNode channelSnippet = null;
            JsonNode channelStatistics = null;

            if (channelDetails != null) {
                channelSnippet = channelDetails.path("snippet");
                channelStatistics = channelDetails.path("statistics");
            } else {
                System.err.println("Channel details not found for videoId: " + videoId);
                ObjectMapper mapper = new ObjectMapper();
                channelSnippet = mapper.createObjectNode();
                channelStatistics = mapper.createObjectNode();
            }

            model.addAttribute("videoId", videoId);
            model.addAttribute("title", snippet.path("title").asText());
            model.addAttribute("description", snippet.path("description").asText());
            model.addAttribute("thumbnailUrl", snippet.path("thumbnails").path("maxres").path("url").asText());
            model.addAttribute("publishedAt", snippet.path("publishedAt").asText());
            
            model.addAttribute("viewCount", statistics.path("viewCount").asText());
            model.addAttribute("likeCount", statistics.path("likeCount").asText());
            model.addAttribute("commentCount", statistics.path("commentCount").asText());
            
            model.addAttribute("channelTitle", channelSnippet.path("title").asText());
            model.addAttribute("channelId", snippet.path("channelId").asText());
            model.addAttribute("channelThumbnail", channelSnippet.path("thumbnails").path("high").path("url").asText());
            model.addAttribute("channelDescription", channelSnippet.path("description").asText());
            model.addAttribute("subscriberCount", channelStatistics.path("subscriberCount").asText());
            model.addAttribute("videoCount", channelStatistics.path("videoCount").asText());
            
            model.addAttribute("youtubeUrl", "https://www.youtube.com/watch?v=" + videoId);

            return "video-analytics";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    // New Content Generator Endpoints
    @PostMapping("/generate-title")
public String generateTitle(@RequestParam String keywords, Model model) {
    try {
        // Improved title generation with patterns
        String[] patterns = {
            "The Ultimate Guide to {keywords} | Expert Tips",
            "{keywords}: Everything You Need to Know",
            "10 Amazing Facts About {keywords}",
            "How to Master {keywords} in 2025",
            "{keywords} Explained: The Complete Breakdown"
        };
        
        Random random = new Random();
        String pattern = patterns[random.nextInt(patterns.length)];
        String generatedTitle = pattern.replace("{keywords}", keywords);
        
        model.addAttribute("generatedContent", generatedTitle);
        model.addAttribute("generatorType", "Title");
        return "generated-content-display";
    } catch (Exception e) {
        e.printStackTrace();
        return "error";
    }
}

@PostMapping("/generate-tags")
public String generateTags(@RequestParam String keywords, Model model) {
    try {
        // Generate more relevant tags with variations
        String[] keywordParts = keywords.toLowerCase().split(" ");
        List<String> tags = new ArrayList<>();
        
        // Add basic variations
        tags.add(keywords.toLowerCase().replace(" ", "-"));
        tags.add(keywords.toLowerCase().replace(" ", ""));
        tags.addAll(Arrays.asList(keywordParts));
        
        // Add common YouTube tags
        tags.addAll(Arrays.asList(
            "youtube",
            "video",
            "tutorial",
            "howto",
            "education",
            "2025",
            "trending",
            "viral"
        ));
        
        // Add related terms
        for (String part : keywordParts) {
            tags.add(part + "tutorial");
            tags.add(part + "tips");
            tags.add("best" + part);
            tags.add(part + "2025");
        }
        
        // Remove duplicates and limit to 20 tags
        tags = tags.stream().distinct().limit(20).collect(Collectors.toList());
        
        String generatedTags = String.join(", ", tags);
        model.addAttribute("generatedContent", generatedTags);
        model.addAttribute("generatorType", "Tags");
        return "generated-content-display";
    } catch (Exception e) {
        e.printStackTrace();
        return "error";
    }
}

@PostMapping("/generate-description")
public String generateDescription(@RequestParam String keywords, Model model) {
    try {
        // Improved description template with sections
        String[] intros = {
            "In this comprehensive video, we explore {keywords} in depth.",
            "Welcome to our ultimate guide about {keywords}!", 
            "Today we're diving deep into the world of {keywords}.",
            "Discover everything you need to know about {keywords}."
        };
        
        String[] bodies = {
            "\n\nWe'll cover:\n- Key concepts and fundamentals\n- Practical applications\n- Common mistakes to avoid\n- Expert tips and tricks",
            "\n\nWhat you'll learn:\n✔ The essential principles\n✔ Step-by-step techniques\n✔ Real-world examples\n✔ Best practices for success",
            "\n\nThis video includes:\n→ Detailed explanations\n→ Visual demonstrations\n→ Helpful resources\n→ Actionable advice"
        };
        
        String[] outros = {
            "\n\nDon't forget to like, comment, and subscribe for more content like this!",
            "\n\nIf you found this video helpful, please share it with others who might benefit.",
            "\n\nLeave a comment below with your thoughts or questions about {keywords}.",
            "\n\nSubscribe and hit the notification bell so you don't miss our future videos!"
        };
        
        Random random = new Random();
        String description = intros[random.nextInt(intros.length)].replace("{keywords}", keywords)
                + bodies[random.nextInt(bodies.length)]
                + outros[random.nextInt(outros.length)].replace("{keywords}", keywords);
        
        model.addAttribute("generatedContent", description);
        model.addAttribute("generatorType", "Description");
        return "generated-content-display";
    } catch (Exception e) {
        e.printStackTrace();
        return "error";
    }
}

    
}