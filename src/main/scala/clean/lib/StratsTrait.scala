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

package clean.lib

import al.strategies._
import ml.Pattern
import ml.classifiers.{Learner, NoLearner}

trait StratsTrait {
   def stratsPool(poolForLearner: Seq[Pattern] = Seq(), pool: Seq[Pattern] = Seq()) = Seq(
      //essas ganharam ids por par s/l porque suas medidas de distancia foram afetadas por filtros
      (learner: Learner) => DensityWeightedTrainingUtilityFixo(poolForLearner, learner, pool, "manh")
      , (learner: Learner) => DensityWeightedTrainingUtilityFixo(poolForLearner, learner, pool, "eucl")
      , (learner: Learner) => HTUFixo(poolForLearner, learner, pool, "manh")
      , (learner: Learner) => HTUFixo(poolForLearner, learner, pool, "eucl")
      , (learner: Learner) => DensityWeightedFixo(poolForLearner, learner, pool, 1, "manh")
      , (learner: Learner) => DensityWeightedFixo(poolForLearner, learner, pool, 1, "eucl")

      //essas não precisam ser Fixo porque não são afetadas pelo filtro do learner (e todas agora são tratadas como agnósticas)
      , (learner: Learner) => Margin(learner, poolForLearner)
      , (learner: Learner) => ExpErrorReductionMargin(learner, poolForLearner, "entropy")
      , (learner: Learner) => SGmulti(learner, poolForLearner, "consensus")

      //essas naturalmente não usaram filtro e cada 'learner' decidiu sozinho se usava filtro ou não (all.scala mostra que hits foi feito c/s filtro de acordo com classif)
      //(svm.scala força filtro nas strats, porém, por serem agnósticas, as queries já foram geradas corretamente antes pelo all.scala ou rf.scala
      , (learner: Learner) => RandomSampling(pool) //0
      , (learner: Learner) => ClusterBased(pool) //1
      , (learner: Learner) => AgDensityWeightedTrainingUtility(pool, "manh") //701
      , (learner: Learner) => AgDensityWeightedTrainingUtility(pool, "eucl") //601
   )

   def stratsFpool(poolForLearner: Seq[Pattern] = Seq(), fpool: Seq[Pattern] = Seq()) = Seq(
      //essas ganharam ids por par s/l porque medem distancia filtradas e afetaram seus learners (e precisavam ser reimplementadas para receber pools independentes)
      (learner: Learner) => DensityWeightedTrainingUtilityFixo(poolForLearner, learner, fpool, "maha")
      , (learner: Learner) => HTUFixo(poolForLearner, learner, fpool, "maha")
      , (learner: Learner) => DensityWeightedFixo(poolForLearner, learner, fpool, 1, "maha")

      //essa strat pede filtro, então forçou filtro no classif (que era chamado de learner)
      //mudei id de 991 pra 591
      , (learner: Learner) => AgDensityWeightedTrainingUtility(fpool, "maha")

      //só passou a aceitar classif diferente de learner no acv.scala. não devo mais rodar os antigos
      //mesmo porque já estavam terminados e o acv.scala contempla o caso classif = learner
      , (learner: Learner) => SVMmultiRBF(fpool, "BALANCED_EEw")
      , (learner: Learner) => SVMmultiRBF(fpool, "SIMPLEw")
   )

   def allStrats(learner: Learner = NoLearner(), pool: Seq[Pattern] = Seq()) = stratsemLearnerExterno(pool) ++ stratcomLearnerExterno(learner, pool)

   def stratcomLearnerExterno(learner: Learner = NoLearner(), pool: Seq[Pattern] = Seq()) = stratsComLearnerExterno_FilterFree(pool, learner) ++ stratsComLearnerExterno_FilterDependent(pool, learner)

   def stratsemLearnerExterno(pool: Seq[Pattern] = Seq()) = stratsSemLearnerExterno_FilterFree(pool) ++ stratsSemLearnerExterno_FilterDependent(pool)

   //   def stratsSGmajJS(pool: Seq[Pattern], learner: Learner) = List[Strategy](new SGmulti(learner, pool, "majority"), new SGmultiJS(learner, pool))


   //////////////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////////////

   def stratsSemLearnerExterno_FilterFree(pool: Seq[Pattern]) = List[Strategy](
      RandomSampling(pool) //0
      , ClusterBased(pool) //1
      , AgDensityWeightedTrainingUtility(pool, "eucl") //601
      , AgDensityWeightedTrainingUtility(pool, "manh") //701
      , QBC(pool) //1292212
   )

