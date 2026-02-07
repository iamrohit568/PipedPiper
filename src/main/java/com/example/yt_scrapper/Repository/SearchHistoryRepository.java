package com.example.yt_scrapper.Repository;

import com.example.yt_scrapper.Model.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    
    // Find search history by username, ordered by most recent
    List<SearchHistory> findByUsernameOrderBySearchedAtDesc(String username);
    
    // Find top N most searched queries by user
    @Query("SELECT s FROM SearchHistory s WHERE s.username = :username ORDER BY s.searchCount DESC, s.searchedAt DESC")
    List<SearchHistory> findTopSearchesByUsername(@Param("username") String username);
    
    // Find recent searches within a time period
    List<SearchHistory> findByUsernameAndSearchedAtAfterOrderBySearchedAtDesc(String username, LocalDateTime after);
    
    // Find specific search query for a user
    Optional<SearchHistory> findByUsernameAndSearchQuery(String username, String searchQuery);
    
    // Get distinct search queries for a user (most recent first)
    @Query("SELECT DISTINCT s.searchQuery FROM SearchHistory s WHERE s.username = :username ORDER BY s.searchedAt DESC")
    List<String> findDistinctSearchQueriesByUsername(@Param("username") String username);
    
    // Get top keywords/topics from search history
    @Query(value = "SELECT search_query, SUM(search_count) as total FROM search_history WHERE username = :username GROUP BY search_query ORDER BY total DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopKeywordsByUsername(@Param("username") String username, @Param("limit") int limit);
    
    // Delete old search history
    void deleteByUsernameAndSearchedAtBefore(String username, LocalDateTime before);
}
