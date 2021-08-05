package olon
package mapper

import common._
import util._
import Helpers._
import http.{S, SHtml}

import scala.xml.NodeSeq

abstract class MappedUniqueId[T<:Mapper[T]](override val fieldOwner: T, override val maxLen: Int) extends MappedString[T](fieldOwner, maxLen) {
  override def writePermission_? = false
  override lazy val defaultValue = randomString(maxLen)

  def reset(): T = this(randomString(maxLen))
}

/**
  * A field that holds the birth year for the user
  */
abstract class MappedBirthYear[T <: Mapper[T]](owner: T, minAge: Int) extends MappedInt[T](owner) {
  override def defaultValue = year(now) - minAge

  override def _toForm: Box[NodeSeq] = {
    val end = (year(now) - minAge)
    val start = end - 100
    Full(SHtml.selectObj((start to end).
		  toList.
		  reverse.
		  map(y => (y, y.toString)),
		  Full(get), this.set) % ("id" -> fieldId))
  }
}

abstract class MappedGender[T <: Mapper[T]](owner: T) extends MappedEnum(owner, Genders) {
  override def defaultValue = Genders.Male
}

object Genders extends Enumeration {

  val Male = new I18NGender(1, "male")
  val Female = new I18NGender(2, "female")

  class I18NGender(id : Int, name: String) extends Val(id, name) {
    override def toString = {
      S.?(name)
    }
  }
}

abstract class MappedStringIndex[T<:Mapper[T]](override val fieldOwner: T, override val maxLen: Int) extends MappedUniqueId[T](fieldOwner, maxLen) with IndexedField[String] {

  override def writePermission_? = false // not writable

  override def dbIndexed_? = true

  def defined_? = i_is_! ne null

  override def dbPrimaryKey_? = true

  override def dbDisplay_? = false

  def makeKeyJDBCFriendly(in: String) = in

  def convertKey(in: String): Box[String] = Box.legacyNullTest(in)
  def convertKey(in: Int): Box[String] = Full(in.toString)
  def convertKey(in: Long): Box[String] = Full(in.toString)
  def convertKey(in: AnyRef): Box[String] =
    Box.legacyNullTest(in).map(_.toString)
}


