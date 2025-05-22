package com.example.yt_scrapper.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.yt_scrapper.Service.ytservice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


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
        String videoId=youtubeservice.extractVideoId(videoLink);
        // System.out.println("Video ID: " + videoId);

        if(videoId != null) {
            try {
                JsonNode videoDetails=youtubeservice.getVideoDetails(videoId);
                // System.out.println(videoDetails);
                String title=videoDetails.path("title").asText();
                String description=videoDetails.path("description").asText();
                String thmurl=videoDetails.path("thumbnails").path("standard").path("url").asText();
                String[] tags = new ObjectMapper().treeToValue(videoDetails.path("tags"), String[].class);

                    model.addAttribute("videoDetails", "test");
                    model.addAttribute("videoId", videoId);
                    model.addAttribute("title", title);
                    model.addAttribute("thumbnailUrl", thmurl);
                    model.addAttribute("tags", tags);
                    model.addAttribute("description", description);
                    // System.out.println(model);

                return "details";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "error";
    }
}
