package com.example.findshelter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.example.findshelter.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.example.findshelter.PermissionUtils.isPermissionGranted
import com.example.findshelter.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(),
    OnMyLocationButtonClickListener,
    OnMyLocationClickListener,
    OnMapReadyCallback,
    OnRequestPermissionsResultCallback,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private var permissionDenied = false
    lateinit var toggle: ActionBarDrawerToggle


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initNavigationMenu()

        startGetApi()
    }

    private fun initNavigationMenu() {
        binding.mainNavigationView.setNavigationItemSelectedListener(this)

        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout,
            R.string.drawer_opened, R.string.drawer_closed
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle.syncState()
    }

    private fun startGetApi() {
        val serviceKey =
            "?serviceKey=ANkRyAgZUuuFouNIBZiN%2F9cLuafMaWihg4rYPimPNJsBpTlR3uAsXr%2BJb3KfOVwNLwngOK5O2SU%2BI1C7OW0ZZw%3D%3D"
        val pageNo = "&pageNo=1"
        val numOfRows = "&numOfRows=5"
        val type = "&type=XML"
        val year = "&year=2022"
        val areaCd = "&areaCd=1111064000"
        val equptype = "&equptype=001"
        /*001:노인시설 002:복지회관 003:마을회관 004:보건소 005:주민센터 006:면동사모소 007:종교시설
        008:금융기관 009:정자 010:공원 011:정자,파고라 012:공원 013:교량하부 014:나무그늘 015:하천둔치 099:기타*/

        // api 정보를 가지고있는 주소
        var url = "http://apis.data.go.kr/1741000/HeatWaveShelter2/getHeatWaveShelterList2"
        url = url + serviceKey + pageNo + numOfRows + type + year + areaCd + equptype

        thread(start = true) {
            Log.d("API로그", "스레드 시작")

            try {
                //XML 문서 빌더 객체 생성
                val xml: Document =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
                xml.documentElement.normalize()

                //row 태그를 가지는 노드를 찾는다. 계층적인 노드 구조를 반환한다.
                val list: NodeList = xml.getElementsByTagName("row")
                Log.d("API로그", "list row 갯수: " + list.length)

                //row 태그의 정보를 가져온다
                for (i in 0..list.length - 1) {
                    val n: Node = list.item(i)

                    if (n.nodeType == Node.ELEMENT_NODE) {
                        val elem = n as Element

                        Log.d("API로그", "${i + 1}번째 데이터")
                        Log.d(
                            "API로그",
                            "${i}" + " 번째 item에서 " + elem.getElementsByTagName("lo")
                                .item(0).textContent
                        )

                        var position = LatLng(
                            elem.getElementsByTagName("la").item(0).textContent.toDouble(),
                            elem.getElementsByTagName("lo").item(0).textContent.toDouble()
                        )

                        runOnUiThread {
                            // for문 돌면서 위치 마커 추가
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(position)
                                    .title(
                                        elem.getElementsByTagName("restname").item(0).textContent
                                    )
                            )
                            //TODO: 매번 카메라를 위치시키지 않을 방법?
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
                        }
                    }
                }
                runOnUiThread {
                    //for문 끝난 뒤 카메라 이동
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))
                }
            } catch (e: Exception) {
                Log.d("API로그", "StartgetAPI thread e: " + e.toString())
            }
        }
    }

    //토글 버튼을 누르면 Drawer가 들어가거나 나간다
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    //TODO 버튼 선택 시 해당 위치가 표시되도록 만들어야 함
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item1_tree -> Toast.makeText(this, "나무", Toast.LENGTH_SHORT).show()
            R.id.item2_park -> Toast.makeText(this, "공원", Toast.LENGTH_SHORT).show()
            R.id.item3_building -> Toast.makeText(this, "빌딩", Toast.LENGTH_SHORT).show()
        }
        return false
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
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        initMap()
        enableMyLocation()
    }

    /**
     * FINE 위치 권한 있을 때
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {

        //1. 권한 있는지 확인
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            return
        }

        // 2. 다이얼로그 띄워야한다면 띄우기
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            PermissionUtils.RationaleDialog.newInstance(
                LOCATION_PERMISSION_REQUEST_CODE, true
            ).show(supportFragmentManager, "dialog")
            return
        }

        // 3. Otherwise, request permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
        // [END maps_check_location_permission]
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "내 위치 찾기", Toast.LENGTH_SHORT)
            .show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "현재 위치:\n$location", Toast.LENGTH_LONG)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            enableMyLocation()
        } else {
            permissionDenied = true
        }
    }

    // [END maps_check_location_permission_result]
    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        newInstance(true).show(supportFragmentManager, "dialog")
    }

    /**
     * 기본 지도 설정
     */
    private fun initMap() {
        //최소, 최대 확대 수준을 설정
        mMap.setMinZoomPreference(8.0f)
        mMap.setMaxZoomPreference(20.0f)
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

}
