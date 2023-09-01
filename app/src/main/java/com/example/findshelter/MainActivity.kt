package com.example.findshelter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.example.findshelter.BuildConfig.MAPS_API_KEY
import com.example.findshelter.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.example.findshelter.PermissionUtils.isPermissionGranted
import com.example.findshelter.ShelterPointResponse.ShelterPointResponse
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), OnMyLocationButtonClickListener,
    OnMyLocationClickListener, OnMapReadyCallback, OnRequestPermissionsResultCallback,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private var permissionDenied = false
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var myKorAddress: String
    private lateinit var myAreaCode: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 위치 서비스 클라이언트 생성
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initNavigationMenu()
    }

    private fun initNavigationMenu() {
        binding.mainNavigationView.setNavigationItemSelectedListener(this)

        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, R.string.drawer_opened, R.string.drawer_closed
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle.syncState()
    }

    // http 통신으로 위도 경도를 주소 json으로 받아오는 지오코딩 api
    private fun GeoRequest() {


        //1. retrofit 객체 생성
        val retrofit: Retrofit =
            Retrofit.Builder().baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create()).build()

        //2, service 객체 생성
        val geoService: GeoService = retrofit.create(GeoService::class.java)

        //3. 마지막 확인된 위치로
        // Call 객체 생성
        lateinit var geoCall: Call<GoogleAddressResponse>
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            // 마지막으로 알려진 위치 가져오기.
            // null일 경우에는 기기에서 위치 사용 중지 중일 때 등등임
            if (location != null) {
                Log.d("GEO 로그", "location 에 정보가 있습니다")
                var lalo = location.latitude.toString() + "," + location.longitude.toString()
                geoCall = geoService.getResults(lalo, MAPS_API_KEY, "ko")

                //4. 네트워크 통신
                geoCall.enqueue(object : Callback<GoogleAddressResponse> {
                    override fun onResponse(
                        call: Call<GoogleAddressResponse>,
                        response: Response<GoogleAddressResponse>
                    ) {
                        val addressInfo = response.body()

                        if (addressInfo != null) {
                            Log.d("GEO 로그", "응답 내용 status: " + addressInfo.status)
                        } else {
                            Log.d("GEO 로그", "응답 내용 status 가 null입니다")
                        }
                        if (addressInfo != null) {
                            // 국가와 세부주소를 뺀 주소만 저장
                            myKorAddress = addressInfo.results[0].formatted_address
                            myKorAddress = myKorAddress.substring(
                                myKorAddress.indexOf(" ") + 1, myKorAddress.lastIndexOf(" ")
                            )

                            Log.d("GEO 로그", "지역코드 요청 시작")
                            areaCodeRequest()

                        } else {
                            Log.d("GEO 로그", "응답 내용 주소 내용: null 입니다")
                        }

                    }

                    override fun onFailure(call: Call<GoogleAddressResponse>, t: Throwable) {
                        //오류
                        call.cancel()
                        Log.d("GEO 로그", "Call 실패: " + call.request().toString())
                    }

                })
            } else {
                Log.d("GEO 로그", "location 이 null입니다")
            }
        }

    }

    // 한글주소로 지역코드를 조회하는 법정동코드 조회 api


    /**
     * HttpURLConnection 이용해서 날려보자!!
     */
    private fun areaCodeRequest() {
        var urlBuilder: StringBuilder =
            StringBuilder("https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList") /* URL */
        urlBuilder.append("?" + "serviceKey=" + BuildConfig.AREA_API_KEY) /*Service Key*/
        urlBuilder.append("&" + "pageNo" + "=" + "1") /*페이지번호*/
        urlBuilder.append("&" + "numOfRows" + "=" + "3") /*한 페이지 결과 수*/
        urlBuilder.append("&" + "type" + "=" + "xml") /*호출문서(xml, json) default : xml*/
        urlBuilder.append("&" + "locatadd_nm" + "=" + myKorAddress) /*지역주소명*/

        val url = urlBuilder.toString()
        Log.d("AREA 로그", "url: " + url)

        thread(start = true) {
            try {
                //XML 문서 빌더 객체 생성
                val xml: Document =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
                xml.documentElement.normalize()

                //row 태그를 가지는 노드를 찾는다. 계층적인 노드 구조를 반환한다.
                val list: NodeList = xml.getElementsByTagName("row")

                //row 태그의 정보를 가져온다
                val n: Node = list.item(0)
                if (n.nodeType == Node.ELEMENT_NODE) {
                    val elem = n as Element
                    myAreaCode = elem.getElementsByTagName("region_cd").item(0).textContent
                    Log.d("AREA 로그", "myAreaCode: " + myAreaCode)

                    // 쉼터 위치 찾아오기
                    shelterPointRequest()

                } else {
                    Log.d("AREA 로그", "myAreaCode에 넣을 노드정보가 없습니다")
                }
            } catch (e: Exception) {
                Log.d("AREA 로그", "스레드실패: " + e.toString())
            }
        }

    }


    // areaCd(지역 코드)를 이용해서 쉼터 위치 xml을 받아오는 api
    private fun shelterPointRequest() {
        val shelterPointService: ShelterPointService = ShelterPointService.create()
        val shelterCall: Call<ShelterPointResponse>

        shelterCall = shelterPointService.getShelterPoint(
            BuildConfig.AREA_API_KEY,
            1,
            5,
            "JSON",
            2022,
            "1111064000",
            "001"
        )
        // TODO: myAreaCode로 해야하지만 안 되서 4691034000 로 테스트하기.

        /**
         * <areaCd>4691034000</areaCd>
         * <areaNm>전라남도 신안군 비금면</areaNm>
         *
         * 1111064000
         * 서울 종로구 이화동
         */
        //TODO 문의 및 오류신고 했음.

        Log.d("Shelter 로그", shelterCall.request().toString())

        shelterCall.enqueue(object : Callback<ShelterPointResponse> {
            override fun onResponse(
                call: Call<ShelterPointResponse>,
                response: Response<ShelterPointResponse>
            ) {
                val shelterInfo = response.body()
                if (shelterInfo != null) {
                    // 제대로 안 될 때는 기본위치. 서울 종로구 이화동 6.25참전 유공자 경로당
                    var position = LatLng(37.57660100, 127.00554200)

                    // for (i in 0..shelterInfo.HeatWaveShelter.get(0).head.get(0).totalCount) {
                    // TODO 검색은 5개 하는데 검색결과로 36개가 나와서 이걸 어떻게 다 표시할 지...

                    var totalCount = shelterInfo.HeatWaveShelter.get(0).head.get(0).totalCount
                    if (shelterInfo.HeatWaveShelter.get(0).head.get(0).totalCount > 5) {totalCount = 5}

                    for (i in 0..totalCount-1) {
                        Log.d("Shelter 로그", shelterInfo.HeatWaveShelter.get(1).row.get(i).restname)

                        position = LatLng(
                            shelterInfo.HeatWaveShelter.get(1).row.get(i).la,
                            shelterInfo.HeatWaveShelter.get(1).row.get(i).lo
                        )
                        // 마커 찍고
                        runOnUiThread {
                            mMap.addMarker(
                                MarkerOptions().position(position)
                                    .title(shelterInfo.HeatWaveShelter.get(1).row.get(i).restname)
                            )
                        }
                    }

                    // 마지막 위치로 카메라 이동
                    runOnUiThread {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
                        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))
                    }
                } else {
                    Log.d("Shelter 로그", "실패: Call 했지만 Response가 null 입니다.")
                }
            }

            override fun onFailure(call: Call<ShelterPointResponse>, t: Throwable) {
                call.cancel()
                Log.d("Shelter 로그", "Call 실패: " + t.message)
            }

        })

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
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            return
        }

        // 2. 다이얼로그 띄워야한다면 띄우기
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            PermissionUtils.RationaleDialog.newInstance(
                LOCATION_PERMISSION_REQUEST_CODE, true
            ).show(supportFragmentManager, "dialog")
            return
        }

        // 3. Otherwise, request permission
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_PERMISSION_REQUEST_CODE
        )
        // [END maps_check_location_permission]
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "내 위치 찾기", Toast.LENGTH_SHORT).show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        GeoRequest()
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "현재 위치:\n$location", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (isPermissionGranted(
                permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION
            ) || isPermissionGranted(
                permissions, grantResults, Manifest.permission.ACCESS_COARSE_LOCATION
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