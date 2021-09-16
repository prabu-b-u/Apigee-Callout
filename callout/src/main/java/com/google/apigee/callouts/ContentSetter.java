// ContentSetter.java
//
// Copyright (c) 2018-2021 Google LLC.
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

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ContentSetter extends CalloutBase implements Execution {
  private static final String varprefix = "cs_";
  private static final boolean wantStringDefault = true;

  public ContentSetter(Map properties) {
    super(properties);
  }

  public String getVarnamePrefix() {
    return varprefix;
  }

  private String getDestination(MessageContext msgCtxt) throws Exception {
    String destination = getSimpleOptionalProperty("destination", msgCtxt);
    if (destination == null) {
      destination = "message";
    }
    return destination;
  }

  private String getContentVar(MessageContext msgCtxt) throws Exception {
    return getSimpleRequiredProperty("contentVar", msgCtxt);
  }

  private String getContentType(MessageContext msgCtxt) throws Exception {
    return getSimpleOptionalProperty("contentType", msgCtxt);
  }

  public ExecutionResult execute(
      final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      boolean mustSetDestination = false;
      String contentVar = getContentVar(msgCtxt);
      String destination = getDestination(msgCtxt);
      Message message = (Message) msgCtxt.getVariable(destination);
      if (message == null) {
        mustSetDestination = true;
        message =
            msgCtxt.createMessage(
                msgCtxt.getClientConnection().getMessageFactory().createRequest(msgCtxt));
      }
      Object content = msgCtxt.getVariable(contentVar);
      byte[] contentBytes =
          (content instanceof byte[])
              ? (byte[]) content
              : ((String) content).getBytes(StandardCharsets.UTF_8);
      msgCtxt.setVariable(varName("payload_length"), contentBytes.length);
      String contentType = getContentType(msgCtxt);
      if (contentType != null) {
        message.setHeader("content-type", contentType);
      }
      message.setContent(new ByteArrayInputStream(contentBytes));
      if (mustSetDestination) {
        msgCtxt.setVariable(destination, message);
      }
      return ExecutionResult.SUCCESS;
    } catch (IllegalStateException exc1) {
      setExceptionVariables(exc1, msgCtxt);
      return ExecutionResult.ABORT;
    } catch (Exception e) {
      if (getDebug()) {
        String stacktrace = getStackTraceAsString(e);
        msgCtxt.setVariable(varName("stacktrace"), stacktrace);
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    }
  }
}
