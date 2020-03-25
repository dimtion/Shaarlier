package com.dimtion.shaarlier.helpers;

import android.support.annotation.Nullable;

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
    public void pushLink(Link link) {
    }

    @Override
    public List<String> retrieveTags() throws Exception {
        ArrayList<String> tags = new ArrayList<>();
        tags.add("test-tag1");
        tags.add("test-tag2");
        return tags;
    }

    @Override
    public List<Link> getLinks(@Nullable Integer offset, @Nullable Integer limit) {
        ArrayList<Link> links = new ArrayList<>();
        links.add(
                new Link(
                        "https://en.wikipedia.org/wiki/Thundering_herd_problem",
                        "Thundering herd problem - Wikipedia",
                        "Dummy link",
                        "wikipedia, computers",
                        false,
                        null,
                        false,
                        false,
                        null,
                        null
                )
        );
        return links;
    }
}
