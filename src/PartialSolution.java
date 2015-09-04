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

import static bandp.BranchAndPrice.MAX_NUMB_CLIENTS;
import static bandp.BranchAndPrice.problemInstance;

public class PartialSolution {
    private int[][] partialSolutions; 
    private boolean[] slotAllocation;
    private ProblemInstance problemInstance;
    private int numberOfAllocated;
    private int numberOfNonAllocated;
    ProblemSolutionMM problemSolutionMM;

    public PartialSolution(ProblemInstance problemInstance) {
        this.problemInstance = problemInstance;
        partialSolutions = new int [problemInstance.getNumbClients()][problemInstance.getNumbSlots()];
        slotAllocation = new boolean [problemInstance.getNumbSlots()];
        
        for(int j = 0; j < problemInstance.getNumbSlots(); j++){
            slotAllocation[j] = false;

            for(int i = 0; i < problemInstance.getNumbClients(); i++){
                partialSolutions[i][j] = -1;
             }
        }
        
        //Find a client with the least amount of required slots
        int clientMin = -1;
        int numbSlots = 100;
        for(int i = 0; i < problemInstance.getNumbClients(); i++){
            if(numbSlots > problemInstance.ToSingleDevice(i).getRequiredNumberOfSlots()){
                clientMin = i;
                numbSlots = problemInstance.ToSingleDevice(i).getRequiredNumberOfSlots();
            }
        }
        
        for(int i = 0; i < problemInstance.getNumbClients(); i++){
            partialSolutions[i][0] = 0;
        }
        partialSolutions[clientMin][0] = 1;
        slotAllocation[0] = true;
        numberOfAllocated = 1;
        numberOfNonAllocated = 0;
    }
    
    public PartialSolution(ProblemInstance problemInstance, PartialSolution partialSolution){
        this.problemInstance = problemInstance;
        this.partialSolutions = new int [problemInstance.getNumbClients()][problemInstance.getNumbSlots()];
        this.slotAllocation = new boolean [problemInstance.getNumbSlots()];
        this.numberOfNonAllocated = partialSolution.numberOfNonAllocated;
        this.numberOfAllocated = partialSolution.numberOfAllocated;
        this.problemSolutionMM = partialSolution.getProblemSolutionMM();
        
        for(int j = 0; j < problemInstance.getNumbSlots(); j++){
                this.slotAllocation[j] = partialSolution.slotAllocation[j];

                for(int i = 0; i < problemInstance.getNumbClients(); i++){
                    this.partialSolutions[i][j] = partialSolution.partialSolutions[i][j];
                 }
        }
    }
    
    public void setProblemSolutionMM(ProblemSolutionMM problemSolutionMM){
        this.problemSolutionMM = problemSolutionMM;
    }

    public ProblemSolutionMM getProblemSolutionMM() {
        return problemSolutionMM;
    }
    
    public void setPartialSolution(int slot, int device, int value){
        if(value == 1){
           for(int i = 0; i < problemInstance.getNumbClients(); i++){
               partialSolutions[i][slot] = 0;
           }
           partialSolutions[device][slot] = 1;
           slotAllocation[slot] = true;
           numberOfAllocated++;
        }
        else{
            partialSolutions[device][slot] = 0;
            numberOfNonAllocated++;
        }
    }

    public int[][] getPartialSolutions() {
        return partialSolutions;
    }

    public boolean[] getSlotAllocation() {
        return slotAllocation;
    }
    
    public boolean canRunHeuristic(int k, boolean[] heuristicRunsInLevels){
        if(numberOfAllocated % k == 0 && !heuristicRunsInLevels[numberOfAllocated / k]){
            heuristicRunsInLevels[numberOfAllocated / k] = true;
            return true;
        }
        return false;
    }
    
    public boolean canRunOptimally(double percentPositive, double percentNegative){
        if(numberOfAllocated >= percentPositive * problemInstance.getNumbSlots() || numberOfNonAllocated >= percentNegative * problemInstance.getNumbSlots()){
            return true;
        }
        return false;
    }
    
    public int[] getLastAllocated(){
        int lastFixedClient = -1;
        int lastFixedSlot = -1;
        for (int i = 0; i < problemInstance.getNumbClients(); i++) {
            for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                if(partialSolutions[i][j] == 0 || partialSolutions[i][j] == 1){
                    lastFixedClient = i;
                    lastFixedSlot = j;
                }
                else{
                    break;
                }
            }
        }
        
        int[] output = {lastFixedClient, lastFixedSlot, 1};
        return output;
    }
    
    //use only with consecutive allocation strategy
    public boolean hasViolatedLatency(){
        int[] output = getLastAllocatedOne();
        int lastFixedClient = output[0];
        int lastFixedSlotOne = output[1];
        
        //compute how many empty slots there are between this and previous fixed slot for the client
        boolean isAllocMore = true;
        int indexEnd = (int) Math.floor(problemInstance.ToSingleDevice(lastFixedClient).getLatencyRequirement());
        int indexResidual = 0;
        if(lastFixedSlotOne + Math.floor(problemInstance.ToSingleDevice(lastFixedClient).getLatencyRequirement()) > problemInstance.getNumbSlots() - 1){
            indexEnd = problemInstance.getNumbSlots() - 1;
            indexResidual = lastFixedSlotOne + (int) Math.floor(problemInstance.ToSingleDevice(lastFixedClient).getLatencyRequirement()) - problemInstance.getNumbSlots() + 1;
        }
        for (int j = 1; j <= indexEnd; j++) {
            if(partialSolutions[lastFixedClient][lastFixedSlotOne + j] == -1 || partialSolutions[lastFixedClient][lastFixedSlotOne + j] == 1 ){
                isAllocMore = false;
            }
        }
        for (int j = 0; j <= indexResidual; j++) {
            if(partialSolutions[lastFixedClient][j] == -1 || partialSolutions[lastFixedClient][j] == 1){
                isAllocMore = false;
            }
        }
        
        return isAllocMore;
    }
    
    public int[] getLastAllocatedOne(){
        int lastFixedClient = -1;
        int lastFixedSlot = -1;
        for (int i = 0; i < problemInstance.getNumbClients(); i++) {
            for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                if(partialSolutions[i][j] == 1){
                    lastFixedClient = i;
                    lastFixedSlot = j;
                }
                if(partialSolutions[i][j] == -1){
                    break;
                }
            }
        }
        
        int[] output = {lastFixedClient, lastFixedSlot, 1};
        return output;
    }
    
    public boolean isDeviceAllocatedLessLatency(){
        int[] output = getLastAllocated();
        int lastFixedClient = output[0];
        int lastFixedSlot = output[1];
        
        if(partialSolutions[lastFixedClient][lastFixedSlot] == 1){
            //compute how many empty slots there are between this and previous fixed slot for the client
            int numbEmptySlots = 0;
            for (int j = lastFixedSlot - 1; j > 0; j--) {
                if(partialSolutions[lastFixedClient][j] == 0){
                    numbEmptySlots++;
                }
            }
            if(numbEmptySlots < (problemInstance.ToSingleDevice(lastFixedClient).getLatencyRequirement() - 1)){
                return true;
            }
        }
            return false;
    }
}
