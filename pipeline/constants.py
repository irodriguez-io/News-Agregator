"""Frozen V1 identifiers and bounded operational constants."""

CATEGORIES = {
    "science",
    "technology",
    "literature",
    "history",
    "weightlifting",
    "iam",
    "identity_automation",
}
ADAPTERS = {"rss", "atom", "rss_autodiscovery", "html_listing"}
CONTENT_TYPES = {
    "standards_update": ("Standards Update", 20),
    "official_release_notes": ("Official Release Notes", 19),
    "research_reporting": ("Research & Science", 19),
    "reported_science": ("Reported Science", 18),
    "engineering_deep_dive": ("Engineering Deep Dive", 18),
    "evidence_based_training": ("Evidence-Based Training", 17),
    "historical_essay": ("Historical Essay", 16),
    "engineering_journalism": ("Engineering Journalism", 16),
    "literary_essay": ("Literary Essay", 15),
    "reported_journalism": ("Reported Journalism", 14),
}
APPROVED_SOURCE_IDS = {
    "quanta", "science_aaas", "acm_queue", "ieee_spectrum", "ars_features",
    "cloudflare_blog", "anthropic_engineering", "openai_release_notes",
    "paris_review", "public_books", "jstor_daily", "public_domain_review",
    "stronger_by_science", "barbell_medicine", "ietf_oauth", "openid_specs",
    "w3c_webauthn", "okta_identity_engine", "okta_workflows",
    "n8n_release_notes", "entra_releases", "ietf_scim",
}
HTML_SOURCE_IDS = {"anthropic_engineering", "barbell_medicine", "okta_workflows"}
FILTERED_SOURCE_IDS = {"openai_release_notes", "barbell_medicine", "entra_releases"}
FORCED_TAGS = {
    "ietf_oauth": ["oauth"],
    "w3c_webauthn": ["passkeys_webauthn"],
    "ietf_scim": ["scim"],
}
USER_AGENT = "IntentionalReading/1.0 (+https://irodriguez.io/News-Agregator/)"
CONNECT_TIMEOUT_SECONDS = 10
READ_TIMEOUT_SECONDS = 20
MAX_REDIRECTS = 5
MAX_RESPONSE_BYTES = 10 * 1024 * 1024
RETRY_DELAY_SECONDS = 2
TRANSIENT_STATUS_CODES = {408, 429, 500, 502, 503, 504}
SOURCE_CATALOG_SHA256 = "bfde3fe6a7704d7e1b72d28691530f3b901e8acae73b7d3de150cd1c73142a45"
TOPIC_TAXONOMY_SHA256 = "3d09f64ae47eb205361466d3b3f1cb8df1d10f7f077054aa41c8f8fb291d284a"
TITLE_MAX_CHARS = 500
AUTHOR_MAX_CHARS = 200
EXCERPT_MAX_CHARS = 800
MIN_READING_TIME_WORDS = 400
READING_WORDS_PER_MINUTE = 225
