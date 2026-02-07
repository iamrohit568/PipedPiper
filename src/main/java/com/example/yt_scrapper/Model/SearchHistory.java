package com.example.yt_scrapper.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_history")
public class SearchHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String searchQuery;
    
    @Column(nullable = false)
    private LocalDateTime searchedAt;
    
    @Column
    private int searchCount = 1; // Track how many times this query was searched
    
    public SearchHistory() {
        this.searchedAt = LocalDateTime.now();
    }
    
    public SearchHistory(String username, String searchQuery) {
        this.username = username;
        this.searchQuery = searchQuery;
        this.searchedAt = LocalDateTime.now();
        this.searchCount = 1;
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
    
    public String getSearchQuery() {
        return searchQuery;
    }
    
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
    
    public LocalDateTime getSearchedAt() {
        return searchedAt;
    }
    
    public void setSearchedAt(LocalDateTime searchedAt) {
        this.searchedAt = searchedAt;
    }
    
    public int getSearchCount() {
        return searchCount;
    }
    
    public void setSearchCount(int searchCount) {
        this.searchCount = searchCount;
    }
    
    public void incrementSearchCount() {
        this.searchCount++;
        this.searchedAt = LocalDateTime.now();
    }
}
