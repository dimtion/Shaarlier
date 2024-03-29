package com.dimtion.shaarlier.network;

import com.dimtion.shaarlier.models.Link;

import java.io.IOException;
import java.util.List;

public interface NetworkManager {
    /**
     * returns whether the distant Shaarli server is compatible with this network manager
     *
     * @return true if compatible, false otherwise
     * @throws IOException
     */
    boolean isCompatibleShaarli() throws IOException;

    /**
     * Check if the provided credentials are valid
     * Can set some state in the NetworkManager (like a cookie if necessary)
     *
     * @return true if the credentials are correct, false otherwise
     * @throws IOException
     */
    boolean login() throws IOException;

    Link prefetchLinkData(Link link) throws IOException;

    void pushLink(Link link) throws IOException;

    List<String> retrieveTags() throws Exception;
}
