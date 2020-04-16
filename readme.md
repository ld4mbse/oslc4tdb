[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

An RDF Data Store Web Application.
==================================

# 1. Prerequisites.

### 1.1 Software
To get a deployable component of this project you will need to have the
following software installed in at least the specified version:

| Software                                                                                        | Version |
| ------------------------------------------------------------------------------------------------|---------|
| [git](https://git-scm.com/download/win)                                                         | 2.19.1  |
| [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) | 1.8     |
| [Apache Maven](http://maven.apache.org/download.cgi)                                            | 3.5.4   |

To deploy and run this application you will have to have
[Tomcat](http://tomcat.apache.org/) server installed in at least version 7.

### 1.2 Configuration

Make sure the next environment variables are properly set in your system:

| Variable    | Value                                                     |
| ------------|-----------------------------------------------------------|
| `JAVA_HOME` | Java JDK installation directory.                          |
| `M2_HOME`   | Maven installation directory.                             |
| `PATH`      | Contains the paths `<JAVA_HOME>/bin` and `<M2_HOME>/bin`. |


# 2. Getting the source code.

1. On a console terminal, choose or create a directory to clone this project's
   code.
2. Change your location to the previous selected/created directory.
3. Execute `git clone https://github.com/ld4mbse/oslc4tdb`

A new `oslc4tdb` directory will be created in your location. This will be the
**project's home directory**.


# 3. Customizing your copy.

The RDF Data Store Web Application stores its resources in
[TDB](https://jena.apache.org/documentation/tdb/index.html), a native high
performance triple store that requires a location to save its files. This
important location can be configured as property
of the **`pom.xml` file of this module**:

| Property                  | Description                 | Notes        | Default                                  |
| --------------------------|-----------------------------|--------------|------------------------------------------|
| `tdb.location`            | The TDB location.           | Avoid blanks | `${user.home}/oslc4tdb/tdb`          |

Where the `${user.home}` will be the user home directory on your OS.

Besides the above location property, you can configure the next
properties that have to do with URL paths:

| Property       | Description                             | Notes      | Default   |
| ---------------|-----------------------------------------|------------|-----------|
| `path.context` | The context path for this application.  | It is the value showed at the addresses bar after the port number, e.g. `http://example.com:8080/oslc4tdb/`. It must not include slashes.| `oslc4tdb` |
| `path.rest`    | The path of the Resource service.       | The relative path, after the context path, to access RDF resources, e.g. `http://example.com:8080/oslc4tdb/rest`. It must not include slashes.| `rest` |
| `path.graphs`  | The path of the Graph service.          | The relative path, after the context path, to access Graph resources, e.g. `http://example.com:8080/oslc4tdb/graph`. It must not include slashes.| `graph` |

The rest of the properties are for internal or test use. Do not change them.

# 4. Building the module.

   1. Open a console terminal.
   2. Get inside the `oslc4tdb` directory.
   3. Execute: `mvn package`

This will create a `target` directory containing the `war` archive ready to
deploy.

```
oslc4tdb
 |
 |-- target
        `-- oslc4tdb.war
```

# 5. Deploying the `war` archive.

Take the `oslc4tdb.war` file generated in the previous step and deploy it in a
Tomcat instance by following any of the procedures described
[here](https://tomcat.apache.org/tomcat-8.0-doc/deployer-howto.html).

# 6. Managing the RDF Stores.

For managing the RDF Stores you will have to make a http request to an endpoint
for creating or deleting stores for the application, even for listing the
stores you could have for the application.

## 6.1 Creating Stores (RDF Store)

To create a store, you have to call the `URL` of the endpoint using the `POST`
method, for creating a single store, yo need to make an http request like this:

```
POST http://example.com:8080/oslc4tdb/oslc/rdfstores
Slug: myStore
```

> Note: `myStore` in the `Slug` header parameter means the name or ID
for the store you want to create.

Summarizing, you need to:

   1. Make an http `POST` request on the `rdfstores` endpoint of the OSLC adapater.
   2. Add the `Slug` header specifying the id/name for the store yo want to create.


Supposing you want to create a store with the name `myStore`, you will need to do this for test:

   - From command line:

      ```
      $ curl -X POST http://localhost:8080/oslc4tdb/oslc/rdfstores -H "Slug:=myStore"
      ```

   - Using maven Tests (considering you are in `webapp` application folder)

      ```
      $ mvn test -Dtest=RDFStoreResourceTest#testSet_CreateStore
      ```

If there is not already a store with the given id/name and the other parameters are right,
then the store will be created with the name and the definition you sent. The http response
will include the `Location` header whose value will contain the final `URL` of your store,
for our example it will be:

```
201 Created http/1.1
Message: The RDF store myStore was created successfuly.
Location: http://example.com:8080/oslc4tdb/oslc/myStore/catalog
```

In case of sending and id/name for the store, and there is already a store with the same
name or id, the http response will be like this:

```
409 Conflict http/1.1
Message: There is a store with the sama name.
```

## 6.2 Listing the Stores.

As you can see, after the insertion, you have the `URL` for your store in the `Location`
header of the response, but if you want to list all the stores you have created,
you need to make a simple http request using the `GET` method over the `URL`  like this:

```
GET http://example.com:8080/oslc4tdb/oslc/rdfstores
```

If you have not created yet any store for the application, you will receive a response
telling you, the TDB Store is empty, like this:

```
204 Not Content http/1.1
```

If you have created one or more stores for the application, you will receive a list for
the ServiceProvider for these stores in a `RDF` formated object, like this:

```xml
<rdf:RDF
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:oslc="http://open-services.net/ns/core#"
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
    <oslc:ServiceProvider rdf:about="http://example.com:8080/oslc4tdb/oslc/myStore/catalog"/>
</rdf:RDF>
```

By default, the response will include the data on `RDF` format, but you can customize this
by adding the http header `Accept` and the `RDF` serialization format that you need, e.g:

```
GET http://example.com:8080/oslc4tdb/rest/tdbstores
Content-Type: application/ld+json
```

Supposing you have created some stores and you want to list them,
you will use the next commands for this:

   - From command line:

      ```
      $ curl -X GET http://localhost:8080/oslc4tdb/oslc/rdfstores
      ```

   - Using maven Tests (considering you are in `webapp` application folder)

      ```
      $  mvn test -Dtest=RDFStoreResourceTest#testSet_ListingEmptyRDFStore
      ```

## 6.3 Deleting a Store.

If you don't need a store anymore, you can delete it, using a `DELETE` method over the
TDB Store manager, sending the required parameters, like this:

```
DELETE http://example.com:8080/oslc4tdb/oslc/rdfstores/myStore
```

As you can see, you only will need to sent the `DELETE` request, indicating the name of the
store you want to remove in the `URL` path.

If someone is using the store, for retrieving or updating information, you will receive a
response like this.

```
409 Conflict http/1.1
Message: The RDF Store you want to delete is being using.
```

If the store is not being used for anyone, this will be deleted sending the next response.

```
200 OK http/1.1
Message: The RDF Store has been deleted.
```

Taking the store you are using in the lasts steps, you can delete it making this call:

   - From command line:

      ```
      $ curl -X DELETE http://localhost:8080/oslc4tdb/oslc/rdfstores/myStore
      ```

   - Using maven Tests (considering you are in `webapp` application folder)

      ```
      $  mvn test -Dtest=RDFStoreResourceTest#testSet_DeleteRDFStore
      ```

# 7. The Named Graph Store service.

The prime goal of this application is to serve as an RDF named graph store
location. So the first service we will see is the one that let you post and
get RDF data on and from the server.

The *default* relative path of this service is:

```
/oslc4tdb/oslc/stores/myStore
```

> The `myStore` element in the URL path represents the ID or name for the
RDF Store we want to work on.

Do not forget that the first part of the URL depends on the server IP address
(and port number) where you have deployed the application.
e.g. `http://example.com:8080/oslc4tdb/oslc/stores/myStore`

## 7.1 Creating a Named Graph Store.

For creating a named graph store in a RDF Store, we will need to
send a POST request to the endpoint for the specific RDF Store endpoint.

In the creation of the Named Graph Store, we have to send within the
body of the request, the SHACL schema document.

```
POST http://example.com:8080/oslc4tdb/oscl/stores/myStore
Content-Type: text/turtle
Slug: myGraph

@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix oslc: <http://open-services.net/ns/core#> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix dcterms: <http://purl.org/dc/terms/> .

<urn:PersonShape> a sh:NodeShape ;
 sh:targetClass foaf:Person ;
 rdfs:label "Person" ;
 sh:property [
     sh:path foaf:name ;
     sh:nodeKind sh:Literal ;
     sh:datatype xsd:string ;
 ] ;
.

<urn:DogShape> a sh:NodeShape ;
 sh:targetClass foaf:Dog ;
 rdfs:label "Dog" ;
 sh:property [
     sh:path foaf:familyName ;
     sh:nodeKind sh:Literal ;
     sh:datatype xsd:string ;
 ] ;
.

```

Summarizing, you need to:

   1. Make an http `POST` request on the `stores/myStore` OSLC endpoint.
   2. Add the `Content-Type` header with the serialization format of the sent data,
   it could be `Turtle` or `RDF` format, but you can set the format depending on
   your Resource Shape definition in the `Body`.
   3. Add the `Slug` header specifying the id/name for the named graph store yo want to create.
   4. Send the Resource Shape definition in the `Body` content, using the format you selected
   in the `Content-Type` header.

> Note: By default, the endpoint expect a `Turtle` format in the `Body` content and if you
don't specify the format, de endpoint will apply the `Turtle` format for the Resource Shape.

You will receive a response depending on the result of the operation in the endpoint.
The response, will include the location of the named graph store.

```
201 Created http/1.1
Message: The resource was created.
Location: http://example.com:8080/oslc4tdb/oslc/stores/myStore/myData
```

In case of sending and id/name for the named graph store, and there is already
a named graph store with the same name or id, the http response will be like this:

```
409 Conflict http/1.1
Message: Resource 'http://example:8080/oslc4tdb/oslc/stores/myStore/myData' already exists.
```

## 7.2 Storing RDF data in a Named Graph Store.

To store RDF data on this server we have at least two differents
ways to do it

### 7.2.1 Using Bulk Loader

For creating resources in a Named Graph Store we have a method
which allows to upload information in a bulk. For doing this, we have
to send a POST request to the next endpoint.

```
POST http://example.com:8080/oslc4tdb/oslc/stores/myStore/myGraph
Content-Type: application/rdf+xml

<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:foaf="http://xmlns.com/foaf/0.1/">
	<rdf:Description rdf:about="Dog/1">
		<foaf:familyName>Fido</foaf:familyName>
		<rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Dog" />
	</rdf:Description>
	<rdf:Description rdf:about="Dog/2">
		<foaf:familyName>Doggy</foaf:familyName>
		<rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Dog" />
	</rdf:Description>
</rdf:RDF>
```

Summarizing, you need to:

   1. Make an http POST request on the named graph store service endpoint.
   2. Send the RDF data on the request body.
   3. Add the `Content-Type` header with the serialization format of the sent data.

All the resources described in the RDF will be stored. The http response will
include the `Location` header whose value will contain the final URL of your data,
for our example it will be:

```
201 Created http/1.1
Location: http://example.com:8080/oslc4tdb/oslc/myStore/myGraph/stores
```

If we are sending the same resources for storing, the response you will receive
will be like this:

```
304 Not Modified http/1.1
```

On the other hand, since the Named Graph Store has a SHACL schema document,
then the endpoint will validate the structure of the RDF data you want
to insert against the definition, if the structure  does not pass the validation,
you will receive a response like this:

```
400 Bad request http/1.1
Invalid syntax: [line: X, col: Y] The element type ...
```

Supposing you have created a store with the name `myStore` which has a validation schema for the `foaf` type `Person`
as described in `family.ttl` file, and you wan to insert data on it, you need to execute the next commands to test,
but we have two options, send a malformed data, and send a perfect formed data, for both cases you can use the next
examples. (We will use the `malformed.rdf` and `person.rdf` files)

   - From command line:

      - Testing a malformed RDF data
         ```
         $ curl -X POST http://localhost:8080/oslc4tdb/oslc/stores/myStore/myGraph -H "Content-Type:application/rdf+xml" -d "@src/test/resources/malformed.rdf"
         ```
      - Testing a perfct formed RDF data.
         ```
         $ curl -X POST http://localhost:8080/oslc4tdb/oslc/stores/myStore/myGraph -H "Content-Type:application/rdf+xml" -d "@src/test/resources/person.rdf"
         ```

> In the command line case, for the first attempt, with a malformed RDF data,
you will receive an error message with the description of the error.
>
> In the second case, the RDF data will be inserted on the graph.

### 7.2.2 Using a Typed Insertion endpoint.

To insert a resource on the Named Graph Store using the normal way,
we need to specify the endpoint of the RDF Store, including
the name of the Named Graph Store, and the Type of the resource we will insert,
this ype was defined in the SHACL schema we used on the creation
of the Named Graph store.

```
POST http://example.com:8080/oslc4tdb/oslc/stores/myStore/myGraph/myType
Content-Type: application/rdf+xml
Slug: myDataID

<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:foaf="http://xmlns.com/foaf/0.1/">
	<rdf:Description rdf:about="">
		<foaf:familyName>Fido</foaf:familyName>
		<rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Dog" />
	</rdf:Description>
</rdf:RDF>
```

Summarizing, you need to:

   1. Make a http POST request on the Named Graphs Store service endpoint.
   2. Send the RDF data on the request body.
   3. Add the `Content-Type` header with the serialization format of the sent data.
   4. Add the `Slug` header specifying the id for the resource.

If we omit the ID, the endpoint will generate an ID for the resource, if we
send an ID on the Slug header paramter, this will be the ID for the resource,
if there is not already a graph with the given id, and the `Content-Type`
value you provided is allowed, then a new resource will be created with your data
and suggested id. The http response will include the `Location` header whose
value will contain the final URL of your data, for our example it will be:

```
201 Created http/1.1
Location: http://example.com:8080/oslc4tdb/oslc/myStore/myGraph/myType/myDataID
```

If there is a graph with the same id as you are sending, the response you will receive
will be like this:

```
409 Conflic http/1.1
Message: Another resource exists at http://example.com:8080/oslc4tdb/oslc/myStore/myGrapg/myType/myDataID.
```

On the other hand, if your store has a defined Resource Shape, then the endpoint will validate
the structure of the RDF data you want to insert against the definition, if the structure
does not pass the validation, you will receive a response like this:

```
400 Bad request http/1.1
Invalid syntax: [line: X, col: Y] The element type ...
```

Supposing you have created a store with the name `myStore` which has a validation schema for the `foaf` type `Person`
as described in `family.ttl` file, and you wan to insert data on it, you need to execute the next commands to test,
but we have two options, send a malformed data, and send a perfect formed data, for both cases you can use the next
examples. (We will use the `malformed.rdf` and `person.rdf` files)

   - From command line:

      - Testing a malformed RDF data
         ```
         $ curl -X POST http://localhost:8080/oslc4tdb/oslc/myStore/myGraph/myType -H "Content-Type:application/rdf+xml" -H "Slug:Person-1" -d "@src/test/resources/malformed-typed.rdf"
         ```
      - Testing a perfct formed RDF data.
         ```
         $ curl -X POST http://localhost:8080/oslc4tdb/oslc/myStore/myGraph/myType -H "Content-Type:application/rdf+xml" -H "Slug:Person-1" -d "@src/test/resources/person-typed.rdf"
         ```

   - Using maven Tests (considering you are in `webapp` application folder)

      - Testing a malformed RDF data
         ```
         $ mvn test -Dtest=TDBStoreResourceValidateSHACLTest#testSet_ValidateMalformed
         ```
      - Testing a perfct formed RDF data.
         ```
         $ mvn test -Dtest=TDBStoreResourceValidateSHACLTest#testSet_ValidatePerfect
         ```

> In the command line case, for the first attempt, with a malformed RDF data,
you will receive an error message with the description of the error.
>
> In the second case, the RDF data will be inserted on the named graph store.


## 7.3 Listing RDF data.

After an insertion of a RDF data on your named graph store, maybe you want to list
the resources you have been stored on this store, and for doing this, you need to
make an http request with the `GET` method to your store URL, like this.

### 7.3.1 Listing resources from bulk loader.

For listing resources created using the bulk loader you culd request to the next enpoint.

```
GET http://example.com:8080/oslc4tdb/oslc/myStore/myGraph/stores
```

You will receive a list of resources stored in your named graph store.

```
200 OK http/1.1
Message: <rdf:RDF
             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
             xmlns:foaf="http://xmlns.com/foaf/0.1/">
             <foaf:Dog rdf:about="http://localhost:8080/oslc4tdb/oslc/myStore/myGraph/myType/myDataID">
                 <foaf:familyName>Fido by bulk loader</foaf:familyName>
             </foaf:Dog>
             <foaf:Person rdf:about="http://localhost:8080/oslc4tdb/oslc/myStore/myGraph/myType/myDataID">
                 <foaf:name>John</foaf:name>
             </foaf:Person>
         </rdf:RDF>
```

### 7.3.2 Listing Typed resources.

For listing the resources from a specific type, you need to request to the next
endpoint.

```
GET http://example.com:8080/oslc4tdb/oscl/myStore/myGraph/myType
```

You will receive a list of graphs stored in your store within the response, e.g:

```
200 OK http/1.1
Message: <rdf:RDF
             xmlns:dcterms="http://purl.org/dc/terms/"
             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
             xmlns:oslc="http://open-services.net/ns/core#"
             xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
             xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
             <oslc:ResponseInfo rdf:about="http://example.com:8080/oslc4tdb/oslc/myStore/myGraph/myType">
                 <oslc:totalCount rdf:datatype="http://www.w3.org/2001/XMLSchema#integer">1</oslc:totalCount>
                 <rdfs:member rdf:resource="http://example.com:8080/oslc4tdb/oslc/myStore/myGraph/myType/myDataID"/>                 
             </oslc:ResponseInfo>
         </rdf:RDF>
```

By default, the response will include the data on `RDF` format, but you can
customize this by adding the request http header `Accept` and the RDF
serialization format that you need, e.g:

```
GET http://example.com:8080/oslc4tdb/oscl/myStore/myGraph/myType
Content-Type: text/turtle
```

Notice that the response will get you back all your graphs stored in the store.
To get individual resource information you will need to use the next request format.


## 7.4 Retrieving RDF data.

With the final URL of your inserted data which you got in the `Location` header parameter,
you can retrieve the detailed information of your RDF data, making an http `GET` request on
the URL, e.g:

```
GET http://example.com:8080/oslc4tdb/oslc/myStore/myGraph/myType/myDataID
```

By default, the response will include the data on `RDF` format, but you can
customize this by adding the request http header `Accept` and the RDF
serialization format that you need, e.g:

```
GET http://example.com:8080/oslc4tdb/rest/stores/myStore/myGraph
Accept: application/ld+json
```

Notice that this request will get you back all the attributes of your graph.

## 7.5 Querying RDF data.

It is also possible to execute queries over a graph URL to filter some
resources that match a certain condition, or to select only some properties
back. Notice that this functionality is NOT available for HTML clients like a
browser; specifically, it is not available if the `Accept` header includes the
MIME type `text/html`, just Apache jena compliance MIME types can use it.
Having said that, you will need a REST client to test the following examples.

To filter resources that match a condition use the `where` parameter as follows:

```
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myData?oslc.select=rdf:type=rdfs:Class
```

You can also ask for literal values:

```
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myData?oslc.where=vocab:myNumber=5
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myData?oslc.where=vocab:myText="Hello"
```

Notice that string values must be quoted and that the prefixes you used must be
known by the target graph of the query, otherwise you will see an error message
of "unknown prefix".

Full URLs for properties or values are allowed if they are enclosed by angle
brackets, the next requests are equivalent:

```
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myData?oslc.where=rdf:type=rdfs:Class

GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myData?oslc.prefix=<http://www.w3.org/1999/02/22-rdf-syntax-ns%23type>=<http://www.w3.org/2000/01/rdf-schema%23Class>
```

Notice that with this full syntax you will need to escape all special
characters, that may be on the URL like the `#` character, by its ASCII
hexadecimal equivalent code (`%23`).

To add more conditions, just separate them by the `" and "` string (including
single blanks after and before):

```
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myData?oslc.where=vocab:myNumber=5 and vocab:myText="Hello"
```

If you want to perform a full text search over a property you need switch to the
`~` operator instead of `=`;

```
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myData?oslc.where=vocab:myText~"Hello"
```

This way, all resources that contains the `Hello` string (case insensitive) on
the `myText` property value will be returned.

Finally, if you want to list the properties to be returned for each matching
resource, you can use the `select` parameter as follows:

```
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myData?oslc.where=vocab:myText~"Hello"&oslc.select=vocab:myText,vocab:myNumber
```

Notice that each wanted property must be prefixed (or you must use the full URL
syntax as before for the `where` parameter) and that they are separated by
commas. The order of the `where` and `select` parameters is not relevant and
you can use any of them without the other.

# 8. The Resource service.

One single graph may contain a lot of individual RDF resources. To get one of
these resources alone, you need to use the Resource service that has the
following *default* relative path:

```
/oslc4tdb/rest/oslc/myStore/myGraph
```

After this path, you need to specify the canonical id of the resource you are
interested into, and the name of the graph that contains it as a query
parameter with an http GET request, e.g:

```
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myResourceType/myId
```

Again, the default serialization format will be Turtle, but you can use the
`Accept` header as before to change it.

Important: The resource intended to be returned by this URL must have the same
URL as its identifier; otherwise it wont be found. This application is not
responsible for making sure of this, the loading user or application is the
one that has to guarantee this.

# 9. The OSLC Adapter.

All of the last operations we are talking about have been implemented using the
REST API approach, but as you can see, some of the resources path or endpoints path,
have the `oslc` term; this is because there is an implementation os an **OSLC Adapter**
for managing the information in the same manner of the REST API.

Following the OSLC spec, we have implemented the endpoints to retrieve information and
insert information in the TDB Store.

## 9.1. Master Catalog.

Following the OSLC spec we have and endpoint to retrieve information about what services
we have available on our adapter, and to get this information we have to do a GET request
like this.

```
GET http://example.com:8080/oslc4tdb/rest/oslc/catalog
```

This endpoint will return the list of catalogs or stores defined in our multiple TDB Store.

```
200 OK http/1.1
Message:    <rdf:RDF
                xmlns:dcterms="http://purl.org/dc/terms/"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:oslc="http://open-services.net/ns/core#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                <oslc:ServiceProviderCatalog rdf:about="http://example.com:8080/oslc4tdb/rest/oslc/catalog">
                    <oslc:serviceProviderCatalog rdf:resource="http://example.com:8080/oslc4tdb/rest/oslc/myStore/catalog"/>
                    <dcterms:description>Encapsulates all other available catalogs</dcterms:description>
                    <dcterms:title>Master Service Provider Catalog</dcterms:title>
                </oslc:ServiceProviderCatalog>
            </rdf:RDF>
```

## 9.2. Getting an specific Catalog.

After we knew the catalogs configured in the multiple TDB Store, we can get information about an
specific store. making a call to the endpoint have in the `ServiceProviderCatalog` section of
our last result.

```
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/catalog
```

We will obtain the result of the `ServiceProvider` for this specific catalog.

```
200 OK http/1.1
Message:    <rdf:RDF
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:dcterms="http://purl.org/dc/terms/"
                xmlns:oslc="http://open-services.net/ns/core#"
                xmlns:sh="http://www.w3.org/ns/shacl#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                <oslc:ServiceProviderCatalog rdf:about="http://example.com:8080/oslc4tdb/rest/oslc/myStore/catalog">
                    <dcterms:title>persons</dcterms:title>
                    <oslc:serviceProvider>
                        <oslc:ServiceProvider rdf:about="http://example.com:8080/oslc4tdb/rest/oslc/myStore/serviceProvider/myGraph">
                            <dcterms:title>graph-1</dcterms:title>
                            <oslc:details rdf:resource="http://example.com:8080/oslc4tdb/rest/oslc/myStore/serviceProvider/myGraph/about"/>
                            <oslc:service>
                                <oslc:Service>
                                    <oslc:creationFactory>
                                        <oslc:CreationFactory>
                                            <dcterms:title>Dog Resource Creation Factory</dcterms:title>
                                            <oslc:creation rdf:resource="http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myShape"/>
                                            <oslc:resourceType rdf:resource="http://xmlns.com/foaf/0.1/Shape"/>
                                            <oslc:resourceShape rdf:resource="http://example.com:8080/oslc4tdb/rest/oslc/myStore/shape/myShape"/>
                                        </oslc:CreationFactory>
                                    </oslc:creationFactory>
                                    <oslc:queryCapability>
                                        <oslc:QueryCapability>
                                            <dcterms:title>Dog Resource Query Capability</dcterms:title>
                                            <oslc:queryBase rdf:resource="http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myShape"/>
                                            <oslc:resourceType rdf:resource="http://xmlns.com/foaf/0.1/Shape"/>
                                        </oslc:QueryCapability>
                                    </oslc:queryCapability>
                                </oslc:Service>
                            </oslc:service>
                        </oslc:ServiceProvider>
                    </oslc:serviceProvider>
                </oslc:ServiceProviderCatalog>
            </rdf:RDF>
```

In this response, we have two news endpoints one for creating new resources, and another one to
retrieve information for all of graphs available or stored in th TDB Store, and we could get information
for an specific graph.

## 9.3. Getting the Service Providers

To obtain information or to know the service provider for an specific graph we can use the next
request.

```
GET http://example.com:8080/oslc4tdb/rest/oslc/myStore/serviceProvider/myGraph
```

And we will obtain the specific endpoints to create and query over the resources stored in that
specific graph.

```
200 OK http/1.1
Message:    <rdf:RDF
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:dcterms="http://purl.org/dc/terms/"
                xmlns:oslc="http://open-services.net/ns/core#"
                xmlns:sh="http://www.w3.org/ns/shacl#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                <oslc:ServiceProvider rdf:about="http://example.com:8080/oslc4tdb/rest/oslc/myStore/serviceProvider/myGraph">
                    <dcterms:title>graph-1</dcterms:title>
                    <oslc:details rdf:resource="http://example.com:8080/oslc4tdb/rest/oslc/myStore/serviceProvider/myGraph/about"/>
                    <oslc:service>
                        <oslc:Service>
                            <oslc:creationFactory>
                                <oslc:CreationFactory>
                                    <dcterms:title>Dog Resource Creation Factory</dcterms:title>
                                    <oslc:creation rdf:resource="http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myShape"/>
                                    <oslc:resourceType rdf:resource="http://xmlns.com/foaf/0.1/Shape"/>
                                    <oslc:resourceShape rdf:resource="http://example.com:8080/oslc4tdb/rest/oslc/myStore/shape/myShape"/>
                                </oslc:CreationFactory>
                            </oslc:creationFactory>
                            <oslc:queryCapability>
                                <oslc:QueryCapability>
                                    <dcterms:title>Dog Resource Query Capability</dcterms:title>
                                    <oslc:queryBase rdf:resource="http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myShape"/>
                                    <oslc:resourceType rdf:resource="http://xmlns.com/foaf/0.1/Shape"/>
                                </oslc:QueryCapability>
                            </oslc:queryCapability>
                        </oslc:Service>
                    </oslc:service>
                </oslc:ServiceProvider>
            </rdf:RDF>
```

## 9.4. Inserting RDF data using OSLC Adapter.

To insert information in the TDB Store using the OSLC Adapter approach, we need to do
a POST request to the Creation Factory end point and give the information like the next example:

```
POST http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myShape
Content-Type: application/rdf+xml

<rdf:RDF ...
```

Summarizing, you need to:

   1. Make an http POST request on the store specifying the absolute URL with the store, graph and shape name.
   2. Send the RDF data on the request body.
   3. Add the `Content-Type` header with the serialization format of the sent data.

If there is not already a graph with the given id, and the `Content-Type`
value you provided is allowed, then a new graph will be created with your data
and suggested id. The http response will include the `Location` header whose
value will contain the final URL of your data, for our example it will be:

```
201 Created http/1.1
Location: http://example.com:8080/oslc4tdb/rest/oslc/myStore/myGraph/myShape/newID
```
