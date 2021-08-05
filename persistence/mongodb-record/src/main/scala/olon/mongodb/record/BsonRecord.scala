package olon
package mongodb
package record

import olon.common._

import java.util.prefs.BackingStoreException
import java.util.regex.Pattern
import scala.collection.JavaConverters._

import olon.mongodb.record.codecs.{RecordCodec, RecordTypedCodec}
import olon.mongodb.record.field._
import olon.record.{Field, MetaRecord, Record}
import olon.record.field._

import org.bson._
import org.bson.codecs.{BsonTypeClassMap, Codec, DecoderContext, EncoderContext}
import org.bson.codecs.configuration.{CodecRegistries, CodecRegistry}
import org.bson.conversions.Bson
import com.mongodb._

/** Specialized Record that can be encoded and decoded from BSON (DBObject) */
trait BsonRecord[MyType <: BsonRecord[MyType]] extends Record[MyType] {
  self: MyType =>

  /** Refines meta to require a BsonMetaRecord */
  def meta: BsonMetaRecord[MyType]

  /**
    * Encode a record instance into a DBObject
    */
  @deprecated("RecordCodec is now used instead.", "3.4.3")
  def asDBObject: DBObject = meta.asDBObject(this)

  @deprecated("RecordCodec is now used instead.", "3.4.3")
  def asDocument: Document = meta.asDocument(this)

  /**
    * Set the fields of this record from the given DBObject
    */
  @deprecated("RecordCodec is now used instead.", "3.4.3")
  def setFieldsFromDBObject(dbo: DBObject): Unit = meta.setFieldsFromDBObject(this, dbo)

 /**
   * Save the instance and return the instance
   */
  override def saveTheRecord(): Box[MyType] = throw new BackingStoreException("BSON Records don't save themselves")

  /**
    * Pattern.equals doesn't work properly so it needs a special check. If you use PatternField, be sure to override equals with this.
    */
  @deprecated("PatternField now has a properly functioning `equals` method.", "3.4.1")
  protected def equalsWithPatternCheck(other: Any): Boolean = {
    other match {
      case that: BsonRecord[MyType] =>
        that.fields.corresponds(this.fields) { (a,b) =>
          (a.name == b.name) && ((a.valueBox, b.valueBox) match {
            case (Full(ap: Pattern), Full(bp: Pattern)) => ap.pattern == bp.pattern && ap.flags == bp.flags
            case _ => a.valueBox == b.valueBox
          })
        }
      case _ => false
    }
  }
}

/** Specialized MetaRecord that deals with BsonRecords */
trait BsonMetaRecord[BaseRecord <: BsonRecord[BaseRecord]] extends MetaRecord[BaseRecord] with JsonFormats with MongoCodecs {
  self: BaseRecord =>

  def codecRegistry: CodecRegistry = MongoRecordRules.defaultCodecRegistry.vend

  /**
   * The `BsonTypeClassMap` to use with this record.
   */
  def bsonTypeClassMap: BsonTypeClassMap = MongoRecordRules.defaultBsonTypeClassMap.vend
  def bsonTransformer: Transformer =  MongoRecordRules.defaultTransformer.vend

  def codec: RecordTypedCodec[BaseRecord] =
    RecordCodec(this, introspectedCodecRegistry, bsonTypeClassMap, bsonTransformer)

  /**
   * Check this record's fields and add any Codecs needed.
   */
  protected lazy val introspectedCodecRegistry: CodecRegistry = {
    val fields = metaFields()

    val codecs: List[Codec[_]] = fields.map { field => field match {
      case f: BsonRecordTypedField[BaseRecord, _] =>
        f.valueMeta.codec :: Nil
      case f: BsonRecordListField[BaseRecord, _] =>
        f.valueMeta.codec :: Nil
      case f: BsonRecordMapField[BaseRecord, _] =>
        f.valueMeta.codec :: Nil
      case _ =>
        Nil
    }}.flatten

    CodecRegistries.fromRegistries(
      CodecRegistries.fromCodecs(codecs.distinct.asJava),
      codecRegistry
    )
  }

