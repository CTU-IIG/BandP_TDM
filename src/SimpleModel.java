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
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class SimpleModel {
    private IloCplex simpleModelSolver;
    private ProblemInstance problemInstance;
    private IloNumVar[][] x;
    private IloNumVar[][] r;
    private IloObjective NumberOfAllocatedSlots;
    private IloRange[][][] minNumberOfDoneWork;
    private IloRange[] bandRequirements;
    private IloRange[][] latRequirements;
    private IloRange[][] fixedAssignmentRequirements;
    private IloRange[] slotAllocatedOnce;
    private IloRange[] minNumbOfAllocSlots;
    private IloRange criterionBound;
    
    //Create the model
    public SimpleModel(ProblemInstance problemInstance, PartialSolution partialSolutions) throws IloException {
        this.problemInstance = problemInstance;
        simpleModelSolver = new IloCplex();
        simpleModelSolver.setParam(IloCplex.IntParam.AdvInd, 0);
        
        x = new IloNumVar[problemInstance.getNumbClients()][problemInstance.getNumbSlots()];
        r = new IloNumVar[problemInstance.getNumbClients()][problemInstance.getNumbSlots()];
        for (int i = 0; i < problemInstance.getNumbClients(); i++) {
            x[i] = simpleModelSolver.numVarArray(BranchAndPrice.MAX_NUMB_SLOTS, 0.0, 1.0, IloNumVarType.Int);
            r[i] = simpleModelSolver.numVarArray(BranchAndPrice.MAX_NUMB_SLOTS, 0.0, BranchAndPrice.MAX_NUMB_SLOTS, IloNumVarType.Int);
        }
        simpleModelSolver.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Primal);
  
        fixedAssignmentRequirements = new IloRange [problemInstance.getNumbClients()][problemInstance.getNumbSlots()];
        minNumberOfDoneWork = new IloRange[problemInstance.getNumbClients()][problemInstance.getNumbSlots()][problemInstance.getNumbSlots()];
        bandRequirements = new IloRange[problemInstance.getNumbClients()];
        latRequirements = new IloRange[problemInstance.getNumbClients()][problemInstance.getNumbSlots()];
        slotAllocatedOnce = new IloRange[problemInstance.getNumbSlots()];
        minNumbOfAllocSlots = new IloRange[problemInstance.getNumbClients()];
        
        double[] tmp = new double[BranchAndPrice.MAX_NUMB_SLOTS];
        Helpers.InitializeTo(tmp, 1, 0, problemInstance.getNumbSlots());
        Helpers.InitializeTo(tmp, 0, problemInstance.getNumbSlots(), tmp.length);
        
        IloNumExpr[] sums = new IloNumExpr[problemInstance.getNumbClients()];
        for (int j = 0; j < problemInstance.getNumbClients(); j++) {
            sums[j] = simpleModelSolver.scalProd(x[j], tmp);
            minNumbOfAllocSlots[j] = simpleModelSolver.addGe(sums[j], problemInstance.ToSingleDevice(j).getNumberOfRequiredSlots());
        }
        NumberOfAllocatedSlots = simpleModelSolver.addMinimize(simpleModelSolver.sum(sums)); 
        //criterionBound = simpleModelSolver.addLe(simpleModelSolver.sum(sums), problemInstance.getNumbSlots());
        simpleModelSolver.setParam(IloCplex.DoubleParam.ObjLLim, BranchAndPrice.UpperBound - 1.0 / problemInstance.getNumbSlots());
       
        for (int i = 0; i < problemInstance.getNumbSlots(); i++) {
            for (int j = 0; j < problemInstance.getNumbClients(); j++) {
                Helpers.InitializeTo(tmp, 0, 0, tmp.length);
                tmp[i] = 1;
                sums[j] = simpleModelSolver.scalProd(x[j], tmp);
            }
            slotAllocatedOnce[i] = simpleModelSolver.addLe(simpleModelSolver.sum(sums), 1);
        }
        
        //Processing partial solutions
        for (int nClient = 0; nClient < problemInstance.getNumbClients(); nClient++) {
            for(int j = 0; j < problemInstance.getNumbSlots(); j++){
                    if(partialSolutions.getPartialSolutions()[nClient][j] == 1){
                        fixedAssignmentRequirements[nClient][j] = simpleModelSolver.addEq(x[nClient][j], 1);
                    }

                    if(partialSolutions.getPartialSolutions()[nClient][j] == 0){
                        fixedAssignmentRequirements[nClient][j] = simpleModelSolver.addEq(x[nClient][j], 0);
                    }
             }

            double[] tmp2 = new double[BranchAndPrice.MAX_NUMB_SLOTS];
            Helpers.InitializeTo(tmp2, 0, problemInstance.getNumbSlots(), tmp2.length);

            //Create latency checking constraints. Different for latency- and non-latency- dominated use-cases
            if(problemInstance.ToSingleDevice(nClient).isIsLatDom()){
                if(problemInstance.ToSingleDevice(nClient).getLatencyRequirement() < problemInstance.getNumbSlots()){
                    int j = (int) Math.floor(problemInstance.ToSingleDevice(nClient).getLatencyRequirement());
                    for(int i = 0; i < problemInstance.getNumbSlots(); i++){
                        if(i == j) tmp[i] = 1.0;
                        else tmp[i] = 0.0;
                     }

                    for(int k = 0; k < problemInstance.getNumbSlots(); k++){
                       int help = -1;
                       if(k + j >= problemInstance.getNumbSlots()){
                           help = (k + j) % problemInstance.getNumbSlots();
                       }

                       for(int i = 0; i < problemInstance.getNumbSlots(); i++){
                           if(i >= k && i <= (k + j)) tmp2[i] = -1.0;
                           else
                               if(i <= help) tmp2[i] = -1.0;
                               else tmp2[i] = 0.0;
                       }
                       minNumberOfDoneWork[nClient][0][k] = simpleModelSolver.addLe(simpleModelSolver.sum(simpleModelSolver.scalProd(tmp, r[nClient]), simpleModelSolver.scalProd(tmp2,x[nClient])),0);
                    }
                } 
                
            }
            else{
                if(problemInstance.ToSingleDevice(nClient).getLatencyRequirement() < problemInstance.getNumbSlots()){
                    for(int j = (int) Math.floor(problemInstance.ToSingleDevice(nClient).getLatencyRequirement()); j < problemInstance.getNumbSlots(); j++ ) {
                         for(int i = 0; i < problemInstance.getNumbSlots(); i++){
                            if(i == j) tmp[i] = 1.0;
                            else tmp[i] = 0.0;
                         }
                         for(int k = 0; k < problemInstance.getNumbSlots(); k++){
                            int help = -1;
                            if(k + j >= problemInstance.getNumbSlots()){
                                help = (k + j) % problemInstance.getNumbSlots();
                            }

                            for(int i = 0; i < problemInstance.getNumbSlots(); i++){
                                if(i >= k && i <= (k + j)) tmp2[i] = -1.0;
                                else
                                    if(i <= help) tmp2[i] = -1.0;
                                    else tmp2[i] = 0.0;
                            }
                            minNumberOfDoneWork[nClient][j][k] = simpleModelSolver.addLe(simpleModelSolver.sum(simpleModelSolver.scalProd(tmp, r[nClient]), simpleModelSolver.scalProd(tmp2,x[nClient])),0);
                         }
                     }
                }
             }
            
             Helpers.InitializeTo(tmp, 1, 0, problemInstance.getNumbSlots());
             Helpers.InitializeTo(tmp, 0, problemInstance.getNumbSlots(), tmp.length);
            
             //create constraints for bandwidth requirements
             bandRequirements[nClient] = simpleModelSolver.addGe(simpleModelSolver.scalProd(tmp, x[nClient]),
                    problemInstance.getNumbSlots() * problemInstance.ToSingleDevice(nClient).getBandwidthRequirement());
             
             if(problemInstance.ToSingleDevice(nClient).isIsLatDom()){
                latRequirements[nClient] = new IloRange[1];
                int i = (int) Math.floor(problemInstance.ToSingleDevice(nClient).getLatencyRequirement());
                if(i >= problemInstance.getNumbSlots()){
                    i = problemInstance.getNumbSlots() - 1;
                }
                for(int b = 0; b < problemInstance.getNumbSlots(); b++){
                    if(b == i) tmp[b] = 1.0;
                    else tmp[b] = 0.0;
                }
                Helpers.InitializeTo(tmp, 0, 0, tmp.length);
                tmp[i] = 1;
                latRequirements[nClient][0] = simpleModelSolver.addGe(simpleModelSolver.scalProd(tmp, r[nClient]), problemInstance.ToSingleDevice(nClient).getBandwidthRequirement() * (i + 1 - problemInstance.ToSingleDevice(nClient).getLatencyRequirement()));
             }
             else{
                 for(int i = (int) Math.floor(problemInstance.ToSingleDevice(nClient).getLatencyRequirement()); i < problemInstance.getNumbSlots(); i++) {  
                   if((int) Math.floor(problemInstance.ToSingleDevice(nClient).getLatencyRequirement()) > problemInstance.getNumbSlots()){
                       i = problemInstance.getNumbSlots();
                   }

                   for(int b = 0; b < problemInstance.getNumbSlots(); b++){
                       if(b == i) tmp[b] = 1.0;
                       else tmp[b] = 0.0;
                   }
                   Helpers.InitializeTo(tmp, 0, 0, tmp.length);
                   tmp[i] = 1;
                   latRequirements[nClient][i] = simpleModelSolver.addGe(simpleModelSolver.scalProd(tmp, r[nClient]), problemInstance.ToSingleDevice(nClient).getBandwidthRequirement() * (i + 1 - problemInstance.ToSingleDevice(nClient).getLatencyRequirement()));
                }
             }
             //simpleModelSolver.output().close();
         }
        simpleModelSolver.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Primal);
        simpleModelSolver.setParam(IloCplex.DoubleParam.CutUp, Math.round(BranchAndPrice.UpperBound * problemInstance.getNumbSlots()) - 1);
    }
     
    /*public void ModelToFile(String outputFile){
        try {
            simpleModelSolver.exportModel(outputFile);
        } catch (IloException ex) {
            Logger.getLogger(MasterModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
    
    public int[] Solve() throws IloException{
        if(simpleModelSolver.solve()){
            int[] resultSchedule = new int[problemInstance.getNumbSlots()];
            Helpers.InitializeTo(resultSchedule, 0);

            for (int i = 0; i < problemInstance.getNumbClients(); i++) {
                for (int j = 0; j < problemInstance.getNumbSlots(); j++) {
                    if(Math.round(simpleModelSolver.getValues(x[i])[j]) == 1){
                        resultSchedule[j] = problemInstance.ToSingleDevice(i).getNumbDeviceReal() + 1;
                    }
                }
            }
            this.end();
            return resultSchedule;
        }
        else{
            //this.ModelToFile(Helpers.outputFile);
            this.end();
            return null;
        }
   }
    
    public void end() throws IloException{
       simpleModelSolver.end();
   }
}
