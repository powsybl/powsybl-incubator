#--- COMPUTED DATA ---
# We are assuming that the "variant" input data always equals 1
# VARIANTS check
set VARIANTS = union{(i,j) in VARIANTS_BUSES} {i};
if card(VARIANTS) > 1 then {
    printf "Two different variants in data files !\n";
    quit;
} else
    printf "Correct using of variant input\n";

# PV/PQ BUSES definitions and checks
param set_target_V default 0;
param generator_controling default 0;
set BUSES = union{(i,j) in VARIANTS_BUSES} {j};
set GENS = union{(i,j) in VARIANTS_GENS} {j};
set PV_BUSES = {i in BUSES: card({j in GENS: gen_bus[1, j] == i and gen_vregul[1, j] == "true"}) >= 1};
set PQ_BUSES = {i in BUSES: card({j in GENS: gen_bus[1, j] == i and gen_vregul[1, j] == "true"}) == 0};
param targetV{PV_BUSES};
for{i in PV_BUSES} {
    let set_target_V := 0;
    for{j in GENS: gen_bus[1,j]==i} {
        if gen_conbus[1,j] != i then {
        printf "Control of generator %d is not local.\nHowever it will be turned to local in this simulation.\n", j;
        }
        if set_target_V == 1 then {
            if targetV[i] != gen_targetv[1,j] then printf"Inconsistent target V between generators %d and %d at bus %d.\nGenerator %d is chosen as reference.\n", j, generator_controling, i, generator_controling;
        } else {
            let targetV[i] := gen_targetv[1,j];
            let set_target_V := 1;
            let generator_controling := j;
        }
    }
}

#BRANCHES and LOADS definitions
set BRANCHES = union{(i,j) in VARIANTS_BRANCHES} {j};
set LOADS = union{(i,j) in VARIANTS_LOADS} {j};


# Compute slack node
param slack_bus default 0;
param voltage_level_slack default 0;
param neighbours_slack default 0;
for{i in PV_BUSES} {
    if ss_nomv[1,bus_substations[1,i]] >= voltage_level_slack then {
        if card({j in BUSES: exists {k in BRANCHES} ((br_bus1[1,k]==i and br_bus2[1,k]==j) or (br_bus1[1,k]==j and br_bus2[1,k]==i))}) > neighbours_slack then {
            let slack_bus := i;
            let voltage_level_slack := ss_nomv[1,bus_substations[1,i]];
            let neighbours_slack := card({j in BUSES: exists {k in BRANCHES} ((br_bus1[1,k]==i and br_bus2[1,k]==j) or (br_bus1[1,k]==j and br_bus2[1,k]==i))});
        }
    }    
}

# Branches data computations
param br_y{i in BRANCHES} := 1/sqrt(br_r[1,i]*br_r[1,i] + br_x[1,i]*br_x[1,i]);
param br_ksi{i in BRANCHES} := atan2(br_r[1,i], br_x[1,i]);