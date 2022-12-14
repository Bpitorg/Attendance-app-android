package com.bpitindia.attendance

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

private const val PROFILE = "profile"
private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"
private const val SHARED_PREFERENCES_ID_KEY = "id_key"


class ProfileFragment : Fragment() {
    private var sharedPreferences: SharedPreferences? = null
    private var profile: String? = null
    private lateinit var jsonObject: JSONObject
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            profile = it.getString(PROFILE)
        }
        jsonObject = JSONObject(profile!!)
        sharedPreferences =
            activity?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.name_profile).setOnClickListener {
            showAlertDialog("Name", view)
        }
        view.findViewById<TextView>(R.id.phone_profile).setOnClickListener {
            showAlertDialog("Phone Number", view)
        }
        setProfile(view)
    }

    private fun setProfile(view: View) {

        val imageView: CircleImageView = view.findViewById(R.id.card_image_profile)
        val imageUrl = jsonObject.getString("image_url")
        if (imageUrl != "null" && imageUrl != "") Glide.with(view).load(imageUrl).into(imageView)
        view.findViewById<TextView>(R.id.card_name_profile).text = jsonObject.getString("name")
        view.findViewById<TextView>(R.id.name_profile).text = jsonObject.getString("name")
        view.findViewById<TextView>(R.id.email_profile).text = jsonObject.getString("email")
        view.findViewById<TextView>(R.id.phone_profile).text = jsonObject.getString("phone_number")
        view.findViewById<TextView>(R.id.card_designation_profile).text =
            jsonObject.getString("designation")
        view.findViewById<TextView>(R.id.designation_profile).text =
            jsonObject.getString("designation")
        view.findViewById<TextView>(R.id.doj_profile).text = jsonObject.getString("date_joined")
    }

    private fun showAlertDialog(field: String, view: View) {
        val edittext = EditText(context)
        var textview: TextView? = null
        when (field) {
            "Name" -> textview = view.findViewById(R.id.name_profile)
            "Phone Number" -> textview = view.findViewById(R.id.phone_profile)
        }
        edittext.setText(textview?.text)
        edittext.maxLines = 1
        if (field == "Name") {
            edittext.filters = arrayOf(InputFilter.LengthFilter(25))
        } else if (field == "Phone Number") {
            edittext.filters = arrayOf(InputFilter.LengthFilter(10))
        }
        val layout = FrameLayout(requireContext())
        layout.setPaddingRelative(45, 25, 45, 0)
        layout.addView(edittext)

        AlertDialog.Builder(requireContext()).apply {
            setTitle("Edit $field")
            setView(layout)
            setPositiveButton("Save") { _, _ ->
                val newValue = edittext.text.toString()
                textview?.text = newValue
                when (field) {
                    "Name" -> jsonObject.put("name", newValue)
                    "Phone Number" -> jsonObject.put("phone_number", newValue)
                }
                view.findViewById<TextView>(R.id.card_name_profile).text =
                    jsonObject.getString("name")
                val token = sharedPreferences?.getString(SHARED_PREFERENCES_TOKEN_KEY, null)
                val id = sharedPreferences?.getInt(SHARED_PREFERENCES_ID_KEY, 0)
                updateProfile(token!!, id!!, view)
            }
            setNegativeButton("Discard") { _, _ -> }

        }.create().show()
    }

    private fun updateProfile(token: String, id: Int, view: View) {
        val url = getString(R.string.profile_api_url, id)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonObject.toString().toRequestBody(mediaType)
            val request =
                Request.Builder().url(url).patch(body).addHeader("Authorization", token).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        Snackbar.make(view, "Some error occurred", Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "profile update failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("debug", "profile update response ${response.message}")
                    if (response.isSuccessful) {
                        Log.d("debug", "profile loading successful")
                        (activity as? MainActivity)?.fetchProfile(token, id)
                    } else {
                        Log.d("debug", "profile update failed ${response.message}")
                        activity?.runOnUiThread {
                            Snackbar.make(view, "Profile Update Failed", Snackbar.LENGTH_SHORT)
                                .show()
                        }
                    }
                    response.body?.close()
                }
            })
        }
    }
}