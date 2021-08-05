package olon.mongodb

import java.util.concurrent.ConcurrentHashMap

import com.mongodb.MongoException
import com.mongodb.async.client.{MongoCollection, MongoDatabase}
import com.mongodb.async.SingleResultCallback
import olon.util.ConnectionIdentifier
import org.bson.Document

import scala.concurrent.Promise

@deprecated("No longer supported. This will be removed in Lift 4.", "3.4.3")
private[mongodb] class SingleBooleanVoidCallback(f: () => Unit) extends SingleResultCallback[Void] {
  private[this] val p = Promise[Boolean]()

  override def onResult(result: java.lang.Void, error: Throwable): Unit = {
    Option(error) match {
      case None =>
        f()
        p.success(true)
      case Some(t) =>
        p.failure(t)
    }
  }

  def future = p.future
}

/**
  * Async version of MongoDB.
  *
  * You should only have one instance of MongoClient in a JVM.
  *
  * Example:
  *
  * {{{
  * import com.mongodb.MongoClientSettings
  * import com.mongodb.async.client.MongoClients
  * import olon.util.{ConnectionIdentifier, DefaultConnectionIdentifier}
  * import org.bson.codecs.configuration.CodecRegistries
  *
  * val client = MongoClients.create("mongodb://127.0.0.1:27017")
  *
  * // main database
  * MongoAsync.defineDb(DefaultConnectionIdentifier, client.getDatabase("mydb"))
  *
  * // admin database
  * case object AdminIdentifier extends ConnectionIdentifier {
  *   val jndiName = "admin"
  * }
  *
  * val codecRegistry = CodecRegistries.fromRegistries(
  *   MongoClientSettings.getDefaultCodecRegistry(),
  *   CodecRegistries.fromCodecs(new LongPrimitiveCodec, new IntegerPrimitiveCodec)
  * )

  * val admin = client.getDatabase("admin").withCodecRegistry(codecRegistry)
  * MongoAsync.defineDb(AdminIdentifier, admin)
  *
  * }}}
  */
@deprecated("No longer supported. This will be removed in Lift 4.", "3.4.3")
object MongoAsync {

  /**
    * HashMap of MongoDatabase instances keyed by ConnectionIdentifier
    */
  private[this] val dbs = new ConcurrentHashMap[ConnectionIdentifier, MongoDatabase]

  /**
    * Define a Mongo db using a MongoDatabase instance.
    */
  @deprecated("No longer supported. This will be removed in Lift 4.", "3.4.3")
  def defineDb(id: ConnectionIdentifier, db: MongoDatabase): Unit = {
    dbs.put(id, db)
  }

  /**
    * Get a MongoDatabase reference
    */
  private[this] def getDatabase(name: ConnectionIdentifier): Option[MongoDatabase] = {
    Option(dbs.get(name))
  }

  /**
    * Executes function {@code f} with the mongo database identified by {@code name}.
    */
  @deprecated("No longer supported. This will be removed in Lift 4.", "3.4.3")
  def use[T](name: ConnectionIdentifier)(f: (MongoDatabase) => T): T = {
    val db = getDatabase(name) match {
      case Some(mongo) => mongo
      case _ => throw new MongoException("Mongo not found: "+name.toString)
    }
    f(db)
  }

  /**
    * Executes function {@code f} with the collection named {@code collectionName} from
    * the mongo database identified by {@code name}.
    */
  @deprecated("No longer supported. This will be removed in Lift 4.", "3.4.3")
  def useCollection[T](name: ConnectionIdentifier, collectionName: String)(f: (MongoCollection[Document]) => T): T = {
    val coll = getCollection(name, collectionName) match {
      case Some(collection) => collection
      case _ => throw new MongoException("Mongo not found: "+collectionName+". ConnectionIdentifier: "+name.toString)
    }

    f(coll)
  }

  private[this] def getCollection(name: ConnectionIdentifier, collectionName: String): Option[MongoCollection[Document]] = {
    getDatabase(name).map(_.getCollection(collectionName))
  }

  /**
    * Clear the HashMap.
    */
  @deprecated("No longer supported. This will be removed in Lift 4.", "3.4.3")
  def clear(): Unit = {
    dbs.clear()
  }

  /**
    * Remove a specific ConnectionIdentifier from the HashMap.
    */
  @deprecated("No longer supported. This will be removed in Lift 4.", "3.4.3")
  def remove(id: ConnectionIdentifier): Option[MongoDatabase] = {
    Option(dbs.remove(id))
  }
}