   def stratsSemLearnerExterno_FilterDependent(pool: Seq[Pattern]) = List[Strategy](
      AgDensityWeightedTrainingUtility(pool, "maha"), //901
      //TROQUEI LEARNER DO SVMmultiLinear PARA SVMLibDegree1
      // (aproveitando id/queries das estratégias SVMmulti),
      // demora mais que LibLinear, mas fica em linha com artigo do Tong!
      //      SVMmultiLinear(pool, "BALANCED_EEw"),
      //      SVMmultiLinear(pool, "SIMPLEw"),
      //      SVMmultiRBFW(pool, "BALANCED_EEw"),
      //      SVMmultiRBFW(pool, "SIMPLEw"),
      SVMmultiRBF(pool, "BALANCED_EEw"),
      SVMmultiRBF(pool, "SIMPLEw"),
      ExpELMChange(pool) //1006600
   )

   def stratsComLearnerExterno_FilterFree(pool: Seq[Pattern], learner: Learner) = List[Strategy](
      Entropy(learner, pool) //4
      , Margin(learner, pool) //3
      , DensityWeighted(learner, pool, 1, "eucl") //5
      , DensityWeightedTrainingUtility(learner, pool, "eucl") //7
      , DensityWeightedTrainingUtility(learner, pool, "manh") //7
      , SGmulti(learner, pool, "consensus") //14
      , ExpErrorReductionMargin(learner, pool, "balacc") //74
      , ExpErrorReductionMargin(learner, pool, "entropy") //11
      , HTU(learner, pool, "eucl") //4003006
      , HTU(learner, pool, "manh") //4003007
   )

   def stratsComLearnerExterno_FilterDependent(pool: Seq[Pattern], learner: Learner) = List[Strategy](
      DensityWeightedTrainingUtility(learner, pool, "maha") //9
      , HTU(learner, pool, "maha") //4003009
   )

   //////////////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////////////////////

   //   val stratsForTreeSemSVM = stratsForTree().take(7) ++ stratsForTree().drop(9)
   //   val stratsForTreeSemSVMRedux = stratsForTreeRedux().take(5) ++ stratsForTreeRedux().drop(7)

   def stratsForTree(pool: Seq[Pattern] = Seq(), learner: Learner = NoLearner()) = Seq(
      RandomSampling(pool) //0
      , ClusterBased(pool) //1
      , AgDensityWeightedTrainingUtility(pool, "eucl") //601
      , AgDensityWeightedTrainingUtility(pool, "manh") //701
      , AgDensityWeightedTrainingUtility(pool, "maha") //901
      , HTU(learner, pool, "eucl") //4003006
      , HTU(learner, pool, "manh") //4003007
      , HTU(learner, pool, "maha") //4003009
      , new SGmulti(learner, pool, "consensus") //14
      , Entropy(learner, pool) //4
      , Margin(learner, pool) //3
      , DensityWeighted(learner, pool, 1, "eucl") //5
      , DensityWeightedTrainingUtility(learner, pool, "eucl")
      , DensityWeightedTrainingUtility(learner, pool, "manh")
      , DensityWeightedTrainingUtility(learner, pool, "maha") //9
      , ExpErrorReductionMargin(learner, pool, "balacc") //74
      , ExpErrorReductionMargin(learner, pool, "entropy") //11

      , SVMmultiRBF(pool, "BALANCED_EEw")
      , SVMmultiRBF(pool, "SIMPLEw")
      , ExpELMChange(pool), //1006600
      QBC(pool) //1292212
   )

   def stratsForTreeRedux(pool: Seq[Pattern] = Seq(), learner: Learner = NoLearner()) = Seq(
      RandomSampling(pool) //0
      , ClusterBased(pool) //1
      , AgDensityWeightedTrainingUtility(pool, "eucl")
      , AgDensityWeightedTrainingUtility(pool, "manh")
      , AgDensityWeightedTrainingUtility(pool, "maha")
      , HTU(learner, pool, "eucl")
      , HTU(learner, pool, "manh")
      , HTU(learner, pool, "maha")
      , new SGmulti(learner, pool, "consensus") //14
      , Margin(learner, pool) //3
      , DensityWeighted(learner, pool, 1, "eucl") //5
      , DensityWeightedTrainingUtility(learner, pool, "eucl")
      , DensityWeightedTrainingUtility(learner, pool, "manh")
      , DensityWeightedTrainingUtility(learner, pool, "maha")
      , ExpErrorReductionMargin(learner, pool, "balacc") //74
      , ExpErrorReductionMargin(learner, pool, "entropy") //11

      , SVMmultiRBF(pool, "BALANCED_EEw")
      , SVMmultiRBF(pool, "SIMPLEw")
      , ExpELMChange(pool), //1006600
      QBC(pool) //1292212
   )

