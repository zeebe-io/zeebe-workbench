package io.zeebe.workbench;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.workbench.impl.TestRunner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "runner", defaultPhase = LifecyclePhase.TEST)
public class RunnerMojo extends AbstractMojo {

  private final ObjectMapper mapper = new ObjectMapper();
  private final List<WorkflowResource> workflowResources = new ArrayList<>();
  private final List<TestCase> testCases = new ArrayList<>();

  private final StringBuilder logBuilder = new StringBuilder();
  private final List<FailedVerification> globalFailedVerifications = new ArrayList<>();

  public RunnerMojo() {
    logBuilder
        .append("\n\n-------------------------------------------------------")
        .append("\nZ E E B E - T E S T S\n")
        .append("-------------------------------------------------------");
  }

  @Parameter(property = "resourcesDir", required = true)
  private File resourcesDir;

  @Parameter(property = "outputDir", defaultValue = "${project.build.target}")
  private File outputDir;

  public void execute() throws MojoExecutionException {
    System.setProperty("org.slf4j.simpleLogger.log.io.zeebe", "error");
    System.setProperty("org.slf4j.simpleLogger.log.io.zeebe.workbench", "info");

    if (resourcesDir != null && resourcesDir.isDirectory()) {
      final File[] files = resourcesDir.listFiles();
      if (files != null && files.length > 0) {
        readResources(files);

        if (!testCases.isEmpty()) {
          try (final TestRunner testRunner = new TestRunner()) {
            final List<TestResult> testResults = testRunner.run(workflowResources, testCases);

            for (TestResult result : testResults) {
              final File resultFile = new File(outputDir, result.getName() + ".result");
              mapper.writeValue(resultFile, result);

              logResult(result);
            }

          } catch (Exception ex) {
            throw new MojoExecutionException("Problem in test case execution.", ex);
          }

          logBuilder.append("\n\nResults:\n\n");
          final String resultLog = "Tests run: %d failed: %d";
          final int failedCount = globalFailedVerifications.size();
          logBuilder.append(String.format(resultLog, testCases.size(), failedCount));

          getLog().info(logBuilder.toString());

          if (failedCount > 0) {
            throw new MojoExecutionException("There was " + failedCount + " failing test cases.");
          }
        }
      }
    } else {
      throw new MojoExecutionException("Property 'resourcesDir' need to be a directory.");
    }
  }

  private void logResult(TestResult result) throws MojoExecutionException {
    logBuilder.append("\nTest case '").append(result.getName()).append("'");
    final List<FailedVerification> failedVerifications = result.getFailedVerifications();
    if (failedVerifications.isEmpty()) {
      logBuilder.append(" successful.");
    } else {
      logBuilder.append(" failed.");
      for (FailedVerification failedVerification : failedVerifications) {
        logBuilder.append("\n\tFailed verification: ");
        final String actualPayLoad = failedVerification.getActualPayLoad();
        final Verification verification = failedVerification.getVerification();
        if (actualPayLoad != null) {
          final String payloadLog = "Payload '%s' is not equal to expected payload '%s'.";
          logBuilder.append(
              String.format(payloadLog, actualPayLoad, verification.getExpectedPayload()));
        } else {
          final String verificationLog = "Expected state '%s' of activity '%s' was not reached.";
          logBuilder.append(
              String.format(
                  verificationLog, verification.getExpectedIntent(), verification.getActivityId()));
        }
      }
      this.globalFailedVerifications.addAll(failedVerifications);
    }
  }

  private void readResources(File[] files) throws MojoExecutionException {
    for (File file : files) {
      try {
        if (file.getName().contains(".bpmn")) {

          getLog().debug("Read workflow resource");
          final byte[] bytes = Files.readAllBytes(file.toPath());
          final WorkflowResource resource = new WorkflowResource(bytes, file.getName());
          workflowResources.add(resource);

        } else if (file.getName().contains(".case")) {

          getLog().debug("Read test case");
          final TestCase testCase1 = mapper.readValue(file, TestCase.class);
          testCases.add(testCase1);
        }
      } catch (Exception ex) {
        throw new MojoExecutionException("Failed to open file: " + file.getName(), ex);
      }
    }
  }
}
