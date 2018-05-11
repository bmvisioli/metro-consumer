name := "metro-consumer"

version := "1.0"

scalaVersion := "2.12.6"

val akkaVersion = "2.5.12"
val akkaPackage = "com.typesafe.akka"
val akkaHttpVersion = "10.1.1"

libraryDependencies ++= Seq(
  akkaPackage           %% "akka-actor"                     % akkaVersion,
  akkaPackage           %% "akka-stream"                    % akkaVersion,
  akkaPackage           %% "akka-http"                      % akkaHttpVersion,
  akkaPackage           %% "akka-http-spray-json"           % akkaHttpVersion,
  akkaPackage           %% "akka-stream-kafka"              % "0.20",
  "com.lightbend.akka"  %% "akka-stream-alpakka-cassandra"  % "0.18",
  akkaPackage           %% "akka-testkit"                   % akkaVersion % Test,
  "org.scalatest"       %% "scalatest"                      % "3.0.5"     % Test,
  "com.typesafe.akka"   %% "akka-http-testkit"              % "10.1.1"    % Test

)

enablePlugins(DockerPlugin, DockerComposePlugin)

//Only execute tests tagged as the following
testTagsToExecute := "DockerComposeTag"

//Set the image creation Task to be the one used by sbt-docker
dockerImageCreationTask := docker.value

dockerfile in docker := {
  new Dockerfile {
    val dockerAppPath = "/app/"
    val mainClassString = (mainClass in Compile).value.get
    val classpath = (fullClasspath in Compile).value
    from("java")
    add(classpath.files, dockerAppPath)
    entryPoint("java", "-cp", s"$dockerAppPath:$dockerAppPath/*", s"$mainClassString")
  }
}

imageNames in docker := Seq(ImageName(
  repository = name.value.toLowerCase,
  tag = Some(version.value))
)