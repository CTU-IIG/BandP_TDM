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

public class Heuristic {
    
    //Checks whether a lower bound on allocation is less than 1. Otherwise the problem instance is trivially not satisfiable
    private boolean ProblemSolvability(ProblemInstance problemInstance)
    {
        double[] requiredNumbSlots = problemInstance.ComputingRequiredNumberOfSlotes();
        int sum = 0;
        for(int i = 0; i < problemInstance.getNumbClients(); i++)
            sum += requiredNumbSlots[i];

        if(sum > problemInstance.getNumbSlots())
            return false;
        else
            return true;
    }
    
    //given a problem instance for a single client and a schedule, the function returns
    //whether the service latency requirement is satisfied and in which time slots it is broken in case it is not satisfied
    //implements a sophisticated method from [22]
    public static boolean LatencyControlGeneralAdvancedMethod(ProblemInstanceSingleClient problemInstanceSingleDevice, double[] schedule, int[] problem){
	int subtStart = 0, subtEnd = 0, j;
        int[] start = new int [problemInstanceSingleDevice.getNumbSlots()];
        int[] finish = new int [problemInstanceSingleDevice.getNumbSlots()];
        int[] numbAllocated = new int [problemInstanceSingleDevice.getNumbSlots()];
        int[] numbNotAllocated = new int [problemInstanceSingleDevice.getNumbSlots()];
	boolean isLatencySatisfied = true;
    
	numbAllocated[0] = 0;
	numbNotAllocated[0] = 0;
	int NumberOfSubtables = 0;
    
	//Find start of the 1st subtable
	for(j = 0; j < problemInstanceSingleDevice.getNumbSlots(); j++){
            if((schedule[j] > Helpers.EPS) && (schedule[j + 1] < 1 - Helpers.EPS)){
                //we have end of the current subtable
                subtEnd = j + 1;
                finish[0] = j;
                start[1] = j + 1;

                numbAllocated[0]++;
                break;
            }
            else{
                //we have an allocated slot, but after goes another one
                if(schedule[j] > Helpers.EPS) {
                    numbAllocated[0]++;
                }
                else{
                    numbNotAllocated[0]++;
                }
            }
	}
    
	//Find parameters of the noncyclic subtables
        int subTableIndex = 0;
	while(true){
            subTableIndex++;
            numbNotAllocated[subTableIndex] = 0;
            numbAllocated[subTableIndex] = 0;
            for(j = subtEnd; j < problemInstanceSingleDevice.getNumbSlots(); j++){
                    if((schedule[j] > Helpers.EPS)&& (j != problemInstanceSingleDevice.getNumbSlots() - 1) && (schedule[j + 1] < 1 - Helpers.EPS)){
                        //end of the current subtable
                        subtStart = subtEnd;
                        subtEnd = j + 1;
                        NumberOfSubtables++;
                        finish[NumberOfSubtables] = j;
                        start[NumberOfSubtables + 1] = j + 1;
                        numbAllocated[subTableIndex]++;
                        break;
                    }
                    else{
                        //we have an allocated slot, but after goes another one
                        if(schedule[j] > Helpers.EPS) numbAllocated[subTableIndex]++;
                        else numbNotAllocated[subTableIndex]++;
                    }
            }

            if(j == problemInstanceSingleDevice.getNumbSlots()){
                //the fact, that j equals to problemInstanceSingleDevice.getNumberSlots() - 1 means, 
                //that we are done with the TDM and want to find parameters of the first subtable
                
                //if the last table does not end in the end slot, than it is just the first table, otherwise it was a new table
                if(!((schedule[problemInstanceSingleDevice.getNumbSlots() - 1] > Helpers.EPS) && (schedule[0] < 1 - Helpers.EPS))){
                    numbNotAllocated[0] += numbNotAllocated[NumberOfSubtables + 1];
                    start[0] = start[NumberOfSubtables + 1];
                    numbNotAllocated[NumberOfSubtables + 1] = 0;
                }
                else{
                    NumberOfSubtables++;
                    finish[NumberOfSubtables] = problemInstanceSingleDevice.getNumbSlots() - 1;
                }

                NumberOfSubtables++;
                break;
            }
	}
    
        //compute local latencies and local offsets
        double[] localLatency = new double [problemInstanceSingleDevice.getNumbSlots()];
	double[] localOffset = new double [problemInstanceSingleDevice.getNumbSlots()];
        for(int i = 0; i < NumberOfSubtables - 1; i++) {
            localOffset[i] = numbAllocated[i] + numbNotAllocated[i + 1] - numbAllocated[i] * (1.0 / problemInstanceSingleDevice.getBandwidthRequirement());
            localLatency[i] = numbNotAllocated[i];
        }
        
        localOffset[NumberOfSubtables - 1] = numbAllocated[NumberOfSubtables - 1] + numbNotAllocated[0] - numbAllocated[NumberOfSubtables - 1] * (1.0 / problemInstanceSingleDevice.getBandwidthRequirement());
	localLatency[NumberOfSubtables - 1] = numbNotAllocated[NumberOfSubtables - 1];
        
        //construct the table
        for(j = 0; j < NumberOfSubtables; j++){
            for(int k = 0; k < NumberOfSubtables; k++){
                float currentLatency = 0;
                currentLatency += localLatency[k];
                for(int t = k; t < k + j; t++)
                    currentLatency += localOffset[t % NumberOfSubtables];
                if(currentLatency > problemInstanceSingleDevice.getLatencyRequirement()){
                    problem[0] = start[k];
                    problem[1] = finish[(k + j) % NumberOfSubtables];
                    isLatencySatisfied = false;
                    j = NumberOfSubtables;
                    break;
                }
            }
	}
    
	return isLatencySatisfied;
    }
   