   def stratsForTreeReduxEuc(pool: Seq[Pattern] = Seq(), learner: Learner = NoLearner()) = Seq(
      RandomSampling(pool) //0
      , ClusterBased(pool) //1
      , AgDensityWeightedTrainingUtility(pool, "eucl")
      , HTU(learner, pool, "eucl")
      , new SGmulti(learner, pool, "consensus") //14
      , Margin(learner, pool) //3
      , DensityWeighted(learner, pool, 1, "eucl") //5
      , DensityWeightedTrainingUtility(learner, pool, "eucl")
      , ExpErrorReductionMargin(learner, pool, "balacc") //74
      , ExpErrorReductionMargin(learner, pool, "entropy") //11

      , SVMmultiRBF(pool, "BALANCED_EEw")
      , SVMmultiRBF(pool, "SIMPLEw")
      , ExpELMChange(pool), //1006600
      QBC(pool) //1292212
   )

   def stratsForTreeReduxMan(pool: Seq[Pattern] = Seq(), learner: Learner = NoLearner()) = Seq(
      RandomSampling(pool) //0
      , ClusterBased(pool) //1
      , AgDensityWeightedTrainingUtility(pool, "manh")
      , HTU(learner, pool, "manh")
      , new SGmulti(learner, pool, "consensus") //14
      , Margin(learner, pool) //3
      , DensityWeighted(learner, pool, 1, "eucl") //5
      , DensityWeightedTrainingUtility(learner, pool, "manh")
      , ExpErrorReductionMargin(learner, pool, "balacc") //74
      , ExpErrorReductionMargin(learner, pool, "entropy") //11

      , SVMmultiRBF(pool, "BALANCED_EEw")
      , SVMmultiRBF(pool, "SIMPLEw")
      , ExpELMChange(pool), //1006600
      QBC(pool) //1292212
   )

   def stratsForTreeReduxMah(pool: Seq[Pattern] = Seq(), learner: Learner = NoLearner()) = Seq(
      RandomSampling(pool) //0
      , ClusterBased(pool) //1
      , AgDensityWeightedTrainingUtility(pool, "maha")
      , HTU(learner, pool, "maha")
      , new SGmulti(learner, pool, "consensus") //14
      , Margin(learner, pool) //3
      , DensityWeighted(learner, pool, 1, "eucl") //5
      , DensityWeightedTrainingUtility(learner, pool, "maha")
      , ExpErrorReductionMargin(learner, pool, "balacc") //74
      , ExpErrorReductionMargin(learner, pool, "entropy") //11

      , SVMmultiRBF(pool, "BALANCED_EEw")
      , SVMmultiRBF(pool, "SIMPLEw")
      , ExpELMChange(pool), //1006600
      QBC(pool) //1292212
   )
}

/* ids:
      Majoritary(pool) //21
      , RandomSampling(pool) //0
      , ClusterBased(pool) //1
      Uncertainty(learner, pool) //2
      , Entropy(learner, pool) //4
      , Margin(learner, pool) //3
      , DensityWeighted(learner, pool, 1, "eucl") //5
      , DensityWeighted(learner, pool, 0.5, "eucl") //5005
      , AgDensityWeightedTrainingUtility(learner, pool, "eucl") //600
      , AgDensityWeightedLabelUtility(learner, pool, "eucl") //360
      , AgDensityWeightedTrainingUtility(learner, pool, "eucl", 0.5, 0.5) //650
      , AgDensityWeightedLabelUtility(learner, pool, "eucl", 0.5, 0.5) //410
      , DensityWeightedTrainingUtility(learner, pool, "eucl") //6
      , DensityWeightedTrainingUtility(learner, pool, "manh") //7
      , DensityWeightedLabelUtility(learner, pool, "eucl") //36
      , DensityWeightedTrainingUtility(learner, pool, "eucl", 0.5, 0.5) //50006
      , DensityWeightedTrainingUtility(learner, pool, "manh", 0.5, 0.5) //50007
      , DensityWeightedLabelUtility(learner, pool, "eucl", 0.5, 0.5) //86
      , ExpErrorReductionMargin(learner, pool, "entropy") //11
      , ExpErrorReductionMargin(learner, pool, "balacc") //74
      , new SGmulti(learner, pool, "consensus") //14
      , new SGmulti(learner, pool, "majority") //15
      DensityWeightedTrainingUtility(learner, pool, "maha") //9
      , DensityWeightedLabelUtility(learner, pool, "maha") //39
      , DensityWeightedTrainingUtility(learner, pool, "maha", 0.5, 0.5) //50009
      , DensityWeightedLabelUtility(learner, pool, "maha", 0.5, 0.5) //89
  */