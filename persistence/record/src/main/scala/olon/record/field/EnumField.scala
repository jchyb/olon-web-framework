package olon
package record
package field

import reflect.Manifest
import scala.xml._

import common._
import Box.option2Box
import json._
import util._
import Helpers._
import http.js._
import http.{S, SHtml}
import S._
import JE._


trait EnumTypedField[EnumType <: Enumeration] extends TypedField[EnumType#Value] {
  protected val enum: EnumType
  protected val valueManifest: Manifest[EnumType#Value]

  def toInt: Box[Int] = valueBox.map(_.id)

  def fromInt(in: Int): Box[EnumType#Value] = tryo(enum(in))

  def setFromAny(in: Any): Box[EnumType#Value] = in match {
    case     (value: Int)    => setFromInt(value)
    case Some(value: Int)    => setFromInt(value)
    case Full(value: Int)    => setFromInt(value)
    case (value: Int)::_     => setFromInt(value)
    case     (value: Number) => setFromInt(value.intValue)
    case Some(value: Number) => setFromInt(value.intValue)
    case Full(value: Number) => setFromInt(value.intValue)
    case (value: Number)::_  => setFromInt(value.intValue)
    case _                   => genericSetFromAny(in)(valueManifest)
  }

  def setFromString(s: String): Box[EnumType#Value] =
    if(s == null || s.isEmpty) {
      if(optional_?)
    	  setBox(Empty)
       else
          setBox(Failure(notOptionalErrorMessage))
    } else {
      setBox(asInt(s).flatMap(fromInt))
    }

  def setFromInt(in: Int): Box[EnumType#Value] = setBox(fromInt(in))

  /** Label for the selection item representing Empty, show when this field is optional. Defaults to the empty string. */
  def emptyOptionLabel: String = ""

  /**
   * Build a list of (value, label) options for a select list.  Return a tuple of (Box[String], String) where the first string
   * is the value of the field and the second string is the Text name of the Value.
   */
  def buildDisplayList: List[(Box[EnumType#Value], String)] = {
    val options = enum.values.toList.map(a => (Full(a), a.toString))
    if (optional_?) (Empty, emptyOptionLabel)::options else options
  }


  private def elem = SHtml.selectObj[Box[EnumType#Value]](buildDisplayList, Full(valueBox), setBox(_)) % ("tabindex" -> tabIndex.toString)

  def toForm: Box[NodeSeq]  =
    uniqueFieldId match {
      case Full(id) => Full(elem % ("id" -> id))
      case _ => Full(elem)
    }

  def defaultValue: EnumType#Value = enum.values.iterator.next

  def asJs = valueBox.map(_ => Str(toString)) openOr JsNull

  def asJIntOrdinal: JValue = toInt.map(i => JInt(BigInt(i))) openOr (JNothing: JValue)
  def setFromJIntOrdinal(jvalue: JValue): Box[EnumType#Value] = jvalue match {
    case JNothing|JNull if optional_? => setBox(Empty)
    case JInt(i)                      => setBox(fromInt(i.intValue))
    case other                        => setBox(FieldHelpers.expectedA("JInt", other))
  }

  def asJStringName: JValue = valueBox.map(v => JString(v.toString)) openOr (JNothing: JValue)
  def setFromJStringName(jvalue: JValue): Box[EnumType#Value] = jvalue match {
    case JNothing|JNull if optional_? => setBox(Empty)
    case JString(s)                   => setBox(Option(enum.withName(s)) ?~ ("Unknown value \"" + s + "\""))
    case other                        => setBox(FieldHelpers.expectedA("JString", other))
  }

  def asJValue: JValue = asJIntOrdinal
  def setFromJValue(jvalue: JValue): Box[EnumType#Value] = setFromJIntOrdinal(jvalue)
}

class EnumField[OwnerType <: Record[OwnerType], EnumType <: Enumeration](@deprecatedName('rec) val owner: OwnerType,
  protected val enum: EnumType)(implicit m: Manifest[EnumType#Value]
) extends Field[EnumType#Value, OwnerType] with MandatoryTypedField[EnumType#Value] with EnumTypedField[EnumType] {

  def this(@deprecatedName('rec) owner: OwnerType, enum: EnumType, value: EnumType#Value)(implicit m: Manifest[EnumType#Value]) = {
    this(owner, enum)
    set(value)
  }

  protected val valueManifest = m
}

class OptionalEnumField[OwnerType <: Record[OwnerType], EnumType <: Enumeration](@deprecatedName('rec) val owner: OwnerType,
  protected val enum: EnumType)(implicit m: Manifest[EnumType#Value]
) extends Field[EnumType#Value, OwnerType] with OptionalTypedField[EnumType#Value] with EnumTypedField[EnumType] {

  def this(@deprecatedName('rec) owner: OwnerType, enum: EnumType, value: Box[EnumType#Value])(implicit m: Manifest[EnumType#Value]) = {
    this(owner, enum)
    setBox(value)
  }

  protected val valueManifest = m
}

