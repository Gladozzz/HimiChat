package com.jimipurple.himichat.ui.users

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.R
import com.jimipurple.himichat.models.User
import com.jimipurple.himichat.ui.adapters.UsersListAdapter
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import kotlinx.android.synthetic.main.fragment_users.*

class UsersFragment : BaseFragment() {

    private fun usersToStrings(h: List<User>): ArrayList<String> {
        val s: ArrayList<String> = ArrayList<String>()
        h.forEach {
            val gson = Gson()
            val json = gson.toJson(it)
            s.add(json)
        }
        return s
    }

    private fun stringsToUsers(h: List<String>): ArrayList<User> {
        val u: ArrayList<User> = ArrayList<User>()
        h.forEach {
            val gson = Gson()
            val user = gson.fromJson(it, User::class.java)
            u.add(user)
        }
        return u
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_users, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        mAuth = FirebaseAuth.getInstance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUsers()
        nicknameEdit!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val text = nicknameEdit!!.text.toString()
                if (text.isEmpty()) {
                    updateUsers()
                } else {
                    searchUsers(text)
                }
            }
        })
    }

    private fun searchUsers(request: String) {
        (UsersList.adapter as UsersListAdapter).clearItems()
        usersProgressBar.visibility = View.VISIBLE
        fbSource!!.searchUsers(request, { users ->
            usersProgressBar.visibility = View.GONE
            if (users.isNotEmpty()) {
                setUpAdapter(users)
            }
        })
    }

    private fun updateUsers() {
        usersProgressBar.visibility = View.VISIBLE
        fbSource!!.getUsers({ users ->
            usersProgressBar.visibility = View.GONE
            if (users != null) {
                setUpAdapter(users)
            }
        }, getFavorite = true)
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
        if (UsersList != null) {
            val adapter =
                UsersListAdapter(c!!.applicationContext, users, profile, sendMsg)
            UsersList.adapter = adapter
        }
    }
}
