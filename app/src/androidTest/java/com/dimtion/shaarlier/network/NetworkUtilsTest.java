package com.dimtion.shaarlier.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dimtion.shaarlier.network.NetworkUtils;

public class NetworkUtilsTest {

    @org.junit.Test
    public void isUrl() {
        assertTrue(NetworkUtils.isUrl("https://dimtion.fr/"));
        assertTrue(NetworkUtils.isUrl("http://dimtion.fr/"));
        assertTrue(NetworkUtils.isUrl("http://127.0.0.1/"));

        assertFalse(NetworkUtils.isUrl("http://"));
        assertFalse(NetworkUtils.isUrl("two words"));
    }
}
