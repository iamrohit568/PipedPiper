package com.example.yt_scrapper.Model;

public class VideoSyncMessage {
    private String roomId;
    private String username;
    private String videoId;
    private String videoTitle;
    private double currentTime;
    private boolean isPlaying;
    private SyncAction action;
    
    public enum SyncAction {
        PLAY,
        PAUSE,
        SEEK,
        CHANGE_VIDEO,
        SYNC_REQUEST,
        SYNC_RESPONSE
    }
    
    public VideoSyncMessage() {}
    
    public VideoSyncMessage(String roomId, String username, SyncAction action) {
        this.roomId = roomId;
        this.username = username;
        this.action = action;
    }
    
    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
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
    
    public SyncAction getAction() {
        return action;
    }
    
    public void setAction(SyncAction action) {
        this.action = action;
    }
}
