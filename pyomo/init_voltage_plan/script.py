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
model.V = pyo.Var(buses, domain=pyo.NonNegativeReals) # TO DO : add default values
model.Phi = pyo.Var(buses) # TO DO : add default values
model.Q = pyo.Var(PV_buses) # TO DO : add default values
        
# Objectif function
def obj_expression(m):
    res = 0
    for id_b, row in n.get_buses().iterrows():
        if id_b not in PQ_buses:
            continue
        if not np.isnan(row["v_mag"]):
            v_pu_coeff = n.get_voltage_levels().at[row["voltage_level_id"], "nominal_v"]
            bus_v_pu = row["v_mag"] / v_pu_coeff # TO DO: to be replaced with pu getters
            res += (m.V[bus] - bus_v_pu) * (m.V[bus] - bus_v_pu)
    return res

model.OBJ = pyo.Objective(rule=obj_expression)

# Constraints
## Active power balance
buses_wo_slack = []
for bus in buses:
    if bus == slack_bus:
        continue
    buses_wo_slack.append(bus)

model.I = pyo.Set(initialize=buses_wo_slack)

def active_power_balance_expr(m,i):
    v_pu_coeff_i = n.get_voltage_levels().at[n.get_buses().at[i, "voltage_level_id"], "nominal_v"]
    lhs = 0
    rhs = 0
    is_empty = True
    for id_line, row in n.get_lines().iterrows():
        if row["bus1_id"] == i and row["bus2_id"] in buses:
            is_empy = False
            j = row["bus2_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
            ratio_tr_pu = 1
            line_g1_pu = row[ "g1"] / sus_pu_coeff # TO DO: to be replaced with pu getters
            line_r = row["r"]
            line_x = row["x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += ratio_tr_pu * m.V[i] * ( line_g1_pu * ratio_tr_pu * m.V[i] + line_y_pu * ratio_tr_pu * m.V[i] * pyo.sin(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.sin(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
        elif row["bus2_id"] == i and row["bus1_id"] in buses:
            is_empy = False
            j = row["bus1_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
            ratio_tr_pu = 1
            line_g2_pu = row["g2"] / sus_pu_coeff # TO DO: to be replaced with pu getters
            line_r = row["r"]
            line_x = row["x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += 1 * m.V[i] * ( line_g2_pu * 1 * m.V[i] - line_y_pu * ratio_tr_pu * m.V[j] * pyo.sin(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.sin(line_ksi) )
    for id_tr, row in n.get_2_windings_transformers().iterrows():
        if row["bus1_id"] == i and row["bus2_id"] in buses:
            is_empy = False
            j = row["bus2_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U
            ratio_tr_pu_coeff = v_pu_coeff_j / v_pu_coeff_i
            ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff
            line_r = row["r"]
            line_x = row["x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += ratio_tr_pu * m.V[i] * (line_y_pu * ratio_tr_pu * m.V[i] * pyo.sin(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.sin(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
        elif row["bus2_id"] == i and row["bus1_id"] in buses:
            is_empy = False
            j = row["bus1_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
            ratio_tr_pu_coeff = v_pu_coeff_i / v_pu_coeff_j
            ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff
            line_r = row["r"]
            line_x = row["x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += 1 * m.V[i] * (- line_y_pu * ratio_tr_pu * m.V[j] * pyo.sin(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.sin(line_ksi) )
    for id_gen, row in n.get_generators().iterrows():
        if row["bus_id"] == i:
            rhs += row["target_p"] / p_pu
    for id_ld, row in n.get_loads().iterrows():
        if row["bus_id"] == i:
            rhs -= row["p0"] / p_pu
    if is_empty:
        return pyo.Constraint.Skip
    else:
        return lhs == rhs

model.ActivePowerBalance = pyo.Constraint(model.I,rule=active_power_balance_expr)

## Null phase for slack
model.NullPhaseSlack = pyo.Constraint(expr = model.Phi[slack_bus] == 0)

## Reactive power balance
model.J = pyo.Set(initialize=PQ_buses)

def reactive_power_balance_expr(m,i):
    v_pu_coeff_i = n.get_voltage_levels().at[n.get_buses().at[i, "voltage_level_id"], "nominal_v"]
    lhs = 0
    rhs = 0
    is_empty = True
    for id_line, row in n.get_lines().iterrows():
        if row["bus1_id"] == i and row["bus2_id"] in buses:
            is_empy = False
            j = row["bus2_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
            ratio_tr_pu = 1
            line_b1_pu = row[ "b1"] / sus_pu_coeff # TO DO: to be replaced with pu getters
            line_r = row["r"]
            line_x = row["x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += ratio_tr_pu * m.V[i] * (-line_b1_pu * ratio_tr_pu * m.V[i] + line_y_pu * ratio_tr_pu * m.V[i] * pyo.cos(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.cos(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
        elif row["bus2_id"] == i and row["bus1_id"] in buses:
            is_empy = False
            j = row["bus1_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
            ratio_tr_pu = 1
            line_b2_pu = row["b2"] / sus_pu_coeff # TO DO: to be replaced with pu getters
            line_r = row["r"]
            line_x = row["x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += 1 * m.V[i] * ( - line_b2_pu * 1 * m.V[i] - line_y_pu * ratio_tr_pu * m.V[j] * pyo.cos(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.cos(line_ksi) )
    for id_tr, row in n.get_2_windings_transformers().iterrows():
        if row["bus1_id"] == i and row["bus2_id"] in buses:
            is_empy = False
            j = row["bus2_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U
            ratio_tr_pu_coeff = v_pu_coeff_j / v_pu_coeff_i
            ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff
            line_r = row["r"]
            line_x = row["x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += ratio_tr_pu * m.V[i] * (line_y_pu * ratio_tr_pu * m.V[i] * pyo.cos(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.cos(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
        elif row["bus2_id"] == i and row["bus1_id"] in buses:
            is_empy = False
            j = row["bus1_id"]
            v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
            sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
            ratio_tr_pu_coeff = v_pu_coeff_i / v_pu_coeff_j
            ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff
            line_r = row["r"]
            line_x = row["x"]
            line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff
            line_ksi = np.arctan2(line_r,line_x)
            lhs += 1 * m.V[i] * ( - line_y_pu * ratio_tr_pu * m.V[j] * pyo.cos(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.cos(line_ksi) )
    for id_gen, row in n.get_generators().iterrows():
        if row["bus_id"] == i:
            rhs += row["target_q"] / p_pu
    for id_ld, row in n.get_loads().iterrows():
        if row["bus_id"] == i:
            rhs -= row["q0"] / p_pu
    if is_empty:
        return pyo.Constraint.Skip
    else:
        return lhs == rhs

model.ReactivePowerBalance = pyo.Constraint(model.J,rule=reactive_power_balance_expr)