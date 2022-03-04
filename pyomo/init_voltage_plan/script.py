import numpy as np
import pypowsybl as pp
import pyomo.environ as pyo
from utils import *

network_path = # Insert the path to your network

# Import network
p_pu = 100 # pu is 100MW by default
n = pp.network.load(network_path)
#n = pp.per_unit.per_unit_view(n, p_pu) T be used when per unit will be released

# Create pyomo model
model = pyo.ConcreteModel()

# Define bus types
buses = n.get_buses().index.tolist()
generators = n.get_generators().index.tolist()
PV_buses = []
PQ_buses = []
for generator in generators:
    if n.get_generators().at[generator, "voltage_regulator_on"]:
        if n.get_generators().at[generator, "bus_id"] not in PV_buses: # TO DO: Work only when regulations are local
            PV_buses.append(n.get_generators().at[generator, "bus_id"])

for bus in buses:
    if bus not in PV_buses:
        PQ_buses.append(bus)

# Define slack node
slack_bus = ""
voltage_level_slack = 0
neighbours_slack = 0
PV_neighbours = nb_neighboursPV(n, PV_buses)
for i in PV_buses:
    if n.get_voltage_levels().at[n.get_buses().at[i, "voltage_level_id"], "nominal_v"] >= voltage_level_slack:
        if PV_neighbours[i] > neighbours_slack:
            slack_bus = i
            voltage_level_slack = n.get_voltage_levels().at[n.get_buses().at[i, "voltage_level_id"], "nominal_v"]
            neighbours_slack = PV_neighbours[i]

# Create variables
model.V = pyo.Var(buses, domain=pyo.NonNegativeReals) # TO DO : add default values
model.Phi = pyo.Var(buses) # TO DO : add default values
model.Q = pyo.Var(PV_buses) # TO DO : add default values
        
# Objectif function
def obj_expression(m):
    res = 0
    for bus in PQ_buses:
        if not np.isnan(n.get_buses().at[bus, "v_mag"]):
            v_pu_coeff = n.get_voltage_levels().at[n.get_buses().at[bus, "voltage_level_id"], "nominal_v"]
            bus_v_pu = n.get_buses().at[bus, "v_mag"] / v_pu_coeff # TO DO: to be replaced with pu getters
            res += (m.V[bus] - bus_v_pu) * (m.V[bus] - bus_v_pu)
    return res

model.OBJ = pyo.Objective(rule=obj_expression)

# Constraints
lines = n.get_lines().index.tolist()
two_w_transf = n.get_2_windings_transformers().index.tolist()
gens = n.get_generators().index.tolist()
loads = n.get_loads().index.tolist()

## Active power balance
buses_wo_slack = []
for bus in buses:
    if bus == slack_bus:
        continue
    buses_wo_slack.append(bus)

model.I = pyo.Set(initialize=[slack_bus])

def active_power_balance_expr(m,i):
    v_pu_coeff_i = n.get_voltage_levels().at[n.get_buses().at[i, "voltage_level_id"], "nominal_v"]
    lhs = 0
    rhs = 0
    for line in lines:
        if n.get_lines().at[line, "bus1_id"] == i and n.get_lines().at[line, "bus2_id"] in buses:
            j = n.get_lines().at[line, "bus2_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
            ratio_tr_pu = 1
            line_g1_pu = n.get_lines().at[line, "g1"] / sus_pu_coeff # TO DO: to be replaced with pu getters
            line_r = n.get_lines().at[line, "r"]
            line_x = n.get_lines().at[line, "x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += ratio_tr_pu * m.V[i] * ( line_g1_pu * ratio_tr_pu * m.V[i] + line_y_pu * ratio_tr_pu * m.V[i] * pyo.sin(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.sin(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
        elif n.get_lines().at[line, "bus2_id"] == i and n.get_lines().at[line, "bus1_id"] in buses:
            j = n.get_lines().at[line, "bus1_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
            ratio_tr_pu = 1
            line_g2_pu = n.get_lines().at[line, "g2"] / sus_pu_coeff # TO DO: to be replaced with pu getters
            line_r = n.get_lines().at[line, "r"]
            line_x = n.get_lines().at[line, "x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += 1 * m.V[i] * ( line_g2_pu * 1 * m.V[i] - line_y_pu * ratio_tr_pu * m.V[j] * pyo.sin(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.sin(line_ksi) )
    for tr in two_w_transf:
        if n.get_2_windings_transformers().at[tr, "bus1_id"] == i and n.get_2_windings_transformers().at[tr, "bus2_id"] in buses:
            j = n.get_2_windings_transformers().at[tr, "bus2_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U
            ratio_tr_pu_coeff = v_pu_coeff_j / v_pu_coeff_i
            ratio_tr_pu = n.get_2_windings_transformers().at[tr, "rated_u2"] / n.get_2_windings_transformers().at[tr, "rated_u1"] / ratio_tr_pu_coeff
            line_r = n.get_2_windings_transformers().at[tr, "r"]
            line_x = n.get_2_windings_transformers().at[tr, "x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += ratio_tr_pu * m.V[i] * (0 + line_y_pu * ratio_tr_pu * m.V[i] * pyo.sin(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.sin(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
        elif n.get_2_windings_transformers().at[tr, "bus2_id"] == i and n.get_2_windings_transformers().at[tr, "bus1_id"] in buses:
            j = n.get_2_windings_transformers().at[tr, "bus1_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
            ratio_tr_pu_coeff = v_pu_coeff_i / v_pu_coeff_j
            ratio_tr_pu = n.get_2_windings_transformers().at[tr, "rated_u2"] / n.get_2_windings_transformers().at[tr, "rated_u1"] / ratio_tr_pu_coeff
            line_r = n.get_2_windings_transformers().at[tr, "r"]
            line_x = n.get_2_windings_transformers().at[tr, "x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += 1 * m.V[i] * (0 - line_y_pu * ratio_tr_pu * m.V[j] * pyo.sin(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.sin(line_ksi) )
    for gen in gens:
        if n.get_generators().at[gen, "bus_id"] == i:
            rhs += n.get_generators().at[gen, "target_p"] / p_pu
    for ld in loads:
        if n.get_loads().at[ld, "bus_id"] == i:
            rhs -= n.get_loads().at[ld, "p0"] / p_pu
    return lhs == rhs

model.ActivePowerBalance = pyo.Constraint(model.I,rule=active_power_balance_expr)



