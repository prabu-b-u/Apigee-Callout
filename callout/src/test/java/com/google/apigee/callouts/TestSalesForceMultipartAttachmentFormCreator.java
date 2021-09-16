package com.google.apigee.callouts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;

import mockit.Mock;
import mockit.MockUp;

public class TestSalesForceMultipartAttachmentFormCreator extends TestBase {

	static MessageContext msgCtxt;
	static   InputStream messageContentStream;
	static Message message;
	static ExecutionContext exeCtxt;

	  public static void beforeMethod1() {

	    msgCtxt =
	        new MockUp<MessageContext>() {
	          private Map variables;

	          public void $init() {
	            variables = new HashMap();
	          }

	          @Mock()
	          public <T> T getVariable(final String name) {
	            if (variables == null) {
	              variables = new HashMap();
	            }
	            // T value = null;
	            // if (name.equals("message")) {
	            //     value = (T) message;
	            // }
	            // value = (T) variables.get(name);
	            // if (verbose)
	            //   System.out.printf(
	            //                     "getVariable(%s) <= %s\n", name, (value != null) ? value :
	            // "(null)");
	            // return value;

	            if (name.equals("message")) {
	              return (T) message;
	            }
	            return (T) variables.get(name);
	          }

	          @Mock()
	          public boolean setVariable(final String name, final Object value) {
	            if (verbose)
	              System.out.printf(
	                  "setVariable(%s) <= %s\n", name, (value != null) ? value : "(null)");
	            if (variables == null) {
	              variables = new HashMap();
	            }
	            if (name.equals("message.content")) {
	              if (value instanceof String) {
	                messageContentStream =
	                    new ByteArrayInputStream(((String) value).getBytes(StandardCharsets.UTF_8));
	              } else if (value instanceof InputStream) {
	                messageContentStream = (InputStream) value;
	              }
	            }
	            variables.put(name, value);
	            return true;
	          }

	          @Mock()
	          public boolean removeVariable(final String name) {
	            if (verbose) System.out.printf("removeVariable(%s)\n", name);
	            if (variables == null) {
	              variables = new HashMap();
	            }
	            if (variables.containsKey(name)) {
	              variables.remove(name);
	            }
	            return true;
	          }

	          @Mock()
	          public Message getMessage() {
	            return message;
	          }
	        }.getMockInstance();

	    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();

	    message =
	        new MockUp<Message>() {
	          @Mock()
	          public InputStream getContentAsStream() {
	            return messageContentStream;
	          }

	          @Mock()
	          public void setContent(InputStream is) {
	            // System.out.printf("\n** setContent(Stream)\n");
	            messageContentStream = is;
	          }

	          @Mock()
	          public void setContent(String content) {
	            // System.out.printf("\n** setContent(String)\n");
	            messageContentStream =
	                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	          }

	          @Mock()
	          public String getContent() {
	            // System.out.printf("\n** getContent()\n");
	            try {
	              StringWriter writer = new StringWriter();
	              IOUtils.copy(messageContentStream, writer, StandardCharsets.UTF_8);
	              return writer.toString();
	            } catch (Exception ex1) {
	              return null;
	            }
	          }
	        }.getMockInstance();
	  }
	  
	public static void create_Json_MultipleParts() throws Exception {
		
		beforeMethod1();
		
		String descriptorJson = "{\n" + "  \"part1.json\" : {\n" + "    \"content-var\" :  \"descriptor-json\",\n"
				+ "    \"content-type\" : \"application/json\",\n" + "    \"want-b64-decode\": false\n" + "  },\n"
				+ "  \"part2.png\" : {\n" + "    \"content-var\" :  \"imageBytes\",\n"
				+ "    \"content-type\" : \"image/png\",\n" + "    \"want-b64-decode\": false,\n"
				+ "    \"file-name\": \"Logs_512px.png\"\n" + "  }\n" + "}\n";

		byte[] imageBytes = loadImageBytes("Logs_512px.png");
		msgCtxt.setVariable("binaryPart1", imageBytes);
		msgCtxt.setVariable("attachment-collection", descriptorJson);

		Properties props = new Properties();
		// props.put("descriptor", descriptorJson);
		// props.put("destination", "destination");
		props.put("debug", "true");

		SalesForceMultipartAttachmentFormCreator callout = new SalesForceMultipartAttachmentFormCreator(props);

		// execute and retrieve output
		ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
		ExecutionResult expectedResult = ExecutionResult.SUCCESS;
		Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

		// check result and output
		Object error = msgCtxt.getVariable("mpf_error");
		Assert.assertNull(error, "error");

		Object stacktrace = msgCtxt.getVariable("mpf_stacktrace");
		Assert.assertNull(stacktrace, "stacktrace");

		// cannot directly reference message.content with the mocked MessageContext
		// Object output = msgCtxt.getVariable("message.content");
		Message msg = msgCtxt.getVariable("message");
		InputStream is = msg.getContentAsStream();
		Assert.assertNotNull(is, "no stream");

		// copyInputStreamToFile(is, new File("./create_Json_MultipleParts.out"));
	}
	
	public static void main(String[] args) throws Exception {
		create_Json_MultipleParts();
	}

}
