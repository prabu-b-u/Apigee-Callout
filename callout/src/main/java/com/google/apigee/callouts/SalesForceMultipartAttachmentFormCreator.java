package com.google.apigee.callouts;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jclouds.io.Payloads;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.jclouds.io.payloads.MultipartForm;
import org.jclouds.io.payloads.Part;
import org.jclouds.io.payloads.StringPayload;
import org.jclouds.io.payloads.Part.PartOptions;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.AdapterHttpServletRequest;

public class SalesForceMultipartAttachmentFormCreator extends CalloutBase implements Execution {
	private static final String varprefix = "mpf_";
	public SalesForceMultipartAttachmentFormCreator(@SuppressWarnings("rawtypes") Map properties) {
		super(properties);
	}

	private static List<FileItem> parseForm(final byte[] data, final String contentType) throws Exception {
		final DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
		fileItemFactory.setSizeThreshold(10 * 1024 * 1024);
		final ServletFileUpload upload = new ServletFileUpload(fileItemFactory);
		final HttpServletRequest request = new AdapterHttpServletRequest(data, contentType);
		final boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if ((!isMultipart)) {
			throw new IllegalStateException("Illegal request for uploading files. Multipart request expected.");
		}
		final List<FileItem> iter = upload.parseRequest(request);
		return iter;
	}

	private String getSource(MessageContext msgCtxt) throws Exception {
		String source = getSimpleOptionalProperty("source", msgCtxt);
		if (source == null) {
			source = "message";
		}
		return source;
	}

	private String getDestination(MessageContext msgCtxt) throws Exception {
		String destination = getSimpleOptionalProperty("destination", msgCtxt);
		if (destination == null) {
			destination = "message";
		}
		return destination;
	}

	private List<FileItem> parseMultipartForm(MessageContext msgCtxt) throws Exception {
		System.out.println("Reading the Multipart Form Request Received..");
		// Reading incoming request
		String source = getSource(msgCtxt);
		Message message = (Message) msgCtxt.getVariable(source);
		if (message == null) {
			throw new IllegalStateException("source message is null.");
		}
		InputStream input = message.getContentAsStream();
		byte[] inputBytes = streamToByteArray(input);
		System.out.println("Multipart Form Payload Input bytes size.."+inputBytes.length);
		
		List<FileItem> items = parseForm(inputBytes, message.getHeader("content-type"));
		return items;
	}

	public ExecutionResult execute(MessageContext msgCtxt, ExecutionContext executionContext) {
		MultipartForm mpf = null;
		try {
			List<FileItem> items = parseMultipartForm(msgCtxt);
			System.out.println("Multipart Form Items Size:"+items.size());
			boolean mustSetDestination = false;

			String boundary = "--------------------" + randomAlphanumeric(14);
			String destination = getDestination(msgCtxt);
			Message message = (Message) msgCtxt.getVariable(destination);
			if (message == null) {
				mustSetDestination = true;
				message = msgCtxt
						.createMessage(msgCtxt.getClientConnection().getMessageFactory().createRequest(msgCtxt));
			}
			msgCtxt.setVariable(varName("boundary"), boundary);
			message.setHeader("content-type", "multipart/form-data;boundary=" + boundary);
			System.out.println("Boundary SET*******"+boundary);

			Part[] mpReqParts = new Part[items.size()];
			System.out.println("mpReqParts part size*******"+mpReqParts.length);
			int counter = 0;
			for (FileItem item : items) {
				System.out.println("Item name: "+item.getName()+", Item contentType: "+item.getContentType()+", Content:"+item.getString());
				
				Part.PartOptions partOptions = new Part.PartOptions();
				partOptions.contentType(item.getContentType());
				
				Part part = null;
				
				if (item.isFormField()) {
					System.out.println("Create String payload from the item name:{}"+item.getName());	
					
					StringPayload payload = Payloads.newStringPayload(item.getString());
					part = Part.create(item.getFieldName(), payload, partOptions);
				} else {
					System.out.println("Create Byte Array payload from the item name:{}"+item.getName());	
					byte[] itemBytes = streamToByteArray(item.getInputStream());
					
					partOptions.contentType("application/octet-stream");
				    partOptions.filename(item.getName());
					part = Part.create(item.getFieldName(), new ByteArrayPayload((byte[]) itemBytes),
							partOptions);
				}
				mpReqParts[counter++] = part;
			}

			// Create Multipart form
			mpf = new MultipartForm(boundary, mpReqParts);
			System.out.println("Multipart Form payload formed successfully***********");
			byte[] payload = streamToByteArray(mpf.openStream());
			msgCtxt.setVariable(varName("payload_length"), payload.length);
			
			//message.setHeader("Consumer-Key", "9yYvQHjhxuEoVMVvnaQ0xd6FoxJDTmtP");
			
			message.setHeader("Authorization", "Bearer "+msgCtxt.getVariable("accessToken"));
			message.setHeader("content-length", payload.length);
			message.setContent(new ByteArrayInputStream(payload));

			System.out.println("MustSetDestination value:"+mustSetDestination);
			if (mustSetDestination) {
				msgCtxt.setVariable(destination, message);
			}
            System.out.println("Going to Return Successs******");
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
		} finally {
			if (mpf != null)
				mpf.close();
		}
	}

	@Override
	public String getVarnamePrefix() {
		 return varprefix;
	}
	
	public static void main(String[] args) {
		
	}

}
