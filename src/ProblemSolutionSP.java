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

import java.io.FileWriter;
import java.io.IOException;

public class ProblemSolutionSP {
    private double[] x;
    private double objValue;
    private int nClient;
    private ProblemInstanceSingleClient problemInstanceSingleDevice;

    public ProblemInstanceSingleClient getProblemInstanceSingleDevice() {
        return problemInstanceSingleDevice;
    }

    public double[] getX() {
        return x;
    }

    public double getObjValue() {
        return objValue;
    }
    
    public ProblemSolutionSP(double[] x, double objValue, ProblemInstanceSingleClient problemInstanceSingleDevice) {
        this.x = x;
        this.objValue = objValue;
        this.nClient = problemInstanceSingleDevice.getNumbOfDeviceInArray();
        this.problemInstanceSingleDevice = problemInstanceSingleDevice;
    }
    
    public boolean IsReducedCostPositive(double duals2, double RC_EPS) throws IOException{
        double redPrice = ComputeReducedPrice(duals2);
       /* FileWriter ofst = new FileWriter("output1.txt",true);
        ofst.write(redPrice + "(" + this.nClient + ")\n");
        ofst.close();*/
        if(redPrice < RC_EPS) return false;
        else return true;
    }
    
    public double ComputeReducedPrice(double duals2) throws IOException{
        return objValue + duals2;
    }
    
    public void PrintResults(){
        System.out.println();
        System.out.println("An allocated rate is " + objValue);
    
        System.out.println();
        for(int i = 0; i < x.length; i++) {
                System.out.print("  Schedule[" + i + "] = " + x[i]);
        }
        System.out.println();
        
        System.out.println();
        //for(int i = 0; i < r.length; i++) {
                //System.out.print("  r[" + i + "] = " + r[i]);
        //}
        System.out.println();
    }
    
    public Column ConvertResults(){
        int[] y = new int[x.length];
        int[] b = new int[problemInstanceSingleDevice.getNumberDevices()];
        Helpers.InitializeTo(b, 0);
        b[problemInstanceSingleDevice.getNumbOfDeviceInArray()] = 1;
        
        double price = 0;
        for (int i = 0; i < problemInstanceSingleDevice.getNumbSlots(); i++) {
            price += this.x[i];
            y[i] = (int) Math.round(this.x[i]);
        }
        
        Column column = new Column(y, price / problemInstanceSingleDevice.getNumbSlots(), b);
        return column;
    }
}
