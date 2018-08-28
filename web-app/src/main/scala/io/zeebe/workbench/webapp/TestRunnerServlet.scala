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
  case class TestCases(
    tests:     List[TestCase],
    resources: List[TestResource])

  case class TestCase(
    name:          String,
    resourceName:  String,
    startPayload:  String,
    commands:      List[TestCommand],
    verifications: List[TestVerification])

  case class TestResource(name: String, xml: String)

  case class TestCommand(activityId: String, payload: String)

  case class TestVerification(
    expectedIntent:  String,
    expectedPayload: String,
    activityId:      String)

  // ----

  case class TestResult(
    name:                String,
    failedVerifications: List[FailedVerification])

  case class FailedVerification(
    activityId:      String,
    expectedPayload: String,
    actualPayload:   String)

}

class TestRunnerServlet
  extends ScalatraServlet
  with JacksonJsonSupport
  with FileUploadSupport {

  import TestRunnerServlet._

  val runner = new TestRunner()

  var store = TestCases(List.empty, List.empty)

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

  // REST endpoints
  post("/run") {
    contentType = formats("json")

    val testCases = parsedBody.extract[TestCases]

    store = testCases

    val resources = testCases.resources.map(r =>
      new WorkflowResource(r.xml.getBytes, r.name))

    val tests = testCases.tests.map(t => {

      val commands = t.commands.map(c => new Command(c.activityId, c.payload))
      val verifications = t.verifications.map(v =>
        new Verification(v.expectedIntent, v.expectedPayload, v.activityId))

      new io.zeebe.workbench.TestCase(
        t.name,
        t.resourceName,
        t.startPayload,
        commands.asJava,
        verifications.asJava)
    })

    val result = runner.run(resources.asJava, tests.asJava)

    result.asScala
      .map(r => {
        val verifications = r.getFailedVerifications.asScala.map(
          v =>
            new FailedVerification(
              v.getVerification.getActivityId,
              v.getVerification.getExpectedPayload,
              v.getActualPayLoad))

        new TestResult(r.getName, verifications.toList)
      })
      .toList
  }

  post("/export") {
    contentType = formats("json")

    val testCases = parsedBody.extract[TestCases]

    // store test cases for download
    store = testCases

    // verify and transform format
    testCases.tests
  }

  get("/export") {
    contentType = formats("json")

    store.tests
  }

  // ---

  post("/store") {
    val testCases = parsedBody.extract[TestCases]
    store = testCases
  }

  get("/load") {
    contentType = formats("json")

    store
  }

}
