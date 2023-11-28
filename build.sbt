name := """todo-api"""
organization := "com.ivandev"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.12"

libraryDependencies += guice
libraryDependencies += "com.auth0" % "java-jwt" % "4.4.0"
libraryDependencies += "de.mkammerer" % "argon2-jvm" % "2.11"
libraryDependencies += "io.github.cdimascio" % "dotenv-java" % "3.0.0"
libraryDependencies += "org.mongodb" % "mongodb-driver-sync" % "4.10.2"
libraryDependencies += "dev.morphia.morphia" % "morphia-core" % "2.4.4"
libraryDependencies += "org.apache.tika" % "tika-core" % "2.9.1"
libraryDependencies += "org.apache.pdfbox" % "pdfbox" % "2.0.30"
libraryDependencies += "org.apache.pdfbox" % "jbig2-imageio" % "3.0.4"
libraryDependencies += "com.github.jai-imageio" % "jai-imageio-core" % "1.4.0"
libraryDependencies += "com.github.jai-imageio" % "jai-imageio-jpeg2000" % "1.4.0"
libraryDependencies += "com.twelvemonkeys.imageio" % "imageio-jpeg" % "3.10.1"
libraryDependencies += "org.javatuples" % "javatuples" % "1.2"
