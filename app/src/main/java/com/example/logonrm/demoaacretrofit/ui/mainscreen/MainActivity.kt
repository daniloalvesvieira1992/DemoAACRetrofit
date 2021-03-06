package com.example.logonrm.demoaacretrofit.ui.mainscreen

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import com.example.logonrm.demoaacretrofit.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import com.muddzdev.styleabletoastlibrary.StyleableToast


class MainActivity : AppCompatActivity(),
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient

    val REQUEST_GPS = 0

    lateinit var mainViewModel: MainViewModel




    private fun checkPermission() {
        val permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        if (permission != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {

                val builder = AlertDialog.Builder(this)

                builder.setMessage("Necessária a permissao para GPS")
                        .setTitle("Permissao Requerida")

                builder.setPositiveButton("OK") { dialog, id ->
                    requestPermission()
                }

                val dialog = builder.create()
                dialog.show()

            } else {
                requestPermission()
            }
        }
    }

    protected fun requestPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_GPS)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_GPS -> {
                if (grantResults.size == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("TAG", "Permissão negada pelo usuário")
                } else {
                    Log.i("TAG", "Permissao concedida pelo usuario")
                }
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        btPesquisar.setOnClickListener {
            mainViewModel.pesquisarEndereco(etCEP.text.toString())
        }

        mainViewModel.isLoading.observe(this, Observer { isLoading ->
            if (isLoading!!) {
                llMap.visibility = View.GONE
                llLoading.visibility = View.VISIBLE
            } else {
                llLoading.visibility = View.GONE
            }
        })

        mainViewModel.apiResponse.observe(this, Observer { apiResponse ->
            if (apiResponse?.erro == null) {
                Log.i("TAG", "SUCESSO")

                llMap.visibility = View.VISIBLE

                manipularMapa(apiResponse!!.endereco!!.logradouro)


//                tvResultado.text = "${apiResponse?.endereco?.logradouro}, ${apiResponse?.endereco?.bairro}\n" +
//                        "${apiResponse?.endereco?.localidade}/${apiResponse?.endereco?.uf}"

                StyleableToast.Builder(this)
                        .text("Recebeu o endereço!")
                        .textColor(Color.WHITE)
                        .backgroundColor(Color.RED)
                        .show()


            } else {
                Log.i("TAG", "ERRO: ${apiResponse.erro}")
                StyleableToast.Builder(this)
                        .text("Não recebeu o endereço!")
                        .textColor(Color.WHITE)
                        .backgroundColor(Color.RED)
                        .show()
            }
        })

    }

    fun manipularMapa(logradouro: String) {

        mMap.clear()

        val geocoder = Geocoder(this)
        var address: List<Address>?

        address = geocoder.getFromLocationName(logradouro, 1)

        if (address.isNotEmpty()) {
            val location = address[0]
            adicionarMarcador(location.latitude, location.longitude, location.subAdminArea)
        } else {
            val alert = AlertDialog.Builder(this).create()
            alert.setTitle("Deu pau!")
            alert.setMessage("Endereço não encontrado!")

            alert.setCancelable(true)
            alert.setCanceledOnTouchOutside(false)

            alert.setButton(AlertDialog.BUTTON_POSITIVE, "OK", { dialogInterface, inteiro ->
                alert.dismiss()
            })

            alert.show()
        }
    }

    fun adicionarMarcador(lat: Double, lng: Double, titulo: String) {
        // Add a marker in Sydney and move the camera

        val latLngLocal = LatLng(lat, lng)

        mMap.addMarker(MarkerOptions()
                .position(latLngLocal)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round)))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngLocal, 17f))
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
        callConnection()
    }

    @Synchronized fun callConnection() {

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build()

        mGoogleApiClient.connect()
    }

    override fun onConnected(p0: Bundle?) {
        checkPermission()

        val minhaLocalizacao = LocationServices
                .FusedLocationApi
                .getLastLocation(mGoogleApiClient)

        if (minhaLocalizacao != null) {
            adicionarMarcador(minhaLocalizacao.latitude, minhaLocalizacao.longitude, "Estou aqui")
        }

    }

    override fun onConnectionSuspended(p0: Int) {
        Log.i("TAG", "SUSPENSO")

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.i("TAG", "Erro de Conexão")
    }
}
