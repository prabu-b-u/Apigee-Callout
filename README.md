# Apigee Multipart Form Callout

This directory contains the Java source code and pom.xml file required to build a Java callout for Apigee that
creates a multipart form payload, from a single blob, or parses an inbound multipart form payload.

For creating the multipart form, it relies on Apache-licensed code lifted from [Apache jclouds](https://github.com/jclouds/jclouds).
For parsing, it relies on Apache-licensed code lifted from [javadelight](https://github.com/javadelight/delight-fileupload). I didn't use the entire libraries for either of these things, because they drag in too many un-desired dependencies.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.


## Using this policy

You do not need to build the source code in order to use the policy in
Apigee Edge.  All you need is the built JAR, and the appropriate
configuration for the policy.  If you want to build it, feel free.  The
instructions are at the bottom of this readme. Even without
instructions, you should be able to figure it out if you know and use
maven.


1. copy the jar file, available in
   target/apigee-multipart-form-20210414.jar , if you have built the
   jar, or in [the repo](bundle/apiproxy/resources/java/apigee-multipart-form-20210414.jar)
   if you have not, to your apiproxy/resources/java directory. You can
   do this offline, or using the graphical Proxy Editor in the Apigee
   Edge Admin Portal.

2. include an XML file for the Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:

   ```xml
    <JavaCallout name='Java-Multipart-Form-1'>
        ...
      <ClassName>com.google.apigee.callouts.MultipartFormCreator</ClassName>
      <ResourceURL>java://apigee-multipart-form-20210414.jar</ResourceURL>
    </JavaCallout>
   ```

3. use the Edge UI, or a command-line tool like
   [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js/blob/master/examples/importAndDeploy.js) or
   [apigeetool](https://github.com/apigee/apigeetool-node)
   or similar to
   import your proxy into an Edge organization, and then deploy the proxy .
   Eg, `./importAndDeploy.js -n -v -o ${ORG} -e ${ENV} -d bundle/`

4. Use a client to generate and send http requests to the proxy you just deployed . Eg,
   ```
   curl -i https://$ORG-$ENV.apigee.net/myproxy/foo
   ```


## Notes on Usage

This repo includes three callout classes,

* com.google.apigee.callouts.MultipartFormCreator - create a form payload

* com.google.apigee.callouts.MultipartFormParser - parse a form payload

* com.google.apigee.callouts.ContentSetter - set a byte array into message content

## MultipartFormCreator

This callout will create a form payload, using inputs that you specify.

It accepts two properties as input:

| property name   | description                                                                                  |
| ----------------| -------------------------------------------------------------------------------------------- |
| **descriptor**  | required\*. a JSON string, which describes the parts to add to the form. See details below.  |
| **destination** | optional, a string, the name of a message. If it does not exist, it will be created. Defaults to 'message'.          |


An example for creating a form:

```xml
<JavaCallout name='Java-CreateMultipartForm'>
  <Properties>
    <Property name="descriptor">
    {
      "part1.xml" : {
        "content-var" :  "variable-holding-xml",
        "content-type" : "application/xml",
        "want-b64-decode": false,
      },
      "part2.png" : {
        "content-var" :  "image-bytes",
        "content-type" : "image/png",
        "want-b64-decode": false,
        "file-name": "part2.png"
      }
    }
    </Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.MultipartFormCreator</ClassName>
  <ResourceURL>java://apigee-multipart-form-20210414.jar</ResourceURL>
</JavaCallout>
```

This will tell the policy to create a multipart form, store it in `message`
(because there is no destination property), and include 2 content parts within
that form:

1. the first part, with content-type = application/xml, named part1.xml ,
   using content from a variable named `variable-holding-xml`.

2. the second part, with content-type = image/png, named part2.png, using
   content from a variable named `image-bytes`.


How you get the data into the specified variables is up to you! The result of the policy above would be a form with content like this:

```
----------------------G70E38XDL4FRMV
Content-Disposition: form-data; name="part1.xml"
Content-Type: application/xml
<root>
  <element1>Hello World</element1>
  <element2>
     <note>this is a child element</note>
  </element2>
</root>

----------------------G70E38XDL4FRMV
Content-Disposition: form-data; name="part2.png"; filename="part2.png"
Content-Type: image/png

PNG

...png data here...
----------------------G70E38XDL4FRMV--
```


There is one additional possible field in the descriptor, not shown above
`transfer-encoding`. If it is present, then the policy will add a header
specifying that content-transfer-encoding for the given part.  For example, to send a base64-encoded file and mark it as such, use this as a descriptor:

```
    {
      "part2.png" : {
        "content-var" :  "base64-encoded-image-bytes",
        "content-type" : "image/png",
        "want-b64-decode": false,
        "transfer-encoding": "base64"
      }
    }
```

And the output will be like so:

```
----------------------73B8NBN4LFYLBB
Content-Disposition: form-data; name="image"
Content-Type: image/png
Content-Transfer-Encoding: base64

iVBOR...base64-encoded data here...
----------------------73B8NBN4LFYLBB--
```


## Creating a form with a single part

The table above describes the `descriptor` property as required. This isn't
quite true. It is also possible to specify the input for a form that will
contain _a single part_, using individual properties.  If you do not provide the
`descriptor` in the policy configuration, then the policy will look for these
other properties:


| property name          | status   | description                                                                         |
| ---------------------- | -------- | ----------------------------------------------------------------------------------- |
| **contentVar**         | required | name of a variable containing a string which represents a base64-encoded byte array |
| **contentType**        | required | a string, something like `image/png` or `application/json` etc                      |
| **part-name**          | required | a string, the name of the part within the form.                                     |
| **fileName**          | optional | a string, the file name of the part within the form.                                     |
| **want-base64-decode** | optional | true or false. Whether to decode the contentVar before embedding the content into the form. If not present, assumed false.                     |
| **destination**        | optional | a string, the name of a message. If it does not exist, it will be created. Defaults to 'message'.          |


An example for creating a form:

```xml
<JavaCallout name='Java-CreateMultipartForm'>
  <Properties>
    <Property name="contentVar">base64EncodedImageData</Property>
    <Property name="contentType">image/png</Property>
    <Property name="want-base64-decode">true</Property>
    <Property name="part-name">image</Property>
    <Property name="fileName">image-file.png</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.MultipartFormCreator</ClassName>
  <ResourceURL>java://apigee-multipart-form-20210414.jar</ResourceURL>
</JavaCallout>
```

The variable named in contentVar must hold a string in base64-encoded format.
How you get the string there, is up to you.

The result will be a form, that looks like so:

```
--------------------9WTvUeO4O5
Content-Disposition: form-data; name="image"; filename="image-file.png"
Content-Type: image/png

PNG

...png data here...
----------------------9WTvUeO4O5--

```


## MultipartFormParser

This callout will parse a form, using the content of the specified message as input.

It accepts a single parameter as input:

| property name  | status   | description                                                                |
| -------------- | -------- | -------------------------------------------------------------------------- |
| **source**     | optional | name of a variable containing a message, containing a form. defaults to "message". |

An example for parsing a form:

```xml
<JavaCallout name='Java-CreateMultipartForm'>
  <Properties>
    <Property name="source">message</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.MultipartFormParser</ClassName>
  <ResourceURL>java://apigee-multipart-form-20210414.jar</ResourceURL>
</JavaCallout>
```

The inbound message should look like this:
```
--------------------9WTvUeO4O5
Content-Disposition: form-data; filename="whatever.txt"
Content-Type: text/plain

Hello World
--------------------9WTvUeO4O5
...
```

The callout sets variables in the context containing information about the parts of the inbound form.


| variable name            | description                                                                |
| ------------------------ | -------------------------------------------------------------------------- |
| **items**                | String, a comma-separated list of file items from the form.                |
| **itemcount**            | String, a number indicating the number of  file items found in the form.   |
| **item_filename_N**      | name of item number N.                                                     |
| **item_content_N**       | content for item N.  This is a byte array. You may need to decode it using a subsequent policy.      |
| **item_content-type_N**  | String, the content-type for item N.                                       |
| **item_size_N**          | String, the size in bytes of the content for item N.                       |

Subsequent policies can then read these variables and operate on them.

There is a limit of 5MB for the size of the uploaded files in the multipart
form.  If you have an upload which exceeds that limit the callout will fail.


## ContentSetter

This callout will set a byte array into a message content.
It may be useful after parsing a multipart form, if you want to set ONE of the parsed items into the message content of a different message.

Normally you would use the Apigee `AssignMessage` policy to do this, like so:
```
<AssignMessage name="AM-Response">
    <AssignVariable>
        <Name>response.content</Name>
        <Ref>mpf_item_content_1</Ref>
    </AssignVariable>
    <IgnoreUnresolvedVariables>false</IgnoreUnresolvedVariables>
</AssignMessage>
```

...which says to use the output of a prior "parse" callout as the full content for this response message.

But AssignMessage seems to treat the referenced variable as a string, so the result is that the response.content gets a string like "[B@1cf582a1". Not helpful.

This callout avoids that pitfall.

It accepts two properties as input:

| property name   | description                                                                                  |
| ----------------| -------------------------------------------------------------------------------------------- |
| **destination** | optional, a string, the name of a message. If it does not exist, it will be created. Defaults to 'message'.          |
| **contentVar**  | required. the name of a context variable, which contains a byte array or string.  |
| **contentType** | optional. the value to set into the content-type header of the message. Default: don't set a content-type header.  |

Example:

```xml
<JavaCallout name='Java-SetContent>
  <Properties>
    <Property name="destination">message</Property>
    <Property name="contentVar">mpf_item_content_1</Property>
    <Property name="contentType">{mpf_item_content-type_1</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.ContentSetter</ClassName>
  <ResourceURL>java://apigee-multipart-form-20210414.jar</ResourceURL>
</JavaCallout>
```



## Example API Proxy

You can find an example proxy bundle that uses the policy, [here in this repo](bundle/apiproxy).
The example proxy accepts a post.

You must deploy the proxy in order to invoke it.

Invoke it like this:

* Create a form, with multiple parts, inserting into a new message
  ```
    curl -i -X POST -d '' https://${ORG}-${ENV}.apigee.net/multipart-form/create-multi
  ```

  This flow in the example proxy uses AssignMessage to assign a couple values to
  variables, and then references those variables in the descriptor provided to
  the policy. The flow then invokes the policy, which creates the form payload.
  The proxy then sends the form to a backend system.

  NB: The backend system as of this moment does not correctly handle the
  form.  This is because the backend doesn't handle forms; it's not
  because the form is invalid.


* Create a form with a single part, an image file, using "message":
  ```
    curl -i -X POST -d '' https://${ORG}-${ENV}.apigee.net/multipart-form/create-1a
  ```

  This flow uses the "one part" option for configuration.
  Within the flow for this request, the proxy assigns a fixed string value to
  a variable, and uses THAT as the contentVar for the policy.  It then
  invokes the policy, which creates the form payload.  The proxy then
  just sends that payload as the response (does not proxy to an upstream system).


* Create a form with a single part, using a new message
  ```
    curl -i -X POST -d '' https://${ORG}-${ENV}.apigee.net/multipart-form/create-1b
  ```

* Parse a form

  ```
    curl -i -F field=value -F readme=@README.md https://$ORG-$ENV.apigee.net/multipart-form/parse1
  ```



## Building

Building from source requires Java 1.8, and Maven.

1. unpack (if you can read this, you've already done that).

2. Before building _the first time_, configure the build on your machine by loading the Apigee jars into your local cache:
  ```
  ./buildsetup.sh
  ```

3. Build with maven.
  ```
  mvn clean package
  ```
  This will build the jar and also run all the tests, and copy the jar to the resource directory in the sample apiproxy bundle.


## License

This material is Copyright 2018-2021 Google LLC.
and is licensed under the [Apache 2.0 License](LICENSE). This includes the Java code as well as the API Proxy configuration.

## Bugs

* The automated tests are pretty thin.
* There is no way to adjust the size limit for the uploaded files.
