package com.rosinante.firebasetrackerdisplay

import android.support.v4.app.FragmentActivity
import android.os.Bundle
import android.util.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

import java.util.HashMap

class DisplayTrackingActivity : FragmentActivity(), OnMapReadyCallback {
    private val mMarkers = HashMap<String, Marker>()
    private var mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_tracking)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.setMaxZoomPreference(16f)
        loginToFirebase()
    }

    private fun loginToFirebase() {
        val email = getString(R.string.firebase_email)
        val password = getString(R.string.firebase_password)
        // Authenticate with Firebase and subscribe to updates
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                subscribeToUpdates()
                Log.d(TAG, "firebase auth success")
            } else {
                Log.d(TAG, "firebase auth failed")
            }
        }
    }

    private fun subscribeToUpdates() {
        val ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path))
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String) {
                setMarker(dataSnapshot)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String) {
                setMarker(dataSnapshot)
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String) {}

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun setMarker(dataSnapshot: DataSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        val key = dataSnapshot.key
        val value = dataSnapshot.value as HashMap<*, *>?
        val lat = java.lang.Double.parseDouble(value!!["latitude"].toString())
        val lng = java.lang.Double.parseDouble(value["longitude"].toString())
        val location = LatLng(lat, lng)
        if (!mMarkers.containsKey(key)) {
            mMarkers.put(key, mMap!!.addMarker(MarkerOptions().title(key).position(location).icon(BitmapDescriptorFactory.fromResource(R.drawable.startup))))
        } else {
            mMarkers[key]!!.setPosition(location)
        }
        val builder = LatLngBounds.Builder()
        for (marker in mMarkers.values) {
            builder.include(marker.position)
        }
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 20))
    }

    companion object {

        private val TAG = DisplayTrackingActivity::class.java.simpleName
    }

}
