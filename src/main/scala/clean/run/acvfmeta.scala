///*
//
//active-learning-scala: Active Learning library for Scala
//Copyright (c) 2014 Davi Pereira dos Santos
//
//   This program is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
//   This program is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with this program.  If not, see <http://www.gnu.org/licenses/>.
//*/
//
//package clean.run
//
//import clean.lib._
//import ml.Pattern
//import ml.classifiers.{NoLearner, MetaLearner}
//import weka.filters.Filter
//
//import scala.collection.mutable
//
//object acvfmeta extends Exp with LearnerTrait with StratsTrait {
//  val context = "acvfmetaApp"
//  val arguments = superArguments :+ "leas" :+ "versao"
//  val ignoreNotDone = false
//  var outroProcessoVaiTerminarEsteDataset = false
//  var acabou = true
//  run()
//
//  def poeNaFila(fila: mutable.Set[String], f: => String): Unit =
//    try {
//      fila += f
//    } catch {
//      case e: Throwable => acabou = false
//    }
//
//  def op(ds: Ds, pool: Seq[Pattern], testSet: Seq[Pattern], fpool: Seq[Pattern], ftestSet: Seq[Pattern], learnerSeed: Int, run: Int, fold: Int, binaf: Filter, zscof: Filter) {
//    val fila = mutable.Set[String]()
//    if (ds.nclasses > maxQueries(ds)) ds.error(s"ds.nclasses ${ds.nclasses} > ${maxQueries(ds)} maxtimesteps!")
//    else if (ds.isAliveByOtherJob(run, fold)) {
//      outroProcessoVaiTerminarEsteDataset = true
//      ds.log(s"Outro job está all-izando este pool ($run.$fold). Skipping all' for this pool...", 30)
//    } else {
//      ds.startbeat(run, fold)
//      ds.log(s"Iniciando trabalho para pool $run.$fold ...", 30)
//      //         val best = BestPassiveClassif(ds, learnerSeed, pool)
//
//      //meta depende de quais leas podem ser comparados
//      val seqleas = learners(learnersStr) map (_.limpa)
//      val mapa = ((pool ++ testSet) map (p => p.id -> p)) toMap
//      val fmapa = ((fpool ++ ftestSet) map (p => p.id -> p)) toMap
//
//      stratsFpool(pool, fpool).map { fstrat =>
//        // defr pode ser apenas interessante pra tabela de acurácis, que vai ter também RF1000 e qq outro que seja interessante
//        // MetaLearner(pool, mapa, learnerSeed, ds, "metade", st, seqleas)("defr-a")
//        val fakelearner = MetaLearner(pool, fpool, mapa, fmapa, learnerSeed, ds, fstrat(NoLearner()), seqleas)("PCTr-a")
//        val learner = MetaLearner(pool, fpool, mapa, fmapa, learnerSeed, ds, fstrat(fakelearner), seqleas)("PCTr-a")
//        learner -> fstrat(learner)
//      } foreach { case (learner, fstrat) =>
//        //p/ strats com mahala, filtros não podem ficar null,null
//        val fqueries = if (ds.areQueriesFinished(fpool.size, fstrat, run, fold, binaf, zscof, completeIt = true, maxQueries(ds))) {
//          ds.log(s"fQueries  done for ${fstrat.abr}/${fstrat.learner} at pool $run.$fold. Retrieving from disk.")
//          ds.queries(fstrat, run, fold, binaf, zscof)
//        } else ds.writeQueries(fstrat, run, fold, maxQueries(ds))
//        val queries = ds.queries(fstrat, run, fold, null, null)
//
//        //estou supondo que areHitsFinished() e writeHits() só precisam de filtros para dar build(), update() e cm();
//        // logo não precisam de querfiltro(), exceto por cm() que vai ser tratado por um Modelo() especial
//        ds.log(s"Hits [$learner $fstrat $learner] at pool $run.$fold.")
//        if (ds.areHitsFinished(pool.size, testSet, fstrat, learner, run, fold, null, null, completeIt = true, maxQueries(ds) - ds.nclasses + 1)) ds.log(s"Hits  done for ${fstrat.abr}/$learner at pool $run.$fold.")
//        else ds.writeHits(pool.size, testSet, queries.toVector, fstrat, run, fold, maxQueries(ds) - ds.nclasses + 1)(learner)
//      }
//      if (fila.exists(_.startsWith("insert"))) ds.batchWrite(fila.toList)
//      fila.clear()
//    }
//  }
//
//  def datasetFinished(ds: Ds) = {
//    if (acabou && !outroProcessoVaiTerminarEsteDataset) {
//      ds.markAsFinishedRun("+lapack" + versao)
//      ds.log("Dataset marcado como terminado !", 50)
//    }
//    outroProcessoVaiTerminarEsteDataset = false
//    acabou = true
//  }
//
//  def isAlreadyDone(ds: Ds) = ds.isFinishedRun("+lapack" + versao)
//
//  def end(res: Map[String, Boolean]): Unit = {
//  }
//}
