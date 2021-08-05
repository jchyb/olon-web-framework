package olon
package mapper

import http.provider._
import common._
import util._
import http._
import Helpers._


trait ProtoExtendedSession[T <: ProtoExtendedSession[T]] extends KeyedMapper[Long, T] {
  self: T =>

  override def primaryKeyField: MappedLongIndex[T] = id

  // the primary key for the database
  object id extends MappedLongIndex(this)

  // uniqueId
  object cookieId extends MappedUniqueId(this, 32) {
    override def dbIndexed_? = true
  }

  object userId extends MappedString(this, 64)

  object expiration extends MappedLong(this) {
    override def defaultValue = expirationTime
    override def dbColumnName = expirationColumnName
  }

  /**
   * Change this string to "experation" for compatibility with
   * old mis-spelling
   */
  protected def expirationColumnName = "expiration"

  def expirationTime: Long = millis + 180.days
}

trait UserIdAsString {
  def userIdAsString: String
}

/**
 * The root trait for defining the session cookie path for extended sessions
 * that defines the default session cookie path: "/".
 */
trait ProtoSessionCookiePath {
  def sessionCookiePath: String = "/"
}

trait MetaProtoExtendedSession[T <: ProtoExtendedSession[T]] extends
KeyedMetaMapper[Long, T] with ProtoSessionCookiePath {
  self: T =>

  def CookieName = "ext_id"
  type UserType <: UserIdAsString

  /*
  private object myWrapper extends LoanWrapper {
    def apply[N](f: => N): N = {
      (recoverUserId, S.findCookie(CookieName)) match {
        case (Empty, Full(c)) =>
          find(By(cookieId, c.value openOr "")) match {
            case Full(es) if es.expiration.is < millis => es.delete_!
            case Full(es) => logUserIdIn(es.userId)
            case _ =>
          }

        case _ =>
      }
      f
    }
  }*/

  def logUserIdIn(uid: String): Unit

  def recoverUserId: Box[String]

  def userDidLogin(uid: UserType): Unit = {
    userDidLogout(Full(uid))
    val inst = create.userId(uid.userIdAsString).saveMe()
    val cookie = HTTPCookie(CookieName, inst.cookieId.get).
    setMaxAge(((inst.expiration.get - millis) / 1000L).toInt).
    setPath(sessionCookiePath)
    S.addCookie(cookie)
  }

  def userDidLogout(uid: Box[UserType]): Unit = {
    for (cook <- S.findCookie(CookieName)) {
      S.deleteCookie(cook)
      find(By(cookieId, cook.value openOr "")).foreach(_.delete_!)
    }
  }

  // def requestLoans: List[LoanWrapper] = myWrapper :: Nil

  /**
   * This does the cookie to User lookup.  In Boot.scala:
   * <code>
    LiftRules.earlyInStateful.append(ExtendedSession.testCookieEarlyInStateful)
   * </code>
   */
  def testCookieEarlyInStateful: Box[Req] => Unit = {
    ignoredReq => {
      (recoverUserId, S.findCookie(CookieName)) match {
        case (Empty, Full(c)) =>
          find(By(cookieId, c.value openOr "")) match {
            case Full(es) if es.expiration.get < millis => es.delete_!
            case Full(es) => logUserIdIn(es.userId.get)
            case _ =>
          }

        case _ =>
      }
    }
  }
}

/**
 * Mix this in to your extended session singleton to set the cookie path
 * to the context path for your application. This is useful if you have
 * multiple applications on a single application server and want to ensure
 * their cookies don't cross-pollinate.
 *
 * Example usage:
 *
 * {{{
 * case class AppExtendedSession extends ProtoExtendedSession[AppExtendedSession]
 * object MetaAppExtendedSession extends MetaProtoExtendedSession[AppExtendedSession]
 *   with ContextPathExtendedCookie
 * }}}
 */
trait ContextPathExtendedCookie extends ProtoSessionCookiePath {
  override def sessionCookiePath = S.contextPath
}