    //given a problem instance for a single client and a schedule, the function returns
    //whether the service latency requirement is satisfied and in which time slots it is broken in case it is not satisfied
    //implements a naive method, used in the ILP
    public static boolean LatencyControlGeneral(ProblemInstanceSingleClient problemInstanceSingleDevice, double[] schedule, int[] problem){
        int numbOfAlloctSlotsSoFar;
        for(int k = (int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement()) + 2; k < problemInstanceSingleDevice.getNumbSlots(); k++) {
            for(int j = 0; j < problemInstanceSingleDevice.getNumbSlots(); j++){
                numbOfAlloctSlotsSoFar = 0;
                for(int r = j; r < j + k; r++) {
                    if(schedule[r % problemInstanceSingleDevice.getNumbSlots()] > Helpers.EPS)
                        numbOfAlloctSlotsSoFar += 1;
                }
                if(numbOfAlloctSlotsSoFar < problemInstanceSingleDevice.getBandwidthRequirement() * (k - problemInstanceSingleDevice.getLatencyRequirement())){
                    problem[0] = j;
                    problem[1] = (j + k) % problemInstanceSingleDevice.getNumbSlots();
                    return false;
                }
            }
        }
        return true;
    }
    
    //given a schedule it checks whether the given client is satisfied with service latency requirements
    private int LatencyControlForLatencyDominatedClients(ProblemInstanceSingleClient problemInstanceSingleClient, boolean[] schedule){
        int max = 0;
        int count = 0;
        int count1 = 0;
        boolean t = false;
        for(int i = 0; i < problemInstanceSingleClient.getNumbSlots(); i++){
            if(schedule[i] == false){
                count++;
                if(count > max){
                    max = count;
                }
            }
            else{
                if(!t){
                    count1 = count;
                    t = true;
                }
                count = 0;
            }
        }

        if(count1 + count < max){
            max = count1 + count;
        }
        
        return max;
    }
   
    public void ChangeShadowPrices(ProblemSolutionSP problemSolutionSP, double[] shadowPrices){
        for(int i = 0; i < problemSolutionSP.getProblemInstanceSingleDevice().getNumbSlots(); i++){
            shadowPrices[i] -= problemSolutionSP.getX()[i];
        }
    }
    
    //naive heuristic to generate (not nesessary feasible) solution
    public SetOfColumns CreateInitialSolutionNaive(ProblemInstance problemInstance, 
            SubModel subModel, PartialSolution partialSolutions) throws IloException{
        double[] duals = new double[problemInstance.getNumbSlots()];
        Helpers.InitializeTo(duals, 0);
        
        subModel.ChangeCoefficientsInSubModel(duals, partialSolutions, 1, 0);
        ProblemSolutionSP problemSolutionSP = subModel.Solve();
        SetOfColumns setOfColumns = new SetOfColumns();
        setOfColumns.add(problemSolutionSP.ConvertResults());
        
        for(int i = 0; i < problemInstance.getNumbClients() - 1; i++){
            ChangeShadowPrices(problemSolutionSP, duals);
            subModel.ChangeCoefficientsInSubModel(duals, partialSolutions, 1, 0);
            problemSolutionSP = subModel.Solve();
            setOfColumns.add(problemSolutionSP.ConvertResults());
        }
        
        return setOfColumns;     
    }
    
