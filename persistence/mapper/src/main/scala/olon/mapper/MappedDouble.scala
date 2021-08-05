package olon
package mapper

import java.sql.Types
import java.lang.reflect.Method
import olon.common._
import olon.util._
import java.util.Date
import olon.http._
import scala.xml.{Text, NodeSeq}
import js._
import olon.json._

abstract class MappedDouble[T<:Mapper[T]](val fieldOwner: T) extends MappedField[Double, T] {
	private var data: Double = defaultValue
	private var orgData: Double = defaultValue

	private def st(in: Double): Unit = {
		data = in
		orgData = in
	}

	def defaultValue: Double = 0.0
	def dbFieldClass: Class[Double] = classOf[Double]

	protected def i_is_! : Double = data
	protected def i_was_! : Double = orgData

	override def doneWithSave(): Unit = {
		orgData = data
	}

  import scala.reflect.runtime.universe._
  def manifest: TypeTag[Double] = typeTag[Double]

  /**
   * Get the source field metadata for the field
   * @return the source field metadata for the field
   */
  def sourceInfoMetadata(): SourceFieldMetadata{type ST = Double} =
    SourceFieldMetadataRep(name, manifest, new FieldConverter {
      /**
       * The type of the field
       */
      type T = Double

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
      def asNodeSeq(v: T): Box[NodeSeq] = Full(Text(asString(v)))

      /**
       * Convert the field into a JSON value
       * @param v the field value
       * @return the JSON representation of the field
       */
      def asJson(v: T): Box[JValue] = Full(JsonAST.JDouble(v))

      /**
       * If the field can represent a sequence of SourceFields,
       * get that
       * @param v the field value
       * @return the field as a sequence of SourceFields
       */
      def asSeq(v: T): Box[Seq[SourceFieldInfo]] = Empty
    })

	def toDouble(in: Any): Double = {
		in match {
			case null => 0.0
			case i: Int => i
			case n: Long => n.toDouble
			case n : Number => n.doubleValue
			case (n: Number) :: _ => n.doubleValue
            case Full(n) => toDouble(n) // fixes issue 185
            case _: EmptyBox => 0.0
			case Some(n) => toDouble(n)
			case None => 0.0
			case s: String => s.toDouble
			case x :: _ => toDouble(x)
			case o => toDouble(o.toString)
		}
	}

	override def readPermission_? = true
	override def writePermission_? = true

	protected def i_obscure_!(in : Double): Double = defaultValue

	protected def real_i_set_!(value : Double): Double = {
		if (value != data) {
			data = value
			dirty_?(true)
		}
		data
	}

	def asJsExp: JsExp = JE.Num(get)

  def asJsonValue: Box[JsonAST.JValue] = Full(JsonAST.JDouble(get))

	override def setFromAny(in: Any): Double = {
		in match {
		  case JsonAST.JDouble(db) => this.set(db)
		  case JsonAST.JInt(bi) => this.set(bi.doubleValue)
			case n: Double => this.set(n)
			case n: Number => this.set(n.doubleValue)
			case (n: Number) :: _ => this.set(n.doubleValue)
			case Some(n: Number) => this.set(n.doubleValue)
			case None => this.set(0.0)
			case (s: String) :: _ => this.set(toDouble(s))
			case null => this.set(0L)
			case s: String => this.set(toDouble(s))
			case o => this.set(toDouble(o))
		}
	}

	def real_convertToJDBCFriendly(value: Double): Object = new java.lang.Double(value)

	/**
	* Get the JDBC SQL Type for this field
	*/
	def targetSQLType: Int = Types.DOUBLE
	def jdbcFriendly(field : String) = new java.lang.Double(i_is_!)
	def buildSetBooleanValue(accessor : Method, columnName : String) : (T, Boolean, Boolean) => Unit = null
	def buildSetDateValue(accessor : Method, columnName : String) : (T, Date) => Unit =
		(inst, v) => doField(inst, accessor, {case f: MappedDouble[T] => f.st(if (v == null) defaultValue else v.getTime.toDouble)})

	def buildSetStringValue(accessor: Method, columnName: String): (T, String) =>
		Unit = (inst, v) => doField(inst, accessor, {case f: MappedDouble[T] => f.st(toDouble(v))})

	def buildSetLongValue(accessor: Method, columnName : String) : (T, Long, Boolean) =>
		Unit = (inst, v, isNull) => doField(inst, accessor, {case f: MappedDouble[T] => f.st(if (isNull) defaultValue else v.toDouble)})

	def buildSetActualValue(accessor: Method, data: AnyRef, columnName: String) : (T, AnyRef) =>
		Unit = (inst, v) => doField(inst, accessor, {case f: MappedDouble[T] => f.st(toDouble(v))})

	def fieldCreatorString(dbType: DriverType, colName: String): String = colName + " " + dbType.doubleColumnType + notNullAppender()
}

