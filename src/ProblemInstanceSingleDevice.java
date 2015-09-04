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

public class ProblemInstanceSingleDevice implements Comparable<ProblemInstanceSingleDevice>  {
    private double GivenLatency;
    private double GivenBandwidth;
    private int n;
    private int f;
    private int numbOfDeviceReal;
    private boolean isLatDom;
    private int numbOfDeviceInArray;
    private int requiredNumberOfSlots;

    public void setNumbOfDeviceInArray(int numbOfDeviceInArray) {
        this.numbOfDeviceInArray = numbOfDeviceInArray;
    }
    
    public void OffBandDom(){
        isLatDom = true;
    }
    
    public void OnBandDom(){
        isLatDom = false;
    }
    
    public int compareTo(ProblemInstanceSingleDevice pi) {
        return ((int) Math.floor(this.GivenLatency) - (int) Math.floor(pi.GivenLatency));
    }

    public void setFrameSize(int f) {
        this.f = f;
        requiredNumberOfSlots = (int) Math.max(Math.ceil(f * GivenBandwidth), Math.ceil(f / (Math.floor(GivenLatency) + 1) - Helpers.EPS));
    }
    
    public boolean isIsLatDom() {
        return isLatDom;
    }
    
    public ProblemInstanceSingleDevice(double GivenLatency, double GivenBandwidth, int n, int f, int N) {
        this.GivenBandwidth = GivenBandwidth;
        this.GivenLatency = GivenLatency;
        this.n = n;
        this.numbOfDeviceReal = N;
        this.f = f;
        
        if(this.GivenBandwidth <= 1.0 / (Math.floor(this.GivenLatency) + 1))
            this.isLatDom = true;
        else
            this.isLatDom = false;
    }
    
    public int getNumberOfRequiredSlots(){
        return requiredNumberOfSlots;
    }
    
    public ProblemInstanceSingleDevice(ProblemInstanceSingleDevice problemInstanceSingleDevice) {
        this.GivenBandwidth = problemInstanceSingleDevice.getBandwidthRequirement();
        this.GivenLatency = problemInstanceSingleDevice.getLatencyRequirement();
        this.f = problemInstanceSingleDevice.getNumbSlots();
        this.n = problemInstanceSingleDevice.getNumberDevices();
        this.numbOfDeviceReal = problemInstanceSingleDevice.getNumbDeviceReal();
        this.isLatDom = problemInstanceSingleDevice.isIsLatDom();
        this.numbOfDeviceInArray = problemInstanceSingleDevice.getNumbOfDeviceInArray();
        this.requiredNumberOfSlots = problemInstanceSingleDevice.getRequiredNumberOfSlots();
    }

    public double getLatencyRequirement() {
        return GivenLatency;
    }

    public double getBandwidthRequirement() {
        return GivenBandwidth;
    }

    public int getNumberDevices() {
        return n;
    }

    public int getNumbSlots() {
        return f;
    }

    public int getNumbDeviceReal() {
        return numbOfDeviceReal;
    }

    public int getNumbOfDeviceInArray() {
        return numbOfDeviceInArray;
    }

    public int getRequiredNumberOfSlots() {
        return requiredNumberOfSlots;
    }
    
    
}

