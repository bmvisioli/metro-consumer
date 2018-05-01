name := "metro-consumer"

version := "1.0"

scalaVersion := "2.12.6"

val akkaVersion = "2.5.11"
val akkaPackage = "com.typesafe.akka"
val akkaHttpVersion = "10.1.1"

libraryDependencies ++= Seq(
  akkaPackage     %% "akka-actor"                         % akkaVersion,
  akkaPackage     %% "akka-stream"                        % akkaVersion,
  akkaPackage     %% "akka-http"                          % akkaHttpVersion,
  akkaPackage     %% "akka-http-spray-json"               % akkaHttpVersion,
  akkaPackage     %% "akka-testkit"                       % akkaVersion % Test,
  "org.scalatest" %% "scalatest"                          % "3.0.5"     % Test
)