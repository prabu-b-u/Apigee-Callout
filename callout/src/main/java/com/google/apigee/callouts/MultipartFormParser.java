// MultipartFormParser.java
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
import com.google.apigee.AdapterHttpServletRequest;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class MultipartFormParser extends CalloutBase implements Execution {
  private static final String varprefix = "mpf_";
  private static final boolean wantStringDefault = true;

  public MultipartFormParser(Map properties) {
    super(properties);
  }

  public String getVarnamePrefix() {
    return varprefix;
  }

  private String getSource(MessageContext msgCtxt) throws Exception {
    String source = getSimpleOptionalProperty("source", msgCtxt);
    if (source == null) {
      source = "message";
    }
    return source;
  }

  private static List<FileItem> parseForm(final byte[] data, final String contentType)
      throws Exception {
    final DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
    fileItemFactory.setSizeThreshold(5*1024*1024);
    final ServletFileUpload upload = new ServletFileUpload(fileItemFactory);
    final HttpServletRequest request = new AdapterHttpServletRequest(data, contentType);
    final boolean isMultipart = ServletFileUpload.isMultipartContent(request);
    if ((!isMultipart)) {
      throw new IllegalStateException(
          "Illegal request for uploading files. Multipart request expected.");
    }
    final List<FileItem> iter = upload.parseRequest(request);
    return iter;
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String source = getSource(msgCtxt);
      Message message = (Message) msgCtxt.getVariable(source);
      if (message == null) {
        throw new IllegalStateException("source message is null.");
      }
      InputStream input = message.getContentAsStream();
      byte[] inputBytes = streamToByteArray(input); // read it all
      List<FileItem> items = parseForm(inputBytes, message.getHeader("content-type"));
      List<String> names = new ArrayList<String>();
      int n = 0;
      for (FileItem item : items) {
        if (item.isFormField()) {
          // ... ignore any fields in the form
        } else {
          String fileName = item.getName().replaceAll("[^a-zA-Z0-9_\\. ]", "");
          names.add(fileName);
          byte[] itemBytes = streamToByteArray(item.getInputStream());
          msgCtxt.setVariable(varName("item_filename_" + n), fileName);
          msgCtxt.setVariable(varName("item_content_" + n), itemBytes);
          msgCtxt.setVariable(varName("item_content-type_" + n), item.getContentType());
          msgCtxt.setVariable(varName("item_size_" + n), item.getSize() + "");
          n++;
        }
      }
      msgCtxt.setVariable(varName("items"), String.join(", ", names));
      msgCtxt.setVariable(varName("itemcount"), names.size() + "");
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
