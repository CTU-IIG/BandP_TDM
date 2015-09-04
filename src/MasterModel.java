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

import ilog.concert.*;
import ilog.cplex.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MasterModel {
    private IloCplex tdmSolver;
    private ProblemInstance problemInstance;
    private IloObjective NumberOfUsedSlots;
    private IloRange[] OneScheduleForProcessor;
    private IloRange[] EachSlotAllocatedAtMostOnce;
    //private IloRange CritPropagation;
    private ArrayList<IloRange> BranchingConstraints;
    private ArrayList<IloNumVar> assignment;
    private IloNumVar[] y;
    private SetOfColumns setOfColumns;

    public MasterModel() throws IloException {
        this.tdmSolver = new IloCplex();
    }

    public ProblemInstance getProblemInstance() {
        return problemInstance;
    }
    
    private void PartOfTheConstructor() throws IloException{
        tdmSolver = new IloCplex();

        BranchingConstraints = new ArrayList<>();
        assignment = new ArrayList<>();
        NumberOfUsedSlots = tdmSolver.addMinimize();

        OneScheduleForProcessor = new IloRange[problemInstance.getNumbClients()];
        EachSlotAllocatedAtMostOnce = new IloRange[problemInstance.getNumbSlots()];
        
         for (int f = 0; f < problemInstance.getNumbClients(); f++ ) {
            OneScheduleForProcessor[f] = tdmSolver.addRange(1, Double.MAX_VALUE);
            // OneScheduleForProcessor[f] = tdmSolver.addGe(1, tdmSolver.linearNumExpr());
         }
         for (int f = 0; f < problemInstance.getNumbSlots(); f++ ) {
            EachSlotAllocatedAtMostOnce[f] = tdmSolver.addRange(Double.MIN_VALUE, 1);
            // EachSlotAllocatedAtMostOnce[f] = tdmSolver.addLe(tdmSolver.linearNumExpr(), 1); 
         }

         y = new IloNumVar[problemInstance.getNumbSlots()];

         //columns for y_j
        for(int j = 0; j < problemInstance.getNumbSlots(); j++){
            y[j] = tdmSolver.numVar(tdmSolver.column(NumberOfUsedSlots, 10.0).and(tdmSolver.column(EachSlotAllocatedAtMostOnce[j], -1.0)),
                    0.0, problemInstance.getNumbClients(), IloNumVarType.Float);
        }
        
        //then columns for assignment_{i, k}
        for (int k = 0; k < setOfColumns.columns.size(); k++){
            AddColumnToModel(setOfColumns.columns.get(k), k);
        }

        tdmSolver.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Primal);
        
        //tdmSolver.output().close();
        //tdmSolver.setParam(IloCplex.IntParam. .LogVerbosity, IloCplex.ParameterValues.Quiet);
        //this.ModelToFile(Helpers.outputFile);
    }
    
    public MasterModel(MasterModel masterModel) throws IloException{
        this();
        CreateMasterModel(masterModel);
    }
    
    public MasterModel(ProblemInstance problemInstance, SetOfColumns setOfColumns) throws IloException{
        this();
        CreateMasterModel(problemInstance, setOfColumns);
    }
    
    public void CreateMasterModel(ProblemInstance problemInstance, SetOfColumns setOfColumns) throws IloException {
        this.setOfColumns = setOfColumns;
        this.problemInstance = problemInstance;
        PartOfTheConstructor();
    }
    
    public void CreateMasterModel(MasterModel masterModel) throws IloException{
        //can copy just reference, as it is not changing during run
        this.problemInstance = masterModel.problemInstance;
        this.setOfColumns = new SetOfColumns(masterModel.setOfColumns);
        
        NumberOfUsedSlots = tdmSolver.addMinimize();
        
        PartOfTheConstructor();
        
        this.BranchingConstraints = new ArrayList(masterModel.BranchingConstraints);
        for(int i = 0; i < this.BranchingConstraints.size(); i++){
            IloRange ilo = this.BranchingConstraints.get(i);
            tdmSolver.addRange(0, ilo.getExpr(), 0);
            //tdmSolver.addEq(ilo.getExpr(), 0); 
        }
    }
    
    //NumbOfDevice from 0 to problemInstance.getNumbDevices() - 1
    public int AddRowBranchingConstraint(int numbOfDevice, int numbSlot, int value) throws IloException {        
        //create matrix of coefficients to vatiables assignment
        int[] tmp = new int [setOfColumns.columns.size()];
        Helpers.InitializeTo(tmp, 0);
        int numbRemovedColumns = 0;
        
        for(int i = 0; i < setOfColumns.columns.size(); i++){
            if((setOfColumns.columns.get(i).client[numbOfDevice] == 1) && (setOfColumns.columns.get(i).column[numbSlot] != value)){
                tmp[i] = 1;
                numbRemovedColumns++;
            }
        }
        
        if(value == 1){
            for(int i = 0; i < setOfColumns.columns.size(); i++){
                if((setOfColumns.columns.get(i).client[numbOfDevice] == 0) && (setOfColumns.columns.get(i).column[numbSlot] == value)){
                    int nDev = -1;
                    //we need to find out which device it is
                    for(int l = 0; l < problemInstance.getNumbClients(); l++){
                        if(setOfColumns.columns.get(i).client[l] == 1){
                            nDev = l;
                            break;
                        }
                    }
                    tmp[i] = 1;
                    numbRemovedColumns++;
                }
            }
        }
        
        BranchingConstraints.add(tdmSolver.addRange(0, tdmSolver.scalProd(assignment.toArray(new IloNumVar[assignment.size()]), tmp), 0));
        //BranchingConstraints.add(tdmSolver.addEq(tdmSolver.scalProd(assignment.toArray(new IloNumVar[assignment.size()]), tmp), 0)); 
        return numbRemovedColumns;
       //this.ModelToFile("output.lp");
    }
    
    public void DeleteRowBranchingConstraint(){
        //this.ModelToFile("output1.lp");
        try {
            tdmSolver.remove(BranchingConstraints.get(BranchingConstraints.size() - 1));
            BranchingConstraints.remove(BranchingConstraints.size() - 1);
        } catch (IloException ex) {
            Logger.getLogger(MasterModel.class.getName()).log(Level.SEVERE, null, ex);
        }
        //this.ModelToFile("output.lp");
    }
    
    public void DeleteLastColumns(int numbColumns) throws IloException{
        //this.ModelToFile("output.lp");
        for(int i = 0; i < numbColumns; i++){
            tdmSolver.delete(assignment.get(assignment.size() - 1));
            assignment.remove(assignment.size() - 1);
            setOfColumns.deleteLast();
        }
    }
    
    public void AddColumnToSetOfColumnsAndModel(Column pattern, int client) throws IloException{
        setOfColumns.add(pattern);
        AddColumnToModel(pattern, client);
    }
    
    public void AddColumnToModel(Column pattern, int client) throws IloException{
        //tdmSolver.exportModel("out.lp");
        IloNumVar help;

        IloColumn column = tdmSolver.column(NumberOfUsedSlots, pattern.cost);
        column = column.and(tdmSolver.column(OneScheduleForProcessor[client], 1));

        for(int j = 0; j < problemInstance.getNumbSlots(); j++){
            column = column.and(tdmSolver.column(EachSlotAllocatedAtMostOnce[j], pattern.column[j]));
        }
        help = tdmSolver.numVar(column, 0.0, 1.0, IloNumVarType.Float);

        assignment.add(help);
        //tdmSolver.exportModel("out.lp");
    }
    
   public ProblemSolutionMM Solve(PartialSolution partialSolutions) throws IOException{
        try {
            //tdmSolver.exportModel("out.lp");
            if(tdmSolver.solve()){
                ProblemSolutionMM problemSolutionMM = new ProblemSolutionMM(tdmSolver.getValues(assignment.toArray(new IloNumVar[assignment.size()])), 
                    tdmSolver.getValues(y), tdmSolver.getDuals(EachSlotAllocatedAtMostOnce), 
                        tdmSolver.getDuals(OneScheduleForProcessor), tdmSolver.getObjValue(), setOfColumns);
                //problemSolutionMM.PrintResults();
                //tdmSolver.setParam(IloCplex.IntParam.AdvInd, 1);
                return problemSolutionMM;
            }
            else{
               int problem = -1;
                //this.ModelToFile("out.lp");
               
                boolean isGoodColumn;
                for(int i = 0; i < problemInstance.getNumbClients(); i++){
                    if(problem != -1){
                        break;
                    }
                    isGoodColumn = false;
                    
                    for(int j = 0; j < setOfColumns.columns.size(); j++){
                        if(isGoodColumn == false){
                            
                            if(setOfColumns.columns.get(j).client[i] == 1){
                                for(int k = 0; k < problemInstance.getNumbSlots(); k++){                  
                                    if((partialSolutions.getPartialSolutions()[i][k] == 1) && (setOfColumns.columns.get(j).column[k] == 0)){
                                        if((j == (setOfColumns.columns.size() - 1))){
                                            problem = i;
                                        }
                                        break;
                                    }
                                    
                                    if((partialSolutions.getPartialSolutions()[i][k] == 0) && (setOfColumns.columns.get(j).column[k] == 1)){
                                        if((j == (setOfColumns.columns.size() - 1))){
                                            problem = i;
                                        }
                                        break;
                                    }
                                    
                                    if(k == problemInstance.getNumbSlots() - 1){
                                        isGoodColumn = true;
                                    }
                                }
                            }
                            else{
                                if((j == (setOfColumns.columns.size() - 1)) && (isGoodColumn == false)){
                                        problem = i;
                                    }
                            }
                        }
                        else{
                            break;
                        }
                    }
                }
                
                ProblemSolutionMM problemSolutionMM = new ProblemSolutionMM(problem);
                return problemSolutionMM;
            }
        } catch (IloException ex) {
            Logger.getLogger(SubModel.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
   }
 
   public ProblemSolutionMM SolveAsILP(){
      IloConversion[] conversions = new IloConversion[assignment.size()];
      for (int i = 0; i < assignment.size(); i++) {
            try {
                conversions[i] = tdmSolver.conversion(assignment.get(i), IloNumVarType.Int);
                tdmSolver.add(conversions[i]);
            } catch (IloException ex) {
                Logger.getLogger(MasterModel.class.getName()).log(Level.SEVERE, null, ex);
            }
      }
      
      try {
        ProblemSolutionMM problemSolutionMM;
        if(tdmSolver.solve()){
            problemSolutionMM = new ProblemSolutionMM(tdmSolver.getValues(assignment.toArray(new IloNumVar[assignment.size()])),
                    tdmSolver.getValues(y), tdmSolver.getObjValue(), setOfColumns);
        }
        else{
            problemSolutionMM = null;
        } 
        
        for (int i = 0; i < assignment.size(); i++) {
            try {
                tdmSolver.remove(conversions[i]);
            } catch (IloException ex) {
                Logger.getLogger(MasterModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return problemSolutionMM;
      } catch (IloException ex) {
          Logger.getLogger(MasterModel.class.getName()).log(Level.SEVERE, null, ex);
          return null;
      }

   }
  /* public void ModelToFile(String outputFile){
        try {
            tdmSolver.exportModel(outputFile);
        } catch (IloException ex) {
            Logger.getLogger(MasterModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
     
   public void end() throws IloException{
      tdmSolver.remove(NumberOfUsedSlots);
      
      for(int i = 0; i < OneScheduleForProcessor.length; i++) {
          tdmSolver.remove(OneScheduleForProcessor[i]);
      }
      
       for(int i = 0; i < EachSlotAllocatedAtMostOnce.length; i++) {
           tdmSolver.remove(EachSlotAllocatedAtMostOnce[i]);
       }
      
       for(int i = 0; i < BranchingConstraints.size(); i++) {
           tdmSolver.remove(BranchingConstraints.get(i));
       }
       BranchingConstraints.clear();
       
       for(int i = 0; i < assignment.size(); i++) {
           tdmSolver.delete(assignment.get(i));
       }
       
       int length = assignment.size();
       for(int i = 0; i < length; i++) {
           assignment.remove(assignment.size() - 1);
           setOfColumns.deleteLast();
       }
                
       for(int i = 0; i < y.length; i++) {
           tdmSolver.remove(y[i]);
       }
   }
   
   public void endEnd(){
       tdmSolver.end();
   }
   
   public void writeBasis(String file) throws IloException{
       tdmSolver.writeBasis(file);
   }
   
   public void readBasis(String file) throws IloException{
       tdmSolver.readBasis(file);
   }
   
    public SetOfColumns getColumns() {
        return setOfColumns;
    }
}
