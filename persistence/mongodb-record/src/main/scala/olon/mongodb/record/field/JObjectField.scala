package olon
package mongodb
package record
package field

import com.mongodb._
import olon.common._
import olon.json._
import olon.record._
import olon.util.Helpers.tryo

import org.bson._
import org.bson.codecs.{BsonDocumentCodec, BsonTypeCodecMap, Codec, DecoderContext, EncoderContext}
import org.bson.codecs.configuration.CodecRegistry

import scala.xml.NodeSeq

trait JObjectTypedField[OwnerType <: BsonRecord[OwnerType]] extends TypedField[JObject]
  with Field[JObject, OwnerType]
  with MongoFieldFlavor[JObject]
  with BsonDocumentJObjectField[JObject]
{

  override implicit val formats = owner.meta.formats

  def setFromJValue(jvalue: JValue): Box[JObject] = jvalue match {
    case JNothing|JNull if optional_? => setBox(Empty)
    case jo: JObject => setBox(Full(jo))
    case other => setBox(FieldHelpers.expectedA("JObject", other))
  }

  def setFromAny(in: Any): Box[JObject] = in match {
    case dbo: DBObject => setBox(setFromDBObject(dbo))
    case doc: Document => setBox(setFromDocument(doc))
    case jv: JObject => setBox(Full(jv))
    case Some(jv: JObject) => setBox(Full(jv))
    case Full(jv: JObject) => setBox(Full(jv))
    case seq: Seq[_] if seq.nonEmpty => seq.map(setFromAny).head
    case (s: String) :: _ => setFromString(s)
    case null => setBox(Full(null))
    case s: String => setFromString(s)
    case None | Empty | Failure(_, _, _) => setBox(Full(null))
    case o => setFromString(o.toString)
  }

  // assume string is json
  def setFromString(in: String): Box[JObject] = {
    // use lift-json to parse string into a JObject
    setBox(tryo(JsonParser.parse(in).asInstanceOf[JObject]))
  }

  def toForm: Box[NodeSeq] = Empty

  @deprecated("This was replaced with the functions from 'BsonableField'.", "3.4.3")
  def asDBObject: DBObject = valueBox
    .map { v => JObjectParser.parse(v)(owner.meta.formats) }
    .openOr(new BasicDBObject)

  @deprecated("This was replaced with the functions from 'BsonableField'.", "3.4.3")
  def setFromDBObject(obj: DBObject): Box[JObject] =
    Full(JObjectParser.serialize(obj)(owner.meta.formats).asInstanceOf[JObject])

  def setFromDocument(obj: Document): Box[JObject] =
    Full(JObjectParser.serialize(obj)(owner.meta.formats).asInstanceOf[JObject])

  def asJValue: JValue = valueBox openOr (JNothing: JValue)
}

class JObjectField[OwnerType <: BsonRecord[OwnerType]](@deprecatedName('rec) val owner: OwnerType)
  extends JObjectTypedField[OwnerType] with MandatoryTypedField[JObject] {

  def this(owner: OwnerType, value: JObject) = {
    this(owner)
    setBox(Full(value))
  }

  def defaultValue = JObject(List())

}

class OptionalJObjectField[OwnerType <: BsonRecord[OwnerType]](val owner: OwnerType)
  extends JObjectTypedField[OwnerType] with OptionalTypedField[JObject] {

  def this(owner: OwnerType, value: Box[JObject]) = {
    this(owner)
    setBox(value)
  }

}