    //manually fill the set of initial columns
    public SetOfColumns CreateInitialSolutionByHand(){
        SetOfColumns setOfColumns;
        
        int[][] columns = {{1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0}, {0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1}
               , {1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0}, {0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1}};
        int[][] b = {{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}};
        double[] c = {0.33, 0.4, 0.4, 0.36667};
       
        setOfColumns = new SetOfColumns(columns, c, b);
        
        return setOfColumns;
    }
    
    //return false if the result schedule has a collision, otherwise true
    private boolean AddStatisticsAndLastSolutions(boolean[] conflicts, int noClient, int[][] statistics,
         int[][] lastSolution, ProblemInstance problemInstance, Column column){
         // saving statistics
         int[] resultSchedule = new int [problemInstance.getNumbSlots()];
         for(int i = 0; i < problemInstance.getNumbSlots(); i++){
            resultSchedule[i] = 0;
            lastSolution[noClient][i] = column.column[i];
            if(column.column[i] == 1){
                    statistics[i][noClient]++;
            }
         }
         
         // checking whether the schedule is feasible
         boolean noConflict = true;
         for(int i = 0; i < problemInstance.getNumbSlots(); i++){
            conflicts[i] = false;
            for(int j = 0; j < problemInstance.getNumbClients(); j++){
                if(resultSchedule[i] == 1 && lastSolution[j][i] == 1){
                    noConflict = false;
                    conflicts[i] = true;
                    break;
                }
                if(lastSolution[j][i] == 1){
                    resultSchedule[i] = 1;
                }
            }
         } 
         
         return noConflict;
    }
    
    private double ComputeScalarMultiplication(int[] array1, double[] array2, int length){
        double result = 0;
        for(int i = 0; i < length; i++) {
            result += array1[i] * array2[i];
        }
        return result;
    }
    
