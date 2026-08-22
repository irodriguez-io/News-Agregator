package io.irodriguez.intentionalreading.data

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
import io.irodriguez.intentionalreading.domain.validation.DatasetValidator
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SampleDatasetTest {
    @Test
    fun `the shipped UTF-8 asset validates and follows catalog and ordering contracts`() {
        val bytes = javaClass.getResourceAsStream("/sample_articles.json")?.use { it.readBytes() }
            ?: fail("sample_articles.json was not placed on the test classpath")
        val decoded = bytes.toString(Charsets.UTF_8)
        assertContentEquals(bytes, decoded.toByteArray(Charsets.UTF_8), "asset must be valid UTF-8")

        val dataset = when (val result = DatasetValidator().validate(bytes)) {
            is DatasetResult.Success -> result.dataset
            is DatasetResult.Failure -> fail("shipped asset failed ${result.code}: ${result.message}")
        }

        dataset.articles.zipWithNext().forEachIndexed { index, (left, right) ->
            assertTrue(
                canonicalOrder.compare(left, right) <= 0,
                "articles ${index} and ${index + 1} violate canonical order",
            )
        }
        dataset.articles.forEachIndexed { index, article ->
            assertTrue(article.source.id in approvedSourceIds, "articles[$index].source.id is not approved")
            article.tags.forEachIndexed { tagIndex, tag ->
                assertTrue(tag.id in approvedTopicIds, "articles[$index].tags[$tagIndex].id is unknown")
            }
        }
    }

    private companion object {
        private val canonicalOrder = Comparator<Article> { left, right ->
            right.score.base.compareTo(left.score.base)
                .takeIf { it != 0 }
                ?: comparePublishedDescending(left.publishedAt, right.publishedAt)
                    .takeIf { it != 0 }
                ?: left.source.id.compareTo(right.source.id)
                    .takeIf { it != 0 }
                ?: left.id.compareTo(right.id)
        }

        private fun comparePublishedDescending(left: Instant?, right: Instant?): Int = when {
            left == null && right == null -> 0
            left == null -> 1
            right == null -> -1
            else -> right.compareTo(left)
        }

        private val approvedSourceIds = setOf(
            "quanta", "science_aaas", "acm_queue", "ieee_spectrum", "ars_features",
            "cloudflare_blog", "anthropic_engineering", "paris_review", "public_books", "jstor_daily",
            "public_domain_review", "stronger_by_science", "barbell_medicine", "ietf_oauth",
            "openid_specs", "w3c_webauthn", "okta_identity_engine", "n8n_release_notes",
            "entra_releases", "ietf_scim",
        )

        private val approvedTopicIds = setOf(
            "physics_quantum", "space_cosmology", "mathematics", "biology_evolution", "neuroscience",
            "earth_climate", "scientific_method", "software_architecture", "distributed_systems",
            "cloud_infrastructure", "networking", "cybersecurity", "ai_ml", "programming_languages",
            "data_systems", "devops_sre", "hardware", "fiction", "poetry", "literary_criticism",
            "writing_craft", "author_interviews", "translation", "literary_history", "ancient_history",
            "medieval_history", "early_modern_history", "modern_history", "archaeology",
            "social_cultural_history", "science_technology_history", "economic_history",
            "military_history", "primary_sources", "strength", "hypertrophy", "programming",
            "technique", "powerlifting", "olympic_weightlifting", "recovery", "nutrition",
            "injury_rehab", "conditioning", "research_methods", "authentication", "authorization",
            "oauth", "oidc", "saml", "passkeys_webauthn", "mfa", "federation",
            "identity_proofing", "identity_governance", "privileged_access", "identity_security",
            "workload_identity", "agent_identity", "tokens_jwt", "provisioning", "scim",
            "lifecycle_jml", "workflow_orchestration", "api_automation", "connectors_integrations",
            "event_driven", "access_requests", "data_transformation", "observability_audit",
            "identity_sources", "agent_automation",
        )
    }
}
