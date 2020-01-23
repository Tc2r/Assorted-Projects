package com.dreams.kotlingeofencedemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private val TAG = "MapsActivity"
    private val REQUEST_PERMISSIONS_ID_CODE = 123
    private var mapFragment: SupportMapFragment? = null

    private lateinit var map: GoogleMap


    private var locationManager: LocationManager? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var lastLocation: Location? = null
    private var locationMarker: Marker? = null
    private var initFindUser: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Get a Ref to Location Manager.
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check for and request required permissions.
        if(setPermissions()){
            // Start Requesting Updates.
            startLocationUpdates()
        }

        // Obtain the SupportMapFragment
        initGmaps()

        createGoogleApi()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0.0f, this)
    }


    // PERMISSION MANAGEMENT
    private fun setPermissions(): Boolean {
        val permissionsNeededList = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getPermissionsNeededListPost0(permissionsNeededList)
        } else {
            getPermissionsNeededListPre0(permissionsNeededList)
        }

        if (permissionsNeededList.isNotEmpty()) {
            makeRequest(permissionsNeededList)
            return false
        }
        return true
    }

    // Workaround for Permission Bug: https://github.com/permissions-dispatcher/PermissionsDispatcher/issues/646
    private fun getPermissionsNeededListPre0(permissionsNeededList: MutableList<String>) {
        val permissionFineLocation =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionFineLocation != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Fine Location Permission Denied")
            permissionsNeededList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    }

    // Workaround for Permission Bug: https://github.com/permissions-dispatcher/PermissionsDispatcher/issues/646
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun getPermissionsNeededListPost0(permissionsNeededList: MutableList<String>) {
        val permissionFineLocation =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionFineLocation != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Fine Location Permission Denied")
            permissionsNeededList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val permissionBackgroundLocation =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )

        if (permissionBackgroundLocation != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Access Background Permission Denied")
            permissionsNeededList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun makeRequest(permissionsNeededList: MutableList<String>) {

        ActivityCompat.requestPermissions(
            this,
            permissionsNeededList.toTypedArray(),
            REQUEST_PERMISSIONS_ID_CODE
        )

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS_ID_CODE -> {
                Log.d(TAG, "onRequestPermissionsResult")

                val perms: HashMap<String, Int> = HashMap<String, Int>()

                perms.put(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    PackageManager.PERMISSION_GRANTED
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    perms.put(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        PackageManager.PERMISSION_GRANTED
                    )
                }


                if (grantResults.isNotEmpty()) {

                    for (i in permissions.indices) {
                        perms[permissions[i]] = grantResults[i]
                    }

                    // Workaround for Permission Bug: https://github.com/permissions-dispatcher/PermissionsDispatcher/issues/646
                    // I hate it >_<
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                        // Check for both permissions
                        if (perms.get(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.d(TAG, "Background and Fine location services permission granted")

                            // Start Requesting Updates.
                            startLocationUpdates()
                        } else {
                            Log.d(TAG, "Some permissions are not granted ask again ")
                            //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
                            //                        // shouldShowRequestPermissionRationale will return true
                            //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            ) {
                                val builder = AlertDialog.Builder(this)
                                builder.setMessage("Location Permissions are needed for this application.")
                                builder.setTitle("Permissions Required")
                                builder.setPositiveButton("Okay")
                                { dialog, which ->
                                    Log.d(TAG, "Permissions Accepted")
                                    makeRequest(
                                        mutableListOf(
                                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                }
                                val dialog = builder.create()
                                dialog.show()

                                // Proceed with Logic by disabling the related features or close the app.
                            } else {
                                Toast.makeText(
                                    this,
                                    "Please Enable Permissions for this app in phone settings.",
                                    Toast.LENGTH_LONG
                                ).show()

                                this.finish()

                            }
                            //permission is denied (and never ask again is  checked)
                        }
                    } else {
                        // Check for both permissions
                        if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "Fine location services permission granted")

                            // Start Requesting Updates.
                            startLocationUpdates()
                        } else {
                            Log.d(TAG, "Some permissions are not granted ask again ")
                            //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
                            // shouldShowRequestPermissionRationale will return true
                            //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            ) {
                                val builder = AlertDialog.Builder(this)
                                builder.setMessage("Location Permissions are needed for this application.")
                                builder.setTitle("Permissions Required")
                                builder.setPositiveButton("Okay")
                                { dialog, which ->
                                    Log.d(TAG, "Clicked")
                                    makeRequest(
                                        mutableListOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                }
                                val dialog = builder.create()
                                dialog.show()

                                // Proceed with Logic by disabling the related features or close the app.
                            } else {
                                Toast.makeText(
                                    this,
                                    "Please Enable Permissions for this app in phone settings.",
                                    Toast.LENGTH_LONG
                                ).show()

                                this.finish()

                            }
                            //permission is denied (and never ask again is  checked)
                        }
                    }


                }
            }
        }
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
        Log.d(TAG, "onMapReady()")
        map = googleMap


    }

    private fun initGmaps() {
        Log.d(TAG, "initGmaps()")
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        initFindUser = true
    }

    private fun createGoogleApi() {
        Log.d(TAG, "createGoogleApi()")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }


    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderEnabled(provider: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(provider: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //Define the listener
    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "onLocationChanged() [$location ]")


        // If lastLocation is initialized check to see if it is still up-to-date.
        // If not, update it and redraw marker.
        if (lastLocation != null) {
            if (lastLocation != location) {
                if (locationMarker != null)
                {
                    locationMarker!!.remove()
                }

                val latitude = location.latitude
                val longitude = location.longitude
                val latLng = LatLng(latitude, longitude)

                // Use Google's Geocoder api to find out information about the coordinates.
                val geocoder = Geocoder(applicationContext)

                val addressList: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)

                var addressInfo = addressList[0].locality + " " + addressList[0].countryName

                // Create marker with location of user and address information.
                locationMarker = map.addMarker(MarkerOptions().position(latLng).title(addressInfo))

                // On first run, animate to user's location marker.
                if (initFindUser)
                {
                    initFindUser = false
                    centerCameraOnLocation(locationMarker!!.position);
                }
            }
        }
        else
        {
            lastLocation = location
            val latitude = location.latitude
            val longitude = location.longitude
            val latLng = LatLng(latitude, longitude)

            val geocoder = Geocoder(applicationContext)

            val addressList: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)

            var addressInfo = addressList[0].locality + " " + addressList[0].countryName

            locationMarker = map.addMarker(MarkerOptions().position(latLng).title(addressInfo))

            if (initFindUser)
            {
                initFindUser = false
                centerCameraOnLocation(locationMarker!!.position);
            }
        }
    }
    private fun centerCameraOnLocation(position: LatLng) {
        Log.i(TAG, "centerCameraOnLocation()")

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15.0f))
    }
}
