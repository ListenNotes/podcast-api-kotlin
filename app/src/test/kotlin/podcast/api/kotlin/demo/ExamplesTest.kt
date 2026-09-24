package podcast.api.kotlin.demo

import com.listennotes.podcast_api.Client
import com.listennotes.podcast_api.exception.PermissionDeniedException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import podcast.api.testing.Support

class ExamplesTest {
    @TestFactory
    fun allMethods() = Support.operations().map { op ->
        DynamicTest.dynamicTest(op.getString("func")) {
            Support().use { server ->
                val client = Client("kotlin-test", server.baseUrl())
                val parameters = Support.parameters(op)
                val before = HashMap(parameters)
                val response = GeneratedExamples.call(client, op.getString("operationId"), parameters)
                assertTrue(response.toJSON().getBoolean("ok"))
                assertEquals(200, response.getStatusCode())
                assertEquals(12, response.getUsage())
                assertEquals(300, response.getFreeQuota())
                val request = server.take()
                assertEquals("kotlin-test", request.key())
                Support.verify(op, parameters, request)
                assertEquals(before, parameters)
            }
        }
    }

    @Test
    fun nestedPathsEmptyFieldsAndClientIsolation() {
        Support().use { first -> Support().use { second ->
            val one = Client("first", first.baseUrl())
            val two = Client("second", second.baseUrl())
            val parameters = mapOf("id" to "a/b ?#é", "item_id" to "23", "notes" to "")
            one.updatePlaylistItemNotes(parameters)
            val request = first.take()
            assertEquals("/api/v2/playlists/a%2Fb%20%3F%23%C3%A9/items/23", request.uri().rawPath)
            assertEquals(mapOf("notes" to ""), Support.decode(request.body()))
            assertEquals("first", request.key())
            two.updatePlaylist(mapOf("id" to "abc", "description" to ""))
            val other = second.take()
            assertEquals("second", other.key())
            assertEquals(mapOf("description" to ""), Support.decode(other.body()))
            one.search(mapOf("q" to "café + & /"))
            assertEquals(mapOf("q" to "café + & /"), Support.decode(first.take().uri().rawQuery))
            first.status = 403
            val error = assertThrows(PermissionDeniedException::class.java) { one.search(emptyMap()) }
            assertEquals(403, error.getResponse().getStatusCode())
            assertTrue(error.getResponse().toJSON().getBoolean("ok"))
        } }
    }
}

@Tag("integration")
class MockIntegrationTest {
    // Never read credentials or a destination from the environment.
    @TestFactory
    fun allMethods() = Support.operations().map { op ->
        DynamicTest.dynamicTest(op.getString("func")) {
            val response = GeneratedExamples.call(Client(), op.getString("operationId"), Support.parameters(op))
            assertTrue(response.getStatusCode() in listOf(200, 201))
            assertFalse(response.toJSON().isEmpty)
        }
    }

    @Test
    fun clearFieldsAndAddPodcast() {
        val client = Client()
        assertFalse(client.updatePlaylistItemNotes(mapOf("id" to "m1pe7z60bsw", "item_id" to "23", "notes" to "")).toJSON().isEmpty)
        assertFalse(client.updatePlaylist(mapOf("id" to "m1pe7z60bsw", "description" to "")).toJSON().isEmpty)
        assertFalse(client.addPlaylistItem(mapOf("id" to "m1pe7z60bsw", "podcast_id" to "4d3fe717742d4963a85562e9f84d8c79")).toJSON().isEmpty)
    }
}
