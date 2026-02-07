package com.example.yt_scrapper.Repository;

import com.example.yt_scrapper.Model.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    
    // Find watch history by username, ordered by most recent
    List<WatchHistory> findByUsernameOrderByWatchedAtDesc(String username);
    
    // Find top N most watched videos by user
    @Query("SELECT w FROM WatchHistory w WHERE w.username = :username ORDER BY w.watchCount DESC, w.watchedAt DESC")
    List<WatchHistory> findTopWatchedByUsername(@Param("username") String username);
    
    // Find recent watches within a time period
    List<WatchHistory> findByUsernameAndWatchedAtAfterOrderByWatchedAtDesc(String username, LocalDateTime after);
    
    // Find specific video in watch history
    Optional<WatchHistory> findByUsernameAndVideoId(String username, String videoId);
    
    // Get distinct video IDs watched by user (most recent first)
    @Query("SELECT DISTINCT w.videoId FROM WatchHistory w WHERE w.username = :username ORDER BY w.watchedAt DESC")
    List<String> findDistinctVideoIdsByUsername(@Param("username") String username);
    
    // Get most watched channels by user
    @Query("SELECT w.channelTitle, COUNT(w) as watchCount FROM WatchHistory w WHERE w.username = :username AND w.channelTitle IS NOT NULL GROUP BY w.channelTitle ORDER BY watchCount DESC")
    List<Object[]> findTopChannelsByUsername(@Param("username") String username);
    
    // Get most watched categories by user
    @Query("SELECT w.category, COUNT(w) as watchCount FROM WatchHistory w WHERE w.username = :username AND w.category IS NOT NULL GROUP BY w.category ORDER BY watchCount DESC")
    List<Object[]> findTopCategoriesByUsername(@Param("username") String username);
    
    // Get recent video titles for keyword extraction
    @Query("SELECT w.videoTitle FROM WatchHistory w WHERE w.username = :username ORDER BY w.watchedAt DESC")
    List<String> findRecentVideoTitlesByUsername(@Param("username") String username);
    
    // Delete old watch history
    void deleteByUsernameAndWatchedAtBefore(String username, LocalDateTime before);
    
    // Count videos watched by user
    long countByUsername(String username);
}
