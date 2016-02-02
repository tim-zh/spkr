name := "spkr"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
	cache,
	ws,
	"org.mongodb.morphia" % "morphia" % "1.1.0",
	"org.scalatest" %% "scalatest" % "2.2.4" % Test
	)

routesGenerator := InjectedRoutesGenerator

fork in run := true

val createNginxConf = taskKey[Unit]("create nginx.conf")

createNginxConf := {
	val generatedConfigFile = new File(baseDirectory.value/"nginx"/"nginx.conf" getAbsolutePath)
	if (! generatedConfigFile.exists()) {
		val replacements = Seq(
			"cores" -> (java.lang.Runtime.getRuntime.availableProcessors() + ""),
			"pwd" -> (baseDirectory.value.getAbsolutePath.replace("\\", "/") + "/nginx" ),
			"static" -> (baseDirectory.value.getAbsolutePath.replace("\\", "/") + "/static")
		)
		val pw = new java.io.PrintWriter(generatedConfigFile)
		scala.io.Source.fromFile(baseDirectory.value/"nginx"/"nginx.template.conf" getAbsolutePath).
				getLines().
				map { line =>
					replacements.foldLeft(line) {
						case (str, (key, value)) => str.replace("${" + key + "}", value)
					}
				}.foreach(pw.println)
		pw.close()
		println("nginx.conf created")
	}
}

val setup = taskKey[Unit]("create nginx.conf and start/stop scripts for nginx and mongodb")

setup := {
	createNginxConf.value
	val path = baseDirectory.value/"nginx" getAbsolutePath;
	write("start") { pw =>
		val content =
			s"""#!/bin/bash
				 |nginx -c $path/nginx.conf
				 |mongod
			 """.stripMargin
		pw.print(content)
	}
	"chmod u+x start".!

	write("stop") { pw =>
		val content =
			s"""#!/bin/bash
			    |nginx -c $path/nginx.conf -s quit
					|mongod --shutdown
			 """.stripMargin
		pw.print(content)
	}
	"chmod u+x stop".!
}

def write(filename: String)(f: java.io.PrintWriter => Unit) = {
	val file = new File(filename)
	val printWriter = new java.io.PrintWriter(file)
	try
		f(printWriter)
	finally
		printWriter.close()
}
