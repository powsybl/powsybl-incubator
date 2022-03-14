import time
import pypowsybl as pp
import pyomo.environ as pyo
from utils import *

debug = True
if debug:
    tic = time.perf_counter()
    print("Beginning...")

# Import network
if debug:
    toc = time.perf_counter()
    print(f"\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("Import network...")
p_pu = 100 # pu is 100MW by default
n = pp.network.load(network_path)
#n = pp.per_unit.per_unit_view(n, p_pu) T be used when per unit will be released

# Create pyomo model
if debug:
    toc = time.perf_counter()
    print(f"\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("Create pyomo model...")
model = pyo.ConcreteModel()

# Define bus types
if debug:
    toc = time.perf_counter()
    print(f"\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("Define bus types...")
buses = n.get_buses().index.tolist()
PV_buses = []
PQ_buses = []
for id_gen, row in n.get_generators().iterrows():
    if row["voltage_regulator_on"]:
        if row["bus_id"] not in PV_buses: # TO DO: Work only when regulations are local
            PV_buses.append(row["bus_id"])

for bus in buses:
    if bus not in PV_buses:
        PQ_buses.append(bus)

# Define slack node
if debug:
    toc = time.perf_counter()
    print(f"\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("Define slack node...")
slack_bus = ""
voltage_level_slack = 0
neighbours_slack = 0
PV_neighbours = nb_neighboursPV(n, PV_buses)
for id_b, row in n.get_buses().iterrows():
    if id_b not in PV_buses:
        continue
    if n.get_voltage_levels().at[row["voltage_level_id"], "nominal_v"] >= voltage_level_slack:
        if PV_neighbours[id_b] > neighbours_slack:
            slack_bus = id_b
            voltage_level_slack = n.get_voltage_levels().at[row["voltage_level_id"], "nominal_v"]
            neighbours_slack = PV_neighbours[id_b]

# Create variables
if debug:
    toc = time.perf_counter()
    print(f"\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("Create variables...")
model.Buses = pyo.Set(initialize=buses)
model.PVbuses = pyo.Set(initialize=PV_buses)
model.V = pyo.Var(model.Buses, domain=pyo.NonNegativeReals) # TO DO : add default values
model.Phi = pyo.Var(model.Buses, domain=pyo.Reals) # TO DO : add default values
model.Q = pyo.Var(model.PVbuses, domain=pyo.Reals) # TO DO : add default values
        
# Objectif function
if debug:
    toc = time.perf_counter()
    print(f"\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("Objectif function...")
model.OBJ = pyo.Objective(rule=obj_expression(n, PQ_buses))

# Constraints
if debug:
    toc = time.perf_counter()
    print(f"\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("Constraints...")
## Active power balance
if debug:
    toc = time.perf_counter()
    print(f"\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("\tActive power balance...")
buses_wo_slack = []
for bus in buses:
    if bus == slack_bus:
        continue
    buses_wo_slack.append(bus)

model.Buses_wo_slack = pyo.Set(initialize=buses_wo_slack)
model.ActivePowerBalance = pyo.Constraint(model.Buses_wo_slack, rule=active_power_balance_expr(n, buses, p_pu))

## Null phase for slack
if debug:
    toc = time.perf_counter()
    print(f"\t\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("\tNull phase...")
model.NullPhaseSlack = pyo.Constraint(expr = model.Phi[slack_bus] == 0)

## Reactive power balance for PQ buses
if debug:
    toc = time.perf_counter()
    print(f"\t\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("\tReactive power balance PQ...")
model.PQbuses = pyo.Set(initialize=PQ_buses)
model.ReactivePowerBalancePQ = pyo.Constraint(model.PQbuses,rule=reactive_power_balancePQ_expr(n, buses, p_pu))

## Reactive power balance for PV buses
if debug:
    toc = time.perf_counter()
    print(f"\t\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("\tReactive power balance PV...")
model.ReactivePowerBalancePV = pyo.Constraint(model.PVbuses,rule=reactive_power_balancePV_expr(n, buses, p_pu))

# End
if debug:
    toc = time.perf_counter()
    print(f"\t\tin {toc-tic:0.4f} seconds")
    tic = time.perf_counter()
    print("End")