# OAICAT-figshare

**OAICAT-figshare** is an extension library for
[OAICat](https://www.oclc.org/research/areas/data-science/oaicat.html)
that implements customised interfaces for harvesting the figshare repository.
By configuring a *FigshareOAICatalog.searchFilter* you can present a virtual
repository, eg. an institutional figshare repository, specific groups, specific tags.
The *JSON2oai_dc* Crosswalk outputs rich Dublin Core metadata (DC).
Beyond ordinary DC elements, figshare files and custom_fields can be configured
as output metadata.

[OAICat](https://www.oclc.org/research/areas/data-science/oaicat.html) is an
open source software project. It is a Java Servlet web application which
provides a repository framework that conforms to the
[Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH) v2.0](https://www.openarchives.org/pmh/).
OAICat can be customised to work with arbitrary data repositories.

**OAICAT-figshare** has been built with
*[oaicat-1.5.63](https://github.com/OCLC-Research/oaicat)* and tested on
*[figshare API v2](https://docs.figshare.com/)* (2021-Nov-21);
*[Apache Tomcat Version 9.0.41](http://tomcat.apache.org/)*;
Ubuntu 20.04.1 LTS; default-jdk package (openjdk 11.0.9.1).

## INSTALL NOTES

Instructions for Tomcat:
1. Download the following files required for install:
   * **oaicat-figshare.jar** from [releases](https://github.com/lylewinton/oaicat-figshare/releases)
   * **oaicat.war** and **json-simple-1.1.1.jar** from the [lib](https://github.com/lylewinton/oaicat-figshare/tree/master/lib) folder
   * **oaicat-figshare-example.properties** and **oaicat.xsl** from the [/](https://github.com/lylewinton/oaicat-figshare/) folder
2. First deploy oaicat.war on a running Tomcat. This should create the `webapps\oaicat` folder.
3. Copy the other libraries (oaicat-figshare.jar, json-simple-1.1.1.jar) where they can be
   found by Tomcat, ideally the oaicat lib folder `webapps\oaicat\WEB-INF\lib`
4. Replace oaicat.properties with oaicat-figshare-example.properties in the
   `webapps\oaicat\WEB-INF` folder. In the web.xml file in this folder, find the
   `<context-param` block containing `<param-name>properties</param-name>` and `<param-value>` should specify the oaicat.properties file.
   Modify the `<param-value>` line to specifying the full path to the file, often necessary.
5. Update new oaicat.properties values to use oaicat-figshare custom classes:
   ```
   AbstractCatalog.oaiCatalogClassName=net.datanoid.oaipmh.figshare.FigshareOAICatalog
   AbstractCatalog.recordFactoryClassName=net.datanoid.oaipmh.figshare.JSONRecordFactory
   Crosswalks.oai_dc=net.datanoid.oaipmh.figshare.JSON2oai_dc
   ```
6. Update oaicat.properties settings, especially for:
   ```
   Identify.* - normal OAICAT settings
   FigshareOAICatalog.*
   JSON2oai_dc.*
   ```
7. Optionally install the example logging.properties file so you can get an
   oaicat logfile including oaicat-figshare info or debug. (Install it here
   `webapps\oaicat\WEB-INF\classes` )
8. Optionally replace the oaicat.xsl (web browser transform) as oaicat-figshare
   makes use of xmlns:dcterms, URI types and this improves presentation.
9. Restart Tomcat then access oaicat (http://localhost:8080/oaicat/) and watch
   the console for errors.


## BUILD NOTES

Source for OAICAT files:
https://github.com/OCLC-Research/oaicat

Source for OAICAT library, look in the distribution war file:
oaicat.war\WEB-INF\lib\oaicat.jar



## OAICAT INSTALL NOTES
README.txt from OAICAT has essential information for installation,
included here for reference:
```
To upgrade OAICat with the latest code changes, copy the latest
oaicat.jar file to webapps/oaicat/WEB-INF/lib/.

Before customizing OAICat, first install oaicat.war in a J2EE Servlet
Engine and verify that the default configuration works. If so, proceed
with any necessary code and configuration changes as described below.

Before building this probject with Ant, create a 'build.properties'
file in the project directory with the following entries:

catalina.home=/path/to/jakarta-tomcat

To create a new distribution set, issue the command:

ant dist

To customize OAICat, answer these questions:

Q1: What Java package should I use to hold my custom classes?
    a) For example, if you work for Acme Inc., create a directory
       hierarchy somewhere named:

       com/acme/oai

Q2: What database engine will I use?
    a) For example, if using the Foo database, copy
       oaicatjar/src/ORG/oclc/oai/server/catalog/DummyOAICatalog.java
       to com/acme/oai/server/catalog/FooOAICatalog.java and modify the
       code so the class name matches the new filename.
    b) Change the code in this class to use the Foo database Java API.
       In general, all this class needs to know about the records
       is that they are black-box Java Objects. To make life easier
       downstream, however, it may be worthwhile to convert the records
       to a more convenient processing form immediately after reading.
       For example, if the records are stored as XML Strings, load
       them into DOM objects as soon as they are read. Beyond that,
       though, leave it to the Crosswalk and RecordFactory
       implementations to understand the true semantics of the records.
       Doing this may mean you can't reuse this class for cases where
       the database returns non-XML byte arrays, but then again, what
       are the chances of that?
    c) Make a corresponding package/class name change to the
       AbstractCatalog.oaiCatalogClassName entry in the
       webapps/oaicat/WEB-INF/oaicat.properties file to have OAICat use
       your custom class.

Q3: What are the semantics of these record objects?
    a) If FooOAICatalog returns records as byte arrays, examples can be
       anything such as MARC Communications Format. If FooOAICatalog
       returns Strings, examples might include MARC BER, or any kind of
       XML String. If FooOAICatalog returns DOM Documents, examples can
       be any XML-based metadata format. Let's assume FooOAICatalog
       returns records as DOM Documents containing MARCXML content.
    b) Copy oaicatjar/src/ORG/oclc/oai/server/catalog/XMLRecordFactory.java
       to com/acme/oai/server/catalog/MARCXMLDOMRecordFactory.java
       and modify the code so the class name matches the new filename.
    c) Change the methods to cast each Object nativeItem parameter to a
       org.w3c.dom.Document and use it to extract the relevant data for
       each method.
    d) Make a corresponding package/class name change to the
       AbstractCatalog.recordFactoryClassName entry in the
       webapps/oaicat/WEB-INF/oaicat.properties file to have OAICat use
       your custom class.

Q4: What OAI metadatdaFormats will be supported?
    a) Examples include oai_dc, marcxml, or oai_etdms.
    b) For oai_dc, copy oaicatjar/src/ORG/oclc/oai/crosswalk/XML2oai_dc.java
       to com/acme/oai/server/catalog/MARCXMLDOM2oai_dc.java and modify
       the code so the class name matches the new filename.
    c) Change the constructor to use the appropriate schemaLocation for
       this metadataFormat.
    d) Change the methods to cast each Object nativeItem parameter to a
       org.w3c.dom.Document and use it to service the method accordingly.
       In this case, you could use the Library of Congress MARCXML to DC
       XSL stylesheet (see http://www.loc.gov/standards/marcxml/) to
       perform the crosswalk to Dublin Core.
    e) Repeat steps b, c, and d for each metadatdaFormat to be supported.
    f) Make a corresponding package/class name change to the
       Crosswalks.* entries in the webapps/oaicat/WEB-INF/oaicat.properties
       file to have OAICat use your custom classes.

Finally, change other properties in oaicat.properties according to your
preferences.

That's essentially what it takes to customize OAICat. Contact Jeff Young
at jyoung@oclc.org with questions and comments.
```
