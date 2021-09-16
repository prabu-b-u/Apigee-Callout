// Copyright 2016 Apigee Corp, 2017-2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ------------------------------------------------------------------

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.Message;
import java.util.Properties;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestContentSetter extends TestBase {

  @Test
  public void setContent1() throws Exception {
    byte[] imageBytes = loadImageBytes("Logs_512px.png.b64");
    msgCtxt.setVariable("imageData", imageBytes);

    Properties props = new Properties();
    props.put("debug", "true");
    props.put("contentVar", "imageData");
    // props.put("destination", "message");
    props.put("contentType", "image/png");
    props.put("fileName", "Logs_512px.png");

    ContentSetter callout = new ContentSetter(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("cs_error");
    Assert.assertNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("cs_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    // cannot directly reference message.content with the mocked MessageContext
    // Object output = msgCtxt.getVariable("message.content");
    Message msg = msgCtxt.getMessage();
    // Message msg = msgCtxt.getVariable("message");
    Assert.assertNotNull(msg, "message");

    Object output = msg.getContent();
    Assert.assertNotNull(output, "no output");
  }
}
