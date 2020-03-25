package com.dimtion.shaarlier.helpers

import com.dimtion.shaarlier.utils.Link

class LinksSource {
    var LINKS: MutableList<Link> = ArrayList()

    /**
     * A map of sample (dummy) items, by ID.
     */
    val LINKS_MAP: MutableMap<Int, Link> = HashMap()

    var COUNT = 0

    private fun addItem(link: Link) {
        LINKS.add(link)
        LINKS_MAP[link.id] = link
        COUNT++
    }

    fun newLink(url: String, title: String, description: String, tags: String, isPrivate: Boolean) {
        val link = Link(
                url,
                title,
                description,
                tags,
                isPrivate,
                null,
                false,
                false,
                "",
                null
        )
        link.id = LINKS.size
        addItem(link)
    }

    fun setLinks(links: List<Link>) {
        LINKS.clear()
        COUNT = 0
        for (link in links) {
            addItem(link)
        }
    }
}