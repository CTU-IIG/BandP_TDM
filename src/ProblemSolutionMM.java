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

import static bandp.BranchAndPrice.problemInstance;
import java.io.FileWriter;
import java.io.IOException;

public class ProblemSolutionMM {
    private double[] w;
    private double[] y;
    private double[] duals1;
    private double[] duals2;
    private double objFunct;
    private int numbOfAllocSlots;
    private SetOfColumns setOfColumns;
    private int problem;
    int[] schedule;

    public double[] getSolution() {
        return w;
    }

    public SetOfColumns getSetOfColumns() {
        return setOfColumns;
    }
    
    public int getDeviceWithMissingColumns() {
        return problem;
    }

    public ProblemSolutionMM(double[] w, double[] y, double[] duals1, double[] duals2, double objFunction, SetOfColumns setOfColumns) {
        this.w = w;
        this.y = y;
        this.duals1 = duals1;
        this.duals2 = duals2;
        this.objFunct = objFunction;
        this.setOfColumns = setOfColumns;
        this.problem = -1;
    }
    
    public ProblemSolutionMM(double[] w, double[] y, double objFunction, SetOfColumns setOfColumns) {
        this.w = w;
        this.y = y;
        this.objFunct = objFunction;
        this.setOfColumns = setOfColumns;
        this.problem = -1;
    }
    
    public ProblemSolutionMM(int problem) {
        this.problem = problem;
    }
     
    public ProblemSolutionMM(SetOfColumns setOfColumns) {
        this.setOfColumns = new SetOfColumns(setOfColumns);
        this.objFunct = setOfColumns.getCost();
    }
    
    public ProblemSolutionMM(int[] schedule, double objValue) {
        this.schedule = schedule;
        this.objFunct = objValue;
    }

    public double getObjValue() {
        return objFunct;
    }

    public int getNumbOfAllocSlots() {
        return numbOfAllocSlots;
    }
    
    public double[] getDuals1() {
        return duals1;
    }
    
     public double[] getDuals2() {
        return duals2;
    }
    
    public void PrintResults(){
        System.out.println();
        if(w != null){
            System.out.println("An allocated rate is " + objFunct);

            System.out.println();
            for(int i = 0; i < w.length; i++) {
                System.out.println();
                System.out.print("  Assignment[" + i + "] = " + w[i]);
            }
            System.out.println();

            System.out.println();
            for(int i = 0; i < y.length; i++) {
               System.out.println("  y[" + i + "] = " + y[i]);
            }
            System.out.println();

            /*for (int i = 0; i < duals1.length; i++) 
               System.out.println("  Duals1[" + i + "] = " + duals1[i]);
            System.out.println();

            for (int i = 0; i < duals2.length; i++) 
               System.out.println("  Duals2[" + i + "] = " + duals2[i]);*/
        }
        else{
            if(setOfColumns != null){
                System.out.println("The solution was found by the heuristic!");
                System.out.println("An allocated rate is " + setOfColumns.getCost());

                System.out.println("The found schedule is ");

                for(int i = 0; i < problemInstance.getNumbSlots(); i++){
                    boolean isSlotEmpty = true;
                    for (int j = 0; j < problemInstance.getNumbClients(); j++) {
                        if(setOfColumns.columns.get(j).column[i] == 1){
                            System.out.print((problemInstance.ToSingleDevice(j).getNumbDeviceReal() + 1) + " ");
                            isSlotEmpty = false;
                        }
                    }
                    if(isSlotEmpty){
                        System.out.print("0 ");
                    }
                }
            }
            else{
                System.out.println();
                System.out.println();
                for (int i = 0; i < problemInstance.getNumbSlots(); i++) {
                    System.out.print(problemInstance.ToSingleDevice((schedule[i] - 1) + 1) + " ");
                } 
            }

            System.out.println();
            }
            
    }
    
