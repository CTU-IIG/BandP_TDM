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

import ilog.concert.IloNumVar;

public class IloNumVarArray {
      private int num = 0;
      private IloNumVar[][] array = new IloNumVar[num + 2][1];

    public void setNumPatterns(int num) {
        this.num = num;
    }  
      
    public int getNumbPatterns() {
        return num;
    }

    public IloNumVar[][] getPatterns() {
        return array;
    }
 
      void add(IloNumVar[] ivar) {
         if ( num >= array.length ) {
            IloNumVar[][] array1 = new IloNumVar[2 * array.length][1];
            System.arraycopy(array, 0, array1, 0, num);
            array = array1;
         }
         array[num++] = ivar;
      }

      void removeLast(){
          IloNumVar[][] newArray = new IloNumVar[num - 1][1];
          
          for(int i = 0; i < num - 1; i++){
              newArray[i] = array[i];
          }
         
          array = newArray;
          num--;
      }
      
      IloNumVar[] getElement(int i) { return array[i]; }
   }