package io.irodriguez.intentionalreading.data

internal object DatasetTestFixtures {
    fun validDatasetBytes(
        schemaVersion: Int = 1,
        articleId: String = "00000000000000000001",
        title: String = "A readable article title",
    ): ByteArray =
        """
        {
          "schemaVersion": $schemaVersion,
          "generatedAt": "2026-08-24T12:00:00Z",
          "pipeline": {
            "enabledSourceCount": 1,
            "successfulSourceCount": 1,
            "failedSourceCount": 0,
            "articleCount": 1
          },
          "articles": [
            {
              "id": "$articleId",
              "title": "$title",
              "url": "https://example.com/articles/$articleId",
              "source": {"id": "ietf_oauth", "name": "IETF OAuth WG"},
              "category": "iam",
              "publishedAt": "2026-08-24T11:00:00Z",
              "author": null,
              "excerpt": "A plain-text excerpt.",
              "readingTimeMinutes": null,
              "tags": [{"id": "oauth", "label": "OAuth"}],
              "contentType": {"id": "standards_update", "label": "Standards Update"},
              "score": {
                "base": 91,
                "sourceQuality": 50,
                "contentType": 20,
                "freshness": 15,
                "topicSignal": 5,
                "metadata": 1
              }
            }
          ]
        }
        """.trimIndent().encodeToByteArray()
}
