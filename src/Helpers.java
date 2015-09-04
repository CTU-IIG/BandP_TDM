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

public class Helpers {
    //------------constants-------------
    public static final double EPS = 1.0E-6;
    public static final String outputFileForModels = "out.lp";
    public static final String outputFileMain = "output.txt";
    public static final String outputFileForGraphWiz = "tree.dot";
    
    //------------functions--------------
    public static void InitializeTo(long[] array, int value){
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }
    
    public static void InitializeTo(int[] array, int value){
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }
    
    public static void InitializeTo(boolean[] array, boolean value){
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }
    
    public static void InitializeTo(double[] array, double value){
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }
    
    public static void InitializeTo(double[] array, double value, int indexStart, int indexEnd){
        for (int i = indexStart; i < indexEnd; i++) {
            array[i] = value;
        }
    }
    
    public static double[] Copy(double[] arrayInit, int indexStart, int indexEnd){
        double[] arrayResult = new double [indexEnd - indexStart];
        if(indexEnd < arrayInit.length){
            for(int i = 0; i < indexEnd - indexStart; i++) {
                arrayResult[i] = arrayInit[indexStart + i];
            }
        }
        else{
            for(int i = 0; i < arrayInit.length - indexStart; i++) {
                arrayResult[i] = arrayInit[indexStart + i];
            }
            for(int i = arrayInit.length; i < indexEnd; i++) {
                arrayResult[i] = 0;
            }
        }
        
        return arrayResult;
    }
    
    public static int SumOverArray(int[] array){
        int sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }
    
    public static double SumOverArray(double[] array){
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    //Check whether lower bound on the criterion equals to the found optimal solution criteion
    public static boolean IsOptimal(double optimalSolution, ProblemInstance problemInstance) {
        long numbOfSlotsInOptimalSolution = Math.round(optimalSolution * problemInstance.getNumbSlots());
        int sumMinAllocSlots = 0;
        for (int i = 0; i < problemInstance.getNumbClients(); i++) {
            sumMinAllocSlots += problemInstance.ToSingleDevice(i).getNumberOfRequiredSlots();
        }
        sumMinAllocSlots += problemInstance.getNumbCollisions();
        if (sumMinAllocSlots == numbOfSlotsInOptimalSolution) {
            return true;
        }
        return false;
    }
    
    public static double[] CreateDoubleArrayFromIntArray(int[] array){
        double[] arrayDouble = new double[array.length];
        for(int i = 0; i < array.length; i++) {
            arrayDouble[i] = array[i];
        }
        return arrayDouble;
    }
}
