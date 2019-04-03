package com.gitcoins.jsonparser

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.springframework.test.util.AssertionErrors.assertTrue

class ResponseParserTests {

    @Test
    fun `extract github username from comment`() {
        val msg = """{ "comment": { "user": { "login": "testUser" }, "body": "createKey" } }"""
        val result = ResponseParser.extractGitHubUsername(".*comment.*user.*login.*", msg)
        assertEquals("testUser", result)
    }

    @Test
    fun `extract github username from push`() {
        val msg = """{"pusher": { "name": "testUser" } }"""
        val result = ResponseParser.extractGitHubUsername(".*pusher.*name.*", msg)
        assertEquals("testUser", result)
    }

    @Test
    fun `extract github username from pull request review`() {
        val msg = """{ "review": { "user": { "login": "testUser" } } }"""
        val result = ResponseParser.extractGitHubUsername(".*review.*user.*login.*", msg)
        assertEquals("testUser", result)
    }

    @Test
    fun `verify createKey`() {
        val notValid = """{ "comment": { "user": { "login": "testUser" }, "body": "hello" } }"""
        val resultFalse =  ResponseParser.verifyCreateKey(notValid)
        assertFalse(resultFalse)

        val msg = """{ "comment": { "user": { "login": "testUser" }, "body": "createKey" } }"""
        val valid =  ResponseParser.verifyCreateKey(msg)
        assertTrue("", valid)
    }
}
