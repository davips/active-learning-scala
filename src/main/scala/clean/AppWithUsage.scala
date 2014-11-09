package clean

import clean.res._
import com.sun.xml.internal.ws.developer.MemberSubmissionEndpointReference.AttributedQName

/*
active-learning-scala: Active Learning library for Scala
Copyright (c) 2014 Davi Pereira dos Santos

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

trait AppWithUsage extends App with Log with ArgParser {
  //  Class.forName("org.sqlite.JDBC")
  val superArguments = List("debug-verbosity:-1,0,1,2,...,30", "files-with-dataset-names-or-dataset-names:file1,file2|#d1,d2,d3", "paralleliz(runs folds):r|f|rf|d")
  val arguments: List[String]
  lazy val runs = Global.runs
  lazy val folds = Global.folds
  lazy val debugIntensity = if (args.isEmpty) 20 else args(0).toInt
  lazy val sql = args(3)
  lazy val path = args(3) + "/"
  lazy val datasets = if (args(1).startsWith("#")) args(1).drop(1).split(',') else datasetsFromFiles(args(1))
  lazy val parallelRuns = args(2).contains("r")
  lazy val parallelFolds = args(2).contains("f")
  lazy val parallelDatasets = args(2).contains("d")
  //args(3).contains("d")
  lazy val learnerStr = if (args.size < 4) "learner-undefined" else args(3)
  lazy val learnersStr = if (args.size < 4) Array("learners-undefined") else args(3).split(",")
  lazy val measure = args.last match {
    case "q" => Q()
    case "alca" => ALCacc()
    case "alcg" => ALCgmeans()
    case "aatq" => accAtQ()
    case "gatq" => gmeansAtQ()
    case "pa" => passiveAcc()
    case "pg" => passiveGme()
  }
  lazy val memlimit = Global.memlimit

  def memoryMonitor() = {
    Global.running = true
    new Thread(new Runnable() {
      def run() {
        while (Global.running) {
          1 to 250 takeWhile { _ =>
            Thread.sleep(20)
            Global.running
          }
          if (Runtime.getRuntime.totalMemory() / 1000000d > memlimit) {
            Global.running = false
            error(s"Limite de $memlimit MB de memoria atingido.")
          }
        }
      }
    }).start()
  }

  def run() {
    try {
      Global.debug = debugIntensity
      println(args.mkString(" "))
      if (args.size != arguments.size) {
        println(s"Usage: java -cp your-path/als-version.jar ${this.getClass.getCanonicalName.dropRight(1)} ${arguments.mkString(" ")}")
        sys.exit(1)
      }
    } catch {
      case ex: Throwable => Global.running = false
        ex.printStackTrace()
        justQuit("Erro: " + ex.getMessage)
    }
  }
}
