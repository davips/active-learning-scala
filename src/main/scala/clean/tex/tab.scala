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

import al.strategies.{PassiveGme, RandomSampling, PassiveAcc}
import clean.{AppWithUsage, Ds, LearnerTrait, StratsTrait}
import ml.classifiers.NB
import util.{Stat, StatTests}

import scala.collection.mutable

object tab extends AppWithUsage with LearnerTrait with StratsTrait {
  lazy val arguments = superArguments ++ List("learners:nb,5nn,c45,vfdt,ci,...|eci|i|ei|in|svm", "medida:alca|alcg")
  val context = "tabtex"
  val sl = mutable.LinkedHashSet[String]()
  run()

  override def run() = {
    super.run()
    val res = datasets map { dataset =>
      val ds = Ds(path, dataset)
      ds.open()
      sl += s"Q/N"
      val ms = for {
        s <- Seq(measure.id match {
          case 11 | 1 => PassiveAcc(NB(), Seq())
          case 12 | 2 => PassiveGme(NB(), Seq())
        }) ++ allStrats()
      } yield {

        if (s.id == 22 || s.id == 23) {
          val learner = s.learner
          if (ds.isMeasureComplete(measure, s.id, learner.id)) {
            sl += s"${s.abr} ${learner.toString.take(2)}"
            val vs = for {
              r <- 0 until runs
              f <- 0 until folds
            } yield {
              if (measure.id == 0) -1
              else ds.getMeasure(s.mea, RandomSampling(Seq()), learner, r, f) match {
                case Some(v) => v
                case None => ds.quit(s"No pass measure for ${(s.mea, s, learner, r, f)}!")
              }
            }
            Seq(Stat.media_desvioPadrao(vs.toVector))
          } else Seq()

        } else if (s.id >= 17 && s.id <= 21) {
          val learner = s.learner
          if (ds.isMeasureComplete(measure, s.id, learner.id)) {
            sl += s"${s.abr} ${learner.toString.take(2)}"
            val vs = for {
              r <- 0 until runs
              f <- 0 until folds
            } yield {
              if (measure.id == 0) -1
              else ds.getMeasure(measure, s, learner, r, f) match {
                case Some(v) => v
                case None => ds.quit(s"No svm/maj measure for ${(measure, s, learner, r, f)}!")
              }
            }
            Seq(Stat.media_desvioPadrao(vs.toVector))
          } else Seq()
        } else {

          (learners(learnersStr) map { learner =>
            if (ds.isMeasureComplete(measure, s.id, learner.id)) {
              sl += s"${s.abr} ${learner.toString.take(2)}"
              val vs = for {
                r <- 0 until runs
                f <- 0 until folds
              } yield {
                if (measure.id == 0) -1
                else ds.getMeasure(measure, s, learner, r, f) match {
                  case Some(v) => v
                  case None => ds.quit(s"No measure for ${(measure, s, learner, r, f)}!")
                }
              }
              Seq(Stat.media_desvioPadrao(vs.toVector))
            } else Seq()
          }).flatten
        }
      }
      val res = ds.dataset -> (Seq((ds.Q.toDouble, ds.n.toDouble)) ++ ms.flatten)
      ds.close()
      res
    }
    println(s"")
    println(s"")
    println(s"")
    val tbs = res.sortBy(_._2.head._1) grouped (61)
    tbs foreach { case res0 =>
      StatTests.extensiveTable2(res0.toSeq.map(x => x._1.take(3) + x._1.takeRight(3) -> x._2), sl.toVector.map(_.toString), "nomeTab", measure.toString)
    }
    justQuit("Datasets prontos.")
  }
}
