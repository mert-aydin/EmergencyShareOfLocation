package com.mertaydin.emergencyshareoflocation.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mertaydin.emergencyshareoflocation.R
import com.mertaydin.emergencyshareoflocation.databinding.ActivityMainBinding
import com.mertaydin.emergencyshareoflocation.models.Contact
import com.mertaydin.emergencyshareoflocation.utils.Constants.CONTACTS_DATA_STORE_NAME
import com.mertaydin.emergencyshareoflocation.utils.Constants.CONTACTS_PREFERENCES_KEY
import com.mertaydin.emergencyshareoflocation.utils.Constants.READ_CONTACTS_PERMISSION_REQUEST_CODE
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var contacts = arrayListOf<Contact>()
    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it?.data?.data?.let { pickContact(it) } }
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(CONTACTS_DATA_STORE_NAME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadContacts()
    }

    private fun loadContacts() {
        MainScope().launch {
            dataStore.data.map {
                it[stringPreferencesKey(CONTACTS_PREFERENCES_KEY)]
                        ?: Gson().toJson(arrayListOf<Contact>())
            }.collect {
                contacts = Gson().fromJson(it, object : TypeToken<List<Contact>>() {}.type)
                resetChips()
            }
        }
    }

    private fun resetChips() {
        binding.contactChipGroup.removeAllViews()
        contacts.forEach { contact ->
            binding.contactChipGroup.addView(Chip(this).apply {
                text = contact.name
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    contacts.remove(contact)
                    resetChips()
                }
            })
        }

        binding.contactChipGroup.addView(Chip(this).apply {
            text = "Add"
            chipIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.add)
            setOnClickListener {
                checkPermission(Manifest.permission.READ_CONTACTS, READ_CONTACTS_PERMISSION_REQUEST_CODE, ::addContact)
            }
        })
    }

    private fun addContact() {
        pickContactLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE })
    }

    private fun pickContact(uri: Uri) {
        contentResolver?.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.apply {
            moveToFirst()
            contacts.add(Contact(getString(0), getString(1) ?: getString(2)))
            close()
        }

        MainScope().launch {
            dataStore.edit {
                it[stringPreferencesKey(CONTACTS_PREFERENCES_KEY)] = Gson().toJson(contacts)
            }
            resetChips()
        }
    }

    private fun checkPermission(permission: String, requestCode: Int, ifPermissionAlreadyGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            ifPermissionAlreadyGranted()
        } else {
            requestPermissions(arrayOf(permission), requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                READ_CONTACTS_PERMISSION_REQUEST_CODE -> addContact()
            }
        }
    }

}