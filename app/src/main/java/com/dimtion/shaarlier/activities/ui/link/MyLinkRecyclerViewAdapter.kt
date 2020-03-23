package com.dimtion.shaarlier.activities.ui.link

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dimtion.shaarlier.R


import com.dimtion.shaarlier.activities.ui.link.LinkFragment.OnListFragmentInteractionListener
import com.dimtion.shaarlier.utils.Link

import kotlinx.android.synthetic.main.fragment_link.view.*

/**
 * [RecyclerView.Adapter] that can display a [Link] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class MyLinkRecyclerViewAdapter(
        private val mValues: List<Link>,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<MyLinkRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Link
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_link, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val link = mValues[position]
        holder.mTitleView.text = link.title
        holder.mContentView.text = link.description
        holder.mUrlView.text = link.url
        holder.mTagsView.text = link.tags

        with(holder.mView) {
            tag = link
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mTitleView: TextView = mView.title
        val mContentView: TextView = mView.content
        val mUrlView: TextView = mView.url
        val mTagsView: TextView = mView.tags

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
