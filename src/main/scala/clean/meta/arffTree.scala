package clean.meta

import java.io.FileWriter

import clean._
import clean.res.{ALCBalancedAcc, ALCKappa, BalancedAcc}
import util.{StatTests, Stat}

import scala.io.Source

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
object arffTree extends AppWithUsage with StratsTrait with LearnerTrait with RangeGenerator {
   /*
   Antes, se colocasse human=false, apenas um intervalo e apenas um learner,
   daria na mesma que usar arffMeta.scala="Winner" com apenas um learner.

   Quando ties=true, o metaexemplo é repetido em caso de empate.
   Caso contrário, apenas o melhor vencedor serve de rótulo.
    */
   val ties = true
   val context = "metaAttsTreeApp"
   val arguments = superArguments
   //   val measure = ALCKappa
   val measure = ALCBalancedAcc
   run()

   def ff(x: Double) = (x * 100).round / 100d

   override def run() = {
      super.run()
      val ss = stratsForTree().map(_.abr).toVector
      val metadata0 = for {
         name <- datasets.toList

         l <- allLearners().par
         (ti, tf, budix) <- {
            val ds = Ds(name, readOnly = true)
            ds.open()
            val tmp = ranges(ds, 2, 200) // <- verificar!!! verificar tb argumentos do programa!!!
            ds.close()
            tmp.zipWithIndex.map(x => (x._1._1, x._1._2, x._2))
         }

      } yield {
         val ds = Ds(name, readOnly = true)
         println(s"$ds")
         ds.open()
         val seqratts = (for (r <- 0 until Global.runs; f <- 0 until Global.folds) yield ds.attsFromR(r, f)).transpose.map(_.toVector)
         val rattsmd = seqratts map Stat.media_desvioPadrao
         val (rattsm, _) = rattsmd.unzip
         val res = if (ties) {
            val vs = for {
               r <- 0 until runs
               f <- 0 until folds
            //               duplicadorDeAmostra <- 0 to 1
            } yield {
               val poolStr = (100 * r + f).toString
               val medidas = for {
                  s <- stratsForTree() // <- verificar!!!
                  le = if (s.id >= 17 && s.id <= 21 || s.id == 968 || s.id == 969) s.learner else l
               } yield measure(ds, s, le, r, f)(ti, tf).read(ds).getOrElse {
                     ds.log(s" base incompleta para intervalo [$ti;$tf] e pool ${(s, le, r, f)}.", 40)
                     -2d
                  }
               poolStr -> medidas
            }
            val winners = StatTests.clearWinners(vs, ss)
            ss.map { x =>
               if (winners.contains(x)) Option(ds.metaAttsHumanAndKnowingLabels, l.abr, x, if (budix == 0) "baixo" else "alto")
               else None
            }.flatten
         } else {
            val medidas = for {
               s <- stratsForTree() // <- verificar!!!
            } yield {
               val le = if (s.id >= 17 && s.id <= 21 || s.id == 968 || s.id == 969) s.learner else l
               val ms = for {
                  r <- 0 until Global.runs
                  f <- 0 until Global.folds
               } yield measure(ds, s, le, r, f)(ti, tf).read(ds).getOrElse {
                     ds.log(s" base incompleta para intervalo [$ti;$tf] e pool ${(s, le, r, f)}.", 40)
                     -2d
                  }
               s.abr -> Stat.media_desvioPadrao(ms.toVector)
            }
            if (medidas.exists(x => x._2._1 == -2d)) Seq() else Seq((ds.metaAttsHumanAndKnowingLabels, l.abr, medidas.maxBy(_._2._1)._1, if (budix == 0) "baixo" else "alto"))
         }
         ds.close()
         res
      }
      val metadata = metadata0.flatten.toList
      //      metadata foreach println

      //cria ARFF
      val pred = metadata.map(_._3)
      val labels = pred.distinct.sorted
      val data = metadata.map { case (numericos, learner, vencedora, budget) => numericos.mkString(",") + s",$budget,$learner,$vencedora"}
      val numAtts = humanNumAttsNames
      val header = List("@relation data") ++ numAtts.split(",").map(i => s"@attribute $i numeric") ++ List("@attribute \"orçamento\" {baixo,alto}", "@attribute learner {" + allLearners().map(_.abr).mkString(",") + "}", "@attribute class {" + labels.mkString(",") + "}", "@data")
      val pronto = header ++ data
      pronto foreach println

      val fw = new FileWriter("/home/davi/wcs/ucipp/uci/metaTree" + (if (ties) "Ties" else "") + ".arff")
      pronto foreach (x => fw.write(s"$x\n"))
      fw.close()
      println(s"${data.size}")
   }
}