package br.com.gabrielferreira.iottest

import android.content.ContentValues
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import br.com.gabrielferreira.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*

/*
	Name: Gabriel Lucas Ferreira
	Email: gabriel@vrp.com.br
	Date: 26/11/17 19:46
*/

class MainActivity : AppCompatActivity() {

    private val database by lazy { FirebaseDatabase.getInstance() }
    private val relayToggleRef by lazy { database.getReference("relay_toggle") }
    private val tempToggleRef by lazy { database.getReference("temp_toggle") }
    private val tempRef by lazy { database.getReference("temp") }
    private val humRef by lazy { database.getReference("hum") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initListeners()
    }

    private fun initListeners() {
        temp_hum_toggle.setOnCheckedChangeListener { _, isChecked ->
            tempToggleRef.setValue(isChecked)
        }
        relay_toggle.setOnCheckedChangeListener { _, isChecked ->
            relayToggleRef.setValue(isChecked)
        }
        tempRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) return
                val value = dataSnapshot.getValue(String::class.java) ?: "Temp: -"
                temp.text = value
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
        humRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) return
                val value = dataSnapshot.getValue(String::class.java) ?: "Hum: -"
                hum.text = value
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
        tempToggleRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (!dataSnapshot.exists()) return
                val value = dataSnapshot.getValue(Boolean::class.java) ?: false
                if (temp_hum_toggle.isChecked == value) return
                temp_hum_toggle.isChecked = value
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
        relayToggleRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (!dataSnapshot.exists()) return
                val value = dataSnapshot.getValue(Boolean::class.java) ?: false
                if (relay_toggle.isChecked == value) return
                relay_toggle.isChecked = value
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
    }
}
