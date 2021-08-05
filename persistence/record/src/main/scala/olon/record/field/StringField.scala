package olon
package record
package field

import scala.xml._

import common._
import json._
import util._
import Helpers._
import http.js._
import http.S
import S._
import JE._


trait StringTypedField extends TypedField[String] with StringValidators {
  val maxLength: Int

  def maxLen = maxLength
  
  def setFromAny(in: Any): Box[String] = in match {
    case seq: Seq[_] if seq.nonEmpty => setFromAny(seq.head)
    case _ => genericSetFromAny(in)
  }

  def setFromString(s: String): Box[String] = s match {
    case null|"" if optional_? => setBox(Empty)
    case null|"" => setBox(Failure(notOptionalErrorMessage))
    case _ => setBox(Full(s))
  }

  private def elem = S.fmapFunc(SFuncHolder(this.setFromAny(_))) {
    funcName =>
    <input type={formInputType} maxlength={maxLength.toString}
      name={funcName}
      value={valueBox openOr ""}
      tabindex={tabIndex.toString}/>
  }

  def toForm: Box[NodeSeq] =
    uniqueFieldId match {
      case Full(id) => Full(elem % ("id" -> id))
      case _ => Full(elem)
    }


  def defaultValue = ""

  def asJs = valueBox.map(Str) openOr JsNull

  def asJValue: JValue = valueBox.map(v => JString(v)) openOr (JNothing: JValue)
  def setFromJValue(jvalue: JValue): Box[MyType] = jvalue match {
    case JNothing|JNull if optional_? => setBox(Empty)
    case JString(s)                   => setFromString(s)
    case other                        => setBox(FieldHelpers.expectedA("JString", other))
  }
}

class StringField[OwnerType <: Record[OwnerType]](@deprecatedName('rec) val owner: OwnerType, val maxLength: Int)
  extends Field[String, OwnerType] with MandatoryTypedField[String] with StringTypedField {

  def this(@deprecatedName('rec) owner: OwnerType, maxLength: Int, value: String) = {
    this(owner, maxLength)
    set(value)
  }

  def this(@deprecatedName('rec) owner: OwnerType, value: String) = {
    this(owner, 100)
    set(value)
  }

  protected def valueTypeToBoxString(in: ValueType): Box[String] = toBoxMyType(in)
  protected def boxStrToValType(in: Box[String]): ValueType = toValueType(in)
}

abstract class UniqueIdField[OwnerType <: Record[OwnerType]](rec: OwnerType, override val maxLength: Int) extends StringField[OwnerType](rec, maxLength) {
  override lazy val defaultValue = randomString(maxLen)

  def reset(): OwnerType = this(randomString(maxLen))
}


class OptionalStringField[OwnerType <: Record[OwnerType]](@deprecatedName('rec) val owner: OwnerType, val maxLength: Int)
  extends Field[String, OwnerType] with OptionalTypedField[String] with StringTypedField {

  def this(@deprecatedName('rec) owner: OwnerType, maxLength: Int, value: Box[String]) = {
    this(owner, maxLength)
    setBox(value)
  }

  def this(@deprecatedName('rec) owner: OwnerType, value: Box[String]) = {
    this(owner, 100)
    setBox(value)
  }

  protected def valueTypeToBoxString(in: ValueType): Box[String] = toBoxMyType(in)
  protected def boxStrToValType(in: Box[String]): ValueType = toValueType(in)
}

