package com.gitcoins.webserver

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.junit.runner.RunWith
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.*
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GitWebHookControllerTests {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Test
    fun `No endpoint`() {
        val result = testRestTemplate.postForEntity("/blah", HttpEntity<String>(""), String::class.java)
        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `PR comment invalid`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity<String>("""{ "comment": { "user": { "login": "testUser" }, "body": "Do nothing" } }""")
        val result = testRestTemplate.postForEntity("/api/git/create-key", entity, String::class.java)
        assertNotNull(result)
        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertEquals("Invalid pr comment. Please comment 'createKey'.", result.body.trim())
    }

    @Test
    fun `PR comment createKey`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity<String>("""{ "comment": { "user": { "login": "testUser5" }, "body": "createKey" } }""")
        val result = testRestTemplate.postForEntity("/api/git/create-key", entity, String::class.java)
        assertNotNull(result)
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals("New public key generated for github user: testUser5", result.body.trim())
    }


    @Test
    fun `Push event end point`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity<String>("""{"pusher": { "name": "testUser" } }""", headers)

        val result = testRestTemplate.postForEntity("/api/git/push-event", entity, String::class.java)
        assertNotNull(result)
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals("New push event on the repo by: testUser", result.body)
    }

    @Test
    fun `Pull request event end point`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity<String>("""{ "review": { "user": { "login": "testUser" } } }""", headers)

        val result = testRestTemplate.postForEntity("/api/git/pr-event", entity, String::class.java)
        assertNotNull(result)
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals("New pull request review event on the repo by: testUser", result.body.trim())
    }
}
