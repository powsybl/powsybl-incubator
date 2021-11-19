# Variables
var V{BUSES};
var Phi{BUSES};
var Q{PV_BUSES};

# Objectif function
minimize Quadratic_Error:
   sum {i in PV_BUSES} ( (V[i]-targetV[i])*(V[i]-targetV[i]) );
   
# Constraints
subject to Active_power_balance{i in BUSES: i != slack_bus}:
    sum {j in BRANCHES: br_bus1[1,j] == i and br_bus2[1,j] != -1} ( br_cstr[1,j] * V[i] * ( br_g1[1,j] * br_cstr[1,j] * V[i] + br_y[j] * br_cstr[1,j] * V[i] * sin(br_ksi[j]) - br_y[j] * 1 * V[br_bus2[1,j]] * sin(br_ksi[j] - 0 + 0 - Phi[i] + Phi[br_bus2[1,j]]) ) )
    + sum {j in BRANCHES: br_bus2[1,j] == i and br_bus1[1,j] != -1} ( 1 * V[i] * ( br_g2[1,j] * 1 * V[i] - br_y[j] * br_cstr[1,j] * V[br_bus1[1,j]] * sin(br_ksi[j] + 0 - 0 + Phi[br_bus1[1,j]] - Phi[i]) + br_y[j] * 1 * V[i] * sin(br_ksi[j]) ) )
    == sum {j in GENS: gen_bus[1,j] == i} gen_targetp[1,j] - sum {j in LOADS: ld_bus[1,j] == i} ld_p0[1,j];
    
subject to Null_phase_slack{i in BUSES: i == slack_bus}:
    Phi[i] == 0;

subject to Rective_power_balance_PQ{i in PQ_BUSES}:
    sum {j in BRANCHES: br_bus1[1,j] == i and br_bus2[1,j] != -1} ( br_cstr[1,j] * V[i] * (- br_b1[1,j] * br_cstr[1,j] * V[i] + br_y[j] * br_cstr[1,j] * V[i] * cos(br_ksi[j]) - br_y[j] * 1 * V[br_bus2[1,j]] * cos(br_ksi[j] - 0 + 0 - Phi[i] + Phi[br_bus2[1,j]]) ) )
    + sum {j in BRANCHES: br_bus2[1,j] == i and br_bus1[1,j] != -1} ( 1 * V[i] * ( - br_b2[1,j] * 1 * V[i] - br_y[j] * br_cstr[1,j] * V[br_bus1[1,j]] * cos(br_ksi[j] + 0 - 0 + Phi[br_bus1[1,j]] - Phi[i]) + br_y[j] * 1 * V[i] * cos(br_ksi[j]) ) )
    == sum {j in GENS: gen_bus[1,j] == i} gen_targetq[1,j] - sum {j in LOADS: ld_bus[1,j] == i} ld_q0[1,j];
    
subject to Rective_power_balance_PV{i in PV_BUSES}:
    sum {j in BRANCHES: br_bus1[1,j] == i and br_bus2[1,j] != -1} ( br_cstr[1,j] * V[i] * (- br_b1[1,j] * br_cstr[1,j] * V[i] + br_y[j] * br_cstr[1,j] * V[i] * cos(br_ksi[j]) - br_y[j] * 1 * V[br_bus2[1,j]] * cos(br_ksi[j] - 0 + 0 - Phi[i] + Phi[br_bus2[1,j]]) ) )
    + sum {j in BRANCHES: br_bus2[1,j] == i and br_bus1[1,j] != -1} ( 1 * V[i] * ( - br_b2[1,j] * 1 * V[i] - br_y[j] * br_cstr[1,j] * V[br_bus1[1,j]] * cos(br_ksi[j] + 0 - 0 + Phi[br_bus1[1,j]] - Phi[i]) + br_y[j] * 1 * V[i] * cos(br_ksi[j]) ) )
    == Q[i];
    
subject to Reactive_power_inf{i in PV_BUSES}:
    sum {j in GENS: gen_bus[1,j] == i} gen_minq0[1,j] <= Q[i];
    
subject to Reactive_power_sup{i in PV_BUSES}:
    Q[i] <= sum {j in GENS: gen_bus[1,j] == i} gen_maxq0[1,j];
    
subject to Voltage_limit_inf{i in BUSES}:
    v_min[1,i] <= V[i];

subject to Voltage_limit_sup{i in BUSES}:
    V[i] <= v_max[1,i];