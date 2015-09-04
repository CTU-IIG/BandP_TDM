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

import java.util.ArrayList;

public class SetOfColumns {
    ArrayList<Column> columns = new ArrayList();

    public SetOfColumns() {}
    
    public SetOfColumns(int[][] columns1, double[] cost, int[][] b) {
        for(int i = 0; i < cost.length; i++){
           columns.add(new Column(columns1[i], cost[i], b[i])); 
        }  
    }
   
    public SetOfColumns(SetOfColumns setOfColumns) {
        for(int i = 0; i < setOfColumns.columns.size(); i++){
           columns.add(setOfColumns.columns.get(i)); 
        }  
    }
    
    public void add(Column column){
        columns.add(column);
    }
    public void deleteLast(){
        columns.remove(columns.size() - 1);
    }
    public double getCost(){
        double cost = 0;
        for (int i = 0; i < columns.size(); i++) {
            cost += columns.get(i).cost;
        }
        return cost;
    }
}
