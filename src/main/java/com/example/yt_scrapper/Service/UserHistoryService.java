package com.example.yt_scrapper.Service;

import com.example.yt_scrapper.Model.SearchHistory;
import com.example.yt_scrapper.Model.WatchHistory;
import com.example.yt_scrapper.Repository.SearchHistoryRepository;
import com.example.yt_scrapper.Repository.WatchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserHistoryService {
    
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    
    @Autowired
    private WatchHistoryRepository watchHistoryRepository;
    
    @Autowired
    private ytservice youtubeService;
    
    // ==================== Search History ====================
    
    public void saveSearchHistory(String username, String query) {
        if (username == null || query == null || query.trim().isEmpty()) {
            return;
        }
        
        query = query.trim().toLowerCase();
        
        // Check if this query already exists for the user
        Optional<SearchHistory> existing = searchHistoryRepository.findByUsernameAndSearchQuery(username, query);
        
        if (existing.isPresent()) {
            // Increment the search count
            SearchHistory history = existing.get();
            history.incrementSearchCount();
            searchHistoryRepository.save(history);
        } else {
            // Create new search history entry
            SearchHistory history = new SearchHistory(username, query);
            searchHistoryRepository.save(history);
        }
    }
    
    public List<String> getRecentSearches(String username, int limit) {
        List<SearchHistory> history = searchHistoryRepository.findByUsernameOrderBySearchedAtDesc(username);
        return history.stream()
                .map(SearchHistory::getSearchQuery)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<String> getTopSearchKeywords(String username, int limit) {
        List<Object[]> results = searchHistoryRepository.findTopKeywordsByUsername(username, limit);
        return results.stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());
    }
    
    // ==================== Watch History ====================
    
    public void saveWatchHistory(String username, String videoId, String videoTitle, 
                                  String channelTitle, String thumbnailUrl, String category) {
        if (username == null || videoId == null) {
            return;
        }
        
        // Check if this video already exists in watch history
        Optional<WatchHistory> existing = watchHistoryRepository.findByUsernameAndVideoId(username, videoId);
        
        if (existing.isPresent()) {
            // Increment the watch count
            WatchHistory history = existing.get();
            history.incrementWatchCount();
            if (channelTitle != null) history.setChannelTitle(channelTitle);
            if (thumbnailUrl != null) history.setThumbnailUrl(thumbnailUrl);
            if (category != null) history.setCategory(category);
            watchHistoryRepository.save(history);
        } else {
            // Create new watch history entry
            WatchHistory history = new WatchHistory(username, videoId, videoTitle);
            history.setChannelTitle(channelTitle);
            history.setThumbnailUrl(thumbnailUrl);
            history.setCategory(category);
            watchHistoryRepository.save(history);
        }
    }
    
    public List<WatchHistory> getRecentWatchHistory(String username, int limit) {
        List<WatchHistory> history = watchHistoryRepository.findByUsernameOrderByWatchedAtDesc(username);
        return history.stream().limit(limit).collect(Collectors.toList());
    }
    
    public List<String> getTopWatchedChannels(String username, int limit) {
        List<Object[]> results = watchHistoryRepository.findTopChannelsByUsername(username);
        return results.stream()
                .map(row -> (String) row[0])
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<String> getTopCategories(String username, int limit) {
        List<Object[]> results = watchHistoryRepository.findTopCategoriesByUsername(username);
        return results.stream()
                .map(row -> (String) row[0])
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    // ==================== Personalized Recommendations ====================
    
    /**
     * Generate personalized search queries based on user's history
     */
    public List<String> generatePersonalizedKeywords(String username) {
        Set<String> keywords = new LinkedHashSet<>();
        
        // 1. Add top search keywords
        List<String> topSearches = getTopSearchKeywords(username, 5);
        keywords.addAll(topSearches);
        
        // 2. Add top watched channels
        List<String> topChannels = getTopWatchedChannels(username, 3);
        keywords.addAll(topChannels);
        
        // 3. Extract keywords from recent video titles
        List<String> recentTitles = watchHistoryRepository.findRecentVideoTitlesByUsername(username);
        for (String title : recentTitles.stream().limit(10).collect(Collectors.toList())) {
            keywords.addAll(extractKeywordsFromTitle(title));
        }
        
        // 4. Add category-based keywords
        List<String> topCategories = getTopCategories(username, 3);
        keywords.addAll(topCategories);
        
        return new ArrayList<>(keywords).stream().limit(10).collect(Collectors.toList());
    }
    
    /**
     * Extract meaningful keywords from video title
     */
    private Set<String> extractKeywordsFromTitle(String title) {
        Set<String> keywords = new HashSet<>();
        
        if (title == null || title.isEmpty()) {
            return keywords;
        }
        
        // Remove special characters and split into words
        String[] words = title.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .split("\\s+");
        
        // Common words to ignore (using HashSet to avoid duplicate issues)
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall", "can", "need", "dare",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "up",
            "about", "into", "over", "after", "and", "but", "or", "nor", "so",
            "yet", "both", "either", "neither", "not", "only", "own", "same",
            "than", "too", "very", "just", "also", "now", "how", "all", "each",
            "every", "few", "more", "most", "other", "some", "such", "no",
            "video", "official", "new", "full", "hd", "4k", "2024", "2025", "2026"
        ));
        
        for (String word : words) {
            if (word.length() > 2 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords.stream().limit(3).collect(Collectors.toSet());
    }
    
    /**
     * Get personalized video recommendations
     */
    public List<Map<String, String>> getPersonalizedRecommendations(String username, int maxResults) throws Exception {
        List<String> keywords = generatePersonalizedKeywords(username);
        
        if (keywords.isEmpty()) {
            // If no history, return trending videos
            return youtubeService.getTrendingVideos("IN", maxResults);
        }
        
        // Combine keywords to create a search query
        String searchQuery = String.join(" ", keywords.stream().limit(3).collect(Collectors.toList()));
        
        // Get personalized videos based on user interests
        Map<String, Object> searchResults = youtubeService.searchVideosWithPagination(searchQuery, null, maxResults);
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> videos = (List<Map<String, String>>) searchResults.get("videos");
        
        // Also mix in some trending videos for variety
        List<Map<String, String>> trendingVideos = youtubeService.getTrendingVideos("IN", maxResults / 2);
        
        // Merge and shuffle for variety
        Set<String> addedIds = new HashSet<>();
        List<Map<String, String>> combined = new ArrayList<>();
        
        // Add personalized videos first
        for (Map<String, String> video : videos) {
            String videoId = video.get("videoId");
            if (!addedIds.contains(videoId)) {
                combined.add(video);
                addedIds.add(videoId);
            }
        }
        
        // Add trending videos that aren't already in the list
        for (Map<String, String> video : trendingVideos) {
            String videoId = video.get("videoId");
            if (!addedIds.contains(videoId)) {
                combined.add(video);
                addedIds.add(videoId);
            }
        }
        
        // Shuffle to mix personalized and trending
        Collections.shuffle(combined.subList(Math.min(5, combined.size()), combined.size()));
        
        return combined.stream().limit(maxResults).collect(Collectors.toList());
    }
    
    /**
     * Check if user has enough history for personalization
     */
    public boolean hasEnoughHistory(String username) {
        long searchCount = searchHistoryRepository.findByUsernameOrderBySearchedAtDesc(username).size();
        long watchCount = watchHistoryRepository.countByUsername(username);
        return (searchCount + watchCount) >= 3;
    }
    
    /**
     * Get user's interest profile summary
     */
    public Map<String, Object> getUserInterestProfile(String username) {
        Map<String, Object> profile = new HashMap<>();
        
        profile.put("topSearches", getTopSearchKeywords(username, 5));
        profile.put("topChannels", getTopWatchedChannels(username, 5));
        profile.put("topCategories", getTopCategories(username, 5));
        profile.put("recentSearches", getRecentSearches(username, 10));
        profile.put("videosWatched", watchHistoryRepository.countByUsername(username));
        
        return profile;
    }
    
    /**
     * Clear user history
     */
    public void clearSearchHistory(String username) {
        List<SearchHistory> history = searchHistoryRepository.findByUsernameOrderBySearchedAtDesc(username);
        searchHistoryRepository.deleteAll(history);
    }
    
    public void clearWatchHistory(String username) {
        List<WatchHistory> history = watchHistoryRepository.findByUsernameOrderByWatchedAtDesc(username);
        watchHistoryRepository.deleteAll(history);
    }
}
