import numpy as np
import pypowsybl as pp
import pyomo.environ as pyo

network_path = "" # Insert the path to your network

# Import network
n = pp.network.load(network_path)

# Create pyomo model
model = pyo.ConcreteModel()

# Define bus types
buses = n.get_buses().index.tolist()
generators = n.get_generators().index.tolist()
PV_buses = []
PQ_buses = []
for generator in generators:
    if n.get_generators().at[generator, "voltage_regulator_on"]:
        if n.get_generators().at[generator, "bus_id"] not in PV_buses:
            PV_buses.append(n.get_generators().at[generator, "bus_id"])

for bus in buses:
    if bus not in PV_buses:
        PQ_buses.append(bus)

# Create variables
model.V = pyo.Var(buses, domain=pyo.NonNegativeReals) # TO DO : add default values
model.Phi = pyo.Var(buses) # TO DO : add default values
model.Q = pyo.Var(PV_buses) # TO DO : add default values
        
# Objectif function
def obj_expression(m):
    res = 0
    for bus in PQ_buses:
        if not np.isnan(n.get_buses().at[bus, "v_mag"]):
            bus_v_pu = n.get_buses().at[bus, "v_mag"] / n.get_voltage_levels().at[n.get_buses().at[bus, "voltage_level_id"], "nominal_v"]
            res += (m.V[bus] - bus_v_pu) * (m.V[bus] - bus_v_pu)
    return res

model.OBJ = pyo.Objective(rule=obj_expression)
