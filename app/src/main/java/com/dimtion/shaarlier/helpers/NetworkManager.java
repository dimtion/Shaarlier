package com.dimtion.shaarlier.helpers;

import com.dimtion.shaarlier.utils.Link;

import java.io.IOException;
import java.util.List;

public interface NetworkManager {
    // String[] static loadTitleAndDescription(@NonNull String url);

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
     * @return
     * @throws IOException
     */
    boolean login() throws IOException;

    Link prefetchLinkData(Link link) throws IOException;

    void postLink(
            String sharedUrl,
            String sharedTitle,
            String sharedDescription,
            String sharedTags,
            boolean privateShare,
            boolean tweet,
            boolean toot
    ) throws IOException;

    List<String> retrieveTags() throws Exception;
}
