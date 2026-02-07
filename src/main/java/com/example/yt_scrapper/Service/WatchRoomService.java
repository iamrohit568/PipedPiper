package com.example.yt_scrapper.Service;

import com.example.yt_scrapper.Model.WatchRoom;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WatchRoomService {
    
    // In-memory storage for watch rooms (use Redis or DB for production)
    private final Map<String, WatchRoom> rooms = new ConcurrentHashMap<>();
    
    /**
     * Create a new watch room with a unique ID
     */
    public WatchRoom createRoom(String hostUsername) {
        String roomId = generateRoomId();
        WatchRoom room = new WatchRoom(roomId, hostUsername);
        rooms.put(roomId, room);
        return room;
    }
    
    /**
     * Get a room by ID
     */
    public WatchRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }
    
    /**
     * Check if a room exists
     */
    public boolean roomExists(String roomId) {
        return rooms.containsKey(roomId);
    }
    
    /**
     * Add a participant to a room
     */
    public boolean joinRoom(String roomId, String username) {
        WatchRoom room = rooms.get(roomId);
        if (room != null) {
            room.addParticipant(username);
            return true;
        }
        return false;
    }
    
    /**
     * Remove a participant from a room
     */
    public void leaveRoom(String roomId, String username) {
        WatchRoom room = rooms.get(roomId);
        if (room != null) {
            room.removeParticipant(username);
            // Delete room if empty
            if (room.getParticipantCount() == 0) {
                rooms.remove(roomId);
            }
        }
    }
    
    /**
     * Update the current video in a room
     */
    public void updateVideo(String roomId, String videoId, String videoTitle) {
        WatchRoom room = rooms.get(roomId);
        if (room != null) {
            room.setCurrentVideoId(videoId);
            room.setCurrentVideoTitle(videoTitle);
            room.setCurrentTime(0);
            room.setPlaying(false);
        }
    }
    
    /**
     * Update playback state
     */
    public void updatePlaybackState(String roomId, double currentTime, boolean isPlaying) {
        WatchRoom room = rooms.get(roomId);
        if (room != null) {
            room.setCurrentTime(currentTime);
            room.setPlaying(isPlaying);
        }
    }
    
    /**
     * Delete a room
     */
    public void deleteRoom(String roomId) {
        rooms.remove(roomId);
    }
    
    /**
     * Generate a unique room ID (8 characters, URL-friendly)
     */
    private String generateRoomId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, 8).toUpperCase();
    }
    
    /**
     * Get all active rooms (for admin/debugging)
     */
    public Map<String, WatchRoom> getAllRooms() {
        return new ConcurrentHashMap<>(rooms);
    }
}
