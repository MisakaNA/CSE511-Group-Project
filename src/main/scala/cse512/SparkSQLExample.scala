package cse512

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{SaveMode, SparkSession}

object SparkSQLExample {

  Logger.getLogger("org.spark_project").setLevel(Level.WARN)
  Logger.getLogger("org.apache").setLevel(Level.WARN)
  Logger.getLogger("akka").setLevel(Level.WARN)
  Logger.getLogger("com").setLevel(Level.WARN)

  def main(args: Array[String]) {
    val spark = SparkSession
      .builder()
      .appName("CSE512-Phase2")
      .config("spark.some.config.option", "some-value")//.master("local[*]")
      .getOrCreate()

    paramsParser(spark, args)

    spark.stop()
  }

  private def paramsParser(spark: SparkSession, args: Array[String]): Unit =
  {
    var paramOffset = 1
    var currentQueryParams = ""
    var currentQueryName = ""
    var currentQueryIdx = -1

    while (paramOffset <= args.length)
    {
      if (paramOffset == args.length || args(paramOffset).toLowerCase.contains("query"))
      {
        // Turn in the previous query
        if (currentQueryIdx!= -1) queryLoader(spark, currentQueryName, currentQueryParams, args(0)+currentQueryIdx)

        // Start a new query call
        if (paramOffset == args.length) return

        currentQueryName = args(paramOffset)
        currentQueryParams = ""
        currentQueryIdx = currentQueryIdx+1
      }
      else
      {
        // Keep appending query parameters
        currentQueryParams = currentQueryParams + args(paramOffset) +" "
      }

      paramOffset = paramOffset+1
    }
  }

  private def queryLoader(spark: SparkSession, queryName:String, queryParams:String, outputPath: String): Unit =
  {
    var queryResult:Long = -1
    val queryParam = queryParams.split(" ")
    if (queryName.equalsIgnoreCase("RangeQuery"))
    {
      if(queryParam.length!=2) throw new ArrayIndexOutOfBoundsException("[CSE512] Query "+queryName+" needs 2 parameters but you entered "+queryParam.length)
      queryResult = SpatialQuery.runRangeQuery(spark, queryParam(0), queryParam(1))
    }
    else if (queryName.equalsIgnoreCase("RangeJoinQuery"))
    {
      if(queryParam.length!=2) throw new ArrayIndexOutOfBoundsException("[CSE512] Query "+queryName+" needs 2 parameters but you entered "+queryParam.length)
      queryResult = SpatialQuery.runRangeJoinQuery(spark, queryParam(0), queryParam(1))
    }
    else if (queryName.equalsIgnoreCase("DistanceQuery"))
    {
      if(queryParam.length!=3) throw new ArrayIndexOutOfBoundsException("[CSE512] Query "+queryName+" needs 3 parameters but you entered "+queryParam.length)
      queryResult = SpatialQuery.runDistanceQuery(spark, queryParam(0), queryParam(1), queryParam(2))
    }
    else if (queryName.equalsIgnoreCase("DistanceJoinQuery"))
    {
      if(queryParam.length!=3) throw new ArrayIndexOutOfBoundsException("[CSE512] Query "+queryName+" needs 3 parameters but you entered "+queryParam.length)
      queryResult = SpatialQuery.runDistanceJoinQuery(spark, queryParam(0), queryParam(1), queryParam(2))
    }
    else
    {
      throw new NoSuchElementException("[CSE512] The given query name "+queryName+" is wrong. Please check your input.")
    }

    import spark.implicits._
    val resultDf = Seq(queryName, queryResult.toString).toDF()
    resultDf.write.mode(SaveMode.Overwrite).csv(outputPath)
  }

  def ST_Contains(pointString:String, queryRectangle:String): Boolean = {
    var valueArray = pointString.split(',')
    val pointX = valueArray(0).toDouble
    val pointY = valueArray(1).toDouble

    valueArray = queryRectangle.split(',')
    val rectX1 = math.min(valueArray(0).toDouble, valueArray(2).toDouble)
    val rectY1 = math.min(valueArray(1).toDouble, valueArray(3).toDouble)
    val rectX2 = math.max(valueArray(0).toDouble, valueArray(2).toDouble)
    val rectY2 = math.max(valueArray(1).toDouble, valueArray(3).toDouble)

    pointX >= rectX1 && pointX <= rectX2 && pointY >= rectY1 && pointY <= rectY2
  }

  def ST_Within(pointString1:String, pointString2:String, distance:Double): Boolean = {
    var valueArray = pointString1.split(',')
    val point1X = valueArray(0).toDouble
    val point1Y = valueArray(1).toDouble

    valueArray = pointString2.split(',')
    val point2X = valueArray(0).toDouble
    val point2Y = valueArray(1).toDouble

    val point_distance = math.sqrt(math.pow(point1X - point2X, 2.0) + math.pow(point1Y - point2Y, 2.0))

    point_distance <= distance
  }
}
