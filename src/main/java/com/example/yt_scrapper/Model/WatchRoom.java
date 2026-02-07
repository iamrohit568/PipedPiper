package com.example.yt_scrapper.Model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WatchRoom {
    private String roomId;
    private String hostUsername;
    private String currentVideoId;
    private String currentVideoTitle;
    private double currentTime;
    private boolean isPlaying;
    private Set<String> participants;
    private LocalDateTime createdAt;
    
    public WatchRoom(String roomId, String hostUsername) {
        this.roomId = roomId;
        this.hostUsername = hostUsername;
        this.currentVideoId = null;
        this.currentVideoTitle = null;
        this.currentTime = 0;
        this.isPlaying = false;
        this.participants = ConcurrentHashMap.newKeySet();
        this.participants.add(hostUsername);
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public String getHostUsername() {
        return hostUsername;
    }
    
    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }
    
    public String getCurrentVideoId() {
        return currentVideoId;
    }
    
    public void setCurrentVideoId(String currentVideoId) {
        this.currentVideoId = currentVideoId;
    }
    
    public String getCurrentVideoTitle() {
        return currentVideoTitle;
    }
    
    public void setCurrentVideoTitle(String currentVideoTitle) {
        this.currentVideoTitle = currentVideoTitle;
    }
    
    public double getCurrentTime() {
        return currentTime;
    }
    
    public void setCurrentTime(double currentTime) {
        this.currentTime = currentTime;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
    
    public Set<String> getParticipants() {
        return participants;
    }
    
    public void addParticipant(String username) {
        this.participants.add(username);
    }
    
    public void removeParticipant(String username) {
        this.participants.remove(username);
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public int getParticipantCount() {
        return participants.size();
    }
}
