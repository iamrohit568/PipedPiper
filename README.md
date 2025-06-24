```markdown
# YouTube Metadata Scraper 🎬

A Spring Boot application that extracts video metadata (title, description, tags, thumbnail) from YouTube URLs using the YouTube Data API v3.

---

## Features ✨
- **Video ID Extraction**: Parses YouTube URLs (long/short formats) using regex
- **Metadata Fetching**: Retrieves video details via YouTube Data API
- **Thymeleaf UI**: Displays results in a clean web interface
- **Configurable API**: Easy setup with properties file

---

## Tech Stack 💻
- **Backend**: Java 17, Spring Boot 3.4.5
- **API Integration**: YouTube Data API v3, RestTemplate
- **Frontend**: Thymeleaf, HTML
- **Tools**: Lombok, Jackson, Maven

---

## Setup & Installation 🛠️

### Prerequisites
- Java 17+
- YouTube Data API key
- Maven

### Configuration
1. Get YouTube API key from [Google Cloud Console](https://console.cloud.google.com/)
2. Create `application.properties`:
```properties
youtube.api.key=YOUR_API_KEY
youtube.api.url=https://www.googleapis.com/youtube/v3/videos
```

### Run Application
```bash
mvn spring-boot:run
```
Visit `http://localhost:8080`

---

## Usage 🖥️
1. Enter YouTube URL on homepage
2. Submit to view video details:
   - Title & Description
   - Thumbnail image
   - Tags
   - Video ID

---

## Project Structure 📂
```
src/
├── main/
│   ├── java/com/example/yt_scrapper/
│   │   ├── Config/ytConfig.java        # API configuration
│   │   ├── Controller/ytController.java # Request handling
│   │   ├── Service/ytservice.java      # Business logic
│   │   └── YtScrapperApplication.java  # Entry point
│   ├── resources/
│   │   ├── templates/                  # Thymeleaf views
│   │   │   ├── index.html
│   │   │   ├── details.html
│   │   │   └── error.html
│   │   └── application.properties      # API configuration
```

---

## API Reference 🔌
**YouTube Data API Endpoint**:  
`GET https://www.googleapis.com/youtube/v3/videos?part=snippet&id={videoId}&key={apiKey}`

**Response Structure**:
```json
{
  "items": [
    {
      "snippet": {
        "title": "Video Title",
        "description": "...",
        "thumbnails": {
          "standard": {"url": "..."}
        },
        "tags": ["tag1", "tag2"]
      }
    }
  ]
}
```

---

## FAQ ❓
**Q: How do I handle API quota limits?**  
A: Implement caching or request limiting in `ytservice.java`

**Q: Why am I getting empty results?**  
A: Verify:
1. API key is valid
2. Video is not age-restricted
3. URL uses supported format

**Q: How to deploy to production?**  
A: Use:
```bash
mvn clean package && java -jar target/yt-scrapper-0.0.1-SNAPSHOT.jar
```

---

## License 📄
Distributed under the MIT License. See `LICENSE` for details.

---

**Created with ❤️ using Spring Boot**  
[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/yourname)
```

---

## Key Files Overview:
1. **`ytConfig.java`**: Stores YouTube API credentials
2. **`ytservice.java`**:
   - `extractVideoId()`: Regex parsing for YouTube URLs
   - `getVideoDetails()`: Fetches metadata using RestTemplate
3. **`ytController.java`**: Handles requests and serves Thymeleaf views
4. **`pom.xml`**: Manages Spring Boot and Thymeleaf dependencies

Add screenshots of your UI in action for better visual demonstration!
