package olon
package mongodb

import json._
import util.JodaHelpers

import java.util.{Calendar, Date, GregorianCalendar, UUID}
import java.util.regex.Pattern

import org.bson._
import org.bson.types.ObjectId
import org.joda.time._

import com.mongodb._

import JsonDSL._

private[mongodb] object Meta {

  /*
  * For converting scala objects into DBObject values
  */
  object Reflection {
    import java.lang.reflect._

    /*
    * These don't require a conversion and can be put directly into a DBObject
    */
    val primitives = Set[Class[_]](classOf[String], classOf[Int], classOf[Long], classOf[Double],
                                  classOf[Float], classOf[Byte], classOf[BigInt], classOf[Boolean],
                                  classOf[Short], classOf[java.lang.Integer], classOf[java.lang.Long],
                                  classOf[java.lang.Double], classOf[java.lang.Float],
                                  classOf[java.lang.Byte], classOf[java.lang.Boolean],
                                  classOf[java.lang.Short], classOf[scala.Array[Byte]])

    def primitive_?(clazz: Class[_]) = primitives contains clazz

    /*
    * This is used to convert DBObjects into JObjects
    */
    def primitive2jvalue(a: Any): JValue = a match {
      case x: String => JString(x)
      case x: Int => JInt(x)
      case x: Long => JInt(x)
      case x: Double => JDouble(x)
      case x: Float => JDouble(x)
      case x: Byte => JInt(BigInt(x))
      case x: BigInt => JInt(x)
      case x: Boolean => JBool(x)
      case x: Short => JInt(BigInt(x))
      case x: java.lang.Integer => JInt(BigInt(x.asInstanceOf[Int]))
      case x: java.lang.Long => JInt(BigInt(x.asInstanceOf[Long]))
      case x: java.lang.Double => JDouble(x.asInstanceOf[Double])
      case x: java.lang.Float => JDouble(x.asInstanceOf[Float])
      case x: java.lang.Byte => JInt(BigInt(x.asInstanceOf[Byte]))
      case x: java.lang.Boolean => JBool(x.asInstanceOf[Boolean])
      case x: java.lang.Short => JInt(BigInt(x.asInstanceOf[Short]))
      case _ => sys.error("not a primitive " + a.asInstanceOf[AnyRef].getClass)
    }

    /*
    * Date types require formatting
    */
    val datetypes = Set[Class[_]](classOf[Calendar], classOf[Date], classOf[GregorianCalendar], classOf[DateTime])

    def datetype_?(clazz: Class[_]) = datetypes contains clazz

    def datetype2jvalue(a: Any)(implicit formats: Formats): JValue = a match {
      case x: Calendar => JsonDate(x.getTime)(formats)
      case x: Date => JsonDate(x)(formats)
      case x: DateTime => JsonDateTime(x)(formats)
    }

    def datetype2dbovalue(a: Any) = a match {
      case x: Calendar => x.getTime
      case x: Date => x
      case x: DateTime => x.toDate
    }

    /*
    * Extended Mongo types.
    */
    val mongotypes = Set[Class[_]](
      classOf[DBRef], classOf[ObjectId], classOf[Pattern], classOf[UUID])

    def mongotype_?(clazz: Class[_]) = mongotypes contains clazz

    /*
    * Definitive place for JValue conversion of mongo types
    */
    def mongotype2jvalue(a: Any)(implicit formats: Formats): JValue = a match {
      case x: ObjectId => JsonObjectId.asJValue(x, formats)
      case x: Pattern => JsonRegex(x)
      case x: UUID => JsonUUID(x)
      case x: DBRef => sys.error("DBRefs are not supported.")
      case _ => sys.error("not a mongotype " + a.asInstanceOf[AnyRef].getClass)
    }

    /**
     * Bson types
     */

    val bsontypes = Set[Class[_]](
      classOf[BsonBinary], classOf[BsonBoolean], classOf[BsonDateTime],
      classOf[BsonDouble], classOf[BsonInt32], classOf[BsonInt64], classOf[BsonNull],
      classOf[BsonObjectId], classOf[BsonRegularExpression], classOf[BsonString])

    def bsontype_?(clazz: Class[_]) = bsontypes contains clazz

    def bsontype2jvalue(a: Any)(implicit formats: Formats): JValue = a match {
      case x: BsonBinary => {
        val binaryType = x.getType()

        if (binaryType == BsonBinarySubType.UUID_LEGACY.getValue()) {
          JsonUUID(x.asUuid(UuidRepresentation.JAVA_LEGACY))
        } else {
          sys.error("invalid binary type " + binaryType)
        }
      }
      case x: BsonBoolean => JBool(x.getValue)
      case x: BsonDateTime => JsonDate(x.getValue)(formats)
      case x: BsonDouble => JDouble(x.getValue)
      case x: BsonInt32 => JInt(x.getValue)
      case x: BsonInt64 => JInt(x.getValue)
      case x: BsonNull => JNull
      case x: BsonObjectId => JsonObjectId.asJValue(x.getValue, formats)
      case x: BsonRegularExpression => JsonRegex(Pattern.compile(x.getPattern, PatternHelper.optionsToFlags(x.getOptions)))
      case x: BsonString => JString(x.getValue)
      case _ => sys.error("not a bsontype " + a.asInstanceOf[AnyRef].getClass)
    }
  }

  @deprecated("use JsonDate.apply", "2.6")
  def dateAsJValue(d: Date, formats: Formats): JValue = ("$dt" -> formats.dateFormat.format(d))
  @deprecated("use JsonObjectId.apply", "2.6")
  def objectIdAsJValue(oid: ObjectId): JValue = JsonObjectId(oid)
  @deprecated("use JsonRegex.apply", "2.6")
  def patternAsJValue(p: Pattern): JValue = JsonRegex(p)
  @deprecated("use JsonUUID.apply", "2.6")
  def uuidAsJValue(u: UUID): JValue = JsonUUID(u)

  @deprecated("use JsonObjectId.asJValue", "2.6")
  def objectIdAsJValue(oid: ObjectId, formats: Formats): JValue =
    JsonObjectId.asJValue(oid, formats)


}
