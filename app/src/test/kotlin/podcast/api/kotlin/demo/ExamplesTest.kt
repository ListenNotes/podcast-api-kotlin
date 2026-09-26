package podcast.api.kotlin.demo

import com.listennotes.podcast_api.Client
import com.listennotes.podcast_api.exception.InvalidRequestException
import com.listennotes.podcast_api.exception.NotFoundException
import com.listennotes.podcast_api.exception.PermissionDeniedException
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.Assertions.*
import podcast.api.testing.Support

class ExamplesTest {
    @TestFactory
    fun allMethods() = Support.operations().also { assertEquals(31, it.size) }.map { op ->
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
    fun deletePlaylistResponseEncodingAndErrors() {
        Support().use { server ->
            val client = Client("kotlin-test", server.baseUrl())
            val parameters = mapOf("id" to "list/+ ?#é")
            server.responseBody = """{"id":"list/+ ?#é","deleted":true}"""
            val response = GeneratedExamples.call(client, "deletePlaylist", parameters)
            assertEquals(200, response.getStatusCode())
            assertTrue(response.toJSON().getBoolean("deleted"))
            assertEquals(parameters["id"], response.toJSON().getString("id"))
            assertEquals(12, response.getUsage())
            val request = server.take()
            assertEquals("DELETE", request.method())
            assertEquals("/api/v2/playlists/list%2F%2B%20%3F%23%C3%A9", request.uri().rawPath)
            assertNull(request.uri().rawQuery)
            assertEquals("", request.body())
            assertEquals("kotlin-test", request.key())
            assertEquals(mapOf("id" to "list/+ ?#é"), parameters)
            for (invalid in listOf(emptyMap(), mapOf("id" to ""), mapOf("id" to " "))) {
                assertThrows(InvalidRequestException::class.java) {
                    GeneratedExamples.call(client, "deletePlaylist", invalid)
                }
            }
            server.status = 404
            server.responseBody = """{"error":"Playlist not found"}"""
            val error = assertThrows(NotFoundException::class.java) { GeneratedExamples.deletePlaylist(client) }
            assertEquals(404, error.getStatusCode())
            assertEquals("Playlist not found", error.getResponse().toJSON().getString("error"))
            assertEquals(12, error.getResponse().getUsage())
            val exampleRequest = server.take()
            assertEquals("DELETE", exampleRequest.method())
            assertEquals("/api/v2/playlists/m1pe7z60bsw", exampleRequest.uri().rawPath)
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
            if (op.getString("operationId") == "deletePlaylist") {
                assertEquals(200, response.getStatusCode())
                assertTrue(response.toJSON().getBoolean("deleted"))
                assertEquals(Support.parameters(op)["id"], response.toJSON().getString("id"))
            }
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
