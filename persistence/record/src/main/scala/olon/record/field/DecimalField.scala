package olon
package record
package field

import java.math.{BigDecimal => JBigDecimal,MathContext,RoundingMode}
import scala.xml._
import olon.common._
import olon.http.{S}
import olon.json.JsonAST.JValue
import olon.util._
import Helpers._
import S._


trait DecimalTypedField extends NumericTypedField[BigDecimal] {
  protected val scale: Int
  protected val context: MathContext
  private val zero = BigDecimal("0")

  def defaultValue = zero.setScale(scale)

  def setFromAny(in : Any): Box[BigDecimal] = setNumericFromAny(in, n => BigDecimal(n.toString))

  def setFromString (s : String) : Box[BigDecimal] = 
    if(s == null || s.isEmpty) {
      if(optional_?)
    	  setBox(Empty)
       else
          setBox(Failure(notOptionalErrorMessage))
    } else {
      setBox(tryo(BigDecimal(s)))
    }

  def set_!(in: BigDecimal): BigDecimal = new BigDecimal(in.bigDecimal.setScale(scale, context.getRoundingMode))

  def asJValue: JValue = asJString(_.toString)
  def setFromJValue(jvalue: JValue) = setFromJString(jvalue)(s => tryo(BigDecimal(s)))
}


/**
 * <p>
 * A field that maps to a decimal value. Decimal precision and rounding
 * are controlled via the context parameter. The default value is zero.
 * </p>
 *
 * <p><b><i>Note:</i></b><br/>
 * Using MathContext.UNLIMITED, whether explicitly or implicitly, means
 * that no precision or scaling will be used for the SQL field definition; the
 * default scale for DECIMAL is zero per the SQL standard, but the precision
 * for DECIMAL is vendor-specific. For example, PostgreSQL uses maximum precision
 * if it's not specified, but SQL Server uses a default precision of 18.
 * </p>
 *
 * @author Derek Chen-Becker
 *
 * @param owner The Record that owns this field
 * @param context The MathContext that controls precision and rounding
 * @param scale Controls the scale of the underlying BigDecimal
 */
class DecimalField[OwnerType <: Record[OwnerType]](@deprecatedName('rec) val owner: OwnerType, val context : MathContext, val scale : Int)
  extends Field[BigDecimal, OwnerType] with MandatoryTypedField[BigDecimal] with DecimalTypedField {

  /**
   * Constructs a DecimalField with the specified initial value. The context
   * is set to MathContext.UNLIMITED (see note above about default precision).
   * The scale is taken from the initial value.
   *
   * @param owner The Record that owns this field
   * @param value The initial value
   */
  def this(@deprecatedName('rec) owner : OwnerType, value : BigDecimal) = {
    this(owner, MathContext.UNLIMITED, value.scale)
    set(value)
  }

  /**
   * Constructs a DecimalField with the specified initial value and context.
   * The scale is taken from the initial value.
   *
   * @param owner The Record that owns this field
   * @param value The initial value
   * @param context The MathContext that controls precision and rounding
   */
  def this(@deprecatedName('rec) owner : OwnerType, value : BigDecimal, context : MathContext) = {
    this(owner, context, value.scale)
    set(value)
  }
}


/**
 * <p>
 * A field that maps to a decimal value. Decimal precision and rounding
 * are controlled via the context parameter. The default value is zero.
 * </p>
 *
 * <p><b><i>Note:</i></b><br/>
 * Using MathContext.UNLIMITED, whether explicitly or implicitly, means
 * that no precision or scaling will be used for the SQL field definition; the
 * default scale for DECIMAL is zero per the SQL standard, but the precision
 * for DECIMAL is vendor-specific. For example, PostgreSQL uses maximum precision
 * if it's not specified, but SQL Server uses a default precision of 18.
 * </p>
 *
 * @author Derek Chen-Becker
 *
 * @param owner The Record that owns this field
 * @param context The MathContext that controls precision and rounding
 * @param scale Controls the scale of the underlying BigDecimal
 */
class OptionalDecimalField[OwnerType <: Record[OwnerType]](@deprecatedName('rec) val owner: OwnerType, val context : MathContext, val scale : Int)
  extends Field[BigDecimal, OwnerType] with OptionalTypedField[BigDecimal] with DecimalTypedField {

  /**
   * Constructs a DecimalField with the specified initial value. The context
   * is set to MathContext.UNLIMITED (see note above about default precision).
   * The scale is taken from the initial value.
   *
   * @param owner The Record that owns this field
   * @param value The initial value
   * @param scale the scale of the decimal field, since there might be no value
   */
  def this(@deprecatedName('rec) owner : OwnerType, value : Box[BigDecimal], scale : Int) = {
    this(owner, MathContext.UNLIMITED, scale)
    setBox(value)
  }

  /**
   * Constructs a DecimalField with the specified initial value and context.
   * The scale is taken from the initial value.
   *
   * @param owner The Record that owns this field
   * @param value The initial value
   * @param scale the scale of the decimal field, since there might be no value
   * @param context The MathContext that controls precision and rounding
   */
  def this(@deprecatedName('rec) owner : OwnerType, value : Box[BigDecimal], scale : Int, context : MathContext) = {
    this(owner, context, scale)
    setBox(value)
  }
}

