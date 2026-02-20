package com.example.yt_scrapper.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transcript_cache", indexes = {
        @Index(name = "idx_video_id", columnList = "videoId", unique = true)
})
public class TranscriptCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String videoId;

    @Column(nullable = false, length = 500)
    private String videoTitle;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String transcript;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    @Column
    private int transcriptLength;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public TranscriptCache() {
        this.createdAt = LocalDateTime.now();
    }

    public TranscriptCache(String videoId, String videoTitle, String transcript, String summary) {
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.transcript = transcript;
        this.summary = summary;
        this.transcriptLength = transcript != null ? transcript.length() : 0;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getTranscriptLength() {
        return transcriptLength;
    }

    public void setTranscriptLength(int transcriptLength) {
        this.transcriptLength = transcriptLength;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
