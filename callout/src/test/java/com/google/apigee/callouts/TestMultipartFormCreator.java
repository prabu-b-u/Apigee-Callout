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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestMultipartFormCreator extends TestBase {

  @Test
  public void create_Simple_SinglePart() throws Exception {
    byte[] imageBytes = loadImageBytes("Logs_512px.png.b64");
    msgCtxt.setVariable("base64EncodedImageData", new String(imageBytes, StandardCharsets.UTF_8));

    Properties props = new Properties();
    props.put("contentVar", "base64EncodedImageData");
    props.put("want-decode", "true");
    props.put("contentType", "image/png");
    props.put("part-name", "image");
    props.put("fileName", "Logs_512px.png");
    // props.put("outputVar", outputVar);

    MultipartFormCreator callout = new MultipartFormCreator(props);

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
    Message msg = msgCtxt.getMessage();
    Object output = msg.getContent();
    Assert.assertNotNull(output, "no output");
  }

  private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {

    Boolean wantAppend = false;
    try (FileOutputStream outputStream = new FileOutputStream(file, wantAppend)) {
      int read;
      byte[] bytes = new byte[1024];
      while ((read = inputStream.read(bytes)) != -1) {
        outputStream.write(bytes, 0, read);
      }
    }
  }

  @Test
  public void create_Json_MultipleParts() throws Exception {
    String descriptorJson =
        "{\n"
            + "  \"part1.json\" : {\n"
            + "    \"content-var\" :  \"descriptor-json\",\n"
            + "    \"content-type\" : \"application/json\",\n"
            + "    \"want-b64-decode\": false\n"
            + "  },\n"
            + "  \"part2.png\" : {\n"
            + "    \"content-var\" :  \"imageBytes\",\n"
            + "    \"content-type\" : \"image/png\",\n"
            + "    \"want-b64-decode\": false,\n"
            + "    \"file-name\": \"Logs_512px.png\"\n"
            + "  }\n"
            + "}\n";

    byte[] imageBytes = loadImageBytes("Logs_512px.png");
    msgCtxt.setVariable("imageBytes", imageBytes);
    msgCtxt.setVariable("descriptor-json", descriptorJson);

    Properties props = new Properties();
    props.put("descriptor", descriptorJson);
    // props.put("destination", "destination");
    props.put("debug", "true");

    MultipartFormCreator callout = new MultipartFormCreator(props);

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

    copyInputStreamToFile(is, new File("./create_Json_MultipleParts.out"));
  }

  // It is not practical to test the "destination does not exist" case without
  // a full Apigee Edge runtime. Need ClientConnection.
  //
  // @Test
  // public void test_DestinationMessageDoesNotExist() throws Exception {
  //     String filename = "Logs_512px.png.b64";
  //     String destinationVariable = "foo";
  //     //String outputVar = "output_payload";
  //     Path path = Paths.get(testDataDir, filename);
  //     if (!Files.exists(path)) {
  //         throw new IOException("file(" + path.toString() + ") not found");
  //     }
  //     InputStream imageInputStream = Files.newInputStream(path);
  //     byte[] imageBytes = IOUtils.toByteArray(imageInputStream);
  //     msgCtxt.setVariable("base64EncodedImageData", new String(imageBytes,
  // StandardCharsets.UTF_8));
  //
  //     Properties props = new Properties();
  //     props.put("contentVar", "base64EncodedImageData");
  //     props.put("contentType", "image/png");
  //     props.put("destination", destinationVariable);
  //     props.put("part-name", "image");
  //     // props.put("outputVar", outputVar);
  //
  //     MultipartFormCreator callout = new MultipartFormCreator(props);
  //
  //     // execute and retrieve output
  //     ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
  //     ExecutionResult expectedResult = ExecutionResult.SUCCESS;
  //     Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");
  //
  //     // check result and output
  //     Object error = msgCtxt.getVariable("mpf_error");
  //     Assert.assertNull(error, "error");
  //
  //     Object stacktrace = msgCtxt.getVariable("mpf_stacktrace");
  //     Assert.assertNull(stacktrace, "stacktrace");
  //
  //     Message msg = msgCtxt.getVariable(destinationVariable);
  //     String resultContent = msg.getContent();
  //     Assert.assertNotNull(resultContent, "resultContent");
  // }

  // @Test
  // public void testDataProviders() throws IOException {
  //     Assert.assertTrue(getDataForBatch1().length > 0);
  // }
  //
  // @Test(dataProvider = "batch1")
  // public void test2_Configs(TestCase tc) throws Exception {
  //     if (tc.getDescription()!= null)
  //         System.out.printf("  %10s - %s\n", tc.getTestName(), tc.getDescription() );
  //     else
  //         System.out.printf("  %10s\n", tc.getTestName() );
  //
  //     Path path = Paths.get(testDataDir, tc.getInput());
  //     if (!Files.exists(path)) {
  //         throw new IOException("file("+tc.getInput()+") not found");
  //     }
  //
  //     messageContentStream = Files.newInputStream(path);
  //
  //     Base64 callout = new Base64(tc.getProperties());
  //
  //     // execute and retrieve output
  //     ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
  //
  //     String s = tc.getExpected().get("success");
  //     ExecutionResult expectedResult = (s!=null && s.toLowerCase().equals("true")) ?
  //                                        ExecutionResult.SUCCESS : ExecutionResult.ABORT;
  //     // check result and output
  //     if (expectedResult == actualResult) {
  //         if (expectedResult == ExecutionResult.SUCCESS) {
  //             String fname = tc.getExpected().get("output");
  //             path = Paths.get(testDataDir, fname);
  //             if (!Files.exists(path)) {
  //                 throw new IOException("expected output file("+fname+") not found");
  //             }
  //             byte[] expectedOutputBytes = IOUtils.toByteArray(Files.newInputStream(path));
  //             boolean stringOutput =
  //                 (((String)(msgCtxt.getVariable("b64_action"))).equals("encode")) &&
  //                 (boolean)(msgCtxt.getVariable("b64_wantString"));
  //
  //             //byte[] actualOutputBytes =
  // IOUtils.toByteArray(msgCtxt.getMessage().getContentAsStream());
  //             Object messageContent = msgCtxt.getVariable("message.content");
  //             byte[] actualOutputBytes = null;
  //             if (messageContent instanceof String){
  //                 actualOutputBytes = ((String)messageContent).getBytes(StandardCharsets.UTF_8);
  //             }
  //             else if (messageContent instanceof byte[]){
  //                 actualOutputBytes = (byte[])messageContent;
  //             }
  //             else if (messageContent instanceof InputStream){
  //                 actualOutputBytes = IOUtils.toByteArray((InputStream)messageContent);
  //             }
  //
  //             if (!expectedOutputBytes.equals(actualOutputBytes)) {
  //
  //                 // String digest1 = DigestUtils.sha256Hex(actualOutputBytes);
  //                 // String digest2 = DigestUtils.sha256Hex(expectedOutputBytes);
  //                 // System.err.printf("    digest got: %s\n", digest1);
  //                 // System.err.printf("    expected  : %s\n", digest2);
  //
  //                 System.err.printf("    got: %s\n", Hex.encodeHexString( actualOutputBytes ) ) ;
  //                 System.err.printf("    expected  : %s\n", Hex.encodeHexString(
  // expectedOutputBytes ) ) ;
  //
  //                 // the following will throw
  //                 Assert.assertEquals(actualOutputBytes, expectedOutputBytes, tc.getTestName() +
  // ": result not as expected");
  //             }
  //         }
  //         else {
  //             String expectedError = tc.getExpected().get("error");
  //             Assert.assertNotNull(expectedError, tc.getTestName() + ": broken test: no expected
  // error specified");
  //
  //             String actualError = msgCtxt.getVariable("b64_error");
  //             Assert.assertEquals(actualError, expectedError, tc.getTestName() + ": error not as
  // expected");
  //         }
  //     }
  //     else {
  //         String observedError = msgCtxt.getVariable("b64_error");
  //         System.err.printf("    observed error: %s\n", observedError);
  //
  //         Assert.assertEquals(actualResult, expectedResult, tc.getTestName() + ": result not as
  // expected");
  //     }
  //     System.out.println("=========================================================");
  // }

}
