#--- READ DATA ---
set VARIANTS_BUSES dimen 2;

param bus_substations{VARIANTS_BUSES};
param bus_cc{VARIANTS_BUSES};
param bus_v{VARIANTS_BUSES};
param bus_theta{VARIANTS_BUSES};
param bus_p{VARIANTS_BUSES};
param bus_q{VARIANTS_BUSES};
param bus_fault{VARIANTS_BUSES} binary;
param bus_curative{VARIANTS_BUSES} binary;
param bus_id{VARIANTS_BUSES} symbolic;

set VARIANTS_GENS dimen 2;
param gen_bus{VARIANTS_GENS};
param gen_conbus{VARIANTS_GENS};
param gen_substation{VARIANTS_GENS};
param gen_minp{VARIANTS_GENS};
param gen_maxp{VARIANTS_GENS};
param gen_minqmaxp{VARIANTS_GENS};
param gen_minq0{VARIANTS_GENS};
param gen_minqminp{VARIANTS_GENS};
param gen_maxqmaxp{VARIANTS_GENS};
param gen_maxq0{VARIANTS_GENS};
param gen_maxqminp{VARIANTS_GENS};
param gen_vregul{VARIANTS_GENS} symbolic;
param gen_targetv{VARIANTS_GENS};
param gen_targetp{VARIANTS_GENS};
param gen_targetq{VARIANTS_GENS};
param gen_fault{VARIANTS_GENS} binary;
param gen_curative{VARIANTS_GENS} binary;
param gen_id{VARIANTS_GENS} symbolic;
param gen_desc{VARIANTS_GENS} symbolic;
param gen_p{VARIANTS_GENS};
param gen_q{VARIANTS_GENS};

set VARIANTS_BRANCHES dimen 2;
param br_bus1{VARIANTS_BRANCHES};
param br_bus2{VARIANTS_BRANCHES};
param br_3wtnum{VARIANTS_BRANCHES};
param br_sub1{VARIANTS_BRANCHES};
param br_sub2{VARIANTS_BRANCHES};
param br_r{VARIANTS_BRANCHES};
param br_x{VARIANTS_BRANCHES};
param br_g1{VARIANTS_BRANCHES};
param br_g2{VARIANTS_BRANCHES};
param br_b1{VARIANTS_BRANCHES};
param br_b2{VARIANTS_BRANCHES};
param br_cstr{VARIANTS_BRANCHES};
param br_ratiotc{VARIANTS_BRANCHES};
param br_phasetc{VARIANTS_BRANCHES};
param br_p1{VARIANTS_BRANCHES};
param br_p2{VARIANTS_BRANCHES};
param br_q1{VARIANTS_BRANCHES};
param br_q2{VARIANTS_BRANCHES};
param br_patl1{VARIANTS_BRANCHES};
param br_patl2{VARIANTS_BRANCHES};
param br_merged{VARIANTS_BRANCHES} symbolic;
param br_fault{VARIANTS_BRANCHES} binary;
param br_curative{VARIANTS_BRANCHES} binary;
param br_id{VARIANTS_BRANCHES} symbolic;
param br_desc{VARIANTS_BRANCHES} symbolic;

set VARIANTS_SS dimen 2;
param ss_unused1{VARIANTS_SS} symbolic;
param ss_unused2{VARIANTS_SS};
param ss_nomv{VARIANTS_SS};
param ss_minv{VARIANTS_SS};
param ss_maxv{VARIANTS_SS};
param ss_fault{VARIANTS_SS} binary;
param ss_curative{VARIANTS_SS} binary;
param ss_country{VARIANTS_SS} symbolic;
param ss_id{VARIANTS_SS} symbolic;
param ss_desc{VARIANTS_SS} symbolic;

set VARIANTS_LOADS dimen 2;
param ld_bus{VARIANTS_LOADS};
param ld_ss{VARIANTS_LOADS};
param ld_p0{VARIANTS_LOADS};
param ld_q0{VARIANTS_LOADS};
param ld_fault{VARIANTS_LOADS} binary;
param ld_curative{VARIANTS_LOADS} binary;
param ld_id{VARIANTS_LOADS} symbolic;
param ld_desc{VARIANTS_LOADS} symbolic;
param ld_p{VARIANTS_LOADS};
param ld_q{VARIANTS_LOADS};

param v_min{VARIANTS_BUSES} default 0.8;
param v_max{VARIANTS_BUSES} default 1.2;

param p_pu default 100;