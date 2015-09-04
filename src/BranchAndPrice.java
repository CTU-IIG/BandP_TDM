/*
	This file is part of the BandP_TDM program.
	BandP_TDM is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	BandP_TDM is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	You should have received a copy of the GNU General Public License
	along with BandP_TDM. If not, see <http://www.gnu.org/licenses/>.
 */

package bandp;

import ilog.concert.IloException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class BranchAndPrice {
    //--------parameters to set----------
    static final int TIME_LIMIT = 3000000; //time limit on the full branch-and-price in seconds
    static final int MAX_NUMB_CLIENTS = 128; //maximum possible number of clients
    static final int MIN_NUMB_INSTANCES = 2400; //solving starts from instance_MIN_NUMB_INSTANCES
    static final int MAX_NUMB_INSTANCES = 2599; //solving ends with instance_MAX_NUMB_INSTANCES
    static int branchingWay = 3; //3-consecutive, can be found in ProblemSolutionMM.java
    public static double INTEGRALITY_GAP = 0.05; //integrality gap for solving sub-model in heuristic
    static final double HEURISTIC_COEFFICIENT = 0.1; // alpha parameter i nthe heuristic
    static final int HEURISTIC_NUMB_ITERATIONS = 250; // Nmax - maximum number of iterations in a heuristic
    static final int HEURISTIC_NUMB_RUNS_IN_THE_BEGINNING = 1; // number of times the heuristic is launched in the beginning
    static final double PERCENT_FOR_RUNNING_HEURISTIC = 0.1; // percent of positively decided allocations in the branching tree in order to run a heuristic
    static final boolean IsSavedModel = false;
    static final boolean IsSavedTree = false;//save the branching tree
    static final boolean doLR = true; //lagrangian relaxation
    
    public static int MAX_NUMB_SLOTS;
    public static int MIN_NUMB_SLOTS;
    
    static double percentToSolveOptimalPositive;
    static double percentToSolveOptimalNegative;
    static double currentBest = 1.00001;
    static final int MAX_LEVELS = 1000;
    
    //-----------program variables--------
    public static ProblemInstance problemInstance;
    static ProblemSolutionMM finalProblemSolution;
    static SubModel[] subModel;
    static MasterModel masterModel;
    static SimpleModel simpleModel;
    static FileWriter ofst = null;
    static FileWriter ofstGraphWiz = null;
    static Heuristic heuristic;
    static int numbOfSlotsToRunHeuristic;
    public static double UpperBound = 1 + 1.0 / MIN_NUMB_SLOTS;
    static boolean isOptimal;
    
    //-----------variables for statistics--------
    static long startTime;
    static int numbNodes = -1;
    static long maxNumberOfColumns = 0;
    static long timeInMaster = 0;
    static long timesInMaster = 0;
    static int timesInSub = 0;
    static long maxDepth = 0;
    static long timeInSub = 0;
    static long timeInSimple = 0;
    static long timeInLagRel = 0;
    static int lastTimeInLag;
    static int nodeFound = 0;
    static long numbNoColumn = 0;
    static long numbNoColumnNotFeasible = 0;
    static int[] timeInLevels = new int[MAX_LEVELS];
    //static long[] timeInSubs = new long[MAX_SUBS];
    static int[] timesInLevels = new int[MAX_LEVELS];
    static int numbImprovements = 0;
    static int numbSimpleILP = 0;
    static boolean[] heuristicRunsInLevels;
    static double LB;

    private static void GatherStatistics(int[] depth) {
        //save maximal number of generated columns and print it
        if (masterModel.getColumns().columns.size() > maxNumberOfColumns) {
            maxNumberOfColumns = masterModel.getColumns().columns.size();
            System.out.println(maxNumberOfColumns);
        }
        
        //indicate the depth is increased by 1
        depth[0]++;
        timesInLevels[depth[0]]++;
        //save max depth if it is the case
        if (maxDepth < depth[0]) {
            maxDepth = depth[0];
        }
    }

    private static void PrintTree(PartialSolution partialSolutions, int idParent, int slotBranch, int numbColumn, int numbRemovedColumns, long startTime) throws IOException {
        ofstGraphWiz = new FileWriter(Helpers.outputFileForGraphWiz, true);

        long timeInNode = System.currentTimeMillis() - startTime;
        ofstGraphWiz.write("node" + numbNodes + "[label = \"");
        ofstGraphWiz.write("<f0> " + numbColumn + " | <f1> " + numbRemovedColumns + " |" + "<f2> " + timeInNode + " |");
        for (int i = 0; i < 10; i++) {
            boolean firstTime = true;
            boolean thereIsSomething = false;
            for (int j = 0; j < masterModel.getProblemInstance().getNumbClients(); j++) {
                if (partialSolutions.getPartialSolutions()[j][i] == 1) {
                    ofstGraphWiz.write("<f" + (i + 3) + "> " + j + " ");
                    thereIsSomething = true;
                    break;
                }
                if (partialSolutions.getPartialSolutions()[j][i] == 0 && !partialSolutions.getSlotAllocation()[i]) {
                    if (firstTime) {
                        ofstGraphWiz.write("<f" + i + "> ");
                        firstTime = false;
                        thereIsSomething = true;
                    }
                    ofstGraphWiz.write("!" + j + " ");
                }
            }
            if (thereIsSomething && i != masterModel.getProblemInstance().getNumbSlots() - 1) {
                ofstGraphWiz.write("| ");
            } else {
                ofstGraphWiz.write("<f" + i + " >");
                if (i != masterModel.getProblemInstance().getNumbSlots() - 1) {
                    ofstGraphWiz.write(" | ");
                }
            }
        }
        ofstGraphWiz.write(" \"]; \n");
        if (numbNodes != 0) {
            ofstGraphWiz.write("\"node" + idParent + "\":f" + slotBranch + " -> \"node" + numbNodes + "\":f2;\n");
        }
        ofstGraphWiz.close();
    }

    private static void RunSimpleILP(PartialSolution partialSolutions) throws IloException, IOException {
        simpleModel = new SimpleModel(problemInstance, partialSolutions);

        long start = System.currentTimeMillis();
        int[] resultSchedule = simpleModel.Solve();
        timeInSimple += System.currentTimeMillis() - start;

        int numAllocSlots = 0;
        if (resultSchedule != null) {
            for (int i = 0; i < problemInstance.getNumbSlots(); i++) {
                if (resultSchedule[i] != 0) {
                    numAllocSlots++;
                }
            }
            if (UpperBound > numAllocSlots * 1.0 / problemInstance.getNumbSlots()) {
                UpperBound = numAllocSlots * 1.0 / problemInstance.getNumbSlots();
                finalProblemSolution = new ProblemSolutionMM(resultSchedule, UpperBound);
            }
        }
        if (IsSavedTree) {
            ofstGraphWiz = new FileWriter(Helpers.outputFileForGraphWiz, true);
            ofstGraphWiz.write("\"node" + numbNodes + "\"-> " + "\"node" + (100000 + numbSimpleILP) + "\";\n");
            ofstGraphWiz.close();
        }
        numbSimpleILP++;
    }

    private static boolean RunHeuristic(PartialSolution partialSolutions) throws IloException, IOException {
        SetOfColumns setOfColumns = new SetOfColumns();
        int info = heuristic.CreateInitialSolutionHeuristic(setOfColumns, HEURISTIC_COEFFICIENT, HEURISTIC_NUMB_ITERATIONS, problemInstance, subModel, partialSolutions);

        if (info != 0) {
            for (int i = 0; i < setOfColumns.columns.size(); i++) {
                masterModel.AddColumnToSetOfColumnsAndModel(setOfColumns.columns.get(i), i);
            }
            if (UpperBound > setOfColumns.getCost()) {
                finalProblemSolution = new ProblemSolutionMM(setOfColumns);
                UpperBound = setOfColumns.getCost();
            }
        }
        if (info == 1) {
            return false;
        }
        return true;
    }

    private static int CreateNewColumns(MasterModel masterModel, PartialSolution partialSolutions, ProblemSolutionMM problemSolutionMM) throws IloException {
        int deviceWithMissingColumns = problemSolutionMM.getDeviceWithMissingColumns();
        if (deviceWithMissingColumns != -1) {
            //In case there is no solution to Master Model, which means there are no columns for some client
            double[] fictiveDuals = new double[problemInstance.getNumbSlots()];
            Helpers.InitializeTo(fictiveDuals, 0, 0, problemInstance.getNumbSlots());

            //If there is no column for some client, we generate some feasible (not optimal)
            numbNoColumn++;
            subModel[deviceWithMissingColumns].ChangeCoefficientsInSubModel(fictiveDuals, partialSolutions, 1, 0);
            
            ProblemSolutionSP problemSolutionSP = subModel[deviceWithMissingColumns].Solve();
            timesInSub++;
            timeInSub += (System.currentTimeMillis() - startTime);
            // timeInSubs[timesInSub - 1] = System.currentTimeMillis() - startTime;
            if(problemSolutionSP == null) {
                numbNoColumnNotFeasible++;
                return -1;
            }
            masterModel.AddColumnToSetOfColumnsAndModel(problemSolutionSP.ConvertResults(), deviceWithMissingColumns);

            return 1;
        } else {
            return 0;
        }
    }

    //returns 0 if it can be bounded, -1 if nothing happen and 1 if it is possible not to continue in CG
    private static int LagrangeRelaxation(ProblemSolutionMM problemSolutionMM,
            ProblemSolutionSP problemSolutionSP, int iteration,
            int device, PartialSolution partialSolutions, SubModel[] subModel) throws IOException, IloException{
        boolean isDeviceAllocatedLessLatency = partialSolutions.isDeviceAllocatedLessLatency();
        long startTime = System.currentTimeMillis();
        boolean basicCondition = problemSolutionMM.getObjValue() - problemSolutionSP.ComputeReducedPrice(problemSolutionMM.getDuals2()[device]) > UpperBound
                - 1.0 / masterModel.getProblemInstance().getNumbSlots() && iteration > 40 && numbNodes > 5 && iteration > lastTimeInLag + problemInstance.getNumbClients();
        boolean conditionForNonConsecutiveBranching = basicCondition && problemSolutionSP.ComputeReducedPrice(problemSolutionMM.getDuals2()[device]) < 0.5 && branchingWay != 3;
        boolean conditionForConsecutiveBranching = basicCondition && branchingWay == 3 && (isDeviceAllocatedLessLatency && problemInstance.ToSingleDevice(0).isIsLatDom() || problemSolutionSP.ComputeReducedPrice(problemSolutionMM.getDuals2()[device]) < 0.5);

        if (conditionForNonConsecutiveBranching || conditionForConsecutiveBranching) {
            lastTimeInLag = iteration;
            double sum = problemSolutionSP.ComputeReducedPrice(problemSolutionMM.getDuals2()[device]);
            double tmp = problemSolutionMM.getObjValue() - problemSolutionSP.ComputeReducedPrice(problemSolutionMM.getDuals2()[device]);
            for (int i = 0; i < problemInstance.getNumbClients(); i++) {
                if (i != device) {
                    subModel[i].ChangeCoefficientsInSubModel(problemSolutionMM.getDuals1(), partialSolutions, 1, 0);
                    problemSolutionSP = subModel[i].Solve();
                    sum += problemSolutionSP.ComputeReducedPrice(problemSolutionMM.getDuals2()[i]);
                    if (problemSolutionMM.getObjValue() - sum
                            < (UpperBound - 1.0 / masterModel.getProblemInstance().getNumbSlots())) {
                        break;
                    }
                }
            }
            double lowerBoundTemp = problemSolutionMM.getObjValue() - sum;

            double roundedMM = Math.ceil((lowerBoundTemp - Helpers.EPS) * masterModel.getProblemInstance().getNumbSlots()) / masterModel.getProblemInstance().getNumbSlots();
            if(roundedMM - (UpperBound - 1.0 / masterModel.getProblemInstance().getNumbSlots()) > Helpers.EPS) {
                numbImprovements++;
                timeInLagRel += (System.currentTimeMillis() - startTime);
                return 0;
            }

            roundedMM = Math.ceil(problemSolutionMM.getObjValue() * problemInstance.getNumbSlots());
            double roundedLagr = Math.ceil(lowerBoundTemp * problemInstance.getNumbSlots());
            if(Math.abs(roundedMM - roundedLagr) < Helpers.EPS){
                numbImprovements++;
                timeInLagRel += (System.currentTimeMillis() - startTime);
                return 1;
            }
        }
        
        timeInLagRel += (System.currentTimeMillis() - startTime);
        return -1;
    }
    
    private static ProblemSolutionMM ColumnGeneration(MasterModel masterModel, PartialSolution partialSolutions) throws IloException, IOException {
        long startTime;
        ProblemSolutionMM problemSolutionMM;
        ProblemSolutionSP problemSolutionSP;
        int device = -1;
        int startDevice;
        int iteration = 0;
        lastTimeInLag = 0;

        while(true) {
            // Create columns for each device consequently
            device = (device + 1) % problemInstance.getNumbClients();

            while (true) {
                timesInMaster++;
                startTime = System.currentTimeMillis();
                //masterModel.ModelToFile("out.lp");
                problemSolutionMM = masterModel.Solve(partialSolutions);
                timeInMaster += (System.currentTimeMillis() - startTime);
                iteration++;

                int result = CreateNewColumns(masterModel, partialSolutions, problemSolutionMM);
                if(result == -1) return null;
                if(result == 0)  break;
            }
            
            startDevice = device;
            while(true) {
                subModel[device].ChangeCoefficientsInSubModel(problemSolutionMM.getDuals1(), partialSolutions, 1, 0);
                startTime = System.currentTimeMillis();
                problemSolutionSP = subModel[device].Solve();
                
                timeInSub += (System.currentTimeMillis() - startTime);
                //timeInSubs[timesInSub] = System.currentTimeMillis() - startTime;
                timesInSub++;

                if(problemSolutionSP == null) {
                    return null;
                }
                
                if(problemSolutionSP.IsReducedCostPositive(problemSolutionMM.getDuals2()[device], Helpers.EPS)) {
                    masterModel.AddColumnToSetOfColumnsAndModel(problemSolutionSP.ConvertResults(), device);
                    
                    //try to solve it not to optimality, did not work
                    /*for(int i = 0; i < NUMB_SOLUTIONS_TO_SUB_MODEL; i++) {
                        if(problemSolutionSPMany[i] != null && (Math.abs(problemSolutionSPMany[i].getObjValue() - problemSolutionSPMany[0].getObjValue()) < 0.15)){
                            if(!Heuristic.LatencyControlWithoutDominating(problemInstance.ToSingleDevice(device), problemSolutionSPMany[i].getX(), timeInLevels, true))
                                startTime = 0;
                            masterModel.AddColumnToSetOfColumnsAndModel(problemSolutionSPMany[0].ConvertResults(), device);
                        }
                        else{
                            break;
                        }
                    }*/
                    break;
                } 
                else{
                    //at this point the solution to the sub-model must be optimal
                    /*subModel[device].ChangeCoefficientsInSubModel(problemSolutionMM.getDuals1(), partialSolutions, 1, 0);
                    problemSolutionSP = subModel[device].Solve();
                    if(problemSolutionSP.IsReducedCostPositive(problemSolutionMM.getDuals2()[device], Helpers.EPS)){
                        masterModel.AddColumnToSetOfColumnsAndModel(problemSolutionSP.ConvertResults(), device);
                        break;
                    }
                    else{*/
                        device = (device + 1) % problemInstance.getNumbClients();
                        if(device == startDevice) {
                            if(numbNodes == 0) LB = problemSolutionMM.getObjValue();
                            return problemSolutionMM;
                        }
                    //}
                }
            }
            
            //Lagrangian relaxation
            if(doLR){
                int result = LagrangeRelaxation(problemSolutionMM, problemSolutionSP, iteration, device, partialSolutions, subModel);
                if(result == 0) return null;
                if(result == 1) return problemSolutionMM;
            }
        }
    }

    //the core for recursion
    private static void BranchAndPrice(MasterModel masterModel, PartialSolution partialSolutions, int[] depth, int idParent, int slotBranch, int numbRemovedColumns) throws IloException, IOException {
        //increment number of nodes
        numbNodes++;
        int myId = numbNodes;
        
        long curCompTime = System.currentTimeMillis() - startTime;
        if(curCompTime > TIME_LIMIT){
            isOptimal = false;
            return;
        }
        //compute time in the level
        long start = System.currentTimeMillis();

        //have optimal solution
        if(Helpers.IsOptimal(UpperBound, problemInstance) || (branchingWay == 3 && partialSolutions.hasViolatedLatency())) {
            return;
        }
        
        //Tree saving
        if(IsSavedTree) {
            PrintTree(partialSolutions, idParent, slotBranch, masterModel.getColumns().columns.size(), numbRemovedColumns, start);
        }

        //Running simple ILP if it is deep enough in the tree
        if(partialSolutions.canRunOptimally(percentToSolveOptimalPositive, percentToSolveOptimalNegative)) {
            RunSimpleILP(partialSolutions);
            return;
        }

        ProblemSolutionMM problemSolutionMM = null;
        //finally, perform column generation
        if(numbRemovedColumns != 0){
            problemSolutionMM = ColumnGeneration(masterModel, partialSolutions);
            
            partialSolutions.setProblemSolutionMM(problemSolutionMM);
            //GatherStatistics(depth);

            //the problem is unsolvable -> cut branch
            if (problemSolutionMM == null) {
                return;
            }

            //try heuristic
            if(numbNodes != 0 && partialSolutions.canRunHeuristic(numbOfSlotsToRunHeuristic, heuristicRunsInLevels)) {
                boolean shouldContinue = RunHeuristic(partialSolutions);
                if (shouldContinue == false) {
                    return;
                }
            }
            //follow time in the level of the tree
            //timeInLevels[depth[0]] += System.currentTimeMillis() - start;
        }

        //check whether the found solution is integral
        if(numbRemovedColumns != 0 && problemSolutionMM.IsSolutionIntegral(Helpers.EPS)){
            // in case it is integral and the criterion value is better or equal than the upper bound,
            // save the solution -> the branch is closed
            if(problemSolutionMM.getObjValue() <= UpperBound){
                UpperBound = problemSolutionMM.getObjValue();
                finalProblemSolution = problemSolutionMM;
                nodeFound = (int) numbNodes;
            }
        } else{
            // In case found criterion value even for non-integral solution is more than the upper bound -> close the branch
            if(numbRemovedColumns != 0 && Math.ceil((problemSolutionMM.getObjValue() - Helpers.EPS) * problemInstance.getNumbSlots()) / 
                problemInstance.getNumbSlots() - (UpperBound - 1.0 / problemInstance.getNumbSlots()) > Helpers.EPS) {
                return;
            } else{
                //solve master model as ILP in order to have a better upper bound - does not help!
                /*ProblemSolutionMM problemSolutionMMILP = masterModel.SolveAsILP();
                if(problemSolutionMMILP != null){
                    if(problemSolutionMMILP.getObjValue() < UpperBound){
                        UpperBound = problemSolutionMMILP.getObjValue();
                        finalProblemSolution = problemSolutionMMILP;
                    }
                }*/
                
                //find out which slot to allocate to which device
                int[] branchingDecision;
                if(numbRemovedColumns != 0){
                    branchingDecision = problemSolutionMM.ReturnDeviceToBranch(partialSolutions, branchingWay);
                }
                else{
                    branchingDecision = partialSolutions.getProblemSolutionMM().ReturnDeviceToBranch(partialSolutions, branchingWay);
                }
                int deviceToBranch = branchingDecision[0];
                int slotToAllocate = branchingDecision[1];

                //---------------------case where the slot must be allocated to the device----------------------------------
                int numbColumns = 0;
                int firstValue = 1;
                if(branchingWay == 3){
                    firstValue = 0;
                }
                PartialSolution partialSolutionsCopy = new PartialSolution(problemInstance, partialSolutions);
                partialSolutionsCopy.setPartialSolution(slotToAllocate, deviceToBranch, firstValue);

                MasterModel reference;
                MasterModel copyMM = null;

                // Either create a copy or add and delete branching constraints 
                // each time. The variable IsSavedModel is responsible for it
                int numbRemovedColumnsMy;
                if (!IsSavedModel) {
                    //allocate slotToAllocate to deviceToBranch (because the third parameter is 1)
                    numbRemovedColumnsMy = masterModel.AddRowBranchingConstraint(deviceToBranch, slotToAllocate, firstValue);
                    numbColumns = masterModel.getColumns().columns.size();
                    reference = masterModel;
                } else {
                    copyMM = new MasterModel(masterModel);
                    numbRemovedColumnsMy = copyMM.AddRowBranchingConstraint(deviceToBranch, slotToAllocate, firstValue);
                    reference = copyMM;
                }

                int depthBefore = depth[0];
                BranchAndPrice(reference, partialSolutionsCopy, depth, myId, slotToAllocate, numbRemovedColumnsMy);
                depth[0] = depthBefore;

                //Cleaning to return to the initial master model
                if (!IsSavedModel) {
                    numbColumns -= masterModel.getColumns().columns.size();
                    masterModel.DeleteLastColumns(-numbColumns);
                    masterModel.DeleteRowBranchingConstraint();
                } else {
                    copyMM.end();
                }

                //------------------case where the slot can not belong to the device---------------------------
                int secondValue = 0;
                if(branchingWay == 3){
                    secondValue = 1;
                }
                PartialSolution partialSolutionsCopy2 = new PartialSolution(problemInstance, partialSolutions);
                partialSolutionsCopy2.setPartialSolution(slotToAllocate, deviceToBranch, secondValue);
                copyMM = null;

                if (!IsSavedModel) {
                    numbRemovedColumnsMy = masterModel.AddRowBranchingConstraint(deviceToBranch, slotToAllocate, secondValue);
                    numbColumns = masterModel.getColumns().columns.size();
                    reference = masterModel;
                } else {
                    copyMM.CreateMasterModel(masterModel);
                    numbRemovedColumnsMy = copyMM.AddRowBranchingConstraint(deviceToBranch, slotToAllocate, secondValue);
                    reference = copyMM;
                }

                depthBefore = depth[0];
                //System.out.println("------------------------" + depth[0] + "-------------------------");
                //masterModel.readBasis("C:\\Users\\minaeann\\BAndPv1.1\\helpFiles\\" + String.valueOf(myId) + ".bas");
                BranchAndPrice(reference, partialSolutionsCopy2, depth, myId, slotToAllocate, numbRemovedColumnsMy);
                depth[0] = depthBefore;

                //Cleaning to return to the initial master model
                if (!IsSavedModel) {
                    numbColumns -= masterModel.getColumns().columns.size();
                    masterModel.DeleteLastColumns(-numbColumns);
                    masterModel.DeleteRowBranchingConstraint();
                } else {
                    copyMM.endEnd();
                }

                //cleaning value
                depth[0]--;
            }
        }
    }

    private static void InitializeEverything(){
        finalProblemSolution = null;
        heuristicRunsInLevels = new boolean[MAX_LEVELS];
        numbNodes = -1;
        maxNumberOfColumns = 0;
        timeInMaster = 0;
        timesInMaster = 0;
        timesInSub = 0;
        maxDepth = 0;
        timeInSub = 0;
        timeInSimple = 0;
        timeInLagRel = 0;
        nodeFound = 0;
        numbNoColumn = 0;
        numbNoColumnNotFeasible = 0;
        numbImprovements = 0;
        numbSimpleILP = 0;
        isOptimal = true;
        Helpers.InitializeTo(timeInLevels, 0);
        Helpers.InitializeTo(timesInLevels, 0);
        Helpers.InitializeTo(heuristicRunsInLevels, false);
    }
    
    private static void mainProcedure() throws IloException, IOException {
        InitializeEverything();
        SetOfColumns setOfColumnsFinal = null;
        PartialSolution partialSolution = new PartialSolution(problemInstance);
        
        //Generation of the initial solutions setOfColumns with help of heuristic
        heuristic = new Heuristic();
        int info;
        double heurSolution = 10000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < HEURISTIC_NUMB_RUNS_IN_THE_BEGINNING; i++) {
            SetOfColumns setOfColumns = new SetOfColumns();
            info = heuristic.CreateInitialSolutionHeuristic(setOfColumns,
                    HEURISTIC_COEFFICIENT, HEURISTIC_NUMB_ITERATIONS, problemInstance, subModel, partialSolution);        
            if (info != 0) {
                if (heurSolution > setOfColumns.getCost()) {
                    heurSolution = setOfColumns.getCost();
                    setOfColumnsFinal = new SetOfColumns(setOfColumns);
                     if (UpperBound > setOfColumns.getCost()){
                        finalProblemSolution = new ProblemSolutionMM(setOfColumns);
                        UpperBound = setOfColumns.getCost();
                     }
                     
                    if (info == 1) {
                        ofst = new FileWriter(Helpers.outputFileMain,true);
                        long compTime = -startTime + System.currentTimeMillis();
                        ofst.write("999999 " + compTime + " " + heurSolution + " ");
                        ofst.close();
                        return;
                    }
                }
            }
            else{
                if(i == (HEURISTIC_NUMB_RUNS_IN_THE_BEGINNING - 1) && setOfColumnsFinal == null){
                    setOfColumnsFinal = new SetOfColumns(setOfColumns);
                }
            }
        }
        ofst = new FileWriter(Helpers.outputFileMain,true);
        long compTime = -startTime + System.currentTimeMillis();
        ofst.write("999999 " + compTime + " " + heurSolution + " ");
        ofst.close();

        //to follow maximal number of levels in the search tree
        int[] depth = new int[1];
        depth[0] = 0;

        masterModel.CreateMasterModel(problemInstance, setOfColumnsFinal);
        //masterModel.ModelToFile(outputFile);
        numbNodes = -1;
        BranchAndPrice(masterModel, partialSolution, depth, numbNodes, 0, -100);

        masterModel.end();
    }

    public static void main(String[] args) throws IloException, UnsupportedEncodingException, IOException, InputDataReader.InputDataReaderException {
        String instance = "instances/instance", dat = ".dat";
        masterModel = new MasterModel();
        //Helpers.InitializeTo(timeInSubs, MAX_SUBS);
        
        if(IsSavedTree){
            ofstGraphWiz = new FileWriter(Helpers.outputFileForGraphWiz);
            ofstGraphWiz.write("digraph g {\n" + "node [shape = record,height=.1]; \n");
            ofstGraphWiz.close();
        }
        
        System.out.println("Copyright 2014-2015 Anna Minaeva, Premysl Sucha and Benny Akesson.");
        System.out.println("The program is distributed under the terms of the GNU General Public License.");
        
        int numClientsBefore = 0;
        for (int t = MIN_NUMB_INSTANCES; t <= MAX_NUMB_INSTANCES; t++) {
            //System.out.println(t + "-th instance!");
            problemInstance = new ProblemInstance(instance + String.valueOf(t + 1) + dat, 2);
            MIN_NUMB_SLOTS = problemInstance.getNumbClients() * 8;
            MAX_NUMB_SLOTS = problemInstance.getNumbClients() * 8;
           // MIN_NUMB_SLOTS = 30;
            //MAX_NUMB_SLOTS = 30;

            if(numClientsBefore != problemInstance.getNumbClients()){
                numClientsBefore = problemInstance.getNumbClients();
                switch(problemInstance.getNumbClients()){
                    case 8:
                        percentToSolveOptimalPositive = 0.1;
                        percentToSolveOptimalNegative = 0.4;
                        break;
                    case 16:
                        percentToSolveOptimalPositive = 0.3;
                        percentToSolveOptimalNegative = 1.0;
                        break;
                    case 32:
                        percentToSolveOptimalPositive = 0.6;
                        percentToSolveOptimalNegative = 1.2;
                        break;
                    case 64:
                        percentToSolveOptimalPositive = 0.8;
                        percentToSolveOptimalNegative = 2.6;
                        break;
                    case 128:
                        percentToSolveOptimalPositive = 0.95;
                        percentToSolveOptimalNegative = 3.0;
                        break;
                }
            }

            currentBest = 10.001;
            UpperBound = 1 + 1.0 / MIN_NUMB_SLOTS;
            for(int noSlots = MIN_NUMB_SLOTS; noSlots <= MAX_NUMB_SLOTS; noSlots++){
                //System.out.println("start with frame size " + noSlots);
                numbOfSlotsToRunHeuristic = (int) Math.round(noSlots * PERCENT_FOR_RUNNING_HEURISTIC);
                problemInstance.setFrameSize(noSlots);

                //end old sub-models for the previous problem instance
                if(subModel != null){
                    for(int i = 0; i < subModel.length; i++) {
                        subModel[i].end();
                    }
                }
                
                //create one sub-model for each client
                subModel = new SubModel[problemInstance.getNumbClients()];
                for (int i = 0; i < problemInstance.getNumbClients(); i++) {
                    subModel[i] = new SubModel(problemInstance.ToSingleDevice(i));
                }
                
                //compute lower bound on the criterion in order to exculde trivially unsatisfiable problem instances for some frame sizes
                double minNumberOfSlotsToBeAllocated = 0.0;
                for (int i = 0; i < problemInstance.getNumbClients(); i++) {
                    minNumberOfSlotsToBeAllocated += problemInstance.ToSingleDevice(i).getNumberOfRequiredSlots();
                }

                double lowerBoundOnCriterion = minNumberOfSlotsToBeAllocated / noSlots;
                double upperBoundOnCriterionDiscretized = Math.floor(noSlots * currentBest) / (noSlots * 1.0);
                double upperBoundOnCriterion = currentBest;
                if (lowerBoundOnCriterion <= upperBoundOnCriterionDiscretized && lowerBoundOnCriterion < (1 + Helpers.EPS) && lowerBoundOnCriterion < upperBoundOnCriterion) 
                {
                    problemInstance.ComputeNumberOfDisjunctiveCollisions();
                    startTime = System.currentTimeMillis();
                    mainProcedure();
                    long allTime = -startTime + System.currentTimeMillis();
                    
                    ofst = new FileWriter(Helpers.outputFileMain,true);
                    if(finalProblemSolution != null){
                        if (currentBest > finalProblemSolution.getObjValue()) {
                            currentBest = finalProblemSolution.getObjValue();
                            UpperBound = currentBest;
                        }
                        
                        ofst.write(noSlots + " " + allTime + " " +  finalProblemSolution.getObjValue() + " " + timeInSimple + " " + timeInLagRel + " " + numbNodes + " ");
                        if(!isOptimal){
                            ofst.write(LB + " " + finalProblemSolution.getObjValue());
                        }
                        else{
                           ofst.write("0 0"); 
                        }
                    } else {
                        ofst.write(noSlots + " " + allTime + " 2 " + timeInSimple + " " + timeInLagRel + " " + numbNodes + " "); 
                        if(!isOptimal){
                            ofst.write("2 2");
                        }
                        else{
                            ofst.write("0 0"); 
                        }
                    }
                } else {
                     ofst = new FileWriter(Helpers.outputFileMain,true);
                     ofst.write(noSlots + " 0 2 0 0 0 2 2");
                }
                ofst.write(System.lineSeparator());
                //ofst.write("\n");
                UpperBound = (noSlots * UpperBound + 1) / (noSlots + 1);
                ofst.close();
            }

            /*System.out.println("--------------------Optimal solution-------------------");
            
             if(finalProblemSolution != null){
                 System.out.println();
                 for (int i = 0; i < 64; i++) {
                    // System.out.print(finalProblemSolution.schedule[i] + ", ");
                 }
                 System.out.println();
             }*/
            /* System.out.println("Number of nodes is " + numbNodes);
             System.out.println("Maximal depth is " + maxDepth);
             System.out.println("Maximum number of columns is " + maxNumberOfColumns);
             System.out.println("Time in master is " + timeInMaster);
             System.out.println("Time in sub is " + timeInSub);
             //System.out.println("Total spent time is " + allTime);
             finalProblemSolution.PrintResults();

             System.out.println("No columns were " + numbNoColumn + " times");
             System.out.println("No feasible columns were " + numbNoColumnNotFeasible + " times");

             System.out.println("We were " + timesInMaster + " times in master model");
             System.out.println("We were " + timesInSub + " times in sub model");

             for(int i = 0; i < 50; i++) {
             System.out.println("We spent " + timeInLevels[i] + " msec in " + i +"th level");
             System.out.println("We were " + timesInLevels[i] + " times in " + i +"th level");
             System.out.println();
             if(timeInLevels[i] < Helpers.EPS) break;
             }

             for(int i = 0; i < timesInSub; i++) {
             System.out.println("We spent " + timeInSubs[i] + " msec in " + i +"th submodel run");
             }

             System.out.println();
             System.out.println();
             System.out.println(nodeFound);*/

            /*ofst = new FileWriter("output.txt",true);
             ofst.write("1111111\n");
             ofst.close();*/
            //}
        }
        masterModel.endEnd();
        for (int i = 0; i < numClientsBefore; i++) {
            subModel[i].end();
        }
        //ofstGraphWiz.write("}");
        //ofstGraphWiz.close();
    }
}
