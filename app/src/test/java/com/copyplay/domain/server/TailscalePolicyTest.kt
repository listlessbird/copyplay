package com.copyplay.domain.server

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TailscalePolicyTest {
    @Test
    fun `resolve maps installation and address state`() {
        assertEquals(
            TailscaleStatus.NotInstalled,
            TailscalePolicy.resolve(isInstalled = false, hasTailnetAddress = false),
        )
        assertEquals(
            TailscaleStatus.Disconnected,
            TailscalePolicy.resolve(isInstalled = true, hasTailnetAddress = false),
        )
        assertEquals(
            TailscaleStatus.Connected(emptyList()),
            TailscalePolicy.resolve(isInstalled = true, hasTailnetAddress = true),
        )
        assertEquals(
            TailscaleStatus.NotInstalled,
            TailscalePolicy.resolve(isInstalled = false, hasTailnetAddress = true),
        )
    }

    @Test
    fun `tailnet range boundaries are detected`() {
        assertTrue(TailscalePolicy.isTailnetAddress(InetAddress.getByName("100.64.0.1")))
        assertTrue(TailscalePolicy.isTailnetAddress(InetAddress.getByName("100.101.102.103")))
        assertTrue(TailscalePolicy.isTailnetAddress(InetAddress.getByName("100.127.255.255")))
        assertTrue(TailscalePolicy.isTailnetAddress(InetAddress.getByName("fd7a:115c:a1e0::1")))
    }

    @Test
    fun `addresses outside the tailnet range are rejected`() {
        assertFalse(TailscalePolicy.isTailnetAddress(InetAddress.getByName("100.63.255.255")))
        assertFalse(TailscalePolicy.isTailnetAddress(InetAddress.getByName("100.128.0.0")))
        assertFalse(TailscalePolicy.isTailnetAddress(InetAddress.getByName("8.8.8.8")))
        assertFalse(TailscalePolicy.isTailnetAddress(InetAddress.getByName("::1")))
    }
}
