lazy val commonSettings = Seq(
  organization := "com.lunatech",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.2"
)

val akkaVersion = "2.5.12"
val akkaPackage = "com.typesafe.akka"
val akkaHttpVersion = "10.1.1"
val sparkVersion = "2.2.1"
val sparkPackage = "org.apache.spark"

lazy val commonDependencies = Seq(
  akkaPackage           %% "akka-actor"                     % akkaVersion,
  akkaPackage           %% "akka-stream"                    % akkaVersion,
  akkaPackage           %% "akka-http"                      % akkaHttpVersion,
  akkaPackage           %% "akka-http-spray-json"           % akkaHttpVersion,
  akkaPackage           %% "akka-stream-kafka"              % "0.20",
  "com.lightbend.akka"  %% "akka-stream-alpakka-cassandra"  % "0.19",
  akkaPackage           %% "akka-testkit"                   % akkaVersion % Test,
  "org.scalatest"       %% "scalatest"                      % "3.0.5"     % Test,
  akkaPackage           %% "akka-http-testkit"              % "10.1.1"    % Test
)

lazy val core = (project in file("."))
  .settings(commonSettings)
  .settings(libraryDependencies ++= commonDependencies)

lazy val consumer = (project in file("consumer"))
  .settings(commonSettings)
  .settings(libraryDependencies ++= commonDependencies)
  .dependsOn(core)

lazy val sparkDependencies = commonDependencies ++ Seq(
  sparkPackage          %% "spark-streaming-kafka"          % "1.6.3",
  sparkPackage          %% "spark-streaming"                % sparkVersion,
  sparkPackage          %% "spark-core"                     % sparkVersion,
  sparkPackage          %% "spark-sql"                      % sparkVersion,
  sparkPackage          %% "spark-catalyst"                 % sparkVersion,
  "com.datastax.spark"  %% "spark-cassandra-connector"      % "2.0.7"
)

lazy val aggregator = (project in file("aggregator"))
  .settings(commonSettings)
  .settings(libraryDependencies ++= sparkDependencies)
  .dependsOn(core)

lazy val processor = (project in file("processor"))
  .settings(commonSettings)
  .settings(libraryDependencies ++= commonDependencies)
  .dependsOn(core)

enablePlugins(DockerPlugin, DockerComposePlugin)
docker <<= (docker in api)
dockerImageCreationTask := docker.value
testTagsToExecute := "DockerComposeTag"
val dockerAppPath = "/app/"

lazy val api = (project in file("api"))
  .enablePlugins(DockerPlugin, DockerComposePlugin)
  .settings(commonSettings)
  .settings(libraryDependencies ++= commonDependencies)
  .settings(
    dockerfile in docker := {
      new Dockerfile {
        val mainClassString = (mainClass in Compile).value.get
        val classpath = (fullClasspath in Compile).value
        from("java")
        add(classpath.files, dockerAppPath)
        entryPoint("java", "-cp", s"$dockerAppPath:$dockerAppPath/*", s"$mainClassString")
      }
    },
    imageNames in docker := Seq(ImageName(
      repository = name.value.toLowerCase,
      tag = Some("latest"))
    )
  )
  .dependsOn(processor)
