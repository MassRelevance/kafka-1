/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import Keys._
import java.io.File

import scala.xml.{Node, Elem}
import scala.xml.transform.{RewriteRule, RuleTransformer}

object KafkaBuild extends Build {
  val commonSettings = Seq(
    version := "0.7.2",
    organization := "com.massrelevance",
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    scalaVersion := "2.11.1",
    javacOptions ++= Seq("-Xlint:unchecked", "-source", "1.5"),
    parallelExecution in Test := false, // Prevent tests from overrunning each other
    libraryDependencies ++= Seq(
      "org.scalatest"         %% "scalatest"    % "2.1.7" % "test",
      "log4j"                 %  "log4j"        % "1.2.17",
      "net.sf.jopt-simple"    %  "jopt-simple"  % "3.3",
      "org.slf4j"             %  "slf4j-simple" % "1.7.7",
      "org.scala-lang"        % "scala-actors"  % "2.11.1",
      "org.xerial.snappy" % "snappy-java" % "1.1.0.1",
      "org.json4s" %% "json4s-jackson" % "3.2.10"
    ),
    // The issue is going from log4j 1.2.14 to 1.2.15, the developers added some features which required
    // some dependencies on various sun and javax packages.
    ivyXML := <dependencies>
        <exclude module="javax"/>
        <exclude module="jmxri"/>
        <exclude module="jmxtools"/>
        <exclude module="mail"/>
        <exclude module="jms"/>
        <dependency org="org.apache.zookeeper" name="zookeeper" rev="3.4.6">
          <exclude org="log4j" module="log4j"/>
          <exclude org="jline" module="jline"/>
        </dependency>
        <dependency org="com.github.sgroschupf" name="zkclient" rev="0.1">
        </dependency>
        <dependency org="org.apache.zookeeper" name="zookeeper" rev="3.4.6">
          <exclude module="log4j"/>
          <exclude module="jline"/>
        </dependency>
      </dependencies>
  )

  val coreSettings = Seq(
    pomPostProcess := { (pom: Node) => MetricsDepAdder(ZkClientDepAdder(pom)) }
  )

  lazy val kafka    = Project(id = "Kafka", base = file(".")).aggregate(core).settings(commonSettings: _*)
  lazy val core     = Project(id = "core", base = file("core")).settings(commonSettings: _*).settings(coreSettings: _*)

  // POM Tweaking for core:
  def zkClientDep =
    <dependency>
      <groupId>zkclient</groupId>
      <artifactId>zkclient</artifactId>
      <version>20120522</version>
      <scope>compile</scope>
    </dependency>

  def metricsDeps =
    <dependencies>
      <dependency>
        <groupId>com.codahale.metrics</groupId>
        <artifactId>metrics-core</artifactId>
        <version>3.0.2</version>
        <scope>compile</scope>
      </dependency>
    </dependencies>

  object ZkClientDepAdder extends RuleTransformer(new RewriteRule() {
    override def transform(node: Node): Seq[Node] = node match {
      case Elem(prefix, "dependencies", attribs, scope, deps @ _*) => {
        Elem(prefix, "dependencies", attribs, scope, deps ++ zkClientDep:_*)
      }
      case other => other
    }
  })

  object MetricsDepAdder extends RuleTransformer(new RewriteRule() {
    override def transform(node: Node): Seq[Node] = node match {
      case Elem(prefix, "dependencies", attribs, scope, deps @ _*) => {
        Elem(prefix, "dependencies", attribs, scope, deps ++ metricsDeps:_*)
      }
      case other => other
    }
  })
}
