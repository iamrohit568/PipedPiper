package com.example.yt_scrapper.Controller;

import com.example.yt_scrapper.Model.ChatMessage;
import com.example.yt_scrapper.Model.VideoSyncMessage;
import com.example.yt_scrapper.Model.WatchRoom;
import com.example.yt_scrapper.Service.WatchRoomService;
import com.example.yt_scrapper.Service.ytservice;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@Controller
public class WatchTogetherController {

    @Autowired
    private WatchRoomService watchRoomService;

    @Autowired
    private ytservice youtubeService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Main page to create or join a watch room
     */
    @GetMapping("/watch-together")
    public String watchTogetherLanding(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest" + new Random().nextInt(1000);
        model.addAttribute("username", username);
        return "watch-together-landing";
    }

    /**
     * Create a new watch room
     */
    @PostMapping("/watch-together/create")
    public String createRoom(Principal principal) {
        String username = principal != null ? principal.getName() : "Guest" + new Random().nextInt(1000);
        WatchRoom room = watchRoomService.createRoom(username);
        return "redirect:/watch-together/room/" + room.getRoomId();
    }

    /**
     * Create a new watch room with a specific video already loaded
     */
    @GetMapping("/watch-together/create-with-video")
    public String createRoomWithVideo(
            @RequestParam String videoId,
            @RequestParam String title,
            Principal principal) {
        String username = principal != null ? principal.getName() : "Guest" + new Random().nextInt(1000);
        WatchRoom room = watchRoomService.createRoom(username);
        
        // Set the initial video for this room
        watchRoomService.updateVideo(room.getRoomId(), videoId, title);
        
        return "redirect:/watch-together/room/" + room.getRoomId() + "?videoId=" + videoId + "&title=" + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Join an existing room by ID
     */
    @GetMapping("/watch-together/room/{roomId}")
    public String joinRoom(@PathVariable String roomId,
                          @RequestParam(required = false) String videoId,
                          @RequestParam(required = false) String title,
                          Model model, Principal principal) {
        WatchRoom room = watchRoomService.getRoom(roomId);
        
        if (room == null) {
            model.addAttribute("error", "Room not found");
            return "watch-together-landing";
        }
        
        String username = principal != null ? principal.getName() : "Guest" + new Random().nextInt(1000);
        watchRoomService.joinRoom(roomId, username);
        
        model.addAttribute("roomId", roomId);
        model.addAttribute("username", username);
        model.addAttribute("isHost", room.getHostUsername().equals(username));
        model.addAttribute("room", room);
        
        // Pass initial video info if provided
        if (videoId != null && !videoId.isEmpty()) {
            model.addAttribute("initialVideoId", videoId);
            model.addAttribute("initialVideoTitle", title != null ? title : "");
        } else if (room.getCurrentVideoId() != null) {
            model.addAttribute("initialVideoId", room.getCurrentVideoId());
            model.addAttribute("initialVideoTitle", room.getCurrentVideoTitle() != null ? room.getCurrentVideoTitle() : "");
        }
        
        return "watch-together";
    }

    /**
     * API: Get home feed videos (like YouTube homepage) with pagination
     */
    @GetMapping("/api/watch-together/videos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHomeVideos(
            @RequestParam(defaultValue = "IN") String regionCode,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "20") int maxResults) {
        try {
            Map<String, Object> result = youtubeService.getHomeFeedVideos(regionCode, pageToken, maxResults);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("videos", Collections.emptyList());
            error.put("nextPageToken", null);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * API: Get trending/popular videos from YouTube (legacy endpoint)
     */
    @GetMapping("/api/watch-together/trending")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getTrendingVideos(
            @RequestParam(defaultValue = "IN") String regionCode,
            @RequestParam(defaultValue = "20") int maxResults) {
        try {
            List<Map<String, String>> videos = youtubeService.getTrendingVideos(regionCode, maxResults);
            return ResponseEntity.ok(videos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Collections.emptyList());
        }
    }

    /**
     * API: Search videos with pagination
     */
    @GetMapping("/api/watch-together/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchVideos(
            @RequestParam String query,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "20") int maxResults) {
        try {
            Map<String, Object> result = youtubeService.searchVideosWithPagination(query, pageToken, maxResults);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("videos", Collections.emptyList());
            error.put("nextPageToken", null);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * API: Get videos by category with pagination
     */
    @GetMapping("/api/watch-together/category/{categoryId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getVideosByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "IN") String regionCode,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "20") int maxResults) {
        try {
            Map<String, Object> result = youtubeService.getVideosByCategory(categoryId, regionCode, pageToken, maxResults);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("videos", Collections.emptyList());
            error.put("nextPageToken", null);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * API: Get room info
     */
    @GetMapping("/api/watch-together/room/{roomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomInfo(@PathVariable String roomId) {
        WatchRoom room = watchRoomService.getRoom(roomId);
        
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> roomInfo = new HashMap<>();
        roomInfo.put("roomId", room.getRoomId());
        roomInfo.put("hostUsername", room.getHostUsername());
        roomInfo.put("currentVideoId", room.getCurrentVideoId());
        roomInfo.put("currentVideoTitle", room.getCurrentVideoTitle());
        roomInfo.put("currentTime", room.getCurrentTime());
        roomInfo.put("isPlaying", room.isPlaying());
        roomInfo.put("participants", room.getParticipants());
        roomInfo.put("participantCount", room.getParticipantCount());
        
        return ResponseEntity.ok(roomInfo);
    }

    // ==================== WebSocket Message Handlers ====================

    /**
     * Handle chat messages
     */
    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessage handleChatMessage(@DestinationVariable String roomId, @Payload ChatMessage message) {
        message.setRoomId(roomId);
        return message;
    }

    /**
     * Handle user join notifications
     */
    @MessageMapping("/join/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage handleUserJoin(@DestinationVariable String roomId, @Payload ChatMessage message) {
        message.setRoomId(roomId);
        message.setType(ChatMessage.MessageType.JOIN);
        watchRoomService.joinRoom(roomId, message.getSender());
        return message;
    }

    /**
     * Handle user leave notifications
     */
    @MessageMapping("/leave/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage handleUserLeave(@DestinationVariable String roomId, @Payload ChatMessage message) {
        message.setRoomId(roomId);
        message.setType(ChatMessage.MessageType.LEAVE);
        watchRoomService.leaveRoom(roomId, message.getSender());
        return message;
    }

    /**
     * Handle video sync messages (play, pause, seek)
     */
    @MessageMapping("/sync/{roomId}")
    @SendTo("/topic/sync/{roomId}")
    public VideoSyncMessage handleVideoSync(@DestinationVariable String roomId, @Payload VideoSyncMessage message) {
        message.setRoomId(roomId);
        
        // Update room state
        watchRoomService.updatePlaybackState(roomId, message.getCurrentTime(), message.isPlaying());
        
        return message;
    }

    /**
     * Handle video change
     */
    @MessageMapping("/video-change/{roomId}")
    @SendTo("/topic/sync/{roomId}")
    public VideoSyncMessage handleVideoChange(@DestinationVariable String roomId, @Payload VideoSyncMessage message) {
        message.setRoomId(roomId);
        message.setAction(VideoSyncMessage.SyncAction.CHANGE_VIDEO);
        
        // Update room with new video
        watchRoomService.updateVideo(roomId, message.getVideoId(), message.getVideoTitle());
        
        return message;
    }

    /**
     * Handle sync request (when a new user joins and needs current state)
     */
    @MessageMapping("/sync-request/{roomId}")
    public void handleSyncRequest(@DestinationVariable String roomId, @Payload VideoSyncMessage message) {
        WatchRoom room = watchRoomService.getRoom(roomId);
        
        if (room != null) {
            VideoSyncMessage response = new VideoSyncMessage();
            response.setRoomId(roomId);
            response.setVideoId(room.getCurrentVideoId());
            response.setVideoTitle(room.getCurrentVideoTitle());
            response.setCurrentTime(room.getCurrentTime());
            response.setPlaying(room.isPlaying());
            response.setAction(VideoSyncMessage.SyncAction.SYNC_RESPONSE);
            
            // Send to the specific user who requested sync
            messagingTemplate.convertAndSendToUser(
                message.getUsername(),
                "/queue/sync-response",
                response
            );
        }
    }
}
