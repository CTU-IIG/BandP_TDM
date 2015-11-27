The program BandP_TDM is distributed under the terms of the GNU General Public License.

Authors: Anna Minaeva (minaeann@fel.cvut.cz), Premysl Sucha (suchap@fel.cvut.cz) and Benny Akesson (kessoben@fel.cvut.cz)

This is the implementation of the branch-and-price approach on the TDM configuration problem. 

To run it, IBM ILOG CPLEX Optimization Studio library must be installed and added to the project (“cplex.jar”). Moreover, Project Properties->Run->VM options should contain “-Djava.library.path=“path_to_cplexStudio\cplex\bin\x64_win64” or similar. 

There is a bug in version 12.6 of IBM ILOG CPLEX Optimization Studio that may cause a fatal error. Bug was reported, but did not fixed yet. It is better to use version 12.5 of IBM ILOG CPLEX Optimization Studio.

“BranchAndPrice.java” contains main function and the implementation of branch-and-price.

—————————————————————————————————————————————————————————————————————
The use-cases are in the folder “instances/”, where 

instance{1-800} are the use-cases with 8,16,32 and 64 bandwidth-dominated clients with 200 use-cases in each set.

instance{801-1600} are the use-cases with 8,16,32 and 64 latency-dominated clients with 200 use-cases in each set.

instance{1601-2400} are the use-cases with 8,16,32 and 64 mixed-dominated clients with 200 use-cases in each set.

instance{2401-2600} are the use-cases with 128 bandwidth-dominated clients.
 
instance{2601-2800} are the use-cases with 128 latency-dominated clients.

instance{2801-3000} are the use-cases with 128 mixed-dominated clients.

instance{3001-3200} are the use-cases with 256 bandwidth-dominated clients.

——————————————————————————————————————————————————————————————————————

Remark:
If you find this software useful for your research or you create an algorithm
based on this software, please cite our original paper in your publication list.

Minaeva, A - Šůcha, P. - Akesson, B. - Hanzálek, Z.: Scalable and Efficient Configuration of Time-Division Multiplexed Resource, Journal of Systems and Software, Available online 19 November 2015, ISSN 0164-1212, http://dx.doi.org/10.1016/j.jss.2015.11.019.
