package olon
package mongodb
package record

import org.specs2.mutable.Specification

import org.bson._
import org.bson.codecs.{DecoderContext, EncoderContext}

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters.{eq => eqs}

import olon.mongodb.record.testmodels._
import olon.record._

class PlainRecordSpec extends Specification with MongoTestKit {
  "PlainRecordSpec Specification".title

  /**
   * Encodes then decodes a Record instance to/from Bson and asserts they are equal.
   */
  private def testEncodeDecode(record: RecordTest) = {
    val bson = new BsonDocument()
    val writer = new BsonDocumentWriter(bson)

    RecordTestStore.codec.encode(writer, record, EncoderContext.builder.build)

    val reader = new BsonDocumentReader(bson)
    val result: RecordTest = RecordTestStore.codec.decode(reader, DecoderContext.builder.build)

    result must_== record
  }

  override def before = {
    super.before
    checkMongoIsRunning
  }

  "Record" should {
    "encode and decode properly" in {
      val rec0 = RecordTest.createRecord
      val rec1 = RecordTest.createRecord.stringfield("hello")

      testEncodeDecode(rec0)
      testEncodeDecode(rec1)
    }

    "save and find properly" in {
      val rec0 = RecordTest.createRecord
      val rec1 = RecordTest.createRecord.stringfield("hello")

      RecordTestStore.collection.insertOne(rec0)
      RecordTestStore.collection.insertOne(rec1)

      val rec0fromDb = RecordTestStore.collection.find(eqs("_id", rec0.id.get)).first()
      val rec1fromDb = RecordTestStore.collection.find(eqs("_id", rec1.id.get)).first()

      rec0fromDb must_== rec0
      rec1fromDb must_== rec1

      MongoConfig.main.drop()

      success
    }
  }
}
