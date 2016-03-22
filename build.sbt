name := "spkr"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.mongodb.morphia" % "morphia" % "1.1.0",
  "org.apache.kafka" % "kafka-clients" % "0.9.0.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "org.scalatestplus" %% "play" % "1.4.0" % Test,
  "com.github.fakemongo" % "fongo" % "2.0.3" % Test //newer versions refer to mongodb 3.2
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

val setup = inputKey[Unit]("create nginx.conf and start/stop scripts for nginx and mongodb")

setup := {
  createNginxConf.value
  val args = Def.spaceDelimited("<arg>").parsed
  val kafkaPath = args(0)
  val basePath = baseDirectory.value.getAbsolutePath
  write("start") { pw =>
    val content =
      s"""#!/bin/bash
         |nginx -c $basePath/nginx/nginx.conf &
         |mongod --dbpath $basePath/db &
         |cd $kafkaPath &
         |bin/zookeeper-server-start.sh config/zookeeper.properties &
         |bin/kafka-server-start.bat config/server.properties &
         |wait""".stripMargin
    pw.print(content)
  }
  "chmod u+x start".!
  write("stop") { pw =>
    val content =
      s"""#!/bin/bash
         |nginx -c $basePath/nginx/nginx.conf -s quit
         |mongod --shutdown """.stripMargin
    pw.print(content)
  }
  "chmod u+x stop".!
}

val setupWin = inputKey[Unit]("create nginx.conf and start/stop scripts for nginx and mongodb")

setupWin := {
  createNginxConf.value
  val args = Def.spaceDelimited("<arg>").parsed
  val nginxPath = args(0)
  val kafkaPath = args(1)
  val basePath = baseDirectory.value.getAbsolutePath
  write("start.bat") { pw =>
    val content =
      s"""cd $nginxPath
         |start cmd /c "nginx.exe -c $basePath\\nginx\\nginx.conf"
         |start cmd /c "mongod --dbpath $basePath\\db"
         |cd $kafkaPath
         |start cmd /c "bin\\windows\\zookeeper-server-start.bat config\\zookeeper.properties"
         |start cmd /c "bin\\windows\\kafka-server-start.bat config\\server.properties" """.stripMargin
    pw.print(content)
  }
  write("stop.bat") { pw =>
    val content =
      s"""cd $nginxPath
         |start cmd /c "nginx.exe -c $basePath\\nginx\\nginx.conf -s quit"
         |start cmd /c "mongo admin --eval db.shutdownServer()"
         |cd $kafkaPath
         |start cmd /c "bin\\windows\\kafka-server-stop.bat"
         |start cmd /c "bin\\windows\\zookeeper-server-stop.bat" """.stripMargin
    pw.print(content)
  }
}

def write(filename: String)(f: java.io.PrintWriter => Unit) = {
  val file = new File(filename)
  val printWriter = new java.io.PrintWriter(file)
  try
    f(printWriter)
  finally
    printWriter.close()
}
