package olon.util

import org.specs2.mutable._
import olon.common._

class SoftReferenceCacheSpec extends Specification {
  
  sequential

  object cache extends SoftReferenceCache[String, String](1)
  
  "SoftReferenceCache " should {
    "Accept additions" in {
      cache += ("test" -> "test")
      cache.keys.size() must_== 1
    }
    "Allow objects to be retrieved" in {
      val cached = cache("test")
      cached must beLike { case Full("test") => ok }
    }
    "Properly age out entries" in {
      cache += ("test2" -> "test2")
      cache("test") must_== Empty
    }
  }

}
