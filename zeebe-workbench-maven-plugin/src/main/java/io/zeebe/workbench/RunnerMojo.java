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
import org.agrona.ExpandableArrayBuffer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

@Mojo(name = "runner", defaultPhase = LifecyclePhase.TEST)
public class RunnerMojo extends AbstractMojo {

  @Parameter(property = "resourcesDir", required = true)
  private File resourcesDir;

  public void execute() throws MojoExecutionException {
    if (resourcesDir != null && resourcesDir.isDirectory()) {
      final File[] files = resourcesDir.listFiles();
      if (files != null && files.length > 0) {
        for (File file : files) {
          try {
            if (file.getName().contains(".bpmn")) {
              final BufferedInputStream bufferedInputStream =
                  new BufferedInputStream(new FileInputStream(file));

              // workflow resource
            } else if (file.getName().contains(".case")) {
              final ObjectMapper mapper = new ObjectMapper();
              mapper.readTree(file);
              // test case definition

            }

          } catch (Exception ex) {
            throw new MojoExecutionException("Failed to open file: " + file.getName(), ex);
          }
        }
      }
    } else {
      throw new MojoExecutionException("Property 'resourcesDir' need to be a directory.");
    }
  }

  private byte[] readBytes(File file) throws MojoExecutionException {

    final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();

    try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {


      final List<String> collect = bufferedReader.lines().collect(Collectors.toList());




    } catch (Exception ex) {
      throw new MojoExecutionException("Failed to open file: " + file.getName(), ex);
    }

    return null;
  }
}
