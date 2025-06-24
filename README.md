```markdown
# YouTube Analytics Pro 🚀

A powerful Spring Boot application for extracting video metadata and discovering trending content using YouTube Data API v3.

---

## Features ✨

### Video Analysis
- **Metadata Extraction**: Title, description, tags, thumbnail
- **One-Click Copy**: Easily copy any content to clipboard
- **Responsive UI**: Beautiful dark-themed interface

### Video Discovery
- **Keyword Search**: Find videos by keywords
- **Advanced Filters**: Sort by views, relevance, date, or rating
- **Results Customization**: Choose number of results (5-25)

### Enhanced UI
- **Animated Elements**: Floating UI components
- **Responsive Design**: Works on all devices
- **Visual Feedback**: Interactive hover effects

---

## Tech Stack 💻

- **Backend**: 
  - Java 17, Spring Boot 3.4.5
  - YouTube Data API v3, RestTemplate
  - Lombok, Jackson
  
- **Frontend**:
  - Thymeleaf templates
  - Tailwind CSS for styling
  - Font Awesome icons
  - Custom animations and gradients

---

## Setup & Installation 🛠️

### Prerequisites
- Java 17+
- YouTube Data API key
- Maven

### Configuration
1. Get YouTube API key from [Google Cloud Console](https://console.cloud.google.com/)
2. Update `application.properties`:
```properties
youtube.api.key=YOUR_API_KEY
youtube.api.url=https://www.googleapis.com/youtube/v3/videos
youtube.api.search.url=https://www.googleapis.com/youtube/v3/search
youtube.max.results=50
```

### Run Application
```bash
mvn spring-boot:run
```
Visit `http://localhost:8080`

---

## Usage Guide 🖥️

### Video Analysis
1. Paste YouTube URL in the analyzer card
2. View detailed metadata including:
   - Title & Description
   - High-quality thumbnail
   - Tags list
   - Copy buttons for all content

### Video Discovery
1. Enter search keywords
2. Select number of results (5-25)
3. Choose sort method:
   - Most Viewed
   - Most Relevant
   - Most Recent
   - Highest Rated
4. Browse results with thumbnails
5. Click "Analyze" on any result for detailed view

---

## Project Structure 📂

```
src/
├── main/
│   ├── java/com/example/yt_scrapper/
│   │   ├── Config/ytConfig.java         # API configuration
│   │   ├── Controller/ytController.java # Request handlers
│   │   ├── Service/ytservice.java       # Business logic
│   │   └── YtScrapperApplication.java   # Entry point
│   ├── resources/
│   │   ├── static/                      # CSS/JS assets
│   │   ├── templates/                   # Thymeleaf views
│   │   │   ├── index.html               # Main page
│   │   │   ├── details.html             # Video analysis
│   │   │   ├── search-results.html      # Search results
│   │   │   └── error.html               # Error page
│   │   └── application.properties       # Configuration
```

---

## API Reference 🔌

### Video Details Endpoint
`GET https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics&id={videoId}&key={apiKey}`

### Search Endpoint
`GET https://www.googleapis.com/youtube/v3/search?part=snippet&q={query}&maxResults={count}&type=video&order={sort}&key={apiKey}`

---

## FAQ ❓

**Q: How do I handle API quota limits?**  
A: Implement caching in `ytservice.java` or upgrade your API quota

**Q: Why am I getting empty search results?**  
A: Verify:
1. API key has search permissions
2. Keywords aren't too specific
3. Videos exist for your query

**Q: How to deploy to production?**  
A: Use:
```bash
mvn clean package && java -jar target/yt-scrapper-0.0.1-SNAPSHOT.jar
```

---

## License 📄
MIT License - see [LICENSE](LICENSE) for details.

---

**Created with ❤️ for content creators**  
[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/yourname)
```

Key improvements:
1. Added new sections for video discovery features
2. Updated screenshots recommendation (you should add actual screenshots)
3. Enhanced feature list with UI improvements
4. Added search API documentation
5. Improved project structure documentation
6. Updated FAQ with search-specific questions
7. Better organized usage guide
