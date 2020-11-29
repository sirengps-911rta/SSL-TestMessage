package GeoIDLookUp

import scalaj.http.{Http, HttpOptions}

import play.api.libs.json.{JsValue, Json}


object geoID {

  // Function takes adrress latitude and longitude as arguments and returns an array of corresponding geofenceid

  def geoIDFind(latitude: String, longitude: String): List[Int] = {


    try {

      // Make an http request with the parameters with connection timeout and read timeout intervals

      val result = Http("http://geo.911rta.net/geofence/v1/match/").timeout(connTimeoutMs = 5000, readTimeoutMs = 15000).postData(s"""{"latitude":$latitude,"longitude":$longitude,"radius":"0"}""").asString

      //Extract the body out of the result

      val resultSend = (result.body)

      // Json parse the contents of the body
      val payload = Json.parse(resultSend)

      // Extract the geofence ID's from the response

      val extract = (payload \ "id").validate[Array[Int]].get.toList

      // Return the value

      return (extract)
    }
    catch
      {
        case e: Exception => println("Error in request " + e.getMessage)
          return (List(0))
      }
  }
}