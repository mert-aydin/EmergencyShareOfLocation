package com.mertaydin.emergencyshareoflocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.mertaydin.emergencyshareoflocation.databinding.ActivityMainBinding

private const val PICK_CONTACT_REQUEST = 1
private const val READ_CONTACTS_PERMISSION_REQUEST_CODE = 1
private const val SEND_SMS_PERMISSION_REQUEST_CODE = 2
private const val LOCATION_PERMISSION_REQUEST_CODE = 3

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val contacts = arrayListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resetChips()

        // TODO: keep contacts on persistent memory
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
        startActivityForResult(Intent(Intent.ACTION_PICK, Uri.parse("content://contacts")).apply { type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE }, PICK_CONTACT_REQUEST)
    }

    private fun writeContactsToPersistentMemory() {

    }

    private fun checkPermission(permission: String, requestCode: Int, onPermissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
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

    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        if (requestCode == PICK_CONTACT_REQUEST) {
            val cursor = contentResolver.query(data!!.data!!, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)!!
            cursor.moveToFirst()
            contacts.add(Contact(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)), cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))))
            cursor.close()

            resetChips()
        }
    }
}