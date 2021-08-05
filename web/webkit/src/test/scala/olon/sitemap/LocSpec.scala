package olon
package sitemap

import common._
import http._
import mockweb._
  import MockWeb._
import mocks._

import org.specs2.mutable.Specification


/**
 * Systems under specification for Loc.
 */
class LocSpec extends Specification  {
  "Loc Specification".title

  case class Param(s: String)

  "A Loc" should {

    "calculate default href for basic menu definition" in {
      val loc = (Menu("Test") / "foo" / "bar").toMenu.loc
      loc.calcDefaultHref mustEqual "/foo/bar"
    }

    "calculate href for menu with parameters" in {
      val loc = (Menu.param[Param]("Test", "Test", s => Full(Param(s)), p => p.s) / "foo" / "bar" / *).toLoc
      loc.calcHref(Param("myparam")) mustEqual "/foo/bar/myparam"
    }

    "should not match a Req matching its Link when currentValue is Empty" in {
      val testMenu = Menu.param[Param]("Test", "Test", s => Empty, p => "bacon") / "foo" / "bar" / *
      val testSiteMap = SiteMap(testMenu)

      val testLoc = testMenu.toLoc
      val mockReq = new MockHttpServletRequest("http://test/foo/bar/123")

      testS(mockReq) {
        testReq(mockReq) { req =>
          testLoc.doesMatch_?(req) mustEqual false
        }
      }
    }

    "matchs a Req when currentValue is Empty, a * was used, and MatchWithoutCurrentValue is a param" in {
      val testMenu = Menu.param[Param]("Test", "Test", s => Empty, p => "bacon") / "foo" / "bar" / * >> Loc.MatchWithoutCurrentValue
      val testSiteMap = SiteMap(testMenu)

      val testLoc = testMenu.toLoc
      val mockReq = new MockHttpServletRequest("http://test/foo/bar/123")

      testS(mockReq) {
        testReq(mockReq) { req =>
          testLoc.doesMatch_?(req) mustEqual true
        }
      }
    }

    "matchs a Req when currentValue is Empty, and MatchWithoutCurrentValue is a param" in {
      val testMenu = Menu.param[Param]("Test", "Test", s => Empty, p => "bacon") / "foo" / "bar" >> Loc.MatchWithoutCurrentValue
      val testSiteMap = SiteMap(testMenu)

      val testLoc = testMenu.toLoc
      val mockReq = new MockHttpServletRequest("http://test/foo/bar/123")

      testS(mockReq) {
        testReq(mockReq) { req =>
          val rrq = new RewriteRequest(req.path, GetRequest, req.request)
          val rewriteFn = testLoc.rewrite.openOrThrowException("No rewrite function")

          rewriteFn(rrq) must not(throwA[Exception])
          rewriteFn(rrq)._2 must_== Empty
        }
      }
    }

    "not throw Exceptions on param methods before SiteMap assignment" in {
      val testMenu = Menu.param[Param]("Test", "Test", s => Empty, p => "bacon") / "foo" / "bar" >> Loc.MatchWithoutCurrentValue
      val testLoc = testMenu.toLoc

      testLoc.allParams must not(throwA[Exception])
      testLoc.currentValue must not(throwA[Exception])
      testLoc.siteMap must not(throwA[Exception])
      testLoc.breadCrumbs must not(throwA[Exception])
    }
  }
}
