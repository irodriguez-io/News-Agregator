from __future__ import annotations

from pipeline.scoring import compute_metadata
from pipeline.taxonomy import apply_forced_tags, match_topics, matching_text, passes_admission

from conftest import make_article


def _topic_ids(article):
    return [tag["id"] for tag in article["tags"]]


def test_matching_text_uses_nfkc_casefold_and_punctuation_spaces():
    assert matching_text("ＯAuth 2.1 / FIDO-2") == "oauth 2 1 fido 2"


def test_whole_token_ai_matches_agents_but_not_substrings(sources, topics):
    article = make_article(sources["openai_release_notes"], title="AI agents", summary="")
    match_topics(article, topics)
    assert "ai_ml" in article["_organicTopicIds"]
    for title in ("mail systems", "chair design", "said plainly"):
        article = make_article(sources["openai_release_notes"], title=title, summary="")
        match_topics(article, topics)
        assert "ai_ml" not in article["_organicTopicIds"]


def test_whole_phrase_requires_contiguous_tokens(sources, topics):
    article = make_article(sources["openid_specs"], title="OpenID Connect update", summary="")
    match_topics(article, topics)
    assert "oidc" in article["_organicTopicIds"]
    article = make_article(sources["openid_specs"], title="OpenID adds a secure connect mechanism", summary="")
    match_topics(article, topics)
    assert "oidc" not in article["_organicTopicIds"]


def test_case_and_punctuation_matching_are_equivalent(sources, topics):
    evidence = []
    for title in ("OAuth 2.1", "OAUTH 2 1", "oauth 2-1"):
        article = make_article(sources["ietf_oauth"], title=title, summary="")
        evidence.append(match_topics(article, topics)["oauth"])
    assert evidence == [3, 3, 3]


def test_category_scope_prevents_weightlifting_programming_on_technology(sources, topics):
    article = make_article(sources["acm_queue"], title="Programming language design", summary="")
    match_topics(article, topics)
    assert "programming" not in article["_organicTopicIds"]
    assert "programming_languages" in article["_organicTopicIds"]


def test_topic_evidence_once_per_field_and_signal_sum(sources, topics):
    article = make_article(
        sources["cloudflare_blog"],
        title="AI and artificial intelligence software architecture",
        summary="Machine learning supports software architecture.",
    )
    evidence = match_topics(article, topics)
    assert evidence["ai_ml"] == 4
    assert evidence["software_architecture"] == 4
    assert article["_topicSignal"] == 8


def test_title_and_excerpt_evidence_boundaries(sources, topics):
    title_only = make_article(sources["cloudflare_blog"], title="Cybersecurity update", summary="none")
    excerpt_only = make_article(sources["cloudflare_blog"], title="Update", summary="Cybersecurity details")
    both = make_article(sources["cloudflare_blog"], title="Cybersecurity update", summary="Cybersecurity details")
    assert match_topics(title_only, topics)["cybersecurity"] == 3
    assert match_topics(excerpt_only, topics)["cybersecurity"] == 1
    assert match_topics(both, topics)["cybersecurity"] == 4


def test_topic_signal_uses_all_topics_before_five_tag_limit(sources, topics):
    title = "AI cybersecurity networking databases hardware compiler cloud infrastructure devops"
    article = make_article(sources["cloudflare_blog"], title=title, summary="")
    evidence = match_topics(article, topics)
    assert len(evidence) > 5
    assert len(article["tags"]) == 5
    assert article["_topicSignal"] == 10


def test_organic_tag_order_is_evidence_then_label(sources, topics):
    article = make_article(
        sources["cloudflare_blog"],
        title="Cybersecurity and networking",
        summary="Networking details",
    )
    match_topics(article, topics)
    assert _topic_ids(article)[:2] == ["networking", "cybersecurity"]


def test_forced_tags_exactly_apply_without_signal_or_duplicates(sources, topics):
    topics_by_id = {topic["id"]: topic for topic in topics}
    for source_id, forced_id in (
        ("ietf_oauth", "oauth"),
        ("w3c_webauthn", "passkeys_webauthn"),
        ("ietf_scim", "scim"),
    ):
        article = make_article(sources[source_id], title="Working group document update", summary="")
        match_topics(article, topics)
        before = article["_topicSignal"]
        apply_forced_tags(article, sources[source_id], topics_by_id)
        assert forced_id in _topic_ids(article)
        assert article["_topicSignal"] == before == 0
        apply_forced_tags(article, sources[source_id], topics_by_id)
        assert _topic_ids(article).count(forced_id) == 1


def test_forced_tag_already_organic_is_not_duplicated_or_rescored(sources, topics):
    source = sources["ietf_oauth"]
    article = make_article(source, title="OAuth 2.1 update", summary="")
    match_topics(article, topics)
    before = article["_topicSignal"]
    apply_forced_tags(article, source, {topic["id"]: topic for topic in topics})
    assert _topic_ids(article).count("oauth") == 1
    assert article["_topicSignal"] == before == 3


def test_forced_tag_cannot_satisfy_admission(sources, topics):
    source = sources["ietf_oauth"]
    source["minTopicMatches"] = 1
    article = make_article(source, title="Working group update", summary="")
    match_topics(article, topics)
    apply_forced_tags(article, source, {topic["id"]: topic for topic in topics})
    assert not passes_admission(article, source)


def test_openai_admission_passes_technical_and_rejects_product_noise(sources, topics):
    source = sources["openai_release_notes"]
    technical = make_article(source, title="AI model architecture update", summary="")
    unrelated = make_article(source, title="New sidebar colors", summary="Product interface improvements")
    match_topics(technical, topics)
    match_topics(unrelated, topics)
    assert passes_admission(technical, source)
    assert not passes_admission(unrelated, source)


def test_barbell_admission_passes_training_and_rejects_unrelated_medicine(sources, topics):
    source = sources["barbell_medicine"]
    training = make_article(source, title="Training volume for strength", summary="")
    unrelated = make_article(source, title="Managing seasonal allergies", summary="Clinical guidance")
    match_topics(training, topics)
    match_topics(unrelated, topics)
    assert passes_admission(training, source)
    assert not passes_admission(unrelated, source)


def test_entra_admission_uses_only_approved_allowlist(sources, topics):
    source = sources["entra_releases"]
    relevant = make_article(source, title="SCIM provisioning connector update", summary="")
    non_admission_topic = make_article(source, title="API automation release", summary="")
    unrelated = make_article(source, title="Portal color update", summary="")
    for article in (relevant, non_admission_topic, unrelated):
        match_topics(article, topics)
    assert passes_admission(relevant, source)
    assert not passes_admission(non_admission_topic, source)
    assert not passes_admission(unrelated, source)


def test_metadata_is_computed_after_taxonomy_without_affecting_topics(sources, topics):
    article = make_article(sources["quanta"], title="Quantum physics", summary="Short")
    evidence = match_topics(article, topics)
    assert compute_metadata(article) == 4
    assert evidence["physics_quantum"] == 3
