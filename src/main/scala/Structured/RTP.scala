package Structured

import GeoIDLookUp.geoID
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{ArrayType, DataTypes, StringType, StructType}
import org.apache.spark.sql.functions.{from_json, to_timestamp, udf}



object RTP {

  def main(args: Array[String]): Unit = {

    //Define environment configurations using ConfigFactory object

    val props = ConfigFactory.load()

    //Pass dev or prod are arguments

    val envProps = props.getConfig(args(0))

    //The Sparksession object is the entry point of the spark program and this allows for creation of dataframe


    val spark = SparkSession.builder().master(envProps.getString("execution.mode")).appName("GSDT").getOrCreate

    //Import spark implicits so that we can use the '$' operator

    import spark.implicits._

    //Prevents INFO messages from being displayed

    spark.sparkContext.setLogLevel("ERROR")

    //Configure the number of partitions that are used when shuffling data for joins or aggregations

    spark.conf.set("spark.sql.shuffle.partitions", "2")

    // Increase the network timeout interval to 10000s

    spark.conf.set("spark.network.timeout","10000000")

    // Increase the heartbeat timeout interval to 1000s

    spark.conf.set("spark.executor.heartbeatInterval","1000000")

    // Readstream includes the source, broker, offsets, topic, security protocol, truststore location, truststore password and dataloss configurations

    val lines = spark.
      readStream.
      format("kafka").
      option("kafka.bootstrap.servers", envProps.getString("bootstrap.servers"))
      .option("startingOffsets", "latest").option("subscribe", "RTA-INCIDENT-MESSAGE")
      .option("kafka.security.protocol","SSL")
      .option("kafka.ssl.truststore.location",envProps.getString("ssl.truststore"))
      .option("kafka.ssl.truststore.password","clientpass").option("failOnDataLoss","false")
      .load()

    //Convert the byte stream to String

    val payloadJsonDf = lines.selectExpr("CAST(value AS STRING)")

    //Create schema for the JSON message

    val schema = new StructType()
      .add("Preamble", new StructType()
        .add("PSAP",DataTypes.StringType)
        .add("Gateway", DataTypes.StringType)
        .add("AuthorizationKey",DataTypes.StringType)
        .add("Version",DataTypes.StringType)
        .add("Timestamp",DataTypes.StringType))
      .add("Canon",new StructType()
        .add("UUID", DataTypes.StringType)
        .add("IncidentCode", DataTypes.StringType)
        .add("AddressGeofenceId", ArrayType(StringType)) )
      .add("Call",new StructType()
        .add("ReceivedTime",DataTypes.StringType)
        .add("CallingPhoneNumber",DataTypes.StringType)
        .add("CallNumber",DataTypes.StringType)
        .add("AgencyID",DataTypes.StringType)
        .add("AgencyDescription",DataTypes.StringType)
        .add("DisciplineID",DataTypes.StringType)
        .add("DisciplineDescription",DataTypes.StringType)
        .add("NatureCode",DataTypes.StringType)
        .add("NatureDescription",DataTypes.StringType)
        .add("SituationDisciplineID",DataTypes.StringType)
        .add("SituationDisciplineDescription",DataTypes.StringType)
        .add("SituationCode",DataTypes.StringType)
        .add("SituationDescription",DataTypes.StringType)
        .add("AddressLocationType",DataTypes.StringType)
        .add("AddressPlaceName",DataTypes.StringType)
        .add("AddressStreetNumber",DataTypes.StringType)
        .add("AddressStreetAddress",DataTypes.StringType)
        .add("AddressApartmentNumber",DataTypes.StringType)
        .add("AddressFloor",DataTypes.StringType)
        .add("AddressLocationInformation",DataTypes.StringType)
        .add("AddressLandmark",DataTypes.StringType)
        .add("AddressCity",DataTypes.StringType)
        .add("AddressState",DataTypes.StringType)
        .add("AddressZipCode",DataTypes.StringType)
        .add("AddressZipCodePlus4",DataTypes.StringType)
        .add("AddressCounty",DataTypes.StringType)
        .add("AddressLatitude",DataTypes.StringType)
        .add("AddressLongitude",DataTypes.StringType)
        .add("AddressConfidence", DataTypes.StringType)
        .add("AddressOverridden",DataTypes.StringType)
        .add("AddressClearedLatitude" ,DataTypes.StringType)
        .add("AddressClearedLongitude" ,DataTypes.StringType)
        .add("AddressClearedConfidence" ,DataTypes.StringType)
        .add("AddressClearedAltitude", DataTypes.StringType)
        .add("AddressClearedAltitudeConfidence", DataTypes.StringType)
        .add("BoxCard",DataTypes.StringType)
        .add("AlarmNumber",DataTypes.StringType)
        .add("ReasonTypeCode",DataTypes.StringType)
        .add("Notes",DataTypes.StringType)
        .add("UserDefinedFields",DataTypes.StringType)
        .add("InFrom",DataTypes.StringType)
        .add("StatusCode",DataTypes.StringType)
        .add("Cancelled",DataTypes.StringType)
        .add("IsArchived",DataTypes.StringType)
        .add("RemovedFromHistory",DataTypes.StringType)
        .add("PriorityCode",DataTypes.StringType)
        .add("PriorityDescription",DataTypes.StringType)
        .add("AgencyInCode",DataTypes.StringType)
        .add("LinkCallNumber",DataTypes.StringType)
        .add("LinkType",DataTypes.StringType)
        .add("NoUnitAvailable",DataTypes.StringType)
        .add("Reoccuring",DataTypes.StringType)
        .add("Reassigned",DataTypes.StringType)
        .add("CancellationNarrative",DataTypes.StringType)
        .add("ExternalKey",DataTypes.StringType)
        .add("ExternalSource", DataTypes.StringType)
        .add("IncidentTime",DataTypes.StringType)
        .add("CallZone",DataTypes.StringType) )
      .add("Incident", ArrayType (new StructType()
        .add("CallNumber",DataTypes.StringType)
        .add("AgencyID",DataTypes.StringType)
        .add("AgencyDescription",DataTypes.StringType)
        .add("DisciplineID",DataTypes.StringType)
        .add("DisciplineDescription",DataTypes.StringType)
        .add("IncidentNumber", DataTypes.StringType)
        .add("SchemaID", DataTypes.StringType)),true)
      .add("EnhancedLocation", ArrayType( new StructType()
        .add("AddressGeolocationLatitude", DataTypes.StringType, true)
        .add("AddressGeoLocationLongitude", DataTypes.StringType,true)
        .add("AddressGeolocationProvider", DataTypes.StringType),true))
      .add("RawXML",DataTypes.StringType,true)

    //Apply schema to the JSON message to create a Dataframe

    val payloadNestedDf = payloadJsonDf.select(from_json($"value",schema).as("PL"))

    //We flatten the dataframe because it consists of nested columns

    val payloadFlattenedDf = payloadNestedDf.selectExpr("PL.Preamble","PL.Canon","PL.Call","PL.Incident","PL.EnhancedLocation","PL.RawXML")

    // Create User defined function for geoLookUp

    val GeoUDF = udf[List[Int],String,String](geoID.geoIDFind)

    // Drop Raw message

    val dropRaw = payloadFlattenedDf.drop("RawXML")

    //Cast the NatureCode

    val castNatureCode = dropRaw.withColumn(("IncidentCodeLookUp"),dropRaw.col("Call.NatureCode").cast(DataTypes.StringType))

    //Cast the timestamo

    val castTimestamp = castNatureCode.withColumn("timestampLookUp", to_timestamp($"Call.ReceivedTime", "yyyy-MM-dd'T'HH:mm:ss"))

    //Add geoID

    val geoIddf = castTimestamp.withColumn("AddressGeofenceId",GeoUDF(dropRaw("Call.AddressLatitude"),dropRaw("Call.AddressLongitude")))

    // Filter out of test messages

    val testFilter = geoIddf.filter(geoIddf("Canon.IncidentCode") === "TEST" && geoIddf("Preamble.AuthorizationKey") === "PGA0TEST")

    //Pass the Test message removed Dataset into the deduplication function. Only messages that have arrived in the last 24 hours are passed to the DeDuplication function

    val dropDup = testFilter.withWatermark("timestampLookUp", "24 hours").dropDuplicates("timestampLookUp","IncidentCodeLookUp","AddressGeofenceId")

    // Test messages and results of the Dedupication function are combined

    val combineTestMessage = testFilter.union(dropDup)

    //  Message is json encoded and combined in to the value column

     val frameValue = combineTestMessage.selectExpr("to_json(struct(*)) AS value")

    // The write stream into the kafka topic with kafka broker, topic, checkpoint, data loss, security, truststore and password configuration

    val query = frameValue.writeStream.format("kafka")
      .option("kafka.bootstrap.servers",envProps.getString("bootstrap.servers"))
      .option("topic","RTA-ALERT").option("checkpointLocation",envProps.getString("checkpoint.location"))
      .option("failOnDataLoss", "false")
      .option("kafka.security.protocol","SSL")
    .option("kafka.ssl.truststore.location",envProps.getString("ssl.truststore"))
      .option("kafka.ssl.truststore.password","clientpass").start()

    // The query continues running until it is terminated

    query.awaitTermination()


//

  }


  }
