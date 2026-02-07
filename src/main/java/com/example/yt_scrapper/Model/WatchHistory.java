package com.example.yt_scrapper.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watch_history")
public class WatchHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String videoId;
    
    @Column(nullable = false)
    private String videoTitle;
    
    @Column
    private String channelTitle;
    
    @Column
    private String thumbnailUrl;
    
    @Column
    private String category; // Music, Gaming, etc.
    
    @Column(nullable = false)
    private LocalDateTime watchedAt;
    
    @Column
    private int watchCount = 1; // Track how many times this video was watched
    
    @Column
    private int watchDuration = 0; // How long they watched (in seconds)
    
    public WatchHistory() {
        this.watchedAt = LocalDateTime.now();
    }
    
    public WatchHistory(String username, String videoId, String videoTitle) {
        this.username = username;
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.watchedAt = LocalDateTime.now();
        this.watchCount = 1;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getVideoId() {
        return videoId;
    }
    
    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }
    
    public String getVideoTitle() {
        return videoTitle;
    }
    
    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }
    
    public String getChannelTitle() {
        return channelTitle;
    }
    
    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public LocalDateTime getWatchedAt() {
        return watchedAt;
    }
    
    public void setWatchedAt(LocalDateTime watchedAt) {
        this.watchedAt = watchedAt;
    }
    
    public int getWatchCount() {
        return watchCount;
    }
    
    public void setWatchCount(int watchCount) {
        this.watchCount = watchCount;
    }
    
    public int getWatchDuration() {
        return watchDuration;
    }
    
    public void setWatchDuration(int watchDuration) {
        this.watchDuration = watchDuration;
    }
    
    public void incrementWatchCount() {
        this.watchCount++;
        this.watchedAt = LocalDateTime.now();
    }
}
