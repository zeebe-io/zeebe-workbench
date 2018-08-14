package io.zeebe.workbench.webapp

import org.scalatra._
import org.json4s.{ DefaultFormats, Formats }
import org.scalatra.json._
import org.scalatra.servlet.{
  FileUploadSupport,
  MultipartConfig,
  SizeConstraintExceededException
}
import org.scalatra.servlet.FileItem
import org.scalatra.scalate.ScalateSupport

class TestRunnerServlet
  extends ScalatraServlet
  with JacksonJsonSupport
  with FileUploadSupport
  with ScalateSupport {

  // JSON configuration
  override protected implicit lazy val jsonFormats: Formats =
    DefaultFormats.withBigDecimal

  // file upload configuration
  configureMultipartHandling(
    MultipartConfig(maxFileSize = Some(3 * 1024 * 1024)))

  // JSON data objects
  case class Foo(foo: String)

  // response is JSON
  before() {
    //contentType = formats("json")
  }

  // views
  get("/") {
    contentType = "text/html"

    ssp("index", "foo" -> "bar")
  }

  get("/bpmn-js-test") {
    contentType = "text/html"

    ssp("bpmn-js-test")
  }

  // REST endpoints

}
