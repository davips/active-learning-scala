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

package al.strategies

import clean.lib.Log
import ml.Pattern
import ml.classifiers.{NoLearner, Limpa, Learner}
import util.Graphics.Plot

/**
 * Only distinct patterns are accepted into the pool.
 */
trait Strategy extends Log with Limpa {
   val context = "Strategy"
   val id: Int
   val abr: String
   val pool: Seq[Pattern]
   lazy val seed = pool.take(10).map(_.id).zipWithIndex.map { case (c, i) => c * i}.sum
   lazy val distinct_pool = if (pool.distinct != pool) {
      println("The pool cannot have repeated instances!")
      sys.exit(1)
   } else pool
   val debug: Boolean
   lazy val nclasses = if (distinct_pool.nonEmpty) distinct_pool.head.nclasses else throw new Error("Lazy val nclasses undiscoverable from an empty patterns!")
   val delay: Double = .5
   lazy val plot = new Plot
   lazy val (firstof_each_class, rest) = extract_one_per_class(distinct_pool)
   lazy val old = id

   lazy val abrev = abr + ((this, learner) match {
      case (_, NoLearner()) | (_: SVMmultiRBF, _) => ""
      case _ => " " + learner.abr
   })
   lazy val igualdade = id -> learner.id

   override def equals(that: Any) = that match {
      case that: Strategy => that.igualdade == igualdade
      case _ => false
   }

   def learner: Learner

   def convlid(id: Int) = id match {
      case 773 => 0
      case 2651110 => 10
      case 2 => 20
      case 12 => 30
      case 8001 => 40
      case 666003 => 50
   }

   /**
    * Returns a stream of queries.
    * The first instances are the first from each class,
    * because in practice no one would risk the budget before having at least these initial labels.
    * (except in cluster-based approaches, but even in this case an initial sampling is reasonable,
    * specially because of the guarantee of encompassing all classes)
    * @return
    */
   lazy val queries: Stream[Pattern] = firstof_each_class.toStream ++ resume_queries_impl(rest, firstof_each_class)

   /**
    * Se estourar o tempo limite,
    * espera a query atual terminar
    * e retorna aquelas feitas até esse ponto.
    * A atual não é desperdiçada.
    * @param seconds
    */
   def timeLimitedQueries(seconds: Double, exiting: () => Boolean = () => false) = {
      val ti = System.currentTimeMillis()
      var t = 0d
      var last: Pattern = null
      var continua = true
      val withinTimeLimit = queries.takeWhile { p =>
         last = p
         t = (System.currentTimeMillis() - ti) / 1000d
         continua = t <= seconds && !exiting()
         continua
      }.toSeq
      if (continua) withinTimeLimit else withinTimeLimit :+ last
   }

   /**
    * Se estourar o tempo limite,
    * espera a query atual terminar
    * e retorna aquelas feitas até esse ponto.
    * A atual não é desperdiçada.
    * @param seconds
    */
   def timeLimitedResumeQueries(labeled: Seq[Pattern], seconds: Double, exiting: () => Boolean = () => false) = {
      val ti = System.currentTimeMillis()
      var t = 0d
      var last: Pattern = null
      var continua = true
      val withinTimeLimit = resume_queries(labeled).takeWhile { p =>
         last = p
         t = (System.currentTimeMillis() - ti) / 1000d
         continua = t <= seconds && !exiting()
         continua
      }.toSeq
      if (continua) withinTimeLimit else withinTimeLimit :+ last
   }

   protected def resume_queries_impl(unlabeled: Seq[Pattern], labeled: Seq[Pattern]): Stream[Pattern]

   /**
    * Resume queries from the last performed queries.
    * Returns only the new queries.
    * Like in queries(),
    * the first instances from labeled are the first from each class (in the same order as generated by extract_one_per_class()).
    * Exception if they are not complete yet.
    */
   def resume_queries(labeled: Seq[Pattern]) = {
      //todo: I donk know if resuming queries is a perfectly working idea
      if (firstof_each_class != labeled.take(nclasses)) {
         println("Expected: " + firstof_each_class)
         println(s"")
         println("Found:" + labeled.take(nclasses).toList)
         println(s"")
         println(s"")
         println("Expected: " + firstof_each_class.map(_.label))
         println(s"")
         println("Found:" + labeled.take(nclasses).toList.map(_.label))
         error(s"In dataset '${labeled.head.dataset().relationName()}': queries cannot be resumed, there should be the exact one-instance-per-class subset at the beginning.")
      }
      resume_queries_impl(distinct_pool.diff(labeled), labeled)
   }

   protected def extract_one_per_class(patterns: Seq[Pattern]) = {
      val firstof_each_class = ((0 until nclasses) map {
         c => patterns find (_.label == c) match {
            case Some(pattern) => pattern
            case _ => error("Dataset should have at least one instance from each class per fold! Label index " + c + " not found in dataset " + patterns.head.dataset().relationName() + " !")
         }
      }).toList
      (firstof_each_class, patterns.diff(firstof_each_class))
   }

   protected def visual_test(selected: Pattern, unlabeled: Seq[Pattern], labeled: Seq[Pattern])
}