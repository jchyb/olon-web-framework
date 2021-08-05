package olon
package mongodb

import scala.collection.JavaConverters._

import java.util.{Date, UUID}
import java.util.regex.Pattern

import olon.json._
import olon.common.Box
import olon.util.SimpleInjector

import com.mongodb.{BasicDBObject, BasicDBList, DBObject}
import org.bson.types.ObjectId
import org.bson.Document

@deprecated("Please use BsonParser instead.", "3.4.3")
object JObjectParser extends SimpleInjector {
  /**
    * Set this to override JObjectParser turning strings that are valid
    * ObjectIds into actual ObjectIds. For example, place the following in Boot.boot:
    *
    * <code>JObjectParser.stringProcessor.default.set((s: String) => s)</code>
    */
  @deprecated("Please use BsonParser instead.", "3.4.3")
  val stringProcessor = new Inject(() => defaultStringProcessor _) {}

  def defaultStringProcessor(s: String): Object = {
    if (ObjectId.isValid(s)) new ObjectId(s)
    else s
  }

  /*
  * Parse a JObject into a DBObject
  */
  @deprecated("Please use BsonParser instead.", "3.4.3")
  def parse(jo: JObject)(implicit formats: Formats): DBObject =
    Parser.parse(jo, formats)

  /*
  * Serialize a DBObject into a JObject
  */
  @deprecated("Please use BsonParser instead.", "3.4.3")
  def serialize(a: Any)(implicit formats: Formats): JValue = {
    import mongodb.Meta.Reflection._
    a.asInstanceOf[AnyRef] match {
      case null => JNull
      case x if primitive_?(x.getClass) => primitive2jvalue(x)
      case x if datetype_?(x.getClass) => datetype2jvalue(x)(formats)
      case x if mongotype_?(x.getClass) => mongotype2jvalue(x)(formats)
      case x: BasicDBList => JArray(x.asScala.toList.map( x => serialize(x)(formats)))
      case x: BasicDBObject => JObject(
        x.keySet.asScala.toList.map { f =>
          JField(f.toString, serialize(x.get(f.toString))(formats))
        }
      )
      case x: Document => JObject(
        x.keySet.asScala.toList.map { f =>
          JField(f.toString, serialize(x.get(f.toString), formats))
        }
      )
      case x => {
        JNothing
      }
    }
  }

  @deprecated("Please use BsonParser instead.", "3.4.3")
  object Parser {

    @deprecated("Please use BsonParser instead.", "3.4.3")
    def parse(jo: JObject, formats: Formats): DBObject = {
      parseObject(jo.obj)(formats)
    }

    private def parseArray(arr: List[JValue])(implicit formats: Formats): BasicDBList = {
      val dbl = new BasicDBList
      trimArr(arr).foreach { a =>
        a match {
          case JsonObjectId(objectId) => dbl.add(objectId)
          case JsonRegex(regex) => dbl.add(regex)
          case JsonUUID(uuid) => dbl.add(uuid)
          case JsonDate(date) => dbl.add(date)
          case JArray(arr) => dbl.add(parseArray(arr))
          case JObject(jo) => dbl.add(parseObject(jo))
          case jv: JValue => dbl.add(renderValue(jv))
        }
      }
      dbl
    }

    private def parseObject(obj: List[JField])(implicit formats: Formats): BasicDBObject = {
      val dbo = new BasicDBObject
      trimObj(obj).foreach { jf =>
        jf.value match {
          case JsonObjectId(objectId) => dbo.put(jf.name, objectId)
          case JsonRegex(regex) => dbo.put(jf.name, regex)
          case JsonUUID(uuid) => dbo.put(jf.name, uuid)
          case JsonDate(date) => dbo.put(jf.name, date)
          case JArray(arr) => dbo.put(jf.name, parseArray(arr))
          case JObject(jo) => dbo.put(jf.name, parseObject(jo))
          case jv: JValue => dbo.put(jf.name, renderValue(jv))
        }
      }
      dbo
    }

    private def renderValue(jv: JValue)(implicit formats: Formats): Object = jv match {
      case JBool(b) => java.lang.Boolean.valueOf(b)
      case JInt(n) => renderInteger(n)
      case JDouble(n) => new java.lang.Double(n)
      case JNull => null
      case JNothing => sys.error("can't render 'nothing'")
      case JString(null) => "null"
      case JString(s) => stringProcessor.vend(s)
      case _ =>  ""
    }

    // FIXME: This is not ideal.
    private def renderInteger(i: BigInt): Object = {
      if (i <= java.lang.Integer.MAX_VALUE && i >= java.lang.Integer.MIN_VALUE) {
        new java.lang.Integer(i.intValue)
      }
      else if (i <= java.lang.Long.MAX_VALUE && i >= java.lang.Long.MIN_VALUE) {
        new java.lang.Long(i.longValue)
      }
      else {
        i.toString
      }
    }

    private def trimArr(xs: List[JValue]) = xs.filter(_ != JNothing)
    private def trimObj(xs: List[JField]) = xs.filter(_.value != JNothing)
  }
}

