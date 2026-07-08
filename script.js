// Define your categories and their corresponding RSS feed URLs
// Define your categories and their corresponding RSS feed URLs
const feeds = {
    "Science": "https://www.sciencedaily.com/rss/top/science.xml",
    "Technology": "https://feeds.arstechnica.com/arstechnica/index",
    "Literature": "https://lithub.com/feed",
    "History": "https://www.historyextra.com/feed/",
    "Weightlifting": "https://breakingmuscle.com/feed/"
};

const categoryContainer = document.getElementById('categories');
const articleGrid = document.getElementById('article-grid');
const loadingIndicator = document.getElementById('loading');

// Initialize the page
function init() {
    // Create buttons for each category
    Object.keys(feeds).forEach((category, index) => {
        const btn = document.createElement('button');
        btn.textContent = category;
        btn.addEventListener('click', () => loadCategory(category, btn));
        categoryContainer.appendChild(btn);

        // Load the first category by default
        if (index === 0) {
            loadCategory(category, btn);
        }
    });
}

// Fetch and display articles
async function loadCategory(category, activeBtn) {
    // Update active button styling
    document.querySelectorAll('.categories button').forEach(b => b.classList.remove('active'));
    if (activeBtn) activeBtn.classList.add('active');

    // Show loading state
    articleGrid.innerHTML = '';
    loadingIndicator.classList.remove('hidden');

    // We use rss2json API to convert XML RSS feeds into easy-to-use JSON
    const rssUrl = encodeURIComponent(feeds[category]);
    const apiUrl = `https://api.rss2json.com/v1/api.json?rss_url=${rssUrl}`;

    try {
        const response = await fetch(apiUrl);
        const data = await response.json();
        
        loadingIndicator.classList.add('hidden');

        if (data.status === 'ok') {
            displayArticles(data.items, category);
        } else {
            articleGrid.innerHTML = `<p>Error loading feed. Try again later.</p>`;
        }
    } catch (error) {
        loadingIndicator.classList.add('hidden');
        articleGrid.innerHTML = `<p>Network error occurred.</p>`;
        console.error(error);
    }
}

// Render articles to the DOM
// Render articles to the DOM
function displayArticles(articles, currentCategory) {
    // Only show the top 12 articles
    const topArticles = articles.slice(0, 12);

    topArticles.forEach((article, index) => {
        // Clean up descriptions
        const tempDiv = document.createElement("div");
        tempDiv.innerHTML = article.description;
        const cleanText = tempDiv.textContent || tempDiv.innerText || "";
        const shortDesc = cleanText.substring(0, 150) + "...";

        // 1. Start by checking for a standard thumbnail
        let imgUrl = article.thumbnail;

        // 2. If missing, try to scrape an image from the article's HTML content
        if (!imgUrl) {
            const imgSearchDiv = document.createElement("div");
            imgSearchDiv.innerHTML = article.content || article.description;
            const embeddedImg = imgSearchDiv.querySelector("img");
            
            if (embeddedImg) {
                imgUrl = embeddedImg.src;
            }
        }

        // 3. If it STILL can't find an image, fetch a RANDOM image matching the category.
        if (!imgUrl) {
            // Map each category to a good search term for LoremFlickr.
            // (e.g. "Weightlifting" gets better results searching for "gym,fitness")
            const searchTerms = {
                "Science": "science,laboratory",
                "Technology": "technology,computer",
                "Literature": "books,library",
                "History": "history,ancient",
                "Weightlifting": "gym,fitness,barbell"
            };

            const tag = searchTerms[currentCategory] || "newspaper";

            // The `lock` value forces LoremFlickr to serve a DIFFERENT image per article,
            // otherwise every card would collapse to the same cached image.
            imgUrl = `https://loremflickr.com/400/300/${tag}?lock=${index + 1}`;
        }

        // Format the date
        const dateObj = new Date(article.pubDate);
        const formattedDate = dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

        const card = document.createElement('div');
        card.className = 'article-card';
        card.innerHTML = `
            <img src="${imgUrl}" alt="Article thumbnail" class="article-img">
            <div class="article-content">
                <h2><a href="${article.link}" target="_blank">${article.title}</a></h2>
                <div class="article-date">${formattedDate}</div>
                <div class="article-desc">${shortDesc}</div>
                <a href="${article.link}" target="_blank" class="read-more">Read Full Article</a>
            </div>
        `;
        articleGrid.appendChild(card);
    });
}

// Run the initialization
init();