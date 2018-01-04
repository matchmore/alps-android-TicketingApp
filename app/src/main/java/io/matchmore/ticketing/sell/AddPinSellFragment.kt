package io.matchmore.ticketing.sell

import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.matchmore.sdk.MatchMore
import io.matchmore.sdk.api.models.Location
import io.matchmore.sdk.api.models.PinDevice
import io.matchmore.sdk.api.models.Publication
import io.matchmore.ticketing.Contract
import io.matchmore.ticketing.R
import io.matchmore.ticketing.extensions.showErrorDialog
import io.matchmore.ticketing.extensions.showProgressDialog
import kotlinx.android.synthetic.main.fragment_add_pin.*

class AddPinSellFragment : Fragment(), OnMapReadyCallback {

    private var marker: Marker? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initMap()
        addButton.setOnClickListener { if (validate()) add() }
    }

    private fun initMap() {
        val mapFragment = SupportMapFragment.newInstance(GoogleMapOptions().apply { liteMode(true) })
        childFragmentManager.beginTransaction().replace(R.id.map, mapFragment).commit()
        mapFragment.getMapAsync(this)
    }

    private fun add() {
        val dialog = activity!!.showProgressDialog()
        val location = Location(
                latitude = latitudeView.editText!!.text.toString().toDouble(),
                longitude = longitudeView.editText!!.text.toString().toDouble()
        )
        val pinDevice = PinDevice("pin device ${System.currentTimeMillis()}", location)
        MatchMore.instance.createPinDevice(pinDevice, { device ->
            val publication = Publication(
                    "ticketstosale",
                    rangeView.editText!!.text.toString().toDouble(),
                    durationView.editText!!.text.toString().toDouble(),
                    deviceId = device.id
            )
            publication.properties.apply {
                put(Contract.PROPERTY_CONCERT, concertView.editText!!.text.toString())
                put(Contract.PROPERTY_PRICE, priceView.editText!!.text.toString().toDouble().toString())
                put(Contract.PROPERTY_DEVICE_TYPE, Contract.DEVICE_TYPE_PIN)
                put(Contract.PROPERTY_IMAGE, imageView.editText!!.text.toString())
            }
            MatchMore.instance.createPublication(publication,
                    { _ -> activity?.finish() },
                    {
                        dialog.dismiss()
                        activity?.showErrorDialog(it)
                    })
        }, {
            dialog.dismiss()
            activity?.showErrorDialog(it)
        })
    }

    private fun validate(): Boolean {
        if (validateEditText(concertView)) return false
        if (validateEditText(priceView)) return false
        if (validateEditText(rangeView)) return false
        if (validateEditText(durationView)) return false
        if (validateEditText(latitudeView)) return false
        if (validateEditText(longitudeView)) return false
        return true
    }

    private fun validateEditText(textInputLayout: TextInputLayout): Boolean {
        if (textInputLayout.editText!!.text.isEmpty()) {
            textInputLayout.error = getString(R.string.cant_be_empty)
            return true
        }
        return false
    }

    override fun onMapReady(map: GoogleMap) {
        MatchMore.instance.locationManager.lastLocation?.let {
            val latLng = LatLng(it.latitude!!, it.longitude!!)
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().zoom(10f).target(latLng).build()))
            marker = map.addMarker(MarkerOptions().position(latLng))
            fillPositionFields(latLng)
        }
        map.setOnMapClickListener { latLng ->
            marker?.remove()
            marker = map.addMarker(MarkerOptions().position(latLng))
            fillPositionFields(latLng)
        }
    }

    private fun fillPositionFields(latLng: LatLng) {
        latitudeView.editText?.setText(latLng.latitude.toString())
        longitudeView.editText?.setText(latLng.longitude.toString())
    }
}