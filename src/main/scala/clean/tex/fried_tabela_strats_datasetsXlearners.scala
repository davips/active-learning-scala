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

package clean.tex

import java.io.PrintWriter

import clean.lib._
import util.{Stat, StatTests}

object fried_tabela_strats_datasetsXlearners extends AppWithUsage with LearnerTrait with StratsTrait with RangeGenerator {
  lazy val arguments = superArguments ++ List("learners:nb,5nn,c45,vfdt,ci,...|eci|i|ei|in|svm", "comprimento:all,half,50", "porRisco:r", "dist:euc,man,mah")
  val context = "friedEtabelasALCKappaAll"
  val measure = ALCKappa
  run()

  override def run() = {
    super.run()
    val caption = language match {
      case "pt" => s"Um contra um para todos os algoritmos de aprendizado. Medida: kappa médio. \\textit{Legenda na Tabela \\ref{tab:friedClassif}.}"
      case "en" => s"Pairwise comparison: each asterisk/cross/dot indicates that the algorithm at the row has better $measure than the strategy at the column within a confidence interval of 0.99/0.95/0.90."
    }
    val (sls, res0) = (for {
      dataset <- datasets
      le <- learners(learnersStr)
    } yield {
        val ds = Ds(dataset, readOnly = true)
        ds.open()
        val (ti0, th, tf0, tpass) = ranges(ds)
        val ti = comprimento match {
          case "half" => ti0
          case "half2" => th
          case "all" => ti0
          case "50" => ti0
        }
        val tf = comprimento match {
          case "half" => th
          case "half2" => tf0
          case "all" => tf0
          case "50" => 49
        }
        val (sts, sres) = (for {
        //            s <- stratsForBRACIS15
          s <- stratsTexForGraficoSimples
        } yield {
            val vs = for {
              r <- 0 until runs
              f <- 0 until folds
            } yield measure(ds, s(le), le, r, f)(ti, tf).read(ds).getOrElse(throw new Error("NA"))
            s(le).abr -> (if (!porRisco) Stat.media_desvioPadrao(vs.toVector) else Stat.media_desvioPadrao(vs.toVector)._2 -> NA)
          }).unzip
        ds.close()
        sts -> (renomeia(ds.dataset) -> sres)
      }).unzip
    val sl = sls.head

    val sorted = res0.toList.sortBy(_._1).zipWithIndex.map(x => ((x._2 + 1).toString + "-" + x._1._1) -> x._1._2)
    //      val arq1 = s"/home/davi/wcs/tese/stratstab${dist}" + comprimento + ".tex"
    //      println(arq1)
    // //      val fw = new PrintWriter(arq1, "ISO-8859-1")
    //      val fw = new PrintWriter(arq1, "UTF-8")
    //      sorted.grouped(32).zipWithIndex.foreach { case (res1, i) =>
    //         fw.write(StatTests.extensiveTable2("", true, 100, res1.toSeq.map(x => x._1 -> x._2), sl.toVector.map(_.toString), s"stratstab${i}All" + comprimento + "a", "ALC para todos aprendizes", 7))
    //         fw.write(StatTests.extensiveTable2("", false, 100, res1.toSeq.map(x => x._1 -> x._2), sl.toVector.map(_.toString), s"stratstab${i}All" + comprimento + "b", "ALC para todos aprendizes", 7))
    //         fw.write("\n\\clearpage\n")
    //         //         println(StatTests.extensiveTable2(true, 100, res1.toSeq.map(x => x._1 -> x._2), sl.toVector.map(_.toString), s"stratsALCKappa${i}All" + comprimento + "a", "ALCKappa para todos aprendizes half", 7))
    //         //         println(StatTests.extensiveTable2(false, 100, res1.toSeq.map(x => x._1 -> x._2), sl.toVector.map(_.toString), s"stratsALCKappa${i}All" + comprimento + "b", "ALCKappa para todos aprendizes half", 7))
    //      }
    //      fw.close()

    val res = sorted.filter(!_._2.contains(NA, NA))
    println(s"")
    val pairs = if (!porRisco) {
      res foreach (x => println(x._2.map { case (a, b) => (a * 1000).round / 1000d }.mkString(" ")))
      StatTests.friedmanNemenyi(res.map(x => x._1 -> x._2.map(_._1)), sl.toVector)
    }
    else {
      res foreach (x => println(x._2.map { case (a, b) => (a * -1000).round / 1000d }.mkString(" ")))
      StatTests.friedmanNemenyi(res.map(x => x._1 -> x._2.map(1 - 1 * _._2)), sl.toVector)
    }
    //      val arq2 = s"/home/davi/wcs/tese/stratsfried${dist}" + (if (porRisco) "Risco" else "") + comprimento + ".tex"
    //      println(arq2)
    //      val fw2 = new PrintWriter(arq2, "UTF-8")
    // //      val fw2 = new PrintWriter(arq2, "ISO-8859-1")
    println(s"")
    println(s"")
    println(StatTests.pairTable(pairs, "stratsFried" + (if (porRisco) "Risco" else "") + comprimento, 2, caption))
    //      fw2.write(StatTests.pairTable(pairs, s"strats${measure}FriedAll" + (if (porRisco) "Risco" else "") + comprimento, 2, caption))
    //      fw2.close()
    println(s"${res.size} datasets completos")

    println(sl.toVector)
  }
}
