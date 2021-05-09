/*
 * A demo app to use Listen Notes Podcast API:
 *     https://www.listennotes.com/api/
 */
package podcast.api.kotlin.demo

import com.listennotes.podcast_api.Client
import com.listennotes.podcast_api.exception.*

fun main() {
    try {
        // If apiKey is not set or an empty string, then we'll connect
        // to the api mock server, which returns fake data for testing
        val apiKey: String = System.getenv("LISTEN_API_KEY") ?: ""
        val client = Client(apiKey)

        // Parameters are passed via this HashMap
        val parameters = HashMap<String, String> ()
        parameters.put("q", "mars")
        parameters.put("type", "podcast")

        // response.toJSON() returns an org.json.JSONObject
        val response = client.search(parameters)
        println(response.toJSON().toString(2))

        println("\n=== Some stats of your account ===\n")
        println("Free Quota this month: " + response.getFreeQuota() + " requests")
        println("Usage this month: " + response.getUsage() + " requests")        
    } catch (e: AuthenticationException) {
        println(e)
    } catch (e: ListenApiException) {
        println(e)
    }    
}
