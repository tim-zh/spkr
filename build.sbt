name := "spkr"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
	jdbc,
	cache,
	ws,
	"org.reactivemongo" %% "play2-reactivemongo" % "0.11.9",
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

val nginx = taskKey[Unit]("run nginx for static content")

nginx := {
	createNginxConf.value
	val path = baseDirectory.value/"nginx" getAbsolutePath;
	s"nginx -c $path/nginx.conf" !
}

val nginxStop = taskKey[Unit]("stop nginx")

nginxStop := {
	val path = baseDirectory.value/"nginx" getAbsolutePath;
	s"nginx -c $path/nginx.conf -s quit" !
}