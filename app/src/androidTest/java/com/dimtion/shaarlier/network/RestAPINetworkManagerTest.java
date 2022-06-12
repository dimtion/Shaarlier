package com.dimtion.shaarlier.network;

import com.dimtion.shaarlier.models.ShaarliAccount;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class RestAPINetworkManagerTest {
    private ShaarliAccount mAccount;
    private RestAPINetworkManager mNetworkManager;

    @org.junit.Before
    public void setUp() throws Exception {
        this.mAccount = new ShaarliAccount();
        this.mAccount.setRestAPIKey("azerty");
        this.mNetworkManager = new RestAPINetworkManager(this.mAccount);
    }

    @org.junit.Test
    public void getJwt() throws IOException {
        assertNotNull(this.mAccount);
        String jwt = this.mNetworkManager.getJwt();
        assertNotNull(jwt);
    }
}