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
class WebHookControllerTests {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Test
    fun `No endpoint`() {
        val result = testRestTemplate.postForEntity("/blah", HttpEntity<String>(""), String::class.java)
        assertEquals(result.statusCode, HttpStatus.NOT_FOUND)
    }

    @Test
    fun `Push event end point`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity<String>("{}", headers)
//        val entity = """""{ "pusher": { "name": "badger" } }"""""

        val result = testRestTemplate.postForEntity("/api/git/push-event", entity, String::class.java)
        assertNotNull(result)
        assertEquals(result.statusCode, HttpStatus.OK)
        assertEquals(result.body, "New push event on the repo.")
    }

    @Test
    fun `Pull request event end point`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity<String>("{}", headers)

        val result = testRestTemplate.postForEntity("/api/git/pr-event", entity, String::class.java)
        assertNotNull(result)
        assertEquals(result.statusCode, HttpStatus.OK)
        assertEquals(result.body, "New PR event on the repo..")
    }
}