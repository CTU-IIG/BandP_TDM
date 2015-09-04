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
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import java.util.logging.Level;
import java.util.logging.Logger;
import static sun.security.jgss.GSSToken.debug;

public class SubModel {
    private IloCplex patSolver;
    private IloObjective ReducedCost;
    private IloRange[][] minNumberOfDoneWork;
    private IloNumVar[] x;
    private IloNumVar[] r;
    private IloRange[] latRequirements;
    private IloRange[] fixedAssignmentRequirements;
    private IloRange minNumbSlots;
    private ProblemInstanceSingleClient problemInstanceSingleDevice;
    
    //partialSolution array in the beginning must be initiallized by -1

    public SubModel(ProblemInstanceSingleClient problemInstanceSingleDevice1) throws IloException {
        patSolver = new IloCplex();
        
        //Use lazy constraints
        if(!problemInstanceSingleDevice1.isIsLatDom())
            patSolver.use(new LazyConstraintCallback());
       
        this.problemInstanceSingleDevice = problemInstanceSingleDevice1;
        x = patSolver.numVarArray(problemInstanceSingleDevice.getNumbSlots(), 0, 1, IloNumVarType.Bool);
        r = patSolver.numVarArray(problemInstanceSingleDevice.getNumbSlots(), 0.0, problemInstanceSingleDevice.getNumbSlots(), IloNumVarType.Int);
        
        double[] tmp = new double[problemInstanceSingleDevice.getNumbSlots()];
        
        Helpers.InitializeTo(tmp, 1.0, 0, problemInstanceSingleDevice.getNumbSlots());
        minNumbSlots = patSolver.addGe(patSolver.scalProd(x, tmp), problemInstanceSingleDevice.getNumberOfRequiredSlots());

        double[] tmp2 = new double[BranchAndPrice.MAX_NUMB_SLOTS];
        Helpers.InitializeTo(tmp2, 0, problemInstanceSingleDevice.getNumbSlots(), tmp2.length);

       // if(problemInstanceSingleDevice.isIsLatDom()){
       // Since the lazy constraints trick is used, first we include only the minimal gap constraints 
       // (that are enough only for latency-dominated instances) and only then in the callbacks 
       // others constraints will be checked and added if necessary (if they are not satisfied in the current solution)
            if(problemInstanceSingleDevice.getLatencyRequirement() < problemInstanceSingleDevice.getNumbSlots()){
                minNumberOfDoneWork = new IloRange[1][problemInstanceSingleDevice.getNumbSlots()];
                int j = (int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement()); 
                Helpers.InitializeTo(tmp, 0, 0, tmp.length);
                tmp[j] = 1;

                for(int k = 0; k < problemInstanceSingleDevice.getNumbSlots(); k++){
                   int help = -1;
                   if(k + j >= problemInstanceSingleDevice.getNumbSlots()){
                       help = (k + j) % problemInstanceSingleDevice.getNumbSlots();
                   }

                   for(int i = 0; i < problemInstanceSingleDevice.getNumbSlots(); i++){
                       if(i >= k && i <= (k + j)){
                           tmp2[i] = -1.0;
                       }
                       else{
                           if(i <= help){
                               tmp2[i] = -1.0;
                           }
                           else{
                               tmp2[i] = 0.0;
                           }
                       }     
                   }
                   minNumberOfDoneWork[0][k] = patSolver.addLe(patSolver.sum(patSolver.scalProd(tmp, r), patSolver.scalProd(tmp2,x)),0);
                }
            }
       /* }
        else{
            if(problemInstanceSingleDevice.getLatencyRequirement() < problemInstanceSingleDevice.getNumbSlots()){
                minNumberOfDoneWork = new IloRange[problemInstanceSingleDevice.getNumbSlots() - (int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement())][problemInstanceSingleDevice.getNumbSlots()];
                for(int j = (int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement()); j < problemInstanceSingleDevice.getNumbSlots(); j++ ) {
                     Helpers.InitializeTo(tmp, 0, 0, tmp.length);
                     tmp[j] = 1;

                     for(int k = 0; k < problemInstanceSingleDevice.getNumbSlots(); k++){
                        int help = -1;
                        if(k + j >= problemInstanceSingleDevice.getNumbSlots()){
                            help = (k + j) % problemInstanceSingleDevice.getNumbSlots();
                        }

                        for(int i = 0; i < problemInstanceSingleDevice.getNumbSlots(); i++){
                            if(i >= k && i <= (k + j)){
                                tmp2[i] = -1.0;
                            }
                            else{
                                if(i <= help){
                                    tmp2[i] = -1.0;
                                }
                                else{
                                    tmp2[i] = 0.0;
                                }
                            }
                        }
                        minNumberOfDoneWork[j - (int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement())][k] = patSolver.addLe(patSolver.sum(patSolver.scalProd(tmp, r), patSolver.scalProd(tmp2,x)),0);
                     }
                 }
            }
        }*/
 
         //if(problemInstanceSingleDevice.isIsLatDom()){
            latRequirements = new IloRange[1];
            int i = (int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement());
            if(i >= problemInstanceSingleDevice.getNumbSlots()){
                i = problemInstanceSingleDevice.getNumbSlots() - 1;
            }
            Helpers.InitializeTo(tmp, 0, 0, tmp.length);
            tmp[i] = 1;
            latRequirements[0] = patSolver.addGe(patSolver.scalProd(tmp, r), problemInstanceSingleDevice.getBandwidthRequirement() * (i + 1 - problemInstanceSingleDevice.getLatencyRequirement()));
        /* }
         else{
            latRequirements = new IloRange[problemInstanceSingleDevice.getNumbSlots() - (int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement())];

            for(int i = (int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement()); i < problemInstanceSingleDevice.getNumbSlots(); i++) {  
                if((int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement()) > problemInstanceSingleDevice.getNumbSlots()){
                    i = problemInstanceSingleDevice.getNumbSlots();
                }
                Helpers.InitializeTo(tmp, 0, 0, tmp.length);
                tmp[i] = 1;  
                latRequirements[i - (int) Math.floor(problemInstanceSingleDevice.getLatencyRequirement())] = patSolver.addGe(patSolver.scalProd(tmp, r), problemInstanceSingleDevice.getBandwidthRequirement() * (i + 1 - problemInstanceSingleDevice.getLatencyRequirement()));
            }
         }*/
         
         //patSolver.output().close();
         //this.ModelToFile(Helpers.outputFileForModels);
         patSolver.setParam(IloCplex.IntParam.AdvInd, 0);
         patSolver.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Primal); 
         
