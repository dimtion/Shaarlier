package com.dimtion.shaarlier.activities.ui.link

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dimtion.shaarlier.R
import com.dimtion.shaarlier.activities.MainActivity
import com.dimtion.shaarlier.helpers.AccountsSource

import com.dimtion.shaarlier.helpers.LinksSource
import com.dimtion.shaarlier.services.NetworkService
import com.dimtion.shaarlier.utils.Link
import com.dimtion.shaarlier.utils.ShaarliAccount

/**
 * A fragment representing a list of Links.
 * Activities containing this fragment MUST implement the
 * [LinkFragment.OnListFragmentInteractionListener] interface.
 */
class LinkFragment : Fragment() {

    // TODO: Customize parameters
    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null

    private lateinit var mShaarliAccount: ShaarliAccount
    private lateinit var mLinksSource: LinksSource
    private lateinit var mRefreshLayout: SwipeRefreshLayout
    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }

        val accountsSource = AccountsSource(this.context)
        val defaultAccount = accountsSource.defaultAccount
        if (defaultAccount != null) {
            mShaarliAccount = accountsSource.defaultAccount
        } else {
            val intent = Intent(this.context, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mRefreshLayout = inflater.inflate(R.layout.fragment_link_list, container, false) as SwipeRefreshLayout
        mRecyclerView = mRefreshLayout.findViewById(R.id.list)

        mLinksSource = LinksSource()
        // Set the adapter
        with(mRecyclerView) {
            layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
            adapter = MyLinkRecyclerViewAdapter(mLinksSource.LINKS, listener)
        }

        // Set the refresh callback
        mRefreshLayout.setOnRefreshListener {
            Log.i("LinkFragment", "onRefresh called from SwipeRefreshLayout")
            updateLinks()
        }

        updateLinks()
        return mRefreshLayout
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: Link?)
    }

    companion object {
        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
                LinkFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }

    private fun updateLinks() {
        if (!::mShaarliAccount.isInitialized) {
            // If the account is not init, we go to the settings
            // TODO: probably print a toast
            val intent = Intent(this.context, MainActivity::class.java)
            startActivity(intent)
        }
        mRefreshLayout.isRefreshing = true
        val accountsSource = AccountsSource(this.context)
        val intent = Intent(this.context, NetworkService::class.java)
        intent.putExtra("action", NetworkService.INTENT_GET_LINKS)
        intent.putExtra("account", mShaarliAccount)
        intent.putExtra(NetworkService.EXTRA_MESSENGER, Messenger(NetworkHandler(this)))
        this.context?.startService(intent)
    }

    private class NetworkHandler(parent: LinkFragment) : Handler() {
        val mParent = parent
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)

            Log.d("LinkFragment", msg.toString())
            when (msg?.arg1) {
                NetworkService.GET_LINKS -> {
                    Log.d("LinkFragment", msg.toString())
                    mParent.mLinksSource.setLinks(msg.obj as List<Link>)
                }
                else -> Log.e("LinkFrament", "NetworkServiceError: ${msg?.arg1}")
            }

            mParent.mRefreshLayout.isRefreshing = false
            mParent.mRecyclerView.adapter?.notifyDataSetChanged()
            Log.d("LinkFragment", "LINKS")
            Log.d("LinkFragment", mParent.mLinksSource.LINKS.toString())
        }
    }
}
