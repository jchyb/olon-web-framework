Parsing and formatting utilities for JSON.

A central concept in lift-json library is Json AST which models the structure of
a JSON document as a syntax tree.

    sealed abstract class JValue
    case object JNothing extends JValue // 'zero' for JValue
    case object JNull extends JValue
    case class JString(s: String) extends JValue
    case class JDouble(num: Double) extends JValue
    case class JInt(num: BigInt) extends JValue
    case class JBool(value: Boolean) extends JValue
    case class JObject(obj: List[JField]) extends JValue
    case class JArray(arr: List[JValue]) extends JValue

    case class JField(String, JValue)

All features are implemented in terms of above AST. Functions are used to transform
the AST itself, or to transform the AST between different formats. Common transformations
are summarized in a following picture.

![Json AST](https://github.com/lift/lift/raw/master/framework/lift-base/lift-json/json.png)

Summary of the features:

* Fast JSON parser
* LINQ style queries
* Case classes can be used to extract values from parsed JSON
* Diff & merge
* DSL to produce valid JSON
* XPath like expressions and HOFs to manipulate JSON
* Pretty and compact printing
* XML conversions
* Serialization
* Low level pull parser API

Installation
============

It comes with Lift, but non-Lift users can add lift-json as a dependency in following ways.
Note, replace XXX with correct Lift version.

### SBT users

Add dependency to your project description:

    val lift_json = "olon" %% "lift-json" % "XXX"

### Maven users

Add dependency to your pom:

    <dependency>
      <groupId>olon</groupId>
      <artifactId>lift-json</artifactId>
      <version>XXX</version>
    </dependency>

### Others

Download following jars:

* https://oss.sonatype.org/content/groups/scala-tools/olon/lift-json_2.11/2.6.2/lift-json_2.11-2.6.2.jar
* http://mirrors.ibiblio.org/pub/mirrors/maven2/com/thoughtworks/paranamer/paranamer/2.1/paranamer-2.1.jar
* scalap (Only for Scala-2.9 compatible versions)

Extras
------

* [lift-json-ext](https://github.com/talenteca/olon-web-framework/tree/master/core/json-ext)

Support for Box, Enum, Joda-Time, ...

* [lift-json-scalaz](https://github.com/talenteca/olon-web-framework/tree/master/core/json-scalaz)

Applicative style parsing with Scalaz

Migration from older versions
=============================

2.6 ->
-------------------------------------------------

JField is no longer a JValue. This means more type safety since it is no longer possible
to create invalid JSON where JFields are added directly into JArrays for instance. Most
noticeable consequence of this change is that map, transform, find and filter come in
two versions:

    def map(f: JValue => JValue): JValue
    def mapField(f: JField => JField): JValue
    def transform(f: PartialFunction[JValue, JValue]): JValue
    def transformField(f: PartialFunction[JField, JField]): JValue
    def find(p: JValue => Boolean): Option[JValue]
    def findField(p: JField => Boolean): Option[JField]
    ...

Use *Field functions to traverse fields in the JSON, and use the functions without 'Field'
in the name to traverse values in the JSON.

2.2 ->
------

Path expressions were changed after 2.2 version. Previous versions returned JField which
unnecessarily complicated the use of the expressions. If you have used path expressions
with pattern matching like:

    val JField("bar", JInt(x)) = json \ "foo" \ "bar"

It is now required to change that to:

    val JInt(x) = json \ "foo" \ "bar"

Parsing JSON
============

Any valid json can be parsed into internal AST format.

    scala> import olon.json._
    scala> parse(""" { "numbers" : [1, 2, 3, 4] } """)
    res0: olon.json.JsonAST.JValue =
          JObject(List(JField(numbers,JArray(List(JInt(1), JInt(2), JInt(3), JInt(4))))))

Producing JSON
==============

DSL rules
---------

* Primitive types map to JSON primitives.
* Any seq produces JSON array.

      scala> val json = List(1, 2, 3)

      scala> compact(render(json))

      res0: String = [1,2,3]

* Tuple2[String, A] produces field.

      scala> val json = ("name" -> "joe")

      scala> compact(render(json))

      res1: String = {"name":"joe"}

* ~ operator produces object by combining fields.

      scala> val json = ("name" -> "joe") ~ ("age" -> 35)

      scala> compact(render(json))

      res2: String = {"name":"joe","age":35}

* Any value can be optional. Field and value is completely removed when it doesn't have a value.

      scala> val json = ("name" -> "joe") ~ ("age" -> Some(35))

      scala> compact(render(json))

      res3: String = {"name":"joe","age":35}

      scala> val json = ("name" -> "joe") ~ ("age" -> (None: Option[Int]))

      scala> compact(render(json))

      res4: String = {"name":"joe"}

Example
-------

    object JsonExample extends Application {
      import olon.json._
      import olon.json.JsonDSL._

      case class Winner(id: Long, numbers: List[Int])
      case class Lotto(id: Long, winningNumbers: List[Int], winners: List[Winner], drawDate: Option[java.util.Date])

      val winners = List(Winner(23, List(2, 45, 34, 23, 3, 5)), Winner(54, List(52, 3, 12, 11, 18, 22)))
      val lotto = Lotto(5, List(2, 45, 34, 23, 7, 5, 3), winners, None)

      val json =
        ("lotto" ->
          ("lotto-id" -> lotto.id) ~
          ("winning-numbers" -> lotto.winningNumbers) ~
          ("draw-date" -> lotto.drawDate.map(_.toString)) ~
          ("winners" ->
            lotto.winners.map { w =>
              (("winner-id" -> w.id) ~
               ("numbers" -> w.numbers))}))

      println(compact(render(json)))
    }

    scala> JsonExample
    {"lotto":{"lotto-id":5,"winning-numbers":[2,45,34,23,7,5,3],"winners":
    [{"winner-id":23,"numbers":[2,45,34,23,3,5]},{"winner-id":54,"numbers":[52,3,12,11,18,22]}]}}

Example produces following pretty printed JSON. Notice that draw-date field is not rendered since its value is None:

    scala> pretty(render(JsonExample.json))

    {
      "lotto":{
        "lotto-id":5,
        "winning-numbers":[2,45,34,23,7,5,3],
        "winners":[{
          "winner-id":23,
          "numbers":[2,45,34,23,3,5]
        },{
          "winner-id":54,
          "numbers":[52,3,12,11,18,22]
        }]
      }
    }

Merging & Diffing
-----------------

Two JSONs can be merged and diffed with each other.
Please see more examples in [MergeExamples.scala](./src/test/scala/olon/json/MergeExamples.scala) and [DiffExamples.scala](src/test/scala/olon/json/DiffExamples.scala)

    scala> import olon.json._

    scala> val lotto1 = parse("""{
             "lotto":{
               "lotto-id":5,
               "winning-numbers":[2,45,34,23,7,5,3]
               "winners":[{
                 "winner-id":23,
                 "numbers":[2,45,34,23,3,5]
               }]
             }
           }""")

    scala> val lotto2 = parse("""{
             "lotto":{
               "winners":[{
                 "winner-id":54,
                 "numbers":[52,3,12,11,18,22]
               }]
             }
           }""")

    scala> val mergedLotto = lotto1 merge lotto2
    scala> pretty(render(mergedLotto))
    res0: String =
    {
      "lotto":{
        "lotto-id":5,
        "winning-numbers":[2,45,34,23,7,5,3],
        "winners":[{
          "winner-id":23,
          "numbers":[2,45,34,23,3,5]
        },{
          "winner-id":54,
          "numbers":[52,3,12,11,18,22]
        }]
      }
    }

    scala> val Diff(changed, added, deleted) = mergedLotto diff lotto1
    changed: olon.json.JsonAST.JValue = JNothing
    added: olon.json.JsonAST.JValue = JNothing
    deleted: olon.json.JsonAST.JValue = JObject(List((lotto,JObject(List((winners,
    JArray(List(JObject(List(JField(winner-id,JInt(54)), JField(numbers,JArray(
    List(JInt(52), JInt(3), JInt(12), JInt(11), JInt(18), JInt(22))))))))))))))


Querying JSON
=============

"LINQ" style
------------

JSON values can be extracted using for-comprehensions.
Please see more examples in [JsonQueryExamples.scala](./src/test/scala/olon/json/JsonQueryExamples.scala)

    scala> import olon.json._
    scala> val json = parse("""
             { "name": "joe",
               "children": [
                 {
                   "name": "Mary",
                   "age": 5
                 },
                 {
                   "name": "Mazy",
                   "age": 3
                 }
               ]
             }
           """)

    scala> for { JObject(o) <- json; JField("age", JInt(age)) <- o } yield age
    res0: List[BigInt] = List(5, 3)

    scala> for {
             JObject(child) <- json
             JField("name", JString(name)) <- child
             JField("age", JInt(age)) <- child
             if age > 4
           } yield (name, age)
    res1: List[(String, BigInt)] = List((Mary,5))

XPath + HOFs
------------

Json AST can be queried using XPath like functions. Following REPL session shows the usage of
'\\', '\\\\', 'find', 'filter', 'transform', 'remove' and 'values' functions.

    The example json is:

    {
      "person": {
        "name": "Joe",
        "age": 35,
        "spouse": {
          "person": {
            "name": "Marilyn"
            "age": 33
          }
        }
      }
    }

    Translated to DSL syntax:

    scala> import olon.json._
    scala> import olon.json.JsonDSL._

    scala> val json =
      ("person" ->
        ("name" -> "Joe") ~
        ("age" -> 35) ~
        ("spouse" ->
          ("person" ->
            ("name" -> "Marilyn") ~
            ("age" -> 33)
          )
        )
      )

    scala> json \\ "spouse"
    res0: olon.json.JsonAST.JValue = JObject(List(JField(person,JObject(List((name,JString(Marilyn)), (age,JInt(33)))))))

    scala> compact(render(res0))
    res1: String = {"person":{"name":"Marilyn","age":33}}

    scala> compact(render(json \\ "name"))
    res2: String = {"name":"Joe","name":"Marilyn"}

    scala> compact(render((json removeField { _ == JField("name", JString("Marilyn")) }) \\ "name"))
    res3: String = {"name":"Joe"}

    scala> compact(render(json \ "person" \ "name"))
    res4: String = "Joe"

    scala> compact(render(json \ "person" \ "spouse" \ "person" \ "name"))
    res5: String = "Marilyn"

    scala> json findField {
             case JField("name", _) => true
             case _ => false
           }
    res6: Option[olon.json.JsonAST.JField] = Some(JField(name,JString(Joe)))

    scala> json filterField {
             case JField("name", _) => true
             case _ => false
           }
    res7: List[olon.json.JsonAST.JField] = List(JField(name,JString(Joe)), JField(name,JString(Marilyn)))

    scala> json transformField {
             case ("name", JString(s)) => ("NAME", JString(s.toUpperCase))
           }
    res8: olon.json.JsonAST.JValue = JObject(List(JField(person,JObject(List(
    JField(NAME,JString(JOE)), JField(age,JInt(35)), JField(spouse,JObject(List(
    JField(person,JObject(List(JField(NAME,JString(MARILYN)), JField(age,JInt(33)))))))))))))

    scala> json.values
    res8: scala.collection.immutable.Map[String,Any] = Map(person -> Map(name -> Joe, age -> 35, spouse -> Map(person -> Map(name -> Marilyn, age -> 33))))

Indexed path expressions work too and values can be unboxed using type expressions.

    scala> val json = parse("""
             { "name": "joe",
               "children": [
                 {
                   "name": "Mary",
                   "age": 5
                 },
                 {
                   "name": "Mazy",
                   "age": 3
                 }
               ]
             }
           """)

    scala> (json \ "children")(0)
    res0: olon.json.JsonAST.JValue = JObject(List(JField(name,JString(Mary)), JField(age,JInt(5))))

    scala> (json \ "children")(1) \ "name"
    res1: olon.json.JsonAST.JValue = JString(Mazy)

    scala> json \\ classOf[JInt]
    res2: List[olon.json.JsonAST.JInt#Values] = List(5, 3)

    scala> json \ "children" \\ classOf[JString]
    res3: List[olon.json.JsonAST.JString#Values] = List(Mary, Mazy)


Extracting values
=================

Case classes can be used to extract values from parsed JSON. Non-existing values
can be extracted into scala.Option and strings can be automatically converted into
java.util.Dates.
Please see more examples in [ExtractionExamplesSpec.scala](./src/test/scala/olon/json/ExtractionExamplesSpec.scala)

    scala> import olon.json._
    scala> implicit val formats = DefaultFormats // Brings in default date formats etc.
    scala> case class Child(name: String, age: Int, birthdate: Option[java.util.Date])
    scala> case class Address(street: String, city: String)
    scala> case class Person(name: String, address: Address, children: List[Child])
    scala> val json = parse("""
             { "name": "joe",
               "address": {
                 "street": "Bulevard",
                 "city": "Helsinki"
               },
               "children": [
                 {
                   "name": "Mary",
                   "age": 5
                   "birthdate": "2004-09-04T18:06:22Z"
                 },
                 {
                   "name": "Mazy",
                   "age": 3
                 }
               ]
             }
           """)

    scala> json.extract[Person]
    res0: Person = Person(joe,Address(Bulevard,Helsinki),List(Child(Mary,5,Some(Sat Sep 04 18:06:22 EEST 2004)), Child(Mazy,3,None)))

By default the constructor parameter names must match json field names. However, sometimes json
field names contain characters which are not allowed characters in Scala identifiers. There's two
solutions for this (see src/test/scala/olon/json/LottoExample.scala for bigger example).

Use back ticks.

    scala> case class Person(`first-name`: String)

Use transform function to postprocess AST.

    scala> case class Person(firstname: String)
    scala> json transformField {
             case JField("first-name", x) => ("firstname", x)
           }

Extraction function tries to find the best matching constructor when case class has auxiliary
constructors. For instance extracting from JSON {"price":350} into the following case class
will use the auxiliary constructor instead of the primary constructor.

    scala> case class Bike(make: String, price: Int) {
             def this(price: Int) = this("Trek", price)
           }
    scala> parse(""" {"price":350} """).extract[Bike]
    res0: Bike = Bike(Trek,350)

Primitive values can be extracted from JSON primitives or fields.

    scala> (json \ "name").extract[String]
    res0: String = "joe"

    scala> ((json \ "children")(0) \ "birthdate").extract[Date]
    res1: java.util.Date = Sat Sep 04 21:06:22 EEST 2004

DateFormat can be changed by overriding 'DefaultFormats' (or by implmenting trait 'Formats').

    scala> implicit val formats = new DefaultFormats {
             override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
           }

JSON object can be extracted to Map[String, _] too. Each field becomes a key value pair
in result Map.

    scala> val json = parse("""
             {
               "name": "joe",
               "addresses": {
                 "address1": {
                   "street": "Bulevard",
                   "city": "Helsinki"
                 },
                 "address2": {
                   "street": "Soho",
                   "city": "London"
                 }
               }
             }""")

    scala> case class PersonWithAddresses(name: String, addresses: Map[String, Address])
    scala> json.extract[PersonWithAddresses]
    res0: PersonWithAddresses("joe", Map("address1" -> Address("Bulevard", "Helsinki"),
                                         "address2" -> Address("Soho", "London")))


Serialization
=============

Case classes can be serialized and deserialized.
Please see other examples in [SerializationExamples.scala](./src/test/scala/olon/json/SerializationExamples.scala)

    scala> import olon.json._
    scala> import olon.json.Serialization.{read, write}
    scala> implicit val formats = Serialization.formats(NoTypeHints)
    scala> val ser = write(Child("Mary", 5, None))
    scala> read[Child](ser)
    res1: Child = Child(Mary,5,None)

Serialization supports:

* Arbitrarily deep case class graphs
* All primitive types, including BigInt and Symbol
* List, Seq, Array, Set and Map (note, keys of the Map must be strings: Map[String, _])
* scala.Option
* java.util.Date
* Polymorphic Lists (see below)
* Tuples (see below)
* Recursive types
* Serialization of fields of a class (see below)
* Custom serializer functions for types which are not supported (see below)

Serializing polymorphic Lists
-----------------------------

Type hints are required when serializing polymorphic (or heterogeneous) Lists. Serialized JSON objects
will get an extra field named 'jsonClass' (the name can be changed by overriding 'typeHintFieldName' from Formats).

    scala> trait Animal
    scala> case class Dog(name: String) extends Animal
    scala> case class Fish(weight: Double) extends Animal
    scala> case class Animals(animals: List[Animal])
    scala> implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[Dog], classOf[Fish])))
    scala> val ser = write(Animals(Dog("pluto") :: Fish(1.2) :: Nil))
    ser: String = {"animals":[{"jsonClass":"Dog","name":"pluto"},{"jsonClass":"Fish","weight":1.2}]}

    scala> read[Animals](ser)
    res0: Animals = Animals(List(Dog(pluto), Fish(1.2)))

ShortTypeHints outputs short classname for all instances of configured objects. FullTypeHints outputs full
classname. Other strategies can be implemented by extending TypeHints trait.

Serializing Tuples
-----------------------------

If you need to have a heterogeneous collection of completely unrelated types, you might find tuples
useful for representing that data structure. By default, when you serialize a tuple you'll get an
object based on the constructor of the appropreate tuple class. So, for example, if you serialize
a `("bacon", 2)`, you'll get:

    {"_1": "bacon", "_2": 2}

However, as of Lift 3.1, you can enable `tuplesAsArrays` to represent these tuples as heterogeneous
JSON arrays. To enable this feature you just need to provide a formats object that turns on the
feature.

    scala> import olon.json._
    scala> import Serialization._
    scala> implicit val formats = new DefaultFormats { override val tuplesAsArrays = true }
    scala> write(("bacon", 2))
    res1: String = ["bacon",2]

When this feature is enabled:

* lift-json will write tuples as arrays.
* lift-json will correctly extract tuples from arrays
* lift-json will continue to deserialize tuples that were serialied in the old manner, as an object

The major limitation to this feature is that it doesn't reliably support Scala primitives, so it is
currently disabled by default. If you're using this feature, you should use the Java boxed types
in your class signatures instead of the Scala primitive types. (So, `java.lang.Integer` instead
of `scala.Int`.)

Serializing fields of a class
-----------------------------

To enable serialization of fields, a FieldSerializer can be added for some type:

    implicit val formats = DefaultFormats + FieldSerializer[WildDog]()

Now the type WildDog (and all subtypes) gets serialized with all its fields (+ constructor parameters).
FieldSerializer takes two optional parameters which can be used to intercept the field serialization:

    case class FieldSerializer[A: Manifest](
      serializer:   PartialFunction[(String, Any), Option[(String, Any)]] = Map(),
      deserializer: PartialFunction[JField, JField] = Map()
    )

Those PartialFunctions are called just before a field is serialized or deserialized. Some useful PFs to
rename and ignore fields are provided:

    val dogSerializer = FieldSerializer[WildDog](
      renameTo("name", "animalname") orElse ignore("owner"),
      renameFrom("animalname", "name"))

    implicit val formats = DefaultFormats + dogSerializer

Serializing non-supported types
-------------------------------

It is possible to plug in custom serializer + deserializer functions for any type.
Now, if we have a non case class Interval (thus, not supported by default), we can still serialize it
by providing following serializer.

    scala> class Interval(start: Long, end: Long) {
             val startTime = start
             val endTime = end
           }

    scala> class IntervalSerializer extends CustomSerializer[Interval](format => (
             {
               case JObject(JField("start", JInt(s)) :: JField("end", JInt(e)) :: Nil) =>
                 new Interval(s.longValue, e.longValue)
             },
             {
               case x: Interval =>
                 JObject(JField("start", JInt(BigInt(x.startTime))) ::
                         JField("end",   JInt(BigInt(x.endTime))) :: Nil)
             }
           ))

    scala> implicit val formats = Serialization.formats(NoTypeHints) + new IntervalSerializer

Custom serializer is created by providing two partial functions. The first evaluates to a value
if it can unpack the data from JSON. The second creates the desired JSON if the type matches.

Extensions
----------

Module lift-json-ext contains extensions to extraction and serialization. Following types are supported.

    // Lift's box
    implicit val formats = olon.json.DefaultFormats + new JsonBoxSerializer

    // Scala enums
    implicit val formats = olon.json.DefaultFormats + new EnumSerializer(MyEnum)
    // or
    implicit val formats = olon.json.DefaultFormats + new EnumNameSerializer(MyEnum)

    // Joda Time
    implicit val formats = olon.json.DefaultFormats ++ JodaTimeSerializers.all

XML support
===========

JSON structure can be converted to XML node and vice versa.
Please see more examples in [XmlExamples.scala](./src/test/scala/olon/json/XmlExamples.scala)

    scala> import olon.json.Xml.{toJson, toXml}
    scala> val xml =
             <users>
               <user>
                 <id>1</id>
                 <name>Harry</name>
               </user>
               <user>
                 <id>2</id>
                 <name>David</name>
               </user>
             </users>   

    scala> val json = toJson(xml)
    scala> pretty(render(json))
    res3: {
      "users":{
        "user":[{
          "id":"1",
          "name":"Harry"
        },{
          "id":"2",
          "name":"David"
        }]
      }
    }

Now, the above example has two problems. First, the id is converted to String while we might want it as an Int. This
is easy to fix by mapping JString(s) to JInt(s.toInt). The second problem is more subtle. The conversion function
decides to use JSON array because there's more than one user-element in XML. Therefore a structurally equivalent
XML document which happens to have just one user-element will generate a JSON document without JSON array. This
is rarely a desired outcome. These both problems can be fixed by following transformation function.

    scala> json transformField {
             case JField("id", JString(s))   => ("id", JInt(s.toInt))
             case JField("user", x: JObject) => ("user", JArray(x :: Nil))
           }

Other direction is supported too. Converting JSON to XML:

     scala> toXml(json)
     res5: scala.xml.NodeSeq = <users><user><id>1</id><name>Harry</name></user><user><id>2</id><name>David</name></user></users>

Low level pull parser API
=========================

Pull parser API is provided for cases requiring extreme performance. It improves parsing
performance by two ways. First, no intermediate AST is generated. Second, you can stop
parsing at any time, skipping rest of the stream. Note, this parsing style is recommended
only as an optimization. Above mentioned functional APIs are easier to use.

Consider following example which shows how to parse one field value from a big JSON.

    scala> val json = """
      {
        ...
        "firstName": "John",
        "lastName": "Smith",
        "address": {
          "streetAddress": "21 2nd Street",
          "city": "New York",
          "state": "NY",
          "postalCode": 10021
        },
        "phoneNumbers": [
          { "type": "home", "number": "212 555-1234" },
          { "type": "fax", "number": "646 555-4567" }
        ],
        ...
      }"""

    scala> val parser = (p: Parser) => {
             def parse: BigInt = p.nextToken match {
               case FieldStart("postalCode") => p.nextToken match {
                 case IntVal(code) => code
                 case _ => p.fail("expected int")
               }
               case End => p.fail("no field named 'postalCode'")
               case _ => parse
             }

             parse
           }

    scala> val postalCode = parse(json, parser)
    postalCode: BigInt = 10021

Pull parser is a function `Parser => A`, in this example it is concretely `Parser => BigInt`.
Constructed parser recursively reads tokens until it finds `FieldStart("postalCode")`
token. After that the next token must be `IntVal`, otherwise parsing fails. It returns parsed
integer and stops parsing immediately.

FAQ
===

Q1: I have a JSON object and I want to extract it to a case class:

    scala> case class Person(name: String, age: Int)
    scala> val json = """{"name":"joe","age":15}"""

But extraction fails:

    scala> parse(json).extract[Person]                                                  
    olon.json.MappingException: Parsed JSON values do not match with class constructor

A1:

Extraction does not work for classes defined in REPL. Compile the case class definitions
with scalac and import those to REPL.

Kudos
=====

* The original idea for DSL syntax was taken from Lift mailing list ([by Marius](http://markmail.org/message/lniven2hn22vhupu)).

* The idea for AST and rendering was taken from [Real World Haskell book](http://book.realworldhaskell.org/read/writing-a-library-working-with-json-data.html).
