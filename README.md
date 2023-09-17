# :pushpin: findshelter
> 무더위 쉼터 위치 찾기
> </br>

## 1. 제작 기간 & 참여 인원
- 2023년 8월 9일 ~ 9월 4일
- 개인 프로젝트

</br>

## 2. 사용 기술
  - Kotlin
  - Retrofit: 위도와 경도를 통해서 주소를 받아오는 역지오코딩 요청 시 사용(GET)
  - Google Maps API: 화면에 지도 표시, 지도에 마커 표시, 카메라 이동에 사용
  - 공공데이터 포털 API (https://www.data.go.kr/) : 주소를 통해 지역코드를 받아올 떄(GET), 지역코드와 쉼터 구분코드를 통해 쉼터 위치를 받아올 때(GET)

</br>

## 3. 핵심 기능
이 서비스는 무더위 쉼터로 지정된 곳을 지도 위에 표시합니다.
</br>
내 위치를 지도에서 찾으면 위도와 경도, 주소를 알게 됩니다. 그리고 사용자가 쉼터 종류 중 하나를 선택하면 그에 맞는 쉼터의 위치를 찾아서 지도에 표시합니다.

</br>

## 4. 사용 방법
![캡처](https://github.com/DodoNehir/findshelter/assets/46012435/6e9730b9-0956-4b35-b86f-1f372cefdc22)
- 먼저 위,경도를 확인 후 그 정보로부터 주소를 찾습니다. 그 주소를 이용해서 지역코드를 찾고, 지역코드를 이용해서 쉼터위치를 찾는 순으로 진행됩니다.
</br>

- GeoRequest() 와 sherlterPointRequest()는 Retrofit을 사용합니다. interface와 응답받을 데이터 형식인 GoogleAddressResponse data class를 생성합니다. 그 후 Call 객체를 생성하고 GET통신의 결과를 addressInfo에 저장해 사용합니다.
```
interface GeoService {
    @GET("maps/api/geocode/json")
    fun getResults(    ): Call<GoogleAddressResponse>
}

geoCall = geoService.getResults(     )
geoCall.enqueue(object : Callback<GoogleAddressResponse> {
                    override fun onResponse( ) {
                        val addressInfo = response.body()
                    }
})
```

</br>

- areaCodeRequest() 는 DOM 방식으로 파싱합니다. 그래서 XML 전체를 받은 후 region_cd 태그의 값을 가져와서 사용합니다.

```
val xml: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)

val list: NodeList = xml.getElementsByTagName("region_cd")
val n: Node = list.item(0)
myAreaCode = n.textContent
```

</br>

## 5. 주요 문제점과 해결법 & 개선점
1.
- 문제점: Retrofit을 사용해 GET 통신 시 에러 발생함
- 원인: 지역 코드 요청 시 url 맨 마지막에 한글로 된 주소를 보내야 했는데 Retrofit을 사용하면 한글이 utf-8 인코딩되어 전달되어 발생함
- 해결법: Retrofit 대신 Document 객체를 사용해 DOM 방식으로 파싱해서 해결함

2.
- 문제점: 법정동코드 조회로 얻은 지역코드로 무더위쉼터 조회 시 '데이터 없음' 에러 발생
- 원인: 법정동코드 조회로 얻은 '지역코드'와 무더위쉼터 조회시 사용하는 '지역코드'간 차이가 있음
- 해결법: 공공데이터 포털에 문의해 둔 상태. 현재는 정상적으로 작동되는 한 주소에 고정해두고 쉼터를 찾고 있음

3. 앱을 시작하면 지도에 아무 위치도 표시되지 않는다. 이전 위치를 기억해두고 그 위치에서 다시 시작하도록 할 방법?
4. 매번 버튼을 누를 때마다 새로 요청해서 지도에 그리게 되는데 이미 받아온 정보를 기억할 방법?