    //output:
    //0 - not able to find any solution
    //1 - solution is optimal
    //2 - solution can be improved
    //It is a heuristic in the journal paper
    public int CreateInitialSolutionHeuristic(SetOfColumns setOfColumns, double alpha, 
            int max_nIterations, ProblemInstance problemInstance, SubModel[] subModel, PartialSolution partialSolutions) throws IloException, IOException{
        double[] weights = new double[problemInstance.getNumbSlots()];
        int[][] statistics = new int[problemInstance.getNumbSlots()][problemInstance.getNumbClients()];
        int[][] lastSolution = new int[problemInstance.getNumbClients()][problemInstance.getNumbSlots()];
        boolean[] conflicts = new boolean[problemInstance.getNumbSlots()];
        Helpers.InitializeTo(weights, 0);
        Helpers.InitializeTo(conflicts, false);
        
        for(int i = 0; i < problemInstance.getNumbSlots(); i++){
            for(int j = 0; j < problemInstance.getNumbClients(); j++){
                statistics[i][j] = 0;
                lastSolution[j][i] = 0;
            }
        } 
        
        int nIterations = 0, noClient = 0;
        boolean haveSolution = false;
        long compTime = 0;
        while(nIterations < max_nIterations){
            //compute a bound on the criterion with the previous column but the same reduced prices
            double bound = 1;
            if(nIterations > problemInstance.getNumbClients()) 
               bound = ComputeScalarMultiplication(lastSolution[noClient], weights, problemInstance.getNumbSlots());
            
            //Build and solve the sub-model with the bound on criterion and with integrality gap
            subModel[noClient].ChangeCoefficientsInSubModel(weights, partialSolutions, bound, BranchAndPrice.INTEGRALITY_GAP);
            ProblemSolutionSP problemSolutionSP = subModel[noClient].Solve();
            
            if(problemSolutionSP != null){
                if(AddStatisticsAndLastSolutions(conflicts, noClient, statistics, lastSolution, problemInstance, 
                    problemSolutionSP.ConvertResults()) && nIterations >= problemInstance.getNumbClients()){
                    haveSolution = true;
                    break;
                }
            }
            else{
                if(AddStatisticsAndLastSolutions(conflicts, noClient, statistics, lastSolution, problemInstance, 
                   new Column(lastSolution[noClient])) && nIterations >= problemInstance.getNumbClients()){
                    haveSolution = true;
                    break;
                }
            }
          
            noClient = (noClient + 1) % problemInstance.getNumbClients();
    
            //assign weights (reduced prices coefficients)
            for(int i = 0; i < problemInstance.getNumbSlots(); i++){
                //slot is unallocated with the current schedule -> weight is 1.0
                boolean slotIsUnalloc = true;
                for(int j = 0; j < problemInstance.getNumbClients(); j++){
                    if(lastSolution[j][i] == 1){
                        slotIsUnalloc = false;
                        break;
                    }
                }
                if(slotIsUnalloc == true){
                    weights[i] = -1.0;
                }
                else{
                    //in the current solution slot belongs to the client and there is no conflict -> 0.9
                    if(lastSolution[noClient][i] == 1 && conflicts[i] == false){
                        weights[i] = -0.9;
                        continue;
                    }

                    //in the current solution slot belongs to the client and there is a conflict -> rand(1,2)
                    if(lastSolution[noClient][i] == 1 && conflicts[i] == true){
                        weights[i] = -1 - Math.random()* 1.5;
                        continue;
                    }

                    //there is no collision + some other client has allocated this slot before
                    if(lastSolution[noClient][i] == 0){
                        int timesThisSlotWasAllocated = 0;
                        for(int j = 0; j < problemInstance.getNumbClients(); j++) {
                            if(j != noClient){
                                timesThisSlotWasAllocated += statistics[i][j];
                            }
                        }
                        weights[i] = -Math.min(2, 1 + timesThisSlotWasAllocated * alpha);
                        continue;
                    }
               }
            } 
            nIterations++;
        }
        
        if(haveSolution){
            int[] finalSchedule = new int[problemInstance.getNumbSlots()];
            for(int i = 0; i < problemInstance.getNumbSlots(); i++){
                for(int j = 0; j < problemInstance.getNumbClients(); j++){
                    if(lastSolution[j][i] == 1){
                        finalSchedule[i] = j + 1;
                    }
                }
            } 
            
            System.out.println("The found schedule is ");
            System.out.print("[");
            int numbOfNonAllocSlots = 0;
            for(int i = 0; i < problemInstance.getNumbSlots() - 1; i++){
                System.out.print(finalSchedule[i] + ", ");
                if(finalSchedule[i] == 0){
                    numbOfNonAllocSlots++;
                }
            }
            System.out.println(finalSchedule[problemInstance.getNumbSlots() - 1] + "]");
            System.out.println("number of allocated slots is " + (problemInstance.getNumbSlots() - numbOfNonAllocSlots));
            System.out.println("number of iterations is " + nIterations);
            
            for(int i = 0; i < problemInstance.getNumbClients(); i++) {
                int cost = 0;
                for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                    cost += lastSolution[i][j];
                }
                int[] client = new int[problemInstance.getNumbClients()];
                for (int j = 0; j < problemInstance.getNumbClients(); j++) {
                    client[j] = 0;
                }
                client[i] = 1;
                
                setOfColumns.add(new Column(lastSolution[i], cost * 1.0 / problemInstance.getNumbSlots(), client));
            }
            
            if(Helpers.IsOptimal((problemInstance.getNumbSlots() - numbOfNonAllocSlots) / problemInstance.getNumbSlots() * 1.0, problemInstance)){
                return 1;
            }
            else{
                return 2;
            }
        }
        else{
            System.out.println();
            System.out.println("FAIL!");
            for(int i = 0; i < problemInstance.getNumbClients(); i++) {
                int cost = 0;
                for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                    cost += lastSolution[i][j];
                }
                int[] client = new int[problemInstance.getNumbClients()];
                for (int j = 0; j < problemInstance.getNumbClients(); j++) {
                    client[j] = 0;
                }
                client[i] = 1;
                
                setOfColumns.add(new Column(lastSolution[i], cost * 1.0 / problemInstance.getNumbSlots(), client));
            }
            return 0;
        } 
    }
}
