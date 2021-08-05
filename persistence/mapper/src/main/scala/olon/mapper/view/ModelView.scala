package olon 
package mapper 
package view 

import http.{S, StatefulSnippet}
import S.?
import util.CssSel
import util.Helpers._

import scala.xml.{NodeSeq, Text}


/**
 * A snippet that can list and edit items of a particular Mapper class
 * This trait can help reduce boilerplate in the common scenario where
 * you want a snippet class to provide list and edit snippets for a
 * specific Mapper class.
 * @author nafg
 */
trait ModelSnippet[T <: Mapper[T]] extends StatefulSnippet {
  import mapper.view.{ModelView => MV}
  class ModelView(e: T, snippet: ModelSnippet[T]) extends MV[T](e, snippet) {
    def this(e: T) = {
      this(e, this)
    }
  }
  /**
   * The instance of ModelView that wraps the currently loaded entity
   */
  val view: MV[T]

  /**
   * Action when save is successful. Defaults to using the ModelView's redirectOnSave
   */
  var onSave: MV[T] => Unit = (view: MV[T]) => {
    view.redirectOnSave.foreach(redirectTo)
  }

  /**
   * The list snippet
   */
  def list(ns: NodeSeq): NodeSeq
  /**
   * The edit snippet
   */
  def edit(ns: NodeSeq): NodeSeq
  
  def load(entity: T): Unit = view.entity = entity

  def dispatch: DispatchIt = {
    case "list" =>       list _
    case "edit" =>       edit _
    case "newOrEdit" =>  view.newOrEdit
  }
  
  /**
   * A ".edit" CssSel
  */
  def editAction(e: T): CssSel = ".edit" #> link("edit", ()=>load(e), Text(?("Edit")))
  /**
   * A ".remove" CssSel
  */
  def removeAction(e: T): CssSel = ".remove" #> link("list", ()=>e.delete_!, Text(?("Remove")))
}


/**
 * A wrapper around a Mapper that provides view-related utilities. Belongs to a parent ModelSnippet.
 * @author nafg
 */
class ModelView[T <: Mapper[T]](var entity: T, val snippet: ModelSnippet[T]) {
  /**
   * If Some(string), will redirect to string on a successful save.
   * If None, will load the same page.
   * Defaults to Some("list").
   * This var is used by ModelSnippet.onSave, which is a ModelView=>Unit
   */
  var redirectOnSave: Option[String] = Some("list")
  
  /**
   * Loads this entity into the snippet so it can be edited 
   */
  def load(): Unit = snippet.load(entity)
  
  /**
   * Delete the entity
   */
  def remove: Boolean =
    entity.delete_!
  /**
   * This function is used as a snippet in the edit view
   * to provide alternate text depending on whether an
   * existing entity is being edited or a new one is being
   * created.
   */
  def newOrEdit: CssSel = {
    if (entity.saved_?)
      ".edit ^^" #> "ignored"
    else
      ".new ^^" #> "ignored"
  }
  
  /**
   * This method checks whether the entity
   * validates; if so it saves it, and if
   * successful redirects to the location
   * specified by redirectOnSave, if any.
   * If save or validation fails, the
   * appropriate message(s) is/are displayed
   * and no redirect is performed.
   */
  def save(): Unit = {
    entity.validate match {
      case Nil =>
        if(entity.save)
          snippet.onSave(this)
        else
          S.error("Save failed")
      case errors =>
        S.error(errors)
    }
  }

  /**
   * returns a string that represents the id, or &lt;new&gt;
   * if the entity is a new entity.
   * If the entity has been saved then the id is determined
   * as follows: If it is a KeyedMapper then it calls toString
   * on the entity's primaryKeyField. Otherwise it
   * calls toString on a field named "id."
   */
  def idString: String = if(entity.saved_?)
    entity match {
      case e: KeyedMapper[_,T] => e.primaryKeyField.toString
      case _ => entity.fieldByName("id").dmap("")((f: MappedField[_,T]) => f.toString)
    }
  else
    "<new>"
  
  
  /**
   * Returns a CssSel that binds a link to ".edit" to load and edit this entity
   */
  lazy val editAction: CssSel = ".edit" #> snippet.link("edit", load, Text(?("Edit")))
  /**
   * Returns a CssSel that binds a link to ".remove" that contains a link to delete this entity
   */
  lazy val removeAction: CssSel = ".remove" #> snippet.link("list", ()=>remove, Text(?("Remove")))
  /**
   * Returns a CssSel that binds the contents of an element with class ".<name>"
   * to the field named `name`.
   * If the field has a Full toForm implementation then that is used;
   * otherwise its asHtml is called.
   */
  def edit(name: String): CssSel = {
    entity.fieldByName(name).map { (field: olon.mapper.MappedField[_,_]) =>
      s".$name *" #> field.toForm.openOr(field.asHtml)
    }.openOrThrowException("If nobody has complained about this giving a NPE, I'll assume it is safe")
  }
}

