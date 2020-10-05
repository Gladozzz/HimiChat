package com.jimipurple.himichat.ui.friends

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.jimipurple.himichat.*
import com.jimipurple.himichat.models.User
import com.jimipurple.himichat.ui.adapters.FriendsListAdapter
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import com.squareup.picasso.LruCache
import kotlinx.android.synthetic.main.fragment_find_friend.*
import java.util.regex.Pattern


class FindFriendFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_friend, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nicknameEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val text = nicknameEdit!!.text.toString()
                noUserMessage.visibility = View.GONE
                if (text.isNotEmpty()) {
                    searchFriends(text)
                } else {
                    (FriendsList.adapter as FriendsListAdapter?)?.clearItems()
                    progressBar.visibility = View.GONE
                }
            }
        })
    }

    private fun searchFriends(request: String) {
        (FriendsList.adapter as FriendsListAdapter?)?.clearItems()
        progressBar.visibility = View.VISIBLE
        fbSource!!.searchUsers(request, { users ->
            progressBar.visibility = View.GONE
            if (users.isNotEmpty()) {
                noUserMessage.visibility = View.GONE
                setUpAdapter(users)
            } else {
                noUserMessage.visibility = View.VISIBLE
            }
        })
    }

    private fun setUpAdapter(users: List<User>) {
        val profile = { u: User ->
            Unit
            val b = Bundle()
            b.putString("profile_id", u.id)
            val navController = findNavController()
            navController.navigate(R.id.nav_profile, b, navOptions)
        }
        val sendMsg = { u: User ->
            Unit
            val b = Bundle()
            b.putString("friend_id", u.id)
            b.putString("nickname", u.nickname)
            b.putString("avatar", u.avatar)
            val navController = findNavController()
            navController.navigate(R.id.nav_dialog, b, navOptions)
        }
        if (FriendsList != null) {
            val adapter =
                FriendsListAdapter(c!!.applicationContext, users, profile, sendMsg)
            FriendsList.adapter = adapter
        }
    }
}