    public boolean IsSolutionIntegral(double epsilon){
        boolean t = true;
        for(int i = 0; i < w.length; i++) {
            if((w[i] > Helpers.EPS) && (Math.abs(w[i] - 1) > Helpers.EPS)){
                t = false;
                break;
            } 
        }
        return t;
    }
    
    private boolean FindWhetherClientAllocatesSlot(int numClient, int numSlot){
        for(int i = 0; i < w.length; i++) {
            if(w[i] > Helpers.EPS && setOfColumns.columns.get(i).client[numClient] == 1 && 
                    setOfColumns.columns.get(i).column[numSlot] > Helpers.EPS){
                return true;
            }
        }
        return false;
    }
    
    public int FindDeviceThatCollide(){
        int slotWithInterference = -1;
        for(int i = 0; i < y.length; i++) {
            if(Math.abs(y[i]) > Helpers.EPS){
                slotWithInterference = i;
                break;
            }
        }
        if(slotWithInterference >= 0){
            for(int i = 0; i < duals2.length; i++) {
                if(FindWhetherClientAllocatesSlot(i, slotWithInterference)){
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    //way = 0 - worst fractional from all of the clients
    //way = 1 - closest to 1,
    //way = 2 - worst fractional(closest to 0.5)
    //way = 3 - consecutive slot allocation, described in the paper
    //way = 4 - first fractional
    public int[] ReturnDeviceToBranch(PartialSolution partialSolutions, int way) throws IOException{
        int slotToAllocate = -1;
        int deviceToBranch = -1;
        double[] weightsForSlots = new double[problemInstance.getNumbSlots()];
        
        if(way == 0){
            double closestToHalf = 2;
            for(int i = 0; i < problemInstance.getNumbClients(); i++) {
                Helpers.InitializeTo(weightsForSlots, 0, 0, weightsForSlots.length);
                for (int j = 0; j < setOfColumns.columns.size(); j++) {
                    if(setOfColumns.columns.get(j).client[i] == 1 && w[j] > Helpers.EPS){
                        for (int k = 0; k < problemInstance.getNumbSlots(); k++) {
                            if(setOfColumns.columns.get(j).column[k] == 1){
                                weightsForSlots[k] += w[j];
                            }
                        }
                    }
                }
                
                for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                   if(Math.abs(weightsForSlots[j] - 0.5) < Math.abs(closestToHalf - 0.5) && 
                           partialSolutions.getSlotAllocation()[j] == false && partialSolutions.getPartialSolutions()[i][j] != 0){
                       closestToHalf = weightsForSlots[j];
                       slotToAllocate = j;
                       deviceToBranch = i;
                   }
                }
            }
        }
        
        //double[] sum = new double[problemInstance.getNumbDevices()];
        boolean chosen = false;
        if(way == 1){
             for(int i = 0; i < problemInstance.getNumbClients(); i++) {
                for(int j = 0; j < w.length; j++) {
                    if(setOfColumns.columns.get(j).client[i] == 1 && w[j] > Helpers.EPS){
                        for (int k = 0; k < problemInstance.getNumbSlots(); k++) {
                            if(setOfColumns.columns.get(j).column[k] == 1){
                                weightsForSlots[k] += w[j];
                            }
                        }
                    }
                }
                
                boolean[] isSlotConsidered = new boolean[problemInstance.getNumbSlots()];
                for (int k = 0; k < problemInstance.getNumbSlots(); k++) {
                    double closestToOne = 2;
                    int indexClosestToOne = -1;
                    for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                       if(Math.abs(weightsForSlots[j] - 1) < Math.abs(closestToOne - 1.0) && !isSlotConsidered[j] && (1.0 - weightsForSlots[j] > Helpers.EPS)){
                           closestToOne = weightsForSlots[j];
                           indexClosestToOne = j;
                       }
                    }

                     if(closestToOne > Helpers.EPS && closestToOne < 1 - Helpers.EPS && partialSolutions.getSlotAllocation()[indexClosestToOne] == false && partialSolutions.getPartialSolutions()[i][indexClosestToOne] != 0){
                            slotToAllocate = indexClosestToOne;
                            deviceToBranch = i;
                            chosen = true;
                            break;
                     }
                     else{
                        if(indexClosestToOne > 0){
                            isSlotConsidered[indexClosestToOne] = true;
                        }
                     }
                     
                     if(closestToOne < Helpers.EPS || closestToOne > 1 - Helpers.EPS){
                         break;
                     }
                }
                
                if(chosen){
                    break;
                }
            }
        }
      
        if(way == 2){
            for(int i = 0; i < problemInstance.getNumbClients(); i++) {
                for (int j = 0; j < setOfColumns.columns.size(); j++) {
                    if(setOfColumns.columns.get(j).client[i] == 1 && w[j] > Helpers.EPS){
                        for (int k = 0; k < problemInstance.getNumbSlots(); k++) {
                            if(setOfColumns.columns.get(j).column[k] == 1){
                                weightsForSlots[k] += w[j];
                            }
                        }
                    }
                }
                
                boolean[] isSlotConsidered = new boolean[problemInstance.getNumbSlots()];
                for (int k = 0; k < problemInstance.getNumbSlots(); k++) {
                    double closestToHalf = 2;
                    int indexClosestToHalf = -1;
                    for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                       if(Math.abs(weightsForSlots[j] - 0.5) < Math.abs(closestToHalf - 0.5) && !isSlotConsidered[j]){
                           closestToHalf = weightsForSlots[j];
                           indexClosestToHalf = j;
                       }
                    }

                     if(closestToHalf > Helpers.EPS && closestToHalf < 1 - Helpers.EPS && partialSolutions.getSlotAllocation()[indexClosestToHalf] == false && partialSolutions.getPartialSolutions()[i][indexClosestToHalf] != 0){
                            slotToAllocate = indexClosestToHalf;
                            deviceToBranch = i;
                            chosen = true;
                            break;
                     }
                     else{
                        isSlotConsidered[indexClosestToHalf] = true;
                     }
                     if(closestToHalf < Helpers.EPS || closestToHalf > 1 - Helpers.EPS){
                         break;
                     }
                }
                
                if(chosen){
                    break;
                }
            }
        }
        
        if(way == 3){
            for(int i = 0; i < problemInstance.getNumbClients(); i++) {
                for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                    if(partialSolutions.getSlotAllocation()[j] == false && partialSolutions.getPartialSolutions()[i][j] != 0){
                        slotToAllocate = j;
                        deviceToBranch = i;
                        chosen = true;
                        break;
                    }
                }
                if(chosen){
                    break;
                }
            }
        }
        
        if(way == 4){
            chosen = false;
            for(int i = 0; i < problemInstance.getNumbClients(); i++) {
               for (int j = 0; j < setOfColumns.columns.size(); j++) {
                    if(setOfColumns.columns.get(j).client[i] == 1 && w[j] > Helpers.EPS){
                        for (int k = 0; k < problemInstance.getNumbSlots(); k++) {
                            if(setOfColumns.columns.get(j).column[k] == 1){
                                weightsForSlots[k] += w[j];
                            }
                        }
                    }
                }
                
                for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                    if(weightsForSlots[j] > Helpers.EPS && weightsForSlots[j] < 1 - Helpers.EPS && partialSolutions.getSlotAllocation()[j] == false && partialSolutions.getPartialSolutions()[i][j] != 0){
                        slotToAllocate = j;
                        deviceToBranch = i;
                        chosen = true;
                        break;
                    }
                }
                //sum[i] = Helpers.SumOverArray(weightsForSlots);
                if(chosen){
                    break;
                }
            }
        }
        
        int[] output = {deviceToBranch, slotToAllocate};
        return output;
    }
}
