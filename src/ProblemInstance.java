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

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.String;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

public class ProblemInstance{
    private ProblemInstanceSingleClient[] problemInstance;
    private int n;
    private int f;
    private int numbCollisions;

    public int getNumbCollisions() {
        return numbCollisions;
    }
    
    public void setFrameSize(int f) {
        this.f = f;
        for(int i = 0; i < problemInstance.length; i++) {
            problemInstance[i].setFrameSize(f);
        }
    }

    public void setNumberOfClients(int n) {
        this.n = n;
    }
    
    public ProblemInstance(String fileName, int way)
            throws IOException,
                                InputDataReader.InputDataReaderException {
        if(way == 1){
            InputDataReader reader = new InputDataReader(fileName);

            n = reader.readInt();
            f = reader.readInt();
            double[] givenBandwidth = reader.readDoubleArray();
            double[] givenLatency = reader.readDoubleArray();

            problemInstance = new ProblemInstanceSingleClient[this.n];
            for(int i = 0; i < this.n; i++){
               problemInstance[i] = new  ProblemInstanceSingleClient(givenLatency[i], givenBandwidth[i], n, f, i);
            }
        }
        else{
            Scanner in = new Scanner(new File(fileName));
            
            String s = in.nextLine();
            String[] t = s.split(" ");
            String[] k = t[2].split(";");
            n = Integer.valueOf(k[0]);
            
            s = in.nextLine();
            char[] r  = new char[2];
            r[0] = s.charAt(12);
            r[1] = s.charAt(13);
            s = "" + r[0] + r[1];
            f = Integer.parseInt(s);
            
            s = in.nextLine();
            s = in.nextLine();
            
            s = in.nextLine();
            t = s.split("\\[");
            String[] y = t[1].split("\\,");
            double[] givenBandwidth = new double[n];
            double[] givenLatency = new double[n];
            
            for (int i = 0; i < n - 1; i++) {
              givenBandwidth[i] = Double.valueOf(y[i]);
            }

            t = y[n - 1].split("\\]");
            givenBandwidth[n - 1] = Double.valueOf(t[0]);
            
            s = in.nextLine();
            t = s.split("\\[");
            y = t[1].split("\\,");
            
            for (int i = 0; i < n - 1; i++) {
                givenLatency[i] = Double.valueOf(y[i]);
            }
            t = y[n-1].split("\\]");
            givenLatency[n-1] = Double.valueOf(t[0]);
            
            problemInstance = new ProblemInstanceSingleClient[this.n];
            for(int i = 0; i < this.n; i++){
               problemInstance[i] = new  ProblemInstanceSingleClient(givenLatency[i], givenBandwidth[i], n, f, i);
            }            
         }
        Arrays.sort(problemInstance);
        for(int i = 0; i < this.n; i++){
            problemInstance[i].setNumbOfDeviceInArray(i);
         }    
    }

    public double[] ComputingRequiredNumberOfSlotes()
    {
	double[] requiredNumbSlots = new double [n];
    
	for(int i = 0; i < n; i++){ 
            requiredNumbSlots[i] = problemInstance[i].getNumberOfRequiredSlots();
	}
    
	return requiredNumbSlots;
    }
    
    public int getNumbClients() {
        return n;
    }

    public int getNumbSlots() {
        return f;
    }
    
    public ProblemInstanceSingleClient ToSingleDevice(int device){
        return problemInstance[device];
    }
    
    public ProblemInstance(ProblemInstance problemInstance, double critValue){
        this.f = problemInstance.getNumbSlots();
        this.n = problemInstance.getNumbClients() + 1;
        this.problemInstance = new ProblemInstanceSingleClient[this.n];
        
        for (int i = 0; i < this.n - 1; i++) {
            this.problemInstance[i] = new ProblemInstanceSingleClient(problemInstance.ToSingleDevice(i));
        }
        this.problemInstance[this.n - 1] = new ProblemInstanceSingleClient(Double.MAX_VALUE, critValue, n, f, n - 1);
    }
    
    private static int gcd(int a, int b) {
        return BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).intValue();
}
    
    public void ComputeNumberOfDisjunctiveCollisions(){
        ArrayList<Integer> crucialDevices = new ArrayList();
        for (int i = 0; i < n; i++) {
            if(f * 1.0 % (Math.floor(problemInstance[i].getLatencyRequirement()) + 1) == 0){
                crucialDevices.add(i);
            }
        }
        
        int numbCollisions = 0;
        boolean[] hasCollision = new boolean[crucialDevices.size()];
        for (int i = 0; i < crucialDevices.size(); i++) {
            if(!hasCollision[i]){
                for (int j = i + 1; j < crucialDevices.size(); j++) {
                    if(gcd(crucialDevices.get(i), crucialDevices.get(j)) == 1 && !hasCollision[i] && !hasCollision[j] &&
                            Math.round(Math.floor(problemInstance[crucialDevices.get(i)].getLatencyRequirement()) *  Math.floor(problemInstance[crucialDevices.get(j)].getLatencyRequirement())) == f){
                        numbCollisions++;
                        hasCollision[i] = true;
                        hasCollision[j] = true;
                    }
                }
            }
            
        }
    }
}
