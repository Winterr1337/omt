# Octet Migration Tool (OMT)


OMT is a software program that allows to transfer data from Quartet's database to Octet's database.

![omt](https://imgur.com/a/whvY5AM)


## Installation

OpenJDK 17 or higher is required to run.

Grab a release from [here.](link) Extract archive's content and run

```java -jar omt.jar```


## Usage

Octet uses MySQL as DBMS, so MySQL Server is required on your system. 

You can use Octet's existing database or let the program create a new one.


Provide path to Quartet's Account.db file then connect to the database. Adjust your preferences accordingly and click the “Migrate” button to start the process.


## Building

JavaFX 21 SDK will be required to preview the application if you're going to make make changes inside an IDE.

Use ```mvn clean compile package``` to build the maven project.

Put the `resources` folder in the same place as the JAR file so that the CSS can load correctly.
## TODO

- [ ] Localization