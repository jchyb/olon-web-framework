package olon
package mapper

import java.sql.Types
import java.lang.reflect.Method
import olon.util.Helpers._
import olon.http.{S, SHtml}
import java.util.Date
import olon.util._
import olon.common._
import olon.json._
import olon.http.js._
import scala.xml._
import scala.reflect.runtime.universe._
import json.JsonAST.JValue

abstract class MappedBoolean[T<:Mapper[T]](val fieldOwner: T) extends MappedField[Boolean, T] {
  private var data : Box[Boolean] = Full(defaultValue)
  private var orgData: Box[Boolean] = Full(defaultValue)

  def defaultValue: Boolean = false

  def dbFieldClass = classOf[Boolean]

  /**
   * Get the JDBC SQL Type for this field
   */
  def targetSQLType = Types.BOOLEAN


  def manifest: TypeTag[Boolean] = typeTag[Boolean]

  /**
   * Get the source field metadata for the field
   * @return the source field metadata for the field
   */
  def sourceInfoMetadata(): SourceFieldMetadata{type ST = Boolean} =
    SourceFieldMetadataRep(name, manifest, new FieldConverter {
      /**
       * The type of the field
       */
      type T = Boolean

      /**
       * Convert the field to a String
       * @param v the field value
       * @return the string representation of the field value
       */
      def asString(v: T): String = v.toString

      /**
       * Convert the field into NodeSeq, if possible
       * @param v the field value
       * @return a NodeSeq if the field can be represented as one
       */
      def asNodeSeq(v: T): Box[NodeSeq] = Full(Text(v.toString))

      /**
       * Convert the field into a JSON value
       * @param v the field value
       * @return the JSON representation of the field
       */
      def asJson(v: T): Box[JValue] = Full(JBool(v))

      /**
       * If the field can represent a sequence of SourceFields,
       * get that
       * @param v the field value
       * @return the field as a sequence of SourceFields
       */
      def asSeq(v: T): Box[Seq[SourceFieldInfo]] = Empty
    })


  protected def i_is_! : Boolean = data openOr false
  protected def i_was_! : Boolean = orgData openOr false
  protected[mapper] def doneWithSave(): Unit = {orgData = data}

  protected def real_i_set_!(value : Boolean) : Boolean = {
    val boxed = Full(value)
    if (boxed != data) {
      data = boxed
      dirty_?(true)
    }
    value
  }
  override def readPermission_? = true
  override def writePermission_? = true

  def asJsonValue: Box[JsonAST.JValue] = Full(JsonAST.JBool(get))

  def real_convertToJDBCFriendly(value: Boolean): Object = new java.lang.Integer(if (value) 1 else 0)

  def jdbcFriendly(field : String) = data.map(v => new java.lang.Integer(if(v) 1 else 0)) openOr null

  def asJsExp: JsExp = if (get) JE.JsTrue else JE.JsFalse

  override def setFromAny(in: Any): Boolean = {
    in match {
      case b: Boolean => this.set(b)
      case JsonAST.JBool(v) => this.set(v)
      case (b: Boolean) :: _ => this.set(b)
      case Some(b: Boolean) => this.set(b)
      case Full(b: Boolean) => this.set(b)
      case Empty | Failure(_, _, _) | None => this.set(false)
      case (s: String) :: _ => this.set(toBoolean(s))
      case s :: _ => this.setFromAny(s)
      case null => this.set(false)
      case s: String => this.set(toBoolean(s))
      case o => this.set(toBoolean(o))
    }
  }

  protected def i_obscure_!(in : Boolean) = false

  def buildSetActualValue(accessor : Method, inst : AnyRef, columnName : String) : (T, AnyRef) => Unit = {
    inst match {
      case null => {(inst : T, v : AnyRef) => {val tv = getField(inst, accessor).asInstanceOf[MappedBoolean[T]]; tv.data = Full(false)}}
      case _ => {(inst : T, v : AnyRef) => {val tv = getField(inst, accessor).asInstanceOf[MappedBoolean[T]]; tv.data = Full(toBoolean(v))}}
    }
  }

  private def allSet(in: Box[Boolean]): Unit = {
    this.data = in
    this.orgData = in
  }

  def buildSetLongValue(accessor : Method, columnName : String): (T, Long, Boolean) => Unit =
    (inst, v, isNull) => doField(inst, accessor, {case tv: MappedBoolean[T] => tv.allSet(if (isNull) Empty else Full(v != 0L))})

  def buildSetStringValue(accessor : Method, columnName : String): (T, String) => Unit =
    (inst, v) => doField(inst, accessor, {case tv: MappedBoolean[T] => tv.allSet(if (v == null) Empty else Full(toBoolean(v)))})

  def buildSetDateValue(accessor: Method, columnName: String): (T, Date) => Unit =
    (inst, v) => doField(inst, accessor, {case tv: MappedBoolean[T] => tv.allSet(if (v == null) Empty else Full(true))})

  def buildSetBooleanValue(accessor: Method, columnName : String) : (T, Boolean, Boolean) => Unit   =
    (inst, v, isNull) => doField(inst, accessor, {case tv: MappedBoolean[T] => tv.allSet(if (isNull) Empty else Full(v))})


  /**
   * Given the driver type, return the string required to create the column in the database
   */
  def fieldCreatorString(dbType: DriverType, colName: String): String = colName + " " + dbType.booleanColumnType + notNullAppender()


  /**
   * Create an input field for the item
   */
  override def _toForm: Box[NodeSeq] = Full(SHtml.checkbox(get,this.apply _))
}

