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

  private final List<WorkflowResource> workflowResources = new ArrayList<>();
  private final List<TestCase> testCases = new ArrayList<>();

  @Parameter(property = "resourcesDir", required = true)
  private File resourcesDir;

  public void execute() throws MojoExecutionException {
    if (resourcesDir != null && resourcesDir.isDirectory()) {
      final File[] files = resourcesDir.listFiles();
      if (files != null && files.length > 0) {
        readResources(files);

        if (!testCases.isEmpty()) {

          try (final TestRunner testRunner = new TestRunner()) {
            testRunner.run(workflowResources, testCases);
          } catch (Exception ex) {
            throw new MojoExecutionException("Problem in test case execution.", ex);
          }
        }
      }
    } else {
      throw new MojoExecutionException("Property 'resourcesDir' need to be a directory.");
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
          final byte[] bytes = Files.readAllBytes(file.toPath());
          getLog().debug(new String(bytes));

          final ObjectMapper mapper = new ObjectMapper();
          final TestCase testCase1 = mapper.readValue(bytes, TestCase.class);

          //
          //          final JsonNode jsonNode = mapper.readTree(bytes);
          //
          //          getLog().debug(jsonNode.get("name").asText());
          //          getLog().debug(jsonNode.toString());
          //
          //          final TestCase testCase = mapper.treeToValue(jsonNode, TestCase.class);
          testCases.add(testCase1);
        }
      } catch (Exception ex) {
        throw new MojoExecutionException("Failed to open file: " + file.getName(), ex);
      }
    }
  }
}
