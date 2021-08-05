package olon
package mapper

import java.sql.Types
import java.lang.reflect.Method
import java.util.Date

import util._
import common.{Box, Full, Empty, Failure}
import http.S
import http.js._
import json._
import S._

import scala.xml.{NodeSeq, Text, Elem}

/**
 * Just like MappedString, except it's defaultValue is "" and the length is auto-cropped to
 * fit in the column
 */
abstract class MappedPoliteString[T <: Mapper[T]](towner: T, theMaxLen: Int) extends MappedString[T](towner, theMaxLen) {
  override def defaultValue = ""
  override def setFilter = crop _ :: super.setFilter
}

/**
 * Mix this trait into a MappedString and it will add maximum length validation to the MappedString
 */
trait ValidateLength extends MixableMappedField {
  self: MappedString[_] =>

  def defaultErrorMessage = S.?("Field too long.  Maximum Length")+": "+maxLen

  abstract override def validations = valMaxLen(maxLen, defaultErrorMessage) _ :: super.validations

}

trait HasApplyBoxString[T] {
  def apply(x: String): T
}
abstract class MappedString[T<:Mapper[T]](val fieldOwner: T,val maxLen: Int) extends MappedField[String, T] with olon.util.StringValidators with HasApplyBoxString[T] {
  private val data: FatLazy[String] =  FatLazy(defaultValue) // defaultValue
  private val orgData: FatLazy[String] =  FatLazy(defaultValue) // defaultValue

  def dbFieldClass: Class[String] = classOf[String]

  import scala.reflect.runtime.universe._
  def manifest: TypeTag[String] = typeTag[String]

  /**
   * Get the source field metadata for the field
   * @return the source field metadata for the field
   */
  def sourceInfoMetadata(): SourceFieldMetadata{type ST = String} =
    SourceFieldMetadataRep(name, manifest, new FieldConverter {
      /**
       * The type of the field
       */
      type T = String

      /**
       * Convert the field to a String
       * @param v the field value
       * @return the string representation of the field value
       */
      def asString(v: T): String = v

      /**
       * Convert the field into NodeSeq, if possible
       * @param v the field value
       * @return a NodeSeq if the field can be represented as one
       */
      def asNodeSeq(v: T): Box[NodeSeq] = Full(Text(v))

      /**
       * Convert the field into a JSON value
       * @param v the field value
       * @return the JSON representation of the field
       */
      def asJson(v: T): Box[JValue] = Full(JsonAST.JString(v))

      /**
       * If the field can represent a sequence of SourceFields,
       * get that
       * @param v the field value
       * @return the field as a sequence of SourceFields
       */
      def asSeq(v: T): Box[Seq[SourceFieldInfo]] = Empty
    })

  protected def valueTypeToBoxString(in: String): Box[String] = Full(in)
  protected def boxStrToValType(in: Box[String]): String = in openOr ""
  

  protected def real_i_set_!(value : String) : String = {
    if (!data.defined_? || value != data.get) {
      data() = value
      this.dirty_?( true)
    }
    data.get
  }

  /**
   * Get the JDBC SQL Type for this field
   */
  def targetSQLType: Int = Types.VARCHAR

  def defaultValue = ""

  override def writePermission_? = true
  override def readPermission_? = true

  protected def i_is_! : String = data.get
  protected def i_was_! : String = orgData.get

  def asJsonValue: Box[JsonAST.JValue] = Full(get match {
    case null => JsonAST.JNull
    case str => JsonAST.JString(str)
  })

  /**
   * Called after the field is saved to the database
   */
  override protected[mapper] def doneWithSave(): Unit = {
    orgData.setFrom(data)
  }

  override def _toForm: Box[Elem] =
  fmapFunc({s: List[String] => this.setFromAny(s)}){name =>
    Full(appendFieldId(<input type={formInputType} maxlength={maxLen.toString}
                       name={name}
                       value={get match {case null => "" case s => s.toString}}/>))}

  protected def i_obscure_!(in : String) : String = {
    ""
  }

  override def toForm: Box[Elem] = {

    super.toForm match {
      case Full(IsElem(elem)) => Full(elem)
      case _ =>
        Empty
    }
  }

  override def setFromAny(in: Any): String = {
    in match {
      case JsonAST.JNull => this.set(null) 
      case seq: Seq[_] if seq.nonEmpty => seq.map(setFromAny).head
      case (s: String) :: _ => this.set(s)
      case s :: _ => this.setFromAny(s)
      case JsonAST.JString(v) => this.set(v)
      case null => this.set(null)
      case s: String => this.set(s)
      case Some(s: String) => this.set(s)
      case Full(s: String) => this.set(s)
      case None | Empty | Failure(_, _, _) => this.set(null)
      case o => this.set(o.toString)
    }
  }

  override def apply(v: String): T = super.apply(v)

  def asJsExp: JsExp = JE.Str(get)

  def jdbcFriendly(field : String): String = data.get

  def real_convertToJDBCFriendly(value: String): Object = value

  private def wholeSet(in: String): Unit = {
    this.data() = in
    this.orgData() = in
  }

  def buildSetActualValue(accessor: Method, inst: AnyRef, columnName: String): (T, AnyRef) => Unit =
  (inst, v) => doField(inst, accessor, {case f: MappedString[T] => f.wholeSet(if (v eq null) null else v.toString)})

  def buildSetLongValue(accessor: Method, columnName: String): (T, Long, Boolean) => Unit =
  (inst, v, isNull) => doField(inst, accessor, {case f: MappedString[T] => f.wholeSet(if (isNull) null else v.toString)})

  def buildSetStringValue(accessor: Method, columnName: String): (T, String) => Unit =
  (inst, v) => doField(inst, accessor, {case f: MappedString[T] => f.wholeSet(if (v eq null) null else v)})

  def buildSetDateValue(accessor: Method, columnName: String): (T, Date) => Unit =
  (inst, v) => doField(inst, accessor, {case f: MappedString[T] => f.wholeSet(if (v eq null) null else v.toString)})

  def buildSetBooleanValue(accessor: Method, columnName: String): (T, Boolean, Boolean) => Unit =
  (inst, v, isNull) => doField(inst, accessor, {case f: MappedString[T] => f.wholeSet(if (isNull) null else v.toString)})



  /**
   * Make sure that the field is unique in the database
   */
  def valUnique(msg: => String)(value: String): List[FieldError] =
  fieldOwner.getSingleton.findAll(By(this,value)).
  filter(!_.comparePrimaryKeys(this.fieldOwner)) match {
    case Nil => Nil
    case x :: _ => List(FieldError(this, Text(msg))) // issue 179
  }


  /**
   * Given the driver type, return the string required to create the column in the database
   */
  def fieldCreatorString(dbType: DriverType, colName: String): String = colName+" "+dbType.varcharColumnType(maxLen) + notNullAppender()

}

private[mapper] object IsElem {
  def unapply(in: NodeSeq): Option[Elem] = in match {
    case e: Elem => Some(e)
    case Seq(e: Elem) => Some(e)
    case _ => None
  }
}

sealed trait BoxedStringToken
object BoxedStringToken {
  implicit val theBoxedStringToken: BoxedStringToken = new BoxedStringToken {}
}
