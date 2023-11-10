name := """todo-api"""
organization := "com.ivandev"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.12"

libraryDependencies += guice
libraryDependencies += "com.auth0" % "java-jwt" % "4.4.0"
libraryDependencies += "de.mkammerer" % "argon2-jvm" % "2.11"
