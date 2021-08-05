package olon
package util

import scala.collection.mutable._
import scala.language.postfixOps
import scala.util.parsing.combinator._

/**
 * Parser for a VCard string such as
 * 
 * BEGIN:VCARD
 * VERSION:2.1
 * N:Gump;Forrest
 * FN:Forrest Gump
 * ORG:Bubba Gump Shrimp Co.
 * TITLE:Shrimp Man
 * TEL;WORK;VOICE:(111) 555-1212
 * TEL;HOME;VOICE:(404) 555-1212
 * ADR;WORK:;;100 Waters Edge;Baytown;LA;30314;United States of America
 * END:VCARD
 *
 */
object VCardParser extends Parsers {
  import scala.language.implicitConversions

  type Elem = Char

  implicit def strToInput(in: String): Input = new scala.util.parsing.input.CharArrayReader(in.toCharArray)

  case class VCardKey(name: String, props: List[(String, String)])
  case class VCardEntry(key: VCardKey, value: List[String])

  lazy val multiLineSep = opt(elem('\n') ~ elem(' '))
  lazy val value = (multiLineSep ~> elem("value", {c => !c.isControl && c != ';'}) <~ multiLineSep).* ^^ {case l => l.mkString}
  lazy val spaces = (elem(' ') | elem('\t') | elem('\n') | elem('\r'))*
  lazy val key = elem("key", {c => c.isLetterOrDigit || c == '-' || c == '_' || c == '.'}).+ ^^ {case list => val s = list.mkString; s.replaceFirst("^item\\d+\\.", "")}
  lazy val props = ((((elem(';') ~> key <~ elem('=')) ~ key) ^^ {case a ~ b => (a, b)}) | ((elem(';') ~> key) ^^ {case a => (a, "")}))*
  lazy val left = (key ~ props) ^^ {case k ~ l => VCardKey(k, l)}
  lazy val expr = (((spaces ~> left ~! elem(':')) ~ repsep(value, ';')) ^^ {case a ~ _ ~ b => VCardEntry(a, b)})+

  def parse(in: String): Either[List[VCardEntry], String] = expr(in) match {
    case Success(v, r) => Left(v)
    case err @ _ => Right(err toString)
  }
}

