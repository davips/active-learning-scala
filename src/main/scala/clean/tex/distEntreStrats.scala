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

import al.strategies.Passive
import clean.lib._
import util.{Stat, StatTests}

object distEntreStrats extends AppWithUsage with LearnerTrait with StratsTrait with RangeGenerator {
   lazy val arguments = superArguments ++ List("learners:nb,5nn,c45,vfdt,ci,...|eci|i|ei|in|svm")
   val context = "distEntreStratstex"
   val measure = ALCKappa
   run()

   override def run() = {
      super.run()
      val accs0 = for (s <- stratsForTreeSemSVM.par) yield {
         val res0 = for {
            dataset <- datasets
            l <- learners(learnersStr)
         } yield {
            val ds = Ds(dataset, readOnly = true)
            ds.open()
            val (ti, tf) = maxRange(ds, 2, 200) //<- verificar 100 ou 200
            val vs = for {
               r <- 0 until runs
               f <- 0 until folds
               } yield measure(ds, s, l, r, f)(ti, tf).read(ds).getOrElse(ds.error(s"incompleto para ${(ds, s, l, r, f)}!"))
            //            println(s"$ds $vs")
            ds.close()
            Stat.media_desvioPadrao(vs.toVector)._1
         }
         s.abr -> res0
      }
      val accs = accs0.toList //.sortBy(_._1)
      val dists = for (a <- accs) yield {
         val ds = for (b <- accs) yield {
            val d = math.sqrt(a._2.zip(b._2).map { case (v1, v2) =>
               val v = v1 - v2
               v * v
            }.sum)
            ff(100)(1 / (1 + d))
         }
         a._1 -> ds
      }
      val fw = new PrintWriter("/home/davi/wcs/tese/stratDists.tex", "ISO-8859-1")
      fw.write(StatTests.distTable(dists, "stratDists", "estratégias", measure.toString))
      fw.close()
   }
}
