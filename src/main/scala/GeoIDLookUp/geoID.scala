package GeoIDLookUp

import scalaj.http.{Http, HttpOptions}

import play.api.libs.json.{JsValue, Json}

object geoID {

  // Function takes address latitude and longitude as arguments and returns an array of corresponding geofenceid

  def geoIDFind(latitude: String, longitude: String, proximity: String): List[(String,Int)] = {


    try {

      // Make an http request with the parameters with connection timeout and read timeout intervals

      val result = Http("http://geo.911rta.net/geofence/v2/match/").timeout(connTimeoutMs = 5000, readTimeoutMs = 15000).postData(s"""{"latitude":$latitude,"longitude":$longitude,"radius":$proximity}""").asString

      //Extract the body out of the result

      val resultSend = (result.body)

      // Json parse the contents of the body
      val payload = Json.parse(resultSend)

      // Cast JsValue as Map

      val extract = payload.as[Map[String,Int]]

      // Convert map to list

      val createList = extract.toList

      // Return the value

      return (createList)
    }
    catch
      {
        case e: Exception => println("Error in request " + e.getMessage)
          return (List(("0",0)))
      }
  }

}
