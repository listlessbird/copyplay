package com.copyplay.network.copyparty

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CopypartyServerIdentityTest {
    @Test
    fun `server info label uses the leading host name`() {
        assertEquals(
            "riltop",
            "riltop</span> // <span>741 GiB free of 898 GiB".toCopypartyServerDisplayName(),
        )
    }

    @Test
    fun `blank server info has no label`() {
        assertNull("  ".toCopypartyServerDisplayName())
    }
}