         //Set higher priority for the solver to initial variables
         for (i = 0; i < problemInstanceSingleDevice.getNumbSlots(); i++) {
            patSolver.setPriority(x[i], 1000); 
            patSolver.setPriority(r[i], 0); 
         }

         //playing with solver parameters
        /* if(!problemInstanceSingleDevice.isIsLatDom()){
            //switch off presolve
            patSolver.setParam(IloCplex.BooleanParam.PreInd, false);
            patSolver.setParam(IloCplex.IntParam.RelaxPreInd, 0);
            patSolver.setParam(IloCplex.IntParam.RepeatPresolve, 0);
            patSolver.setParam(IloCplex.IntParam.PreslvNd, -1);
            patSolver.setParam(IloCplex.IntParam.Probe, -1);
            patSolver.setParam(IloCplex.DoubleParam.ProbeTime, 1e+75);
            
            //switch off cuts
            patSolver.setParam(IloCplex.IntParam.ZeroHalfCuts, -1);
            patSolver.setParam(IloCplex.IntParam.FracCuts, -1);
            patSolver.setParam(IloCplex.IntParam.MIRCuts, -1);
            patSolver.setParam(IloCplex.IntParam.EachCutLim, 0); 
             
            //switch off heuristic
            patSolver.setParam(IloCplex.IntParam.HeurFreq, -1);
            patSolver.setParam(IloCplex.IntParam.RINSHeur, -1);
            patSolver.setParam(IloCplex.IntParam.FPHeur, -1);
            patSolver.setParam(IloCplex.IntParam.LiftProjCuts, -1);    
         }*/
         //patSolver.setParam(IloCplex.DoubleParam.TiLim, 0.7);
        // patSolver.setParam(IloCplex.IntParam.CutPass, -1); 
   }
   
    public void ChangeCoefficientsInSubModel(double[] shadowPrices, PartialSolution partialSolutions, 
            double bound, double gap) throws IloException{
        double[] tmp = new double[problemInstanceSingleDevice.getNumbSlots()];
        Helpers.InitializeTo(tmp, 1.0 / problemInstanceSingleDevice.getNumbSlots(), 0, problemInstanceSingleDevice.getNumbSlots());
         
        if(ReducedCost != null) patSolver.remove(ReducedCost);
        ReducedCost = patSolver.addMaximize(patSolver.diff(patSolver.scalProd(x, shadowPrices), patSolver.scalProd(x, tmp)));
       
        //setting bound on the criterion value
        if(bound < 0){
           patSolver.setParam(IloCplex.DoubleParam.CutLo, bound);
        }
        else{
           patSolver.setParam(IloCplex.DoubleParam.CutLo, -Double.MAX_VALUE);
        }
        
        if(fixedAssignmentRequirements != null) patSolver.remove(fixedAssignmentRequirements);
        fixedAssignmentRequirements = new IloRange [problemInstanceSingleDevice.getNumbSlots()];
        
        //Processing partial solutions
        for(int j = 0; j < problemInstanceSingleDevice.getNumbSlots(); j++){
            if(partialSolutions.getPartialSolutions()[problemInstanceSingleDevice.getNumbOfDeviceInArray()][j] == 1){
                fixedAssignmentRequirements[j] = patSolver.addEq(x[j], 1);
            }

            if(partialSolutions.getPartialSolutions()[problemInstanceSingleDevice.getNumbOfDeviceInArray()][j] == 0){
                fixedAssignmentRequirements[j] = patSolver.addEq(x[j], 0);
            }
         }
        
        //sub-model does not solve it to optimum, but integrality gap should be less than "gap" value
        patSolver.setParam(IloCplex.DoubleParam.EpGap, gap);
    }
    
    public ProblemSolutionSP Solve() throws IloException{
       // this.ModelToFile(Helpers.outputFileForModels);
        if(patSolver.solve()){
            ProblemSolutionSP problemSolutionSP = new ProblemSolutionSP(patSolver.getValues(x), patSolver.getObjValue(), problemInstanceSingleDevice);
            return problemSolutionSP;
        }
        else{
            return null;
        }
   }
    
    public ProblemSolutionSP[] SolveWithManySolutions(int numSolutionsRequired) throws IloException{
        patSolver.setParam(IloCplex.IntParam.PopulateLim, numSolutionsRequired); 
        patSolver.setParam(IloCplex.IntParam.SolnPoolIntensity, 3);
        patSolver.setParam(IloCplex.IntParam.SolnPoolReplace, 0);
        ProblemSolutionSP[] problemSolutionSP = new ProblemSolutionSP[numSolutionsRequired];
        try {
            if(patSolver.solve()){
                int colsGen = patSolver.getSolnPoolNsolns();
                for(int i = 0; i < Math.min(colsGen, numSolutionsRequired); i++){
                    try {
                        problemSolutionSP[i] = new ProblemSolutionSP(patSolver.getValues(x, i), patSolver.getObjValue(i), problemInstanceSingleDevice);
                    } catch (IloException ex) {
                        Logger.getLogger(SubModel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return problemSolutionSP;
            }
            else{
                return null;
            }
        } catch (IloException ex) {
            Logger.getLogger(SubModel.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
   }
   
   /*public void ModelToFile(String outputFile){
        try {
            patSolver.exportModel(outputFile);
        } catch (IloException ex) {
            Logger.getLogger(MasterModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
    
   public void end() throws IloException{
       patSolver.end();
   }
   
   private class LazyConstraintCallback extends IloCplex.LazyConstraintCallback {
        @Override
        protected void main() throws IloException {
            this.CheckAndAddIfNeccesary();
        }
        
        private void CheckAndAddIfNeccesary() throws IloException{
            //checking part
            double[] schedule = this.getValues(x);
            double[] tmp = new double[problemInstanceSingleDevice.getNumbSlots()];
            int[] problem = new int[2];
            
            if(!Heuristic.LatencyControlGeneral(problemInstanceSingleDevice, schedule, problem)){
               // debug("---- ADD CONSTRAINT ----");
                //add constraints because the solution is not feasible
                /*double sum = 0;
                for(int i = 0; i < problemInstanceSingleDevice.getNumbSlots(); i++) {
                    if(schedule[i] > Helpers.EPS){
                        tmp[i] = 1;
                        sum += 1;
                    }
                    else tmp[i] = -1;
                    debug(" " + tmp[i] + " ");
                }
                this.add(patSolver.le(patSolver.scalProd(x, tmp), (int) sum - 1));*/

                int indexExceedEnd = -1, lengthOfWindow = 0;
                int start = problem[0];
                int end = problem[1], fictiveEnd = end;
                if(start > end){
                    indexExceedEnd = end;
                    fictiveEnd = problemInstanceSingleDevice.getNumbSlots() - 1;
                }

                for(int i = 0; i < problemInstanceSingleDevice.getNumbSlots(); i++){
                    if(i >= start && i <= fictiveEnd && i != end){
                        tmp[i] = 1.0;
                        lengthOfWindow++;
                    }
                    else{
                        if(i < indexExceedEnd){
                            tmp[i] = 1.0;
                            lengthOfWindow++;
                        }
                        else{
                            tmp[i] = 0.0;
                        }
                    }     
                }
                this.add(patSolver.ge(patSolver.scalProd(tmp,x), problemInstanceSingleDevice.getBandwidthRequirement() * (lengthOfWindow - problemInstanceSingleDevice.getLatencyRequirement())));
            }
        }
   }
}
