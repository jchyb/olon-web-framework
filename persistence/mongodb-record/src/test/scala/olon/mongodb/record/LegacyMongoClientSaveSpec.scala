package olon
package mongodb
package record

import olon.common._
import olon.record.field._

import org.specs2.mutable.Specification

import com.mongodb._


package legacymongoclientsaverecords {

  import field._

  class SaveDoc private () extends MongoRecord[SaveDoc] with ObjectIdPk[SaveDoc] {
    def meta = SaveDoc

    object name extends StringField(this, 12)
  }
  object SaveDoc extends SaveDoc with MongoMetaRecord[SaveDoc] {
    import BsonDSL._

    createIndex(("name" -> 1), true) // unique name
  }
}


/**
  * Systems under specification for LegacyMongoClientSave.
  */
class LegacyMongoClientSaveSpec extends Specification with MongoTestKit {
  "LegacyMongoClientSave Specification".title

  import legacymongoclientsaverecords._

  "MongoMetaRecord with Mongo save" in {

    checkMongoIsRunning

    val sd1 = SaveDoc.createRecord.name("MongoSession")
    val sd2 = SaveDoc.createRecord.name("MongoSession")
    val sd3 = SaveDoc.createRecord.name("MongoDB")

    // save to db
    sd1.save()
    sd2.save(false) // no exception thrown
    sd2.save(true) must throwA[MongoException]
    sd2.saveBox() must beLike {
      case Failure(msg, _, _) => msg must contain("E11000")
    }
    sd3.save()

    success
  }
}
