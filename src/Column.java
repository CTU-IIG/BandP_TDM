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

public class Column {
    int[] column;
    double cost;
    int[] client;
    
    public Column(int[] column, double cost, int[] client) {
        if(cost < 0 || cost > 1){
            System.out.println("--------------------------------COST IS INCORRECT!-----------------------------------------");
        }
        else{
            this.column = column;
            this.cost = cost;
            this.client = client;
        }
    }
    
    public Column(int[] column) {
            this.column = column;
    }
}
