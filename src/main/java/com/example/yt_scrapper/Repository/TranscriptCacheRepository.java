package com.example.yt_scrapper.Repository;

import com.example.yt_scrapper.Model.TranscriptCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranscriptCacheRepository extends JpaRepository<TranscriptCache, Long> {
    Optional<TranscriptCache> findByVideoId(String videoId);

    boolean existsByVideoId(String videoId);
}
