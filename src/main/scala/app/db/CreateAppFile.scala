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

package app.db

import java.io.File

import al.strategies._
import app.ArgParser
import ml.classifiers._

object CreateAppFile extends App {
  //commented code: aproveitar no futuro qnd for gerar tabela de meta-atributos
  //val (path, names) = ArgParser.testArgs(getClass.getSimpleName.dropRight(1), args, 2, "Version " + 0.2 + ". Creates the SQLite file for the whole als app.")
  //case class MetaAtt(dataset: Dataset, name: String, value: Float) extends Entity

  val sampleSize = 500
  val sampleSizePerfect = 1000
  val strats = List(
    RandomSampling(Seq()),
    ClusterBased(Seq()),

    Uncertainty(NoLearner(), Seq()),
    Entropy(NoLearner(), Seq()),
    Margin(NoLearner(), Seq()),
    new SGmulti(NoLearner(), Seq(), "consensus"),
    new SGmulti(NoLearner(), Seq(), "majority"),
    new SGmultiJS(NoLearner(), Seq()),
    ExpErrorReduction(NoLearner(), Seq(), "entropy", sampleSize),
    ExpErrorReduction(NoLearner(), Seq(), "accuracy", sampleSize),
    DensityWeightedTrainingUtility(NoLearner(), Seq(), 1, 1, "cheb"),
    DensityWeightedTrainingUtility(NoLearner(), Seq(), 1, 1, "eucl"),
    DensityWeightedTrainingUtility(NoLearner(), Seq(), 1, 1, "maha"),
    DensityWeightedTrainingUtility(NoLearner(), Seq(), 1, 1, "manh"),
    MahalaWeighted(NoLearner(), Seq(), 1),
    MahalaWeightedRefreshed(NoLearner(), Seq(), 1, sampleSize),
    MahalaWeightedTrainingUtility(NoLearner(), Seq(), 1, 1),
    MahalaWeightedRefreshedTrainingUtility(NoLearner(), Seq(), 1, 1, sampleSize),
    FastPerfectRealisticAccuracy(NoLearner(), Seq(), sampleSizePerfect),
    DensityWeighted(NoLearner(), Seq(), 1, "eucl"),
    ExpErrorReduction(NoLearner(), Seq(), "gmeans", sampleSize),
    ExpErrorReductionMargin(NoLearner(), Seq(), "entropy", sampleSize),
    SVMmulti(Seq(), "SELF_CONF"),
    SVMmulti(Seq(), "KFF"),
    SVMmulti(Seq(), "BALANCED_EE"),
    SVMmulti(Seq(), "SIMPLE")
  )

  //ensures there is no previous file
  val alsFileStr = ArgParser.appPath + "app.db"
  val dbOriginal = new File(alsFileStr)
  if (dbOriginal.exists) {
    println("File " + dbOriginal + " already exists! Delete it first.")
    sys.exit(0)
  }

  val learners = Seq(NoLearner(), NB(), C45(), VFDT(), KNN(5, "eucl", Seq()), KNN(5, "manh", Seq()),
    IELM(1), EIELM(1), CIELM(1), ECIELM(1),
    interaELM(1),
    interaELMNoEM(1), interawELM(15), interawfELM(15),
    OSELM(math.sqrt(1).toInt)
  )

  //opens (creates) app.db
  val af = AppFile(createOnAbsence = true)
  af.open(debug = true)
  af.createOtherTables()
  af.createTableOfLearners(learners)
  af.createTableOfStrategies(strats)
  af.save()
  af.close()
}
