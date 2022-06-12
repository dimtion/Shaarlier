package com.dimtion.shaarlier.network;

import com.dimtion.shaarlier.models.Link;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dummy NetworkManager used for debugging purposes
 */
public class MockNetworkManager implements NetworkManager {
    @Override
    public boolean isCompatibleShaarli() throws IOException {
        return true;
    }

    @Override
    public boolean login() throws IOException {
        return true;
    }

    @Override
    public Link prefetchLinkData(Link link) throws IOException {
        return link;
    }

    @Override
    public void pushLink(Link link) {
    }

    @Override
    public List<String> retrieveTags() throws Exception {
        ArrayList<String> tags = new ArrayList<>();
        tags.add("test-tag1");
        tags.add("test-tag2");
        return tags;
    }
}
