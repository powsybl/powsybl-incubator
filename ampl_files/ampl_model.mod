# Variables
var V{BUSES};
var Phi{BUSES};
var Q{PV_BUSES};

# Objectif function
minimize Quadratic_Error:
   sum {i in PV_BUSES} ( sum {j in GENS: gen_bus[1,j] == i} (V[i]-gen_targetv[1,j])*(V[i]-gen_targetv[1,j]) );
   
# Constraints
subject to Active_power_balance{i in BUSES: i != slack_bus}:
    sum {j in BRANCHES: br_bus1[1,j] == i} ( br_cstr[1,j] * V[i] * ( br_g1[1,j] * br_cstr[1,j] * V[i] + br_y[j] * br_cstr[1,j] * V[i] * sin(br_ksi[j]) - br_y[j] * 1 * V[br_bus2[1,j]] * sin(br_ksi[j] - 0 + 0 - Phi[i] + Phi[br_bus2[1,j]]) ) )
    + sum {j in BRANCHES: br_bus2[1,j] == i} ( 1 * V[i] * ( br_g2[1,j] * 1 * V[i] - br_y[j] * br_cstr[1,j] * V[br_bus1[1,j]] * sin(br_ksi[j] + 0 - 0 + Phi[br_bus1[1,j]] - Phi[i]) + br_y[j] * 1 * V[i] * sin(br_ksi[j]) ) )
    == sum {j in GENS: gen_bus[1,j] == i} gen_targetp[1,j] - sum {j in LOADS: ld_bus[1,j] == i} ld_p0[1,j];