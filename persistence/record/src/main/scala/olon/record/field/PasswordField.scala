package olon
package record
package field

import scala.xml._

import common._
import json._
import util._
import Helpers._
import http.S
import http.js._
import S._
import JE._

import org.mindrot.jbcrypt.BCrypt

object PasswordField {
  @volatile var blankPw = "*******"
  @volatile var minPasswordLength = 5
  @volatile var logRounds = 10
  def hashpw(in: String): Box[String] =  tryo(BCrypt.hashpw(in, BCrypt.gensalt(logRounds))) 
}

trait PasswordTypedField extends TypedField[String] {
  private var invalidPw = false
  private var invalidMsg = ""

  def match_?(toTest: String): Boolean = 
	  valueBox.filter(_.length > 0)
	          .flatMap(p => tryo(BCrypt.checkpw(toTest, p)))
	          .openOr(false) 

  override def set_!(in: Box[String]): Box[String] = {
    // can't be hashed here, because this get's called when setting value from database (Squeryl)
    in
  }
  
  def setPlain(in: String): String = setBoxPlain(Full(in)) openOr defaultValue
  
  def setBoxPlain(in: Box[String]): Box[String] = {
    if(!validatePassword(in)) {
      val hashed = in.map(s => PasswordField.hashpw(s) openOr s)
      setBox(hashed)
    }
    else setBox(defaultValueBox)
  }

  /**
   * If passed value is an Array[String] or a List[String] containing 2 items with equal value, it it hashes this value and sets it as new password.
   * If passed value is a String or a Full[String] that starts with "$2a$", it assumes that it's a hashed version, thus sets it as it is, without hashing.
   * In any other case, it fails the validation with "Passwords do not match" error
   */
  def setFromAny(in: Any): Box[String] = {
    in match {
      case (a: Array[String]) if a.length == 2 && a(0) == a(1) => setBoxPlain(Full(a(0)))
      case (h1: String) :: (h2: String) :: Nil if h1 == h2 => setBoxPlain(Full(h1))
      case (hash: String) if hash.startsWith("$2a$") => setBox(Full(hash))
      case Full(hash: String) if hash.startsWith("$2a$") => setBox(Full(hash))
      case _ =>
        invalidPw = true
        invalidMsg = S.?("passwords.do.not.match")
        Failure(invalidMsg)
    }
  }

  def setFromString(s: String): Box[String] = s match {
    case null|"" if optional_? => setBoxPlain(Empty)
    case null|"" => setBoxPlain(Failure(notOptionalErrorMessage))
    case _ => setBoxPlain(Full(s))
  }

  override def validate: List[FieldError] = {
    if (!invalidPw && valueBox != defaultValueBox) Nil
    else if (invalidPw) List(FieldError(this, Text(invalidMsg)))
    else List(FieldError(this, Text(notOptionalErrorMessage)))
  }

  override def notOptionalErrorMessage = S.?("password.must.be.set")

  override def formInputType = "password"

  private def elem = S.fmapFunc(SFuncHolder(this.setFromAny(_))){
    funcName => <input type={formInputType}
      name={funcName}
      value={valueBox openOr ""}
      tabindex={tabIndex.toString}/>}

  def toForm: Box[NodeSeq] =
    uniqueFieldId match {
      case Full(id) => Full(elem % ("id" -> id))
      case _ => Full(elem)
    }

  protected def validatePassword(pwdValue: Box[String]): Boolean = {
    pwdValue match {
      case Empty|Full(""|null) if !optional_? => { invalidPw = true ; invalidMsg = notOptionalErrorMessage }
      case Full(s) if s == "" || s == PasswordField.blankPw || s.length < PasswordField.minPasswordLength => 
        { invalidPw = true ; invalidMsg = S.?("password.too.short") }
      case _ => { invalidPw = false; invalidMsg = "" }
    }
    invalidPw
  }

  def defaultValue = ""

  def asJs = valueBox.map(Str) openOr JsNull

  def asJValue: JValue = valueBox.map(v => JString(v)) openOr (JNothing: JValue)
  
  def setFromJValue(jvalue: JValue): Box[MyType] = jvalue match {
    case JNothing|JNull if optional_? => setBoxPlain(Empty)
    case JString(s)                   => setFromString(s)
    case other                        => setBoxPlain(FieldHelpers.expectedA("JString", other))
  }
  
  
}

class PasswordField[OwnerType <: Record[OwnerType]](@deprecatedName('rec) val owner: OwnerType)
  extends Field[String, OwnerType] with MandatoryTypedField[String] with PasswordTypedField {

  def this(@deprecatedName('rec) owner: OwnerType, value: String) = {
    this(owner)
    setPlain(value)
  }

  override def apply(in: Box[String]): OwnerType = 
    if(owner.meta.mutable_?) {
      this.setBoxPlain(in)
      owner
    } else {
      owner.meta.createWithMutableField(owner, this, in)
    }

}

class OptionalPasswordField[OwnerType <: Record[OwnerType]](@deprecatedName('rec) val owner: OwnerType)
  extends Field[String, OwnerType] with OptionalTypedField[String] with PasswordTypedField {

  def this(@deprecatedName('rec) owner: OwnerType, value: Box[String]) = {
    this(owner)
    setBoxPlain(value)
  }
  
  override def apply(in: Box[String]): OwnerType =
    if(owner.meta.mutable_?) {
      this.setBoxPlain(in)
      owner
    } else {
      owner.meta.createWithMutableField(owner, this, in)
    }
}

