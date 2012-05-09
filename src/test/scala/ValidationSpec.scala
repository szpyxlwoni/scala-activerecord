package com.github.aselab.activerecord

import org.specs2.mutable._
import org.specs2.specification._
import dsl._

import java.util.{Date, UUID}
import java.sql.Timestamp

object ValidationSpec extends ActiveRecordSpecification {
  class SaveableImpl extends Saveable {
    var calledMethods = List[String]()
    def save = {
      calledMethods :+= "save"
      true
    }
  }

  case class Dummy(e: Seq[String]) extends SaveableImpl with Validatable {
    override def doValidate {
      e.foreach(errors.add(_))
      calledMethods :+= "doValidate"
    }

    override def beforeValidation() {
      calledMethods :+= "beforeValidation"
    }
  }

  case class Dummy2(@Unique s1: String, @Required s2: String) extends ActiveRecord {
    def this() = this(null, null)
  }

  object Dummy2 extends ActiveRecordCompanion[Dummy2]

  case class FormSupportModel(
    string: String,
    boolean: Boolean,
    int: Int,
    long: Long,
    float: Float,
    double: Double,
    bigDecimal: BigDecimal,
    timestamp: Timestamp,
    date: Date,
    uuid: UUID,
    ostring: Option[String],
    oboolean: Option[Boolean],
    oint: Option[Int],
    olong: Option[Long],
    ofloat: Option[Float],
    odouble: Option[Double],
    obigDecimal: Option[BigDecimal],
    otimestamp: Option[Timestamp],
    odate: Option[Date],
    ouuid: Option[UUID]
  ) extends ActiveRecord {
    def this() = this("", false, 0, 0, 0.toFloat, 0.0, BigDecimal(0),
      new Timestamp(0), new Date(0), new UUID(0, 0),
      Some(""), Some(false), Some(0), Some(0L), Some(0.toFloat), Some(0.0),
      Some(BigDecimal(0)), Some(new Timestamp(0)), Some(new Date(0)), Some(new UUID(0, 0))
    )
  }

  object FormSupportModel extends ActiveRecordCompanion[FormSupportModel] with FormSupport[FormSupportModel]

  "Validatable" should {
    "addError" in {
      val m = Dummy(Nil)
      m.errors.add("global error1")
      m.errors.add("global error2")
      m.errors.add("s", "field error1")
      m.errors.add("i", "field error2")

      "errors" in {
        m.errors must contain(
          ValidationError("", "global error1"),
          ValidationError("", "global error2"),
          ValidationError("s", "field error1"),
          ValidationError("i", "field error2")
        ).only
      }

      "globalErrors" in {
        m.globalErrors must contain(
          ValidationError("", "global error1"),
          ValidationError("", "global error2")
        ).only
      }

      "fieldErrors" in {
        m.fieldErrors must contain(
          ValidationError("s", "field error1"),
          ValidationError("i", "field error2")
        ).only
      }
    }

    "validate success" in {
      "validate" in {
        val m = new Dummy(Nil)
        m.validate must beTrue
      }

      "save" in {
        val m = new Dummy(Nil)
        m.save must beTrue
        m.calledMethods must contain("beforeValidation", "doValidate", "save").only
      }
    }

    "validate failure" in {
      "validate" in {
        val m = new Dummy(Seq("error"))
        m.validate must beFalse
      }

      "save" in {
        val m = new Dummy(Seq("error"))
        m.save must beFalse
        m.calledMethods must contain("beforeValidation", "doValidate").only
      }

    }

    "Validator" in {
      val dummyValidator = {(value: Any) => if (value.toString == "dummy") Seq("dummy") else Nil}
      val dummyValidator2 = {(value: Any) => if (value.toString == "dummy2") Seq("dummy2") else Nil}
      Validator.register(classOf[annotations.Unique], dummyValidator)
      Validator.register(classOf[annotations.Required], dummyValidator2)

      "get" in {
        Validator.get(classOf[annotations.Unique]) must beSome(dummyValidator)
        Validator.get(classOf[annotations.Required]) must beSome(dummyValidator2)
      }

      "doValidate" in {
        val m1 = Dummy2("dummy", "")
        val m2 = Dummy2("", "dummy2")
        val m3 = Dummy2("dummy", "dummy2")
        m1.validate
        m2.validate
        m3.validate
        m1.errors must contain(ValidationError("s1", "dummy"))
        m2.errors must contain(ValidationError("s2", "dummy2"))
        m3.errors must contain(ValidationError("s1", "dummy"), ValidationError("s2", "dummy2"))
      }

      "unregister" in {
        Validator.unregister(classOf[annotations.Unique])
        Validator.get(classOf[annotations.Required]) must beSome(dummyValidator2)
        Validator.get(classOf[annotations.Unique]) must beNone
      }
    }

    "bind" >> {
      FormSupportModel.bind(Map("string" -> "string", "ostring" -> "", "int" -> "100")) mustEqual
        new FormSupportModel().copy(string = "string", ostring = Some(""), int = 100)
    }
  }
}

