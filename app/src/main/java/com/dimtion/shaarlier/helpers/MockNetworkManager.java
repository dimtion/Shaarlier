package com.dimtion.shaarlier.helpers;

import com.dimtion.shaarlier.utils.Link;

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
    public void postLink(String sharedUrl, String sharedTitle, String sharedDescription, String sharedTags, boolean privateShare, boolean tweet, boolean toot) throws IOException {

    }

    @Override
    public List<String> retrieveTags() throws Exception {
        ArrayList<String> tags = new ArrayList<>();
        tags.add("test-tag1");
        tags.add("test-tag2");
        return tags;
    }
}
