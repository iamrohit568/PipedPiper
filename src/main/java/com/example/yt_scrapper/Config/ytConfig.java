// package com.example.yt_scrapper.Config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Configuration;

// import lombok.Data;

// @Configuration
// @Data
// public class ytConfig {

//     @Value("${youtube.api.key}")
//     private String apiKey;
    
//     @Value("${youtube.api.url}")
//     private String apiUrl;
// }


package com.example.yt_scrapper.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class ytConfig {
    @Value("${youtube.api.key}")
    private String apiKey;
    
    @Value("${youtube.api.url}")
    private String apiUrl;
    
    @Value("${youtube.api.search.url}")
    private String searchApiUrl;
    
    @Value("${youtube.max.results}")
    private int maxResults;
}