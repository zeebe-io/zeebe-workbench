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
import io.zeebe.workbench.impl.TestRunner
import scala.collection.JavaConverters._
import io.zeebe.workbench.WorkflowResource
import io.zeebe.workbench.Command
import io.zeebe.workbench.Verification

object TestRunnerServlet {

  // JSON data objects
  case class TestCases(tests: Iterable[TestCase])

  case class TestCase(name: String, resource: TestResource, commands: List[TestCommand], verifications: List[TestVerification])

  case class TestResource(name: String, xml: String, content: Any)

  case class TestCommand(activityId: String, payload: String)

  case class TestVerification(expectedIntent: String, expectedPayload: String, activityId: String)

  case class TestResult(name: String, failedVerifications: List[FailedVerification])

  case class FailedVerification(activityId: String, expectedPayload: String, actualPayLoad: String)

}

class TestRunnerServlet
  extends ScalatraServlet
  with JacksonJsonSupport
  with FileUploadSupport
  with ScalateSupport {

  import TestRunnerServlet._

  val runner = new TestRunner()

  // JSON configuration
  override protected implicit lazy val jsonFormats: Formats =
    DefaultFormats.withBigDecimal

  // file upload configuration
  configureMultipartHandling(
    MultipartConfig(maxFileSize = Some(3 * 1024 * 1024)))

  // response is JSON
  before() {
    //contentType = formats("json")
  }

  // views
  get("/") {
    contentType = "text/html"
    ssp("index")
  }

  get("/replay") {
    contentType = "text/html"
    ssp("replay")
  }

  // REST endpoints
  post("/run") {
    contentType = formats("json")

    val testCases = parsedBody.extract[List[TestCase]]

    val resources = testCases.map(t => new WorkflowResource(t.resource.xml.getBytes, t.resource.name))
    val tests = testCases.map(t => {

      val commands = t.commands.map(c => new Command(c.activityId, c.payload))
      val verifications = t.verifications.map(v => new Verification(v.expectedIntent, v.expectedPayload, v.activityId))

      new io.zeebe.workbench.TestCase(t.name, t.resource.name, "{}", commands.asJava, verifications.asJava)
    })

    val result = runner.run(resources.asJava, tests.asJava)

    result.asScala.map(r => {
      val verifications = r.getFailedVerifications.asScala.map(v => new FailedVerification(v.getVerification.getActivityId, v.getVerification.getExpectedPayload, v.getActualPayLoad))

      new TestResult(r.getName, verifications.toList)
    }).toList
  }

}