  /**
    * Create a BasicDBObject from the field names and values.
    * - MongoFieldFlavor types (List) are converted to DBObjects
    *   using asDBObject
    */
  @deprecated("RecordCodec is now used instead.", "3.4.3")
  def asDBObject(inst: BaseRecord): DBObject = {
    val dbo = BasicDBObjectBuilder.start // use this so regex patterns can be stored.

    for {
      field <- fields(inst)
      dbValue <- fieldDbValue(field)
    } { dbo.add(field.name, dbValue) }

    dbo.get
  }

  @deprecated("RecordCodec is now used instead.", "3.4.3")
  def asDocument(inst: BaseRecord): Document = {
    val dbo = new Document()

    for {
      field <- fields(inst)
      dbValue <- fieldDbValue(field)
    } { dbo.append(field.name, dbValue) }

    dbo
  }

  /**
    * Return the value of a field suitable to be put in a DBObject
    */
  @deprecated("RecordCodec is now used instead.", "3.4.3")
  def fieldDbValue(f: Field[_, BaseRecord]): Box[Any] = {
    import Meta.Reflection._
    import field.MongoFieldFlavor

    f match {
      case field if (field.optional_? && field.valueBox.isEmpty) => Empty // don't add to DBObject
      case field: EnumTypedField[_] =>
        field.asInstanceOf[EnumTypedField[Enumeration]].valueBox map {
          v => v.id
        }
      case field: EnumNameTypedField[_] =>
        field.asInstanceOf[EnumNameTypedField[Enumeration]].valueBox map {
          v => v.toString
        }
      case field: MongoFieldFlavor[_] =>
        Full(field.asInstanceOf[MongoFieldFlavor[Any]].asDBObject)
      case field => field.valueBox map (_.asInstanceOf[AnyRef] match {
        case null => null
        case x if primitive_?(x.getClass) => x
        case x if mongotype_?(x.getClass) => x
        case x if datetype_?(x.getClass) => datetype2dbovalue(x)
        case x: BsonRecord[_] => x.asDBObject
        case x: Array[Byte] => x
        case o => o.toString
      })
    }
  }

  /**
    * Creates a new record, then sets the fields with the given DBObject.
    *
    * @param dbo - the DBObject
    * @return Box[BaseRecord]
    */
  @deprecated("RecordCodec is now used instead.", "3.4.3")
  def fromDBObject(dbo: DBObject): BaseRecord = {
    val inst: BaseRecord = createRecord
    setFieldsFromDBObject(inst, dbo)
    inst
  }

  /**
    * Populate the inst's fields with the values from a DBObject. Values are set
    * using setFromAny passing it the DBObject returned from Mongo.
    *
    * @param inst - the record that will be populated
    * @param dbo - The DBObject
    * @return Unit
    */
  @deprecated("RecordCodec is now used instead.", "3.4.3")
  def setFieldsFromDBObject(inst: BaseRecord, dbo: DBObject): Unit = {
    for (k <- dbo.keySet.asScala; field <- inst.fieldByName(k.toString)) {
      field.setFromAny(dbo.get(k.toString))
    }
    inst.runSafe {
      inst.fields.foreach(_.resetDirty)
    }
  }

  def setFieldsFromDocument(inst: BaseRecord, doc: Document): Unit = {
    for (k <- doc.keySet.asScala; field <- inst.fieldByName(k.toString)) {
      field.setFromAny(doc.get(k.toString))
    }
    inst.runSafe {
      inst.fields.foreach(_.resetDirty)
    }
  }

  def fromDocument(doc: Document): BaseRecord = {
    val inst: BaseRecord = createRecord
    setFieldsFromDocument(inst, doc)
    inst
  }

  def diff(inst: BaseRecord, other: BaseRecord): Seq[(String, Any, Any)] = {
    fields(inst).flatMap(field => {
      val otherValue = other.fieldByName(field.name).flatMap(_.valueBox)
      if (otherValue != field.valueBox) {
        Seq((field.name, field.valueBox, otherValue))
      } else {
        Seq.empty[(String, String, String)]
      }
    })
  }
}
