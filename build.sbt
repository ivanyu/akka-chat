name := "akka-chat"
organization := "me.ivanyu"

version := "0.1"

scalaVersion := "2.12.5"

scalacOptions ++= Seq(
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.

  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
  "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match",              // Pattern match may not be typesafe.

  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
  "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
//  "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
//  "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
//  "-Ywarn-unused:locals",              // Warn if a local definition is unused.
//  "-Ywarn-unused:params",              // Warn if a value parameter is unused.
//  "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
//  "-Ywarn-unused:privates",            // Warn if a private member is unused.
  "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
)

val versions = new {
  val akka = "2.5.11"
  val akkaHttp = "10.1.1"
  val circe = "0.9.3"
}

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"          % versions.akka,
  "com.typesafe.akka" %% "akka-stream"         % versions.akka,
  "com.typesafe.akka" %% "akka-testkit"        % versions.akka % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % versions.akka % Test,

  /* Persistence & Database */
  "com.typesafe.akka" %% "akka-persistence"    % versions.akka,
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

  "com.typesafe.akka" %% "akka-http"         % versions.akkaHttp,
  "com.typesafe.akka" %% "akka-http-testkit" % versions.akkaHttp % Test,

  /* JSON */
  "io.circe" %% "circe-core"    % versions.circe,
  "io.circe" %% "circe-generic" % versions.circe,
  "io.circe" %% "circe-parser"  % versions.circe,
  "io.circe" %% "circe-java8"   % versions.circe,

  /* Logging */
  "com.typesafe.akka" %% "akka-slf4j" % versions.akka,
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  /* Testing */
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test
)

(scalastyleConfig in Test) := baseDirectory.value / "scalastyle-test-config.xml"

scalafmtVersion in ThisBuild := "1.4.0"
scalafmtOnCompile in ThisBuild := true
scalafmtOnCompile in Sbt := false
