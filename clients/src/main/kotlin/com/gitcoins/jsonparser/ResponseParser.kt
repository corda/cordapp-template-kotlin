package com.gitcoins.jsonparser

import com.beust.klaxon.Klaxon
import com.beust.klaxon.PathMatcher
import java.io.StringReader
import java.lang.IllegalArgumentException
import java.util.regex.Pattern

/**
 * Utility class for extracting data from the JSON response provided by a GitHub webhook.
 */
class ResponseParser {

    companion object {

        /**
         * Extracts the GitHub username from a JSON response when provided with the path the username should be
         * provided from.
         */
        fun extractGitHubUsername(jsonPath: String, msg: String) : String {
            var username: String?=null
            try {
                val pathMatcher = object: PathMatcher {
                    override fun pathMatches(path: String) = Pattern.matches(jsonPath, path)
                    override fun onMatch(path: String, value: Any) {
                         username = value.toString()
                        }
                    }
                Klaxon().pathMatcher(pathMatcher).parseJsonObject(StringReader(msg))
            } catch (e: IllegalArgumentException){
                e.printStackTrace()
            }
            return username.orEmpty()
        }

        /**
         * Retrieves the body of a comment and checks that it matches the 'createKey' phrase required to initiate the
         * the [com.gitcoins.flows.CreateKeyFlow].
         */
        fun verifyCreateKey(msg: String) : Boolean {
            try {
                val pathMatcher = object : PathMatcher {
                    override fun pathMatches(path: String) = Pattern.matches(".*comment.*body.*", path)
                    override fun onMatch(path: String, value: Any) {
                        if (value.toString() != "createKey")
                            throw IllegalArgumentException("Invalid pr comment. Please comment 'createKey'.")
                    }
                }
                Klaxon().pathMatcher(pathMatcher).parseJsonObject(StringReader(msg))
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                return false
            }
            return true
        }
    }
}