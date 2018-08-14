val shared = Seq(
  organization := "io.zeebe.workbench",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.12.4",
  resolvers += Resolver.mavenLocal,
  resolvers += Classpaths.typesafeReleases,
  resolvers += "camunda-bpm-nexus" at "https://app.camunda.com/nexus/content/groups/public",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
)

val commonDependencies = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "junit" % "junit" % "4.11" % "test",
  "org.scalatest" % "scalatest_2.12" % "3.0.4" % "test",
  "org.apache.logging.log4j" % "log4j-api" % "2.9.0" % "test",
  "org.apache.logging.log4j" % "log4j-core" % "2.9.0" % "test",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.9.0" % "test"
)

val zeebeVersion = "0.11.0"
val scalatraVersion = "2.6.2"

lazy val root = (project in file("."))
  .settings(shared)
  .aggregate(testRunner,
             webApp)

lazy val testRunner = (project in file("test-runner"))
  .settings(
    shared,
    name := "test-runner",
    description := "The test runner",
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= Seq(
      "io.zeebe" % "zeebe-client-java" % zeebeVersion,
      "org.apache.logging.log4j" % "log4j-api" % "2.9.0",
      "org.apache.logging.log4j" % "log4j-core" % "2.9.0",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.9.0",
      "io.zeebe" % "zeebe-test" % zeebeVersion % "test",
      "io.zeebe" % "zeebe-broker-core" % zeebeVersion % "test"
    )
  )

lazy val webApp = (project in file("web-app"))
  .enablePlugins(AssemblyPlugin, SbtTwirl, ScalatraPlugin)
  .settings(
    shared,
    name := "web-app",
    description := "Webapp for the test runner",
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= Seq(
      "org.scalatra" %% "scalatra" % scalatraVersion,
      "org.scalatra" %% "scalatra-json" % scalatraVersion,
      "org.scalatra" %% "scalatra-scalate" % scalatraVersion,
      "org.json4s" %% "json4s-jackson" % "3.5.2",
      "org.eclipse.jetty" % "jetty-webapp" % "9.4.8.v20171121" % "container;compile",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
      "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % "test"
    ),
    assemblyJarName in assembly := s"${name.value}-${version.value}-full.jar"
  )
  .dependsOn(
    testRunner % "test->test;compile->compile"
  )

