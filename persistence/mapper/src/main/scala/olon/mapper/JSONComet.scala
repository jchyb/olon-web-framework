package olon
package mapper

import olon.common._
import olon.util._
import Helpers._
import olon.http.js._
import JsCmds._
import JE._
import olon.http._
import scala.xml.NodeSeq

/**
* Allows for the creation of JSON-based JavaScript widgets with very little
* code
*/
/*
trait JsonComet {
  self: CometActor =>

  val keyStore = new KeyObfuscator

  trait JsonBridge[KeyType, FieldType, KMType <: KeyedMapper[KeyType, KMType]] {
    def meta: KeyedMetaMapper[KeyType, KMType]

    def field: MappedField[FieldType, KMType]

    val FieldId = Helpers.nextFuncName
    val handler: PartialFunction[Any, JsCmd] = {
      case JsonCmd(FieldId, target, value, _) =>
      (for (key <- keyStore.recover(meta, target);
      obj <- meta.find(key);
      cannedNewValue <- Box(cvt)(value);
      newValue <- cannedNewValue
      ) yield {
        val record = meta.getActualField(obj, field)(newValue)
        record.validate match {
          case Nil => record.save // FIXME notice updated
          Noop
          case xs => // FIXME display errors
          Noop
        }
      }) openOr Noop
    }

    def theCall(value: JsExp) = jsonCall(FieldId, JsVar("it", meta.primaryKeyField.name), value)


    def cvt: PartialFunction[Any, Box[FieldType]]

    self.appendJsonHandler(handler)
  }

  class JxCheckbox[KeyType, KMType <: KeyedMapper[KeyType, KMType]](val meta: KeyedMetaMapper[KeyType, KMType],
  val field: MappedField[Boolean, KMType]) extends JxNodeBase with JsonBridge[KeyType, Boolean, KMType] {

    def child = Nil

    def appendToParent(parentName: String) = {
      (renderExp).appendToParent(parentName)
    }

    def renderExp: JsExp = (Jx(buildCheckbox).toJs ~> JsFunc("apply", JsRaw("null"), JsRaw("[it]")))

    def buildCheckbox = <input type="checkbox" onclick={AnonFunc(theCall(JsRaw("this.checked")))}
    defaultChecked={JsVar("it", field.name)} />

    def cvt: PartialFunction[Any, Box[Boolean]] = {
      case b: Boolean => Full(b)
      case "on" => Full(true)
      case "off" => Full(false)
      case x => Full(toBoolean(x))
    }
  }

  class JxTextfield[KeyType, KMType <: KeyedMapper[KeyType, KMType]](val meta: KeyedMetaMapper[KeyType, KMType],
  val field: MappedField[String, KMType]) extends JxNodeBase with JsonBridge[KeyType, String, KMType] {

    def child = Nil

    def appendToParent(parentName: String) = {
      (renderExp).appendToParent(parentName)
    }

    def renderExp: JsExp = Jx(buildInput).toJs ~> JsFunc("apply", JsRaw("null"), JsRaw("[it]"))

    def buildInput: NodeSeq = <input type="text" onblur={AnonFunc(onBlurCmd)}
    value={JsVar("it", field.name)} />

    def onBlurCmd: JsCmd = theCall(JsRaw("this.value"))

    def cvt: PartialFunction[Any, Box[String]] = {
      case null => Empty
      case x => Full(x.toString)
    }
  }

  abstract class JxSelect[KeyType, FieldType, KMType <: KeyedMapper[KeyType, KMType]](val meta: KeyedMetaMapper[KeyType, KMType],
  val field: MappedField[FieldType, KMType], val enum: List[(String, FieldType)]) extends JxNodeBase with JsonBridge[KeyType, FieldType, KMType] {

    def child = Nil

    def appendToParent(parentName: String) = {
      (renderExp).appendToParent(parentName)
    }

    def renderExp: JsExp = Jx(buildInput).toJs ~> JsFunc("apply", JsRaw("null"), JsRaw("[it]"))

    def buildInput: NodeSeq = <select onchange={AnonFunc(onChangeCmd)}>
    {
      values.map(v => buildLine(v))
    }
    </select>

    def buildLine(v: (String, FieldType)) =
    JxIfElse(JsRaw("it."+field.name+" == "+v._2),
    <option selected="true" value={v._2.toString}>{v._1}</option>,
    <option value={v._2.toString}>{v._1}</option> )

    def onChangeCmd: JsCmd = theCall(JsRaw("this.options[this.selectedIndex].value")) & JsRaw("this.blur()")


    def values: List[(String, FieldType)] = enum
  }

  abstract class JxBuiltSelect[KeyType, FieldType, KMType <: KeyedMapper[KeyType, KMType]](val meta: KeyedMetaMapper[KeyType, KMType],
  val field: MappedField[FieldType, KMType]) extends JxNodeBase with JsonBridge[KeyType, FieldType, KMType] {

    def child = Nil

    def appendToParent(parentName: String) = {
      (renderExp).appendToParent(parentName)
    }

    def renderExp: JsExp = Jx(buildInput).toJs ~> JsFunc("apply", JsRaw("null"), JsRaw("[it]"))

    /**
    * A JavaScript expression that builds an array of Name, Value pairs for valid
    * select box stuff
    */
    def buildMapList: JsExp

    def buildInput: NodeSeq = <select onchange={AnonFunc(onChangeCmd)}>
    {
      JxCmd(JsCrVar("current", JsRaw("it"))) ++
      JxMap(buildMapList, buildLine)
    }
    </select>

    def buildLine =
    Jx(JxIfElse(JsRaw("current."+field.name+" == it[1]"),
    <option selected="true" value={JsRaw("it[1]").toJsCmd}>{JsRaw("it[0]")}</option>,
    <option value={JsRaw("it[1]").toJsCmd}>{JsRaw("it[0]")}</option> ))

    def onChangeCmd: JsCmd = theCall(JsRaw("this.options[this.selectedIndex].value")) & JsRaw("this.blur()")

  }

  class JxEnumSelect[KeyType, Enum <: Enumeration, KMType <: KeyedMapper[KeyType, KMType]](val meta: KeyedMetaMapper[KeyType, KMType],
  val field: MappedEnum[KMType, Enum], val enum: Enum) extends JxNodeBase with JsonBridge[KeyType, Enum#Value, KMType] {

    def child = Nil

    def appendToParent(parentName: String) = {
      (renderExp).appendToParent(parentName)
    }

    def renderExp: JsExp = Jx(buildInput).toJs ~> JsFunc("apply", JsRaw("null"), JsRaw("[it]"))

    def buildInput: NodeSeq = <select onchange={AnonFunc(onChangeCmd)}>
    {
      values.map(v => buildLine(v))
    }
    </select>

    def buildLine(v: Enum#Value) =
    JxIfElse(JsRaw("it."+field.name+" == "+v.id),
    <option selected="true" value={v.id}>{v.toString}</option>,
    <option value={v.id}>{v.toString}</option> )

    def onChangeCmd: JsCmd = theCall(JsRaw("this.options[this.selectedIndex].value")) & JsRaw("this.blur()")

    def cvt: PartialFunction[Any, Box[Enum#Value]] = {
      case null => Empty
      case x: Int => tryo(enum(x))
      case x: String => tryo(x.toInt).flatMap(i => tryo(enum(i)))
      case _ => Empty
    }

    def values: List[Enum#Value] = enum.iterator.toList
  }

}
*/
