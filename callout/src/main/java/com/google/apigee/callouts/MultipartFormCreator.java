// MultipartFormCreator.java
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

import com.google.apigee.json.JavaxJson;
import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.jclouds.io.payloads.MultipartForm;
import org.jclouds.io.payloads.Part;

public class MultipartFormCreator extends CalloutBase implements Execution {
  private static final String varprefix = "mpf_";
  private static final boolean wantStringDefault = true;

  public MultipartFormCreator(Map properties) {
    super(properties);
  }

  public String getVarnamePrefix() {
    return varprefix;
  }

  private boolean getWantDecode(MessageContext msgCtxt) throws Exception {
    String wantDecode = getSimpleOptionalProperty("want-base64-decode", msgCtxt);
    if (wantDecode == null) {
      return false;
    }
    return Boolean.parseBoolean(wantDecode.toLowerCase());
  }

  private String getDestination(MessageContext msgCtxt) throws Exception {
    String destination = getSimpleOptionalProperty("destination", msgCtxt);
    if (destination == null) {
      destination = "message";
    }
    return destination;
  }

  private String getDescriptor(MessageContext msgCtxt) throws Exception {
    return getSimpleOptionalProperty("descriptor", msgCtxt);
  }

  private String getPartContentVar(MessageContext msgCtxt) throws Exception {
    return getSimpleRequiredProperty("contentVar", msgCtxt);
  }

  private String getPartContentType(MessageContext msgCtxt) throws Exception {
    return getSimpleRequiredProperty("contentType", msgCtxt);
  }

  private String getPartFileName(MessageContext msgCtxt) throws Exception {
    return getSimpleOptionalProperty("fileName", msgCtxt);
  }

  private String getPartName(MessageContext msgCtxt) throws Exception {
    return getSimpleRequiredProperty("part-name", msgCtxt);
  }

  private ExecutionResult execute_20200309(
      final MessageContext msgCtxt, final ExecutionContext execContext) {
    // original implementation, creates a form with a single part
    try {
      String contentVar = getPartContentVar(msgCtxt);
      boolean mustSetDestination = false;
      String content = (String) msgCtxt.getVariable(contentVar);
      byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
      boolean wantDecode = getWantDecode(msgCtxt);
      if (wantDecode) {
        byte[] decodedBytes = Base64.getDecoder().decode(contentBytes);
        msgCtxt.setVariable(varName("decoded_length"), decodedBytes.length);
        contentBytes = decodedBytes;
      }

      String boundary = "--------------------" + randomAlphanumeric(14);
      String destination = getDestination(msgCtxt);
      Message message = (Message) msgCtxt.getVariable(destination);
      if (message == null) {
        mustSetDestination = true;
        message =
            msgCtxt.createMessage(
                msgCtxt.getClientConnection().getMessageFactory().createRequest(msgCtxt));
      }
      msgCtxt.setVariable(varName("boundary"), boundary);
      message.setHeader("content-type", "multipart/form-data;boundary=" + boundary);

      Part.PartOptions partOptions = new Part.PartOptions();
      partOptions.contentType(getPartContentType(msgCtxt));
      if (getPartFileName(msgCtxt) != null) {
        partOptions.filename(getPartFileName(msgCtxt));
      }
      Part filepart =
          Part.create(
              getPartName(msgCtxt),
              new ByteArrayPayload((byte[]) contentBytes),
              partOptions);

      MultipartForm mpf = new MultipartForm(boundary, new Part[] {filepart});
      byte[] payload = streamToByteArray(mpf.openStream());
      msgCtxt.setVariable(varName("payload_length"), payload.length);
      message.setContent(new ByteArrayInputStream(payload));
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

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String descriptor = getDescriptor(msgCtxt);
      if (descriptor == null) {
        return execute_20200309(msgCtxt, execContext);
      }

      boolean mustSetDestination = false;
      Map<String, Object> map = JavaxJson.fromJson(descriptor, Map.class);
      //Map<String, Object> map = gson.fromJson(new StringReader(), Map.class);
      // eg
      // {
      //   "part1.txt" : {
      //     "content-var" :  "variable-name-here",
      //     "content-type" : "content-type-here",
      //     "want-b64-decode": false
      //   },
      //   "part2.png" : {
      //     "content-var" :  "variable-name-here",
      //     "content-type" : "content-type-here",
      //     "want-b64-decode": false
      //   }
      // }

      String boundary = "--------------------" + randomAlphanumeric(14);
      msgCtxt.setVariable(varName("boundary"), boundary);
      String destination = getDestination(msgCtxt);
      Message message = (Message) msgCtxt.getVariable(destination);
      if (message == null) {
        mustSetDestination = true;
        message =
            msgCtxt.createMessage(
                msgCtxt.getClientConnection().getMessageFactory().createRequest(msgCtxt));
      }
      message.setHeader("content-type", "multipart/form-data;boundary=" + boundary);

      List<Part> parts = new ArrayList<Part>();
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        String partName = entry.getKey();
        Map<String, Object> partDefinition = (Map<String, Object>) entry.getValue();

        Object partContent = msgCtxt.getVariable((String) partDefinition.get("content-var"));
        if (partContent instanceof String) {
          Boolean wantDecode = (Boolean) partDefinition.get("want-b64-decode");
          String s = (String) partContent;
          partContent = s.getBytes(StandardCharsets.UTF_8);
          if (wantDecode) {
            partContent = Base64.getDecoder().decode((byte[]) partContent);
          }
        } else if (!(partContent instanceof byte[])) {
          throw new IllegalStateException(String.format("part %s not of supported type", partName));
        }

        Part.PartOptions partOptions =
          new Part.PartOptions().contentType((String) partDefinition.get("content-type"));
        if (partDefinition.get("file-name") != null && !partDefinition.get("file-name").equals("")) {
          partOptions.filename((String) partDefinition.get("file-name"));
        }

        if (partDefinition.get("transfer-encoding") != null) {
          partOptions.transferEncoding((String) partDefinition.get("transfer-encoding"));
        }

        parts.add(
            Part.create(
                partName,
                new ByteArrayPayload((byte[]) partContent),
                partOptions));
      }

      MultipartForm mpf = new MultipartForm(boundary, parts);
      byte[] payload = streamToByteArray(mpf.openStream());
      msgCtxt.setVariable(varName("payload_length"), payload.length);
      message.setContent(new ByteArrayInputStream(payload));
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
